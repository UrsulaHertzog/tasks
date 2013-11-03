/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.text.TextUtils;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.tags.TagMemberMetadata;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagMetadataDao extends DatabaseDao<TagMetadata> {

    @Autowired
    private Database database;

	public TagMetadataDao() {
        super(TagMetadata.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    public static class TagMetadataCriteria {
        /** Returns all metadata associated with a given task */
        public static Criterion byTag(String tagUuid) {
            return TagMetadata.TAG_UUID.eq(tagUuid);
        }

        /** Returns all metadata associated with a given key */
        public static Criterion withKey(String key) {
            return TagMetadata.KEY.eq(key);
        }

        /** Returns all metadata associated with a given key */
        public static Criterion byTagAndWithKey(String tagUuid, String key) {
            return Criterion.and(withKey(key), byTag(tagUuid));
        }
    }

    public void createMemberLink(long tagId, String tagUuid, String memberId, boolean suppressOutstanding) {
        createMemberLink(tagId, tagUuid, memberId, false, suppressOutstanding);
    }

    public void createMemberLink(long tagId, String tagUuid, String memberId, boolean removedMember, boolean suppressOutstanding) {
        TagMetadata newMetadata = TagMemberMetadata.newMemberMetadata(tagId, tagUuid, memberId);
        if (removedMember) {
            newMetadata.setValue(TagMetadata.DELETION_DATE, DateUtilities.now());
        }
        if (suppressOutstanding) {
            newMetadata.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        }
        if (update(Criterion.and(TagMetadataCriteria.byTagAndWithKey(tagUuid, TagMemberMetadata.KEY),
                TagMemberMetadata.USER_UUID.eq(memberId)), newMetadata) <= 0) {
            if (suppressOutstanding) {
                newMetadata.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            }
            createNew(newMetadata);
        }
    }

    public void removeMemberLink(long tagId, String tagUuid, String memberId, boolean suppressOutstanding) {
        TagMetadata deleteTemplate = new TagMetadata();
        deleteTemplate.setValue(TagMetadata.TAG_ID, tagId); // Need this for recording changes in outstanding table
        deleteTemplate.setValue(TagMetadata.DELETION_DATE, DateUtilities.now());
        deleteTemplate.setValue(TagMemberMetadata.USER_UUID, memberId); // Need this for recording changes in outstanding table

        if (suppressOutstanding) {
            deleteTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        }
        update(Criterion.and(TagMetadataCriteria.withKey(TagMemberMetadata.KEY), TagMetadata.DELETION_DATE.eq(0),
                TagMetadata.TAG_UUID.eq(tagUuid), TagMemberMetadata.USER_UUID.eq(memberId)), deleteTemplate);
    }

    public void synchronizeMembers(TagData tagData, String legacyMembersString, String tagUuid, JSONArray members) {
        long tagId = tagData.getId();
        Set<String> emails = new HashSet<String>();
        Set<String> ids = new HashSet<String>();

        HashMap<String, String> idToEmail = new HashMap<String, String>();

        for (int i = 0; i < members.length(); i++) {
            JSONObject person = members.optJSONObject(i);
            if (person != null) {
                String id = person.optString("id"); //$NON-NLS-1$
                if (!TextUtils.isEmpty(id)) {
                    ids.add(id);
                }

                String email = person.optString("email"); //$NON-NLS-1$
                if (!TextUtils.isEmpty(email)) {
                    emails.add(email);
                }

                if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(email)) {
                    idToEmail.put(id, email);
                }
            }
        }

        if (!TextUtils.isEmpty(legacyMembersString)) {
            try {
                JSONArray legacyMembers = new JSONArray(legacyMembersString);
                for (int i = 0; i < legacyMembers.length(); i++) {
                    JSONObject user = legacyMembers.optJSONObject(i);
                    if (user != null) {
                        String id = user.optString("id"); //$NON-NLS-1$
                        String email = user.optString("email"); //$NON-NLS-1$

                        if (!TextUtils.isEmpty(id)) {
                            createMemberLink(tagId, tagUuid, id, !ids.contains(id), false);
                        } else if (!TextUtils.isEmpty(email)) {
                            createMemberLink(tagId, tagUuid, email, !emails.contains(email), false);
                        }
                    }

                }
            } catch (JSONException e) {
                //
            }
            tagData.setValue(TagData.MEMBERS, ""); //$NON-NLS-1$
            PluginServices.getTagDataDao().saveExisting(tagData);
        }

        TodorooCursor<TagMetadata> currentMembers = query(Query.select(TagMemberMetadata.USER_UUID).where(TagMetadataCriteria.byTagAndWithKey(tagUuid, TagMemberMetadata.KEY)));
        try {
            TagMetadata m = new TagMetadata();
            for (currentMembers.moveToNext(); !currentMembers.isAfterLast(); currentMembers.moveToNext()) {
                m.clear();
                m.readFromCursor(currentMembers);

                String userId = m.getValue(TagMemberMetadata.USER_UUID);
                boolean exists = ids.remove(userId) || emails.remove(userId);
                if (exists && idToEmail.containsKey(userId)) {
                    String email = idToEmail.get(userId);
                    emails.remove(email);
                }

                if (!exists) { // Was in database, but not in new members list
                    removeMemberLink(tagId, tagUuid, userId, false);
                }
            }
        } finally {
            currentMembers.close();
        }

        for (String email : emails) {
            createMemberLink(tagId, tagUuid, email, false);
        }

        for (String id : ids) {
            createMemberLink(tagId, tagUuid, id, false);
        }
    }
}

