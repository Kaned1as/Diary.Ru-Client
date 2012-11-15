package adonai.diary_browser;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

public class LaunchActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        Globals.mSharedPrefs = getApplicationContext().getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
        
        if (!Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "").equals("") && 
            !Globals.mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "").equals("")) 
        {
        	startActivity(new Intent(this, DiaryList.class));
        } 
        else 
        {
        	startActivity(new Intent(this, AuthorizationForm.class));
        }
        
        finish();
    }

    
}
