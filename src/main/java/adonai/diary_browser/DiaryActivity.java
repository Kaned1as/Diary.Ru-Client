package adonai.diary_browser;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.vending.util.IabHelper;
import com.android.vending.util.IabResult;
import com.android.vending.util.Inventory;
import com.android.vending.util.Purchase;

/**
 * Родительская активность для всех остальных.
 * <br/>
 * Здесь хранятся:
 * <ul>
 *     <li>Обработка платёжек и MOTD</li>
 *     <li>Обработка старта {@link NetworkService}</li>
 *     <li>Обработка ошибок исполнения</li>
 *     <li>Обработка посылок сообщений в {@link NetworkService}</li>
 * </ul>
 *
 * @author Адонай
 */
public abstract class DiaryActivity extends AppCompatActivity implements Callback, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int HANDLE_APP_START = -100;
    private static final String SKU_DONATE = "small";

    protected IabHelper mHelper;
    protected boolean mCanBuy = false;

    protected SwipeRefreshLayout swipeList;
    protected SwipeRefreshLayout swipeBrowser;

    protected DiarySlidePane slider;
    protected DiaryFragment mainPane;
    protected DiaryWebView mPageBrowser;
    protected MessageSenderFragment messagePane;
    protected MaterialDialog pd;

    protected Handler mUiHandler;
    protected NetworkService mService;
    protected DiaryHttpClient mHttpClient;
    protected SharedPreferences mSharedPrefs;

    protected String pageToLoad;
    protected String textToWrite;
    protected Uri imageToUpload;

    SlidingPaneLayout.PanelSlideListener sliderListener = new SlidingPaneLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View view, float v) {
            messagePane.setHasOptionsMenu(false);
            mainPane.setHasOptionsMenu(false);
        }

        @Override
        public void onPanelOpened(View view) {
            messagePane.setHasOptionsMenu(true);
            mainPane.setHasOptionsMenu(false);
        }

        @Override
        public void onPanelClosed(View view) {
            messagePane.setHasOptionsMenu(false);
            mainPane.setHasOptionsMenu(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setupTheme(this);
        super.onCreate(savedInstanceState);

        mSharedPrefs = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);
        mUiHandler = new Handler(this);

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjuleYDZj7oG7JeX8+bwJWQrf+DYgqGOSiIA6frTZJ+/C7Jt/+PMbWjd/rOelshuYy5HWqywFjvOPoK18zIRMavS1QtlxIMbA/eaVlk+QKEaqOY0EIuBUEIog9e2H7HMq9BVE7o1j8NFuG0skj2jDYfO2R0OfZS2xetqQcXtEtQLp0osS9GQK20oVfNM+LQyyG5ROcab3TmXXjiR0J43XdD8txhSLRB7gzFflMy9C1zYE7736i/R7NAHdmX6KRWmK+YsbI78Wnoy6xa63npdUTIcTUlUwV9zg6VWxQjSLsWnhkgqqJltmKGXk/d3DGYVlwZBu7XnwU0ufGvC1wBC09wIDAQAB";
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess())
                    mCanBuy = true;
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        slider = (DiarySlidePane) findViewById(R.id.slider);
        slider.setPanelSlideListener(sliderListener);
        slider.setSliderFadeColor(Color.WHITE);

        mUiHandler.sendEmptyMessage(HANDLE_APP_START); // ensure that service is running

        TypedValue color = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, color, true);
        swipeList.setColorSchemeColors(color.data);
        swipeBrowser.setColorSchemeColors(color.data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                View aboutContent = LayoutInflater.from(this).inflate(R.layout.about_d, null);
                TextView author = (TextView) aboutContent.findViewById(R.id.author_info);
                author.setText(Html.fromHtml(getString(R.string.author_description)));
                author.setMovementMethod(LinkMovementMethod.getInstance());
                TextView app = (TextView) aboutContent.findViewById(R.id.app_info);

                String appWithVersion;
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    appWithVersion = String.format(getString(R.string.application_description), pInfo.versionName);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    appWithVersion = String.format(getString(R.string.application_description), "unknown");
                }

                app.setText(Html.fromHtml(appWithVersion));
                app.setMovementMethod(LinkMovementMethod.getInstance());
                new MaterialDialog.Builder(this)
                        .title(R.string.about)
                        .customView(aboutContent, false)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case HANDLE_APP_START:
                mService = NetworkService.getInstance(this);
                if (mService == null)
                    mUiHandler.sendEmptyMessageDelayed(HANDLE_APP_START, 50);
                else {
                    setRequestedOrientation(mService.mOrientation);
                    mHttpClient = mService.mNetworkClient;
                    mUiHandler.sendEmptyMessage(Utils.HANDLE_START); // выполняем стартовые действия для всех остальных

                    if (getPackageName().contains("pro"))
                        break;

                    showChangesPage();
                }
                break;
            case Utils.HANDLE_SERVICE_ERROR:
                Toast.makeText(getApplicationContext(), getString(R.string.service_not_running), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_CONNECTIVITY_ERROR:
                Toast.makeText(getApplicationContext(), getString((Integer) msg.obj), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_NOTFOUND_ERROR:
                Toast.makeText(getApplicationContext(), getString(R.string.notfound_error), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_JUST_DO_GET:
                Toast.makeText(getApplicationContext(), getString(R.string.completed), Toast.LENGTH_SHORT).show();
                break;
        }

        if (pd != null) {
            pd.dismiss();
            pd = null;
        }

        return true;
    }

    private void showChangesPage() {
        // Показываем страничку изменений
        try {
            final String current = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            final String stored = mSharedPrefs.getString("stored.version", "");
            boolean show = mSharedPrefs.getBoolean("show.version", true);
            if (show && !current.equals(stored)) {
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) // бывает при неверной авторизации
                            return;

                        // TODO: move to XML
                        TextView message = new TextView(DiaryActivity.this);
                        message.setMovementMethod(LinkMovementMethod.getInstance());
                        message.setGravity(Gravity.CENTER_HORIZONTAL);
                        message.setText(Html.fromHtml(getString(R.string.ad_text)));
                        TypedValue color = new TypedValue();
                        getTheme().resolveAttribute(R.attr.textColorTitles, color, true);
                        message.setTextColor(color.data);
                        new MaterialDialog.Builder(DiaryActivity.this)
                                .title(R.string.ad_title)
                                .customView(message, false)
                                .positiveText(R.string.help)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                        purchaseGift();
                                    }
                                })
                                .negativeText(R.string.later)
                                .show();
                    }
                }, 5000);

                mSharedPrefs.edit()
                    .putString("stored.version", current)
                    .apply();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // не сработало - и ладно
        }
    }

    protected void purchaseGift() {
        if (mCanBuy) {
            mHelper.launchPurchaseFlow(DiaryActivity.this, SKU_DONATE, 6666, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isSuccess()) {
                        new MaterialDialog.Builder(DiaryActivity.this)
                                .title(R.string.completed)
                                .content(R.string.thanks)
                                .positiveText(android.R.string.ok)
                                .show();
                    }

                    mHelper.queryInventoryAsync(false, new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result.isSuccess()) {
                                if (inv.getPurchase(SKU_DONATE) != null)
                                    mHelper.consumeAsync(inv.getPurchase(SKU_DONATE), null);
                            }
                        }
                    });
                }
            }, "NothingAndNowhere" + getUser().getUserName());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void handleBackground(int opCode, Object body) {
        if(pd != null && pd.isShowing()) {
            pd.setTitle(R.string.loading);
            pd.setContent(getString(R.string.loading_data));
        } else {
            pd = new MaterialDialog.Builder(this)
                    .title(R.string.loading)
                    .content(R.string.loading_data)
                    .progress(true, 0)
                    .build();
            pd.show();
        }

        pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mHttpClient.abort();
            }
        });
        mService.handleRequest(opCode, body);
    }

    public void handleUi(int opCode, Object body) {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(opCode, body));
    }

    public void handleFontChange(String currSize) {
        try {
            int realNum = Integer.parseInt(currSize);
            mPageBrowser.getSettings().setMinimumFontSize(realNum);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (mCanBuy)
            mHelper.dispose();
        super.onDestroy();
    }

    protected UserData getUser() {
        return UserData.getInstance();
    }

    protected void onMessagePaneRemove(boolean reload) {
        slider.closePane();
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        for (int result : grantResults) {
            if(result == PackageManager.PERMISSION_DENIED)
                Toast.makeText(this, R.string.not_permitted, Toast.LENGTH_SHORT).show();
        }
    }

}
