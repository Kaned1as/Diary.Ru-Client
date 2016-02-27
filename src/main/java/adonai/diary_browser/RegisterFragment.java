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
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
import java.util.List;
import java.util.Map;

/**
 * Фрагмент, отвечающий за авторизацию в активности входа ({@link AuthorizationForm})
 * 
 * @author Адонай
 */
public class RegisterFragment extends Fragment implements View.OnClickListener, Validator.ValidationListener {

    private static final int VALIDATE         = 0;
    private static final int REGISTER         = 1;
    private static final int CAPTCHA          = 2;
    
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

    private Handler mHandler;
    private DiaryHttpClient mHttpClient;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View reg = inflater.inflate(R.layout.authorization_form_register, container, false);

        mRequestedLogin = (EditText) reg.findViewById(R.id.register_desired_login);
        mRequestedLogin.addTextChangedListener(mDelayedValidation);
        mRequestedPage = (EditText) reg.findViewById(R.id.register_desired_page);
        mRequestedPage.addTextChangedListener(mDelayedValidation);
        mRequestedPassword = (EditText) reg.findViewById(R.id.register_desired_password);
        mRequestedPassword.addTextChangedListener(mDelayedValidation);
        mPasswordConfirm = (EditText) reg.findViewById(R.id.register_desired_password_again);
        mPasswordConfirm.addTextChangedListener(mDelayedValidation);
        mEmail = (EditText) reg.findViewById(R.id.register_email);
        mEmail.addTextChangedListener(mDelayedValidation);
        mRegister = (Button) reg.findViewById(R.id.register_button);
        mRegister.setOnClickListener(this);

        mRegisterWait = new MaterialDialog.Builder(getActivity())
                .title(R.string.loading)
                .content(R.string.requesting_registration)
                .progress(true, 0)
                .build();

        mValidator = new Validator(this);
        mValidator.setValidationMode(Validator.Mode.IMMEDIATE);
        mValidator.setValidationListener(this);

        mHttpClient = new DiaryHttpClient();

        // не хочу добавлять TextWatcher на всё, раз в секунду хватит
        HandlerThread thread = new HandlerThread("ValidatorThread");
        thread.start();
        mHandler = new Handler(thread.getLooper(), new BackgroundCallback());
        
        return reg;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRegisterWait.dismiss();
        mHandler.getLooper().quit();
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
            edit.setError(firstFailed.getMessage(getActivity()));
        }
        mRegister.setEnabled(false);
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
            final MaterialDialog.Builder successShow =  new MaterialDialog.Builder(getActivity())
                    .title(R.string.successfully_registered)
                    .content(R.string.congratulation_registered)
                    .positiveText(android.R.string.ok)
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            getActivity().onBackPressed();
                        }
                    });

            // показываем диалог
            getActivity().runOnUiThread(new Runnable() {
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
                getActivity().runOnUiThread(new Runnable() {
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
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WebView view = new WebView(getActivity());
                    view.getSettings().setJavaScriptEnabled(true);
                    view.addJavascriptInterface(new WebAppInterface(getActivity()), "RegisterHandler");

                    mCaptchaView = new MaterialDialog.Builder(getActivity())
                            .title(R.string.captcha_request)
                            .customView(view, false).build();

                    view.loadDataWithBaseURL(Utils.MAIN_PAGE, root.html(), null, "utf-8", null);
                    mCaptchaView.show();
                    mRegisterWait.hide();
                }
            });
        }

        public void deduceAndShowError(Document root) {
            final MaterialDialog.Builder showError =  new MaterialDialog.Builder(getActivity())
                    .title(R.string.registration_error)
                    .positiveText(android.R.string.ok);

            // попытаемся вытащить ошибку
            Element error = root.select("td[bgcolor][align] > p > font").first();
            if(error != null) {
                String errorText = error.ownText();
                showError.content(errorText);
            }

            // из фонового потока не получится
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showError.build().show();
                    mRegisterWait.hide();
                }
            });
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_button:
                mHandler.sendEmptyMessage(REGISTER);
                mRegisterWait.show();
                break;
            default:
                Toast.makeText(getActivity(), R.string.not_impl_sc, Toast.LENGTH_LONG).show();
        }

    }

    private class AdverigoCaptcha {
        private String number;
        private String token;
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
