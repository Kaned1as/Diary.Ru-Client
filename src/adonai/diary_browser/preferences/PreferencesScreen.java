package adonai.diary_browser.preferences;

import adonai.diary_browser.AuthorizationForm;
import adonai.diary_browser.R;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class PreferencesScreen extends PreferenceActivity 
{
	EditTextPreference signaturePref;
	
	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    
	    PreferenceManager prefMgr = getPreferenceManager();
	    prefMgr.setSharedPreferencesName(AuthorizationForm.mPrefsFile);
	    prefMgr.setSharedPreferencesMode(MODE_PRIVATE);
	    
	    addPreferencesFromResource(R.xml.preferences);
	}
}
