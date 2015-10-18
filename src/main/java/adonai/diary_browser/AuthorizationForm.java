package adonai.diary_browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.ConfirmPassword;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Order;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adonai.diary_browser.preferences.PreferencePage;

public class AuthorizationForm extends AppCompatActivity implements OnClickListener, Validator.ValidationListener {

    // общее
    private static final String AUTH_PAIR_DELIMITER = "=logPassSplitter=";
    private static final String AUTHS_DELIMITER = "=pairSplitter=";

    private static final int VALIDATE         = 0;
    private static final int REGISTER         = 1;
    private static final int CAPTCHA          = 2;
    
    private static int LOGIN_FORM       = 0;
    private static int REGISTER_FORM    = 1;
    
    private ViewSwitcher mSwitcher;
    private SharedPreferences mPreferences;
    private Handler mHandler;
    private DiaryHttpClient mHttpClient;
    
    // авторизация
    private Button mLogin, mRequestRegistration;
    private EditText mUsername, mPassword;
    private Spinner mLoginPicker;
    private CheckBox mKeepAuth;
    private Map<String, String> mLoginPasswordPairs = new HashMap<>();

    // регистрация
    
    @Order(0)
    @NotEmpty(messageResId = R.string.required_field)
    private EditText mRequestedLogin;

    @Order(1)
    @Pattern(regex = "[A-Za-z]+", messageResId = R.string.invalid_page_name)
    @NotEmpty(messageResId = R.string.required_field)
    private EditText mRequestedPage;

    @Order(2)
    @Password(messageResId = R.string.invalid_password)
    @NotEmpty(messageResId = R.string.required_field)
    private EditText mRequestedPassword;

    @Order(3)
    @ConfirmPassword(messageResId = R.string.passwords_dont_match)
    @NotEmpty(messageResId = R.string.required_field)
    private EditText mPasswordConfirm;

    @Order(4)
    @Email(messageResId = R.string.invalid_email)
    @NotEmpty(messageResId = R.string.required_field)
    private EditText mEmail;

    private Button mRegister;
    private MaterialDialog mRegisterWait, mCaptchaView;
    
