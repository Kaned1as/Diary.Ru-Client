package adonai.diary_browser.preferences;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import adonai.diary_browser.Utils;
import adonai.diary_browser.pro.R;


public class PreferencesScreen extends PreferenceActivity 
{
	EditTextPreference signaturePref;
	
	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    
	    PreferenceManager prefMgr = getPreferenceManager();
	    prefMgr.setSharedPreferencesName(Utils.mPrefsFile);
	    prefMgr.setSharedPreferencesMode(MODE_PRIVATE);
	    
	    addPreferencesFromResource(R.xml.preferences);
	}
}
