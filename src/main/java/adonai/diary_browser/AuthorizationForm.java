package adonai.diary_browser;

import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.HashMap;
import java.util.Map;

import adonai.diary_browser.preferences.PreferencePage;

public class AuthorizationForm extends AppCompatActivity implements OnClickListener {

    // общее
    private static final String AUTH_PAIR_DELIMITER = "=logPassSplitter=";
    private static final String AUTHS_DELIMITER = "=pairSplitter=";

    private static int LOGIN_FORM       = 0;
    private static int REGISTER_FORM    = 1;
    
    private ViewSwitcher mSwitcher;
    private SharedPreferences mPreferences;
    
    // авторизация
    private Button mLogin, mRequestRegistration;
    private EditText mUsername, mPassword;
    private Spinner mLoginPicker;
    private CheckBox mKeepAuth;
    private Map<String, String> mLoginPasswordPairs = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.setupTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorization_form_a);
        getSupportActionBar().setHomeButtonEnabled(true);
        
        mSwitcher = (ViewSwitcher) findViewById(R.id.login_register_switcher);
        mPreferences = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);
                
        mLogin = (Button) findViewById(R.id.login_button);
        mLogin.setOnClickListener(this);
        mRequestRegistration = (Button) findViewById(R.id.request_reg_button);
        mRequestRegistration.setOnClickListener(this);
        
        mLoginPicker = (Spinner) findViewById(R.id.login_spinner);
        mLoginPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String login = (String) mLoginPasswordPairs.keySet().toArray()[position];
                mUsername.setText(login);
                mPassword.setText(mLoginPasswordPairs.get(login));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mUsername = (EditText) findViewById(R.id.login_text);
        mPassword = (EditText) findViewById(R.id.password_text);
        mKeepAuth = (CheckBox) findViewById(R.id.keep_auth_check);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        mKeepAuth.setChecked(mPreferences.getBoolean(Utils.KEY_KEEP_AUTH, true));
        if (mKeepAuth.isChecked()) {
            mUsername.setText(mPreferences.getString(Utils.KEY_USERNAME, ""));
            mPassword.setText(mPreferences.getString(Utils.KEY_PASSWORD, ""));
        }

        String[] logKeyPair = mPreferences.getString(Utils.KEY_USERPASS_CACHE, "").split(AUTHS_DELIMITER);
        if (logKeyPair.length == 1 && logKeyPair[0].isEmpty()) // not found
            return;

        mLoginPicker.setVisibility(View.VISIBLE);
        for (String logKey : logKeyPair) {
            String[] curr = logKey.split(AUTH_PAIR_DELIMITER);
            mLoginPasswordPairs.put(curr[0], curr[1]);
        }
        mLoginPicker.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mLoginPasswordPairs.keySet().toArray()));
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

    @SuppressLint("CommitPrefEdits") // must apply synchronously
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_button:
                if (mUsername.getText().toString().isEmpty() || mPassword.getText().toString().isEmpty()) {
                    Toast.makeText(this, R.string.details_missing, Toast.LENGTH_LONG).show();
                    return;
                }

                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(Utils.KEY_KEEP_AUTH, mKeepAuth.isChecked());
                if (mKeepAuth.isChecked())
                    // сохраняем в списке часто используемых
                    mLoginPasswordPairs.put(mUsername.getText().toString(), mPassword.getText().toString());
                else
                    // удаляем из списка часто используемых
                    mLoginPasswordPairs.remove(mUsername.getText().toString());

                StringBuilder logKeyPairString = new StringBuilder();
                for (Map.Entry<String, String> pair : mLoginPasswordPairs.entrySet())
                    logKeyPairString.append(pair.getKey()).append(AUTH_PAIR_DELIMITER).append(pair.getValue()).append(AUTHS_DELIMITER);

                editor.putString(Utils.KEY_USERPASS_CACHE, logKeyPairString.toString());
                editor.putString(Utils.KEY_USERNAME, mUsername.getText().toString());
                editor.putString(Utils.KEY_PASSWORD, mPassword.getText().toString());
                editor.commit();

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
