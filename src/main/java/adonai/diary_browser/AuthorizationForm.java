package adonai.diary_browser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import adonai.diary_browser.database.DbProvider;
import adonai.diary_browser.database.PersistManager;
import adonai.diary_browser.entities.CredentialsItem;
import adonai.diary_browser.preferences.PreferencePage;

public class AuthorizationForm extends AppCompatActivity implements OnClickListener {

    // общее
    private static final String AUTH_PAIR_DELIMITER = "=logPassSplitter=";
    private static final String AUTHS_DELIMITER = "=pairSplitter=";

    private static int LOGIN_FORM       = 0;
    private static int REGISTER_FORM    = 1;
    
    private ViewSwitcher mSwitcher;
    private SharedPreferences mPreferences;
    private PersistManager mEntityManager;
    
    // авторизация
    private Button mLogin, mRequestRegistration;
    private AutoCompleteTextView mUsername;
    private EditText mPassword;
    private CheckBox mKeepAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.setupTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorization_form_a);
        getSupportActionBar().setHomeButtonEnabled(true);
        
        mPreferences = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);
        mEntityManager = DbProvider.getTempHelper(this);

        mSwitcher = (ViewSwitcher) findViewById(R.id.login_register_switcher);
                
        mLogin = (Button) findViewById(R.id.login_button);
        mLogin.setOnClickListener(this);
        mRequestRegistration = (Button) findViewById(R.id.request_reg_button);
        mRequestRegistration.setOnClickListener(this);
        
        mUsername = (AutoCompleteTextView) findViewById(R.id.login_text);
        
        mPassword = (EditText) findViewById(R.id.password_text);
        mKeepAuth = (CheckBox) findViewById(R.id.keep_auth_check);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DbProvider.releaseTempHelper();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mPreferences.contains(Utils.KEY_USERPASS_CACHE)) {
            migrateAuthToDb();
        }
        loadAuthFromDb();
    }

    private void loadAuthFromDb() {
        RuntimeExceptionDao<CredentialsItem, String> credDao = mEntityManager.getCredentialsDao();
        List<CredentialsItem> saved = credDao.queryForAll();
        ArrayAdapter<CredentialsItem> credAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, saved);
        mUsername.setAdapter(credAdapter);
        mUsername.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CredentialsItem creds = (CredentialsItem) parent.getItemAtPosition(position);
                mUsername.setText(creds.getUsername());
                mPassword.setText(creds.getPassword());
            }
        });
        mUsername.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                
            }
        });
        
        List<CredentialsItem> autoloadSingle = credDao.queryForEq("autologin", true);
        if(autoloadSingle.isEmpty()) { // ещё нет аккаунта, в который производился вход
            return;
        }
        
        CredentialsItem autoload = autoloadSingle.get(0);
        mUsername.setText(autoload.getUsername());
        mPassword.setText(autoload.getPassword());
    }

    private void migrateAuthToDb() {
        // последний раз загрузим по-старому
        String[] logKeyPair = mPreferences.getString(Utils.KEY_USERPASS_CACHE, "").split(AUTHS_DELIMITER);
        if (logKeyPair.length == 1 && logKeyPair[0].isEmpty()) // not found
            return;

        final Map<String, CredentialsItem> oldCreds = new HashMap<>();
        for (String logKey : logKeyPair) {
            String[] curr = logKey.split(AUTH_PAIR_DELIMITER);
            CredentialsItem credItem = new CredentialsItem();
            credItem.setUsername(curr[0]);
            credItem.setPassword(curr[1]);
            oldCreds.put(curr[0], credItem);
        }

        // запомним активный по умолчанию логин (если имеется)
        mKeepAuth.setChecked(mPreferences.getBoolean(Utils.KEY_KEEP_AUTH, true));
        if (mKeepAuth.isChecked()) {
            String activeLogin = mPreferences.getString(Utils.KEY_USERNAME, "");
            mUsername.setText(activeLogin);
            String activePassword = mPreferences.getString(Utils.KEY_PASSWORD, "");
            mPassword.setText(activePassword);

            CredentialsItem activeCredentials = new CredentialsItem();
            activeCredentials.setUsername(activeLogin);
            activeCredentials.setPassword(activePassword);
            oldCreds.put(activeLogin, activeCredentials);
        }
        
        // загружаем данные в БД
        final RuntimeExceptionDao<CredentialsItem, String> credDao = mEntityManager.getCredentialsDao();
        credDao.callBatchTasks(new Callable<Void>() {
            public Void call() throws Exception {
                for (CredentialsItem account : oldCreds.values()) {
                    credDao.create(account);
                }
                return null;
            }
        });

        mPreferences.edit()
                .remove(Utils.KEY_USERPASS_CACHE)
                .remove(Utils.KEY_KEEP_AUTH)
                .remove(Utils.KEY_USERNAME)
                .remove(Utils.KEY_PASSWORD)
                .apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.authorization_form_a, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencePage.class));
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if(mSwitcher.getDisplayedChild() == REGISTER_FORM) {
            mSwitcher.showPrevious();
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            super.onBackPressed();
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_button:
                if (mUsername.getText().toString().isEmpty() || mPassword.getText().toString().isEmpty()) {
                    Toast.makeText(this, R.string.details_missing, Toast.LENGTH_LONG).show();
                    return;
                }

                // убираем автологин у всех аккаунтов
                // по идее, будет всего один
                RuntimeExceptionDao<CredentialsItem, String> credDao = mEntityManager.getCredentialsDao();
                for (CredentialsItem item : credDao.queryForEq("autologin", true)) {
                    item.setAutologin(false);
                    credDao.update(item);
                }

                CredentialsItem entered = new CredentialsItem();
                entered.setUsername(mUsername.getText().toString());
                entered.setPassword(mPassword.getText().toString());
                if (mKeepAuth.isChecked()) {
                    // сохраняем автологин у выбранного
                    entered.setAutologin(true);
                }
                credDao.createOrUpdate(entered);

                // сохраняем последний логин в настройки, чтобы понять позже в NetworkService, под кем заходить
                mPreferences.edit().putString(Utils.KEY_USERNAME, entered.getUsername()).apply();
                
                // переходим в основную активность
                startActivity(new Intent(this, DiaryListActivity.class));
                finish();
                break;
            case R.id.request_reg_button:
                mSwitcher.showNext();
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                break;
            default:
                Toast.makeText(this, R.string.not_impl_sc, Toast.LENGTH_LONG).show();
        }

    }
}
