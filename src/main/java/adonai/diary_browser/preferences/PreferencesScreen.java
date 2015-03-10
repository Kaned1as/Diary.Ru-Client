package adonai.diary_browser.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;
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
    }
}
