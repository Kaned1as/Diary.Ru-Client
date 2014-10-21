package adonai.diary_browser;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class LaunchActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences mSharedPrefs = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);

        if (!mSharedPrefs.getString(Utils.KEY_USERNAME, "").equals("") &&
                !mSharedPrefs.getString(Utils.KEY_PASSWORD, "").equals("") &&
                mSharedPrefs.getBoolean(Utils.KEY_KEEP_AUTH, true))
            startActivity(new Intent(this, DiaryListActivity.class));
        else
            startActivity(new Intent(this, AuthorizationForm.class));

        finish();
    }


}
