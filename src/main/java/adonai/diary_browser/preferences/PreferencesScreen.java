package adonai.diary_browser.preferences;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

import adonai.diary_browser.R;
import adonai.diary_browser.Utils;


public class PreferencesScreen extends PreferenceActivity {

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setupTheme(this);
        super.onCreate(savedInstanceState);

        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(Utils.mPrefsFile);
        prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

        addPreferencesFromResource(R.xml.preferences);
        initializeSummary(getPreferenceScreen());
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {

                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    private static void initializeSummary(Preference preference) {
        if (preference instanceof PreferenceGroup) {
            PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
            for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {

                initializeSummary(preferenceGroup.getPreference(i));
            }
        } else {
            updatePreferenceSummary(preference);
        }
    }

    /**
     * set ListPreference's and EditTextPreference's summary to its value
     *  and bind Preference.OnPreferenceChangeListener to it
     * @param preference
     */
    private static void updatePreferenceSummary(Preference preference) {
        if (preference instanceof ListPreference) {
            bindPreferenceSummaryToValue(preference);
            ListPreference listPreference = (ListPreference) preference;
            preference.setSummary(listPreference.getEntry());
        } else if (preference instanceof EditTextPreference) {
            bindPreferenceSummaryToValue(preference);
            EditTextPreference editTextPreference = (EditTextPreference) preference;
            preference.setSummary(editTextPreference.getText());
        }
    }

}
