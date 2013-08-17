package adonai.diary_browser;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.View;
import android.widget.Toast;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public abstract class DiaryActivity extends FragmentActivity implements Callback
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
    }

    @Override
    protected void onStart()
    {
        mUiHandler.sendEmptyMessage(HANDLE_APP_START); // ensure that service is running
        if(getIntent().getData() != null && pageToLoad == null)
            pageToLoad = getIntent().getDataString();

        slider = (DiarySlidePane) findViewById(R.id.slider);
        slider.setPanelSlideListener(sliderListener);
        slider.setSliderFadeColor(getResources().getColor(R.color.diary_transparent));

        super.onStart();
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

    protected abstract void onFragmentRemove(boolean reload);
}
