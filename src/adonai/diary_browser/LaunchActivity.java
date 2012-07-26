package adonai.diary_browser;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

public class LaunchActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences mSharedPrefs = getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
        if (!mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "").equals("") && 
            !mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "").equals("")) {
        	startActivity(new Intent(this, DiaryList.class));
        } else {
        	startActivity(new Intent(this, AuthorizationForm.class));
        }
        
        finish();
    }

    
}
