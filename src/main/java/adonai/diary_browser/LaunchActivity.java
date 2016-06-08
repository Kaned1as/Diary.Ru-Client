package adonai.diary_browser;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.List;

import adonai.diary_browser.database.DbProvider;
import adonai.diary_browser.database.PersistManager;
import adonai.diary_browser.entities.CredentialsItem;

public class LaunchActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.setupTheme(this);
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(getApplicationContext(), Utils.mPrefsFile, MODE_PRIVATE, R.xml.preferences, true);

        RuntimeExceptionDao<CredentialsItem, String> credDao = DbProvider.getHelper().getCredentialsDao();
        List<CredentialsItem> autologinViable = credDao.queryForEq("autologin", true);
        startActivity(new Intent(this, autologinViable.isEmpty() ? AuthorizationForm.class : DiaryListActivity.class));

        finish();
    }


}
