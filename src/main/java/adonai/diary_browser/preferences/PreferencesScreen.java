package adonai.diary_browser.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import adonai.diary_browser.R;
import adonai.diary_browser.Utils;
import adonai.diary_browser.database.DatabaseHandler;


public class PreferencesScreen extends PreferenceActivity 
{
    DatabaseHandler mDatabase;
	
	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) 
	{
        mDatabase = new DatabaseHandler(this);
	    super.onCreate(savedInstanceState);
	    
	    PreferenceManager prefMgr = getPreferenceManager();
	    prefMgr.setSharedPreferencesName(Utils.mPrefsFile);
	    prefMgr.setSharedPreferencesMode(MODE_PRIVATE);
	    
	    addPreferencesFromResource(R.xml.preferences);
	}

    public DatabaseHandler getDatabase() {
        return mDatabase;
    }
}