    private TextWatcher mDelayedValidation = new DelayedValidation();
    private Validator mValidator;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.setupTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorization_form_a);
        getSupportActionBar().setHomeButtonEnabled(true);
        
        mSwitcher = (ViewSwitcher) findViewById(R.id.login_register_switcher);
        mPreferences = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);
        mHttpClient = new DiaryHttpClient();
                
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
        
        // регистрация
        mRequestedLogin = (EditText) findViewById(R.id.register_desired_login); 
        mRequestedLogin.addTextChangedListener(mDelayedValidation);
        mRequestedPage = (EditText) findViewById(R.id.register_desired_page);
        mRequestedPage.addTextChangedListener(mDelayedValidation);
        mRequestedPassword = (EditText) findViewById(R.id.register_desired_password);
        mRequestedPassword.addTextChangedListener(mDelayedValidation);
        mPasswordConfirm = (EditText) findViewById(R.id.register_desired_password_again);
        mPasswordConfirm.addTextChangedListener(mDelayedValidation);
        mEmail = (EditText) findViewById(R.id.register_email);
        mEmail.addTextChangedListener(mDelayedValidation);
        mRegister = (Button) findViewById(R.id.register_button);
        mRegister.setOnClickListener(this);

        mRegisterWait = new MaterialDialog.Builder(this)
                .title(R.string.loading)
                .content(R.string.requesting_registration)
                .progress(true, 0)
                .build();

        mValidator = new Validator(this);
        mValidator.setValidationMode(Validator.Mode.IMMEDIATE);
        mValidator.setValidationListener(this);
        
        // не хочу добавлять TextWatcher на всё, раз в секунду хватит
        HandlerThread thread = new HandlerThread("ValidatorThread");
        thread.start();
        mHandler = new Handler(thread.getLooper(), new BackgroundCallback());
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
    protected void onDestroy() {
        super.onDestroy();
        mHandler.getLooper().quit();
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
            case R.id.register_button:
                mHandler.sendEmptyMessage(REGISTER);
                mRegisterWait.show();
                break;
            default:
                Toast.makeText(this, R.string.not_impl_sc, Toast.LENGTH_LONG).show();
        }

    }


    @Override
    public void onValidationSucceeded() {
        mRegister.setEnabled(true);
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for(ValidationError error : errors) {
            EditText edit = (EditText) error.getView();
            Rule firstFailed = error.getFailedRules().get(0);
            edit.setError(firstFailed.getMessage(this));
        }
        mRegister.setEnabled(false);
    }
    
    private class AdverigoCaptcha {
        private String number;
        private String token;
    }

    private class BackgroundCallback implements Handler.Callback {
        private String signature;
        
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case VALIDATE:
                    mValidator.validate(true);
                    return true;
                case REGISTER:
                case CAPTCHA:
                    // необходимо для корректного продолжения
                    //handleSignature();

                    List<Pair<String, String>> parameters = new ArrayList<>(16);
                    parameters.add(Pair.create("act", "registration_post"));
                    parameters.add(Pair.create("module", "registration"));
                    parameters.add(Pair.create("username", mRequestedLogin.getText().toString()));
                    parameters.add(Pair.create("shortname", mRequestedPage.getText().toString()));
                    parameters.add(Pair.create("password", mRequestedPassword.getText().toString()));
                    parameters.add(Pair.create("password2", mPasswordConfirm.getText().toString()));
                    parameters.add(Pair.create("email", mEmail.getText().toString()));
                    parameters.add(Pair.create("number", ""));
                    parameters.add(Pair.create("month", "0"));
                    parameters.add(Pair.create("day", "0"));
                    parameters.add(Pair.create("year", ""));
                    parameters.add(Pair.create("education", "1"));
                    parameters.add(Pair.create("sfera", ""));
                    //parameters.add(Pair.create("signature", signature));
                    
                    if(msg.obj != null) { // есть ответ с капчей
                        AdverigoCaptcha captcha = (AdverigoCaptcha) msg.obj;
                        parameters.add(Pair.create("adverigo_captcha_answer", captcha.number));
                        parameters.add(Pair.create("adverigo_sid", captcha.token));
                    }
                    
                    String page = mHttpClient.postPageToString(Utils.REGISTER_PAGE, parameters);
                    if(page != null) {
                        Document root = Jsoup.parse(page);
                        if (page.contains("Используйте ваши логин и пароль для входа")) { // успех
                            handleSuccess();
                        } else if(root.title().equals("Проверка")) { // запрос капчи
                            handleCaptcha(root);
                        } else if(root.title().equals("@дневники:")) { // ошибка в данных
                            deduceAndShowError(root);
                        }
                    }
                    return true;
                default:
                    return false;
            }
        }

        private void handleSignature() {
            if(signature == null) {
                String mainPage = mHttpClient.getPageAsString(Utils.MAIN_PAGE);
                Document mainDoc = Jsoup.parse(mainPage);
                Element sigNode = mainDoc.getElementsByAttributeValue("name", "signature").first();
                if (sigNode != null)
                    signature = sigNode.attr("value");
            }
        }

        private void handleSuccess() {
            final MaterialDialog.Builder successShow =  new MaterialDialog.Builder(AuthorizationForm.this)
                    .title(R.string.successfully_registered)
                    .content(R.string.congratulation_registered)
                    .positiveText(android.R.string.ok)
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            onBackPressed();
                        }
                    });

            // показываем диалог
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    successShow.build().show();
                    mRegisterWait.hide();
                }
            });
        }
        
        private class WebAppInterface {

            Context mContext;

            /** Instantiate the interface and set the context */
            WebAppInterface(Context c) {
                mContext = c;
            }
            
            @JavascriptInterface
            public void grab(String token, String number) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHttpClient.syncCookiesWithClient();
                        mCaptchaView.dismiss();
                        mRegisterWait.show();
                    }
                });
                
                AdverigoCaptcha captcha = new AdverigoCaptcha();
                captcha.number = number;
                captcha.token = token;
                mHandler.sendMessage(mHandler.obtainMessage(CAPTCHA, captcha));
            }
        }

        @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"}) // мы верим в вежливость дайри
        private void handleCaptcha(final Document root) {
            // убираем всё ненужное, показываем только враппер
            root.head().select("link[rel=stylesheet]").remove();
            root.head().append("<link href=\"file:///android_asset/css/registration.css\" rel=\"stylesheet\" type=\"text/css\">");
            root.body().select("p").remove();
            root.body().append("<script type=\"text/javascript\" src=\"file:///android_asset/javascript/registration.js\"> </script>");

            // показываем диалог
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WebView view = new WebView(AuthorizationForm.this);
                    view.getSettings().setJavaScriptEnabled(true);
                    view.addJavascriptInterface(new WebAppInterface(AuthorizationForm.this), "RegisterHandler");

                    mCaptchaView = new MaterialDialog.Builder(AuthorizationForm.this)
                            .title(R.string.captcha_request)
                            .customView(view, false).build();

                    view.loadDataWithBaseURL(Utils.MAIN_PAGE, root.html(), null, "utf-8", null);
                    mCaptchaView.show();
                    mRegisterWait.hide();
                }
            });
        }

        public void deduceAndShowError(Document root) {
            final MaterialDialog.Builder showError =  new MaterialDialog.Builder(AuthorizationForm.this)
                    .title(R.string.registration_error)
                    .positiveText(android.R.string.ok);

            // попытаемся вытащить ошибку
            Element error = root.select("td[bgcolor][align] > p > font").first();
            if(error != null) {
                String errorText = error.ownText();
                showError.content(errorText);
            }

            // из фонового потока не получится
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showError.build().show();
                    mRegisterWait.hide();
                }
            });
        }
    }

    private class DelayedValidation implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mHandler.removeMessages(VALIDATE);
            mHandler.sendEmptyMessageDelayed(VALIDATE, 1000);
        }
    }
}
