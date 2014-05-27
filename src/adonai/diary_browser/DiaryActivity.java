package adonai.diary_browser;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Toast;

import adonai.diary_browser.database.DatabaseHandler;
import adonai.diary_browser.pro.R;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;

public abstract class DiaryActivity extends ActionBarActivity implements Callback
{
    private static final int HANDLE_APP_START = 0;

    DiarySlidePane slider;
    DiaryFragment mainPane;
    MessageSenderFragment messagePane;

    Handler mUiHandler;
    NetworkService mService;
    ProgressDialog pd;

    DiaryHttpClient mDHCL;
    String pageToLoad;

    DatabaseHandler mDatabase;

    DiaryWebView mPageBrowser;
    protected PullToRefreshLayout mPullToRefreshAttacher;

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
        mDatabase = new DatabaseHandler(this);
        mUiHandler = new Handler(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        slider = (DiarySlidePane) findViewById(R.id.slider);
        slider.setPanelSlideListener(sliderListener);
        slider.setSliderFadeColor(getResources().getColor(R.color.diary_transparent));

        mUiHandler.sendEmptyMessage(HANDLE_APP_START); // ensure that service is running
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        setIntent(intent);
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
                mDHCL = mService.mDHCL;
                mUiHandler.sendEmptyMessage(Utils.HANDLE_START); // выполняем стартовые действия для всех остальных
            }
            break;
        case Utils.HANDLE_SERVICE_ERROR:
            Toast.makeText(getApplicationContext(), getString(R.string.service_not_running), Toast.LENGTH_SHORT).show();
            mPullToRefreshAttacher.setRefreshComplete();
            break;
        case Utils.HANDLE_CONNECTIVITY_ERROR:
            Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
            mPullToRefreshAttacher.setRefreshComplete();
            break;
        case Utils.HANDLE_PAGE_INCORRECT:
            Toast.makeText(getApplicationContext(), getString(R.string.page_incorrect), Toast.LENGTH_SHORT).show();
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

    public void handleBackground(int opCode, Object body)
    {
        if(pd == null)
            pd = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data), true, false);
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
            Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    protected UserData getUser()
    {
        if(mService == null)
            return null;

        return mService.mUser;
    }

    protected void onMessagePaneRemove(boolean reload)
    {
        slider.closePane();
    }

}
