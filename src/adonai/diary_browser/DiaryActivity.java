package adonai.diary_browser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SlidingPaneLayout;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.util.IabHelper;
import com.android.vending.util.IabResult;
import com.android.vending.util.Purchase;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshAttacher;

public abstract class DiaryActivity extends FragmentActivity implements Callback
{
    private static final int HANDLE_APP_START = 0;
    private static final String SKU_DONATE = "SMALL";

    protected IabHelper mHelper;
    protected boolean mCanBuy = false;

    DiarySlidePane slider;
    DiaryFragment mainPane;
    MessageSenderFragment messagePane;

    Handler mUiHandler;
    NetworkService mService;
    ProgressDialog pd;

    DiaryHttpClient mDHCL;
    String pageToLoad;
    UserData mUser;

    DiaryWebView mPageBrowser;
    protected PullToRefreshAttacher mPullToRefreshAttacher;

    SlidingPaneLayout.PanelSlideListener sliderListener = new SlidingPaneLayout.PanelSlideListener()
    {
        @Override
        public void onPanelSlide(View view, float v)
        {
            messagePane.setHasOptionsMenu(false);
            mainPane.setHasOptionsMenu(false);
        }

        @Override
        public void onPanelOpened(View view)
        {
            messagePane.setHasOptionsMenu(true);
            mainPane.setHasOptionsMenu(false);
        }

        @Override
        public void onPanelClosed(View view)
        {
            messagePane.setHasOptionsMenu(false);
            mainPane.setHasOptionsMenu(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler(this);
        mPullToRefreshAttacher = PullToRefreshAttacher.get(this);

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjuleYDZj7oG7JeX8+bwJWQrf+DYgqGOSiIA6frTZJ+/C7Jt/+PMbWjd/rOelshuYy5HWqywFjvOPoK18zIRMavS1QtlxIMbA/eaVlk+QKEaqOY0EIuBUEIog9e2H7HMq9BVE7o1j8NFuG0skj2jDYfO2R0OfZS2xetqQcXtEtQLp0osS9GQK20oVfNM+LQyyG5ROcab3TmXXjiR0J43XdD8txhSLRB7gzFflMy9C1zYE7736i/R7NAHdmX6KRWmK+YsbI78Wnoy6xa63npdUTIcTUlUwV9zg6VWxQjSLsWnhkgqqJltmKGXk/d3DGYVlwZBu7XnwU0ufGvC1wBC09wIDAQAB";
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener()
        {
            @Override
            public void onIabSetupFinished(IabResult result)
            {
                if (result.isSuccess())
                    mCanBuy = true;
            }
        });

    }

    @Override
    public void onResume()
    {
        mUiHandler.sendEmptyMessage(HANDLE_APP_START); // ensure that service is running
        if(getIntent().getData() != null && pageToLoad == null)
            pageToLoad = getIntent().getDataString();

        slider = (DiarySlidePane) findViewById(R.id.slider);
        slider.setPanelSlideListener(sliderListener);
        slider.setSliderFadeColor(getResources().getColor(R.color.diary_transparent));

        super.onResume();
    }

    @Override
    public boolean handleMessage(Message msg)
    {
        switch(msg.what)
        {
        case HANDLE_APP_START:
            mService = NetworkService.getInstance(this);
            if(mService == null)
                mUiHandler.sendEmptyMessageDelayed(HANDLE_APP_START, 50);
            else
            {
                mUser = mService.mUser;
                mDHCL = mService.mDHCL;
                mUiHandler.sendEmptyMessage(Utils.HANDLE_START); // выполняем стартовые действия для всех остальных

                // Показываем страничку изменений
                try
                {
                    final String current = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    final String stored = mService.mPreferences.getString("stored.version", "");
                    boolean show = mService.mPreferences.getBoolean("show.version", true);
                    if(show && !current.equals(stored))
                    {
                        mUiHandler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(DiaryActivity.this);
                                TextView message = new TextView(DiaryActivity.this);
                                message.setMovementMethod(LinkMovementMethod.getInstance());
                                message.setGravity(Gravity.CENTER_HORIZONTAL);
                                message.setText(Html.fromHtml(getString(R.string.ad_text)));
                                builder.setTitle(R.string.ad_title).setView(message);
                                builder.setPositiveButton(R.string.help, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        purchaseGift();
                                    }
                                });
                                builder.setNegativeButton(R.string.later, null);
                                builder.create().show();
                            }
                        }, 15000);

                        SharedPreferences.Editor updater = mService.mPreferences.edit();
                        updater.putString("stored.version", current);
                        updater.commit();
                    }


                } catch (PackageManager.NameNotFoundException ignored)
                {
                    // не сработало - и ладно
                }
            }
            break;
        case Utils.HANDLE_SERVICE_ERROR:
            Toast.makeText(getApplicationContext(), getString(R.string.service_not_running), Toast.LENGTH_SHORT).show();
            break;
        case Utils.HANDLE_CONNECTIVITY_ERROR:
            Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
            mPullToRefreshAttacher.setRefreshComplete();
            break;
        case Utils.HANDLE_CLOSED_ERROR:
            Toast.makeText(getApplicationContext(), getString(R.string.closed_error), Toast.LENGTH_SHORT).show();
            mPullToRefreshAttacher.setRefreshComplete();
            break;
        case Utils.HANDLE_JUST_DO_GET:
            Toast.makeText(getApplicationContext(), getString(R.string.completed), Toast.LENGTH_SHORT).show();
            break;
        }

        if(pd != null)
        {
            pd.dismiss();
            pd = null;
        }

        return true;
    }

    protected void purchaseGift()
    {
        if(mCanBuy)
            mHelper.launchPurchaseFlow(DiaryActivity.this, SKU_DONATE, 6666, new IabHelper.OnIabPurchaseFinishedListener()
            {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info)
                {
                    if(result.isSuccess())
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(DiaryActivity.this);
                        builder.setTitle(R.string.completed).setMessage(R.string.thanks);
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.create().show();
                    }
                }
            }, "NothingAndNowhere" + mUser.userName);
    }

    public void handleBackground(int opCode, Object body)
    {
        //WebView hack. It is the only way to stop it.
        //mPageBrowser.getRefreshableView().loadData("<html><body>dummy</body></html>", null, null);
        if(pd == null)
        {
            pd = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data), true, true);
            pd.setOnCancelListener(new OnCancelListener()
            {
                public void onCancel(DialogInterface dialog)
                {
                    mDHCL.abort();
                }
            });
        }
        mService.handleRequest(opCode, body);
    }

    public void handleUi(int opCode, Object body)
    {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(opCode, body));
    }

    public void handleFontChange(String currSize)
    {
        try
        {
            int realNum = Integer.parseInt(currSize);
            mPageBrowser.getSettings().setMinimumFontSize(realNum);
        }
        catch (NumberFormatException ex)
        {
            Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT);
        }
    }

    @Override
    protected void onDestroy()
    {
        mHelper.dispose();
        super.onDestroy();
    }

    protected abstract void onFragmentRemove(boolean reload);
}
