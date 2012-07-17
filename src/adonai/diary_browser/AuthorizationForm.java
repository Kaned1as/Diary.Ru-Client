package adonai.diary_browser;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AuthorizationForm extends Activity implements OnClickListener {

	public static final String KEY_USERNAME = "diary.username.key";
	public static final String KEY_PASSWORD = "diary.password.key";
	public static final String mPrefsFile = "diary.shared.prefs";
	
	SharedPreferences mSharedPrefs;
	Button mLogin;
	EditText mUsername, mPassword;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPrefs = getSharedPreferences(mPrefsFile, MODE_PRIVATE);
        
        setContentView(R.layout.authorization_form_a);
        mLogin = (Button)findViewById(R.id.login_button);
        mLogin.setOnClickListener(this);
        mUsername = (EditText)findViewById(R.id.login_text);
        mPassword = (EditText)findViewById(R.id.password_text);
    }
    
    @Override
	protected void onStart() {
		super.onStart();
        mUsername.setText(mSharedPrefs.getString(KEY_USERNAME, ""));
        mPassword.setText(mSharedPrefs.getString(KEY_PASSWORD, ""));
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.authorization_form_a, menu);
        return true;
    }

	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.login_button:
			if(mUsername.getText().toString().equals("") || mPassword.getText().toString().equals("")) {
				Toast.makeText(this, R.string.details_missing, Toast.LENGTH_LONG).show();
				return;
			}
			
			SharedPreferences.Editor editor = mSharedPrefs.edit();
			editor.putString(KEY_USERNAME, mUsername.getText().toString());
			editor.putString(KEY_PASSWORD, mPassword.getText().toString());
			editor.apply();
			
			startActivity(new Intent(this, DiaryList.class));
			break;
		default:
			Toast.makeText(this, R.string.not_impl_sc, Toast.LENGTH_LONG).show();
			return;
		}
		
	}

    
}
