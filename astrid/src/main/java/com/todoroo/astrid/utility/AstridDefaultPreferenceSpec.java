package com.todoroo.astrid.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;

import org.tasks.R;

public class AstridDefaultPreferenceSpec extends AstridPreferenceSpec {

    public static interface PreferenceExtras {
        public void setExtras(Context context, SharedPreferences prefs, Editor editor, Resources r, boolean ifUnset);
    }

    @Override
    public void setIfUnset() {
        PreferenceExtras extras = new PreferenceExtras() {
            @Override
            public void setExtras(Context context, SharedPreferences prefs, Editor editor, Resources r, boolean ifUnset) {
                String dragDropTestInitialized = "android_drag_drop_initialized"; //$NON-NLS-1$
                if (!Preferences.getBoolean(dragDropTestInitialized, false)) {
                    SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(context);
                    if (publicPrefs != null) {
                        Editor edit = publicPrefs.edit();
                        if (edit != null) {
                            edit.putInt(SortHelper.PREF_SORT_FLAGS, SortHelper.FLAG_DRAG_DROP);
                            edit.putInt(SortHelper.PREF_SORT_SORT, SortHelper.SORT_AUTO);
                            edit.commit();
                            Preferences.setInt(AstridPreferences.P_SUBTASKS_HELP, 1);
                        }
                    }
                    Preferences.setBoolean(dragDropTestInitialized, true);
                }
                BeastModePreferences.setDefaultOrder(context, false);

                if (Constants.MARKET_STRATEGY.defaultPhoneLayout()) {
                    setPreference(prefs, editor, r, R.string.p_force_phone_layout, true, ifUnset);
                }
            }
        };

        setPrefs(extras, true);
    }

    private static void setPrefs(PreferenceExtras extras, boolean ifUnset) {
        Context context = ContextManager.getContext();
        SharedPreferences prefs = Preferences.getPrefs(context);
        Editor editor = prefs.edit();
        Resources r = context.getResources();

        setPreference(prefs, editor, r, R.string.p_default_urgency_key, 0, ifUnset);
        setPreference(prefs, editor, r, R.string.p_default_importance_key, 2, ifUnset);
        setPreference(prefs, editor, r, R.string.p_default_hideUntil_key, 0, ifUnset);
        setPreference(prefs, editor, r, R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE, ifUnset);
        setPreference(prefs, editor, r, R.string.p_rmd_default_random_hours, 0, ifUnset);
        setPreference(prefs, editor, r, R.string.p_fontSize, 16, ifUnset);
        setPreference(prefs, editor, r, R.string.p_showNotes, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_field_missed_calls, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_end_at_deadline, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_rmd_persistent, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_today_filter, true, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_recently_modified_filter, true, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_not_in_list_filter, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_menu_search, true, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_menu_sync, true, ifUnset);
        setPreference(prefs, editor, r, R.string.p_show_menu_sort, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_calendar_reminders, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_use_filters, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_use_dark_theme, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_force_phone_layout, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_quickadd_controls, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_show_task_edit_comments, true, ifUnset);

        setPreference(prefs, editor, r, R.string.p_taskRowStyle_v2, "1", ifUnset); //$NON-NLS-1$

        setPreference(prefs, editor, r, R.string.p_use_date_shortcuts, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_save_and_cancel, false, ifUnset);

        setPreference(prefs, editor, r, R.string.p_hide_plus_button, true, ifUnset);

        extras.setExtras(context, prefs, editor, r, ifUnset);

        editor.commit();
    }
}
