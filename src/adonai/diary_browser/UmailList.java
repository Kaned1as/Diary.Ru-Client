package adonai.diary_browser;

import java.util.Map;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

public class UmailList extends Activity implements IRequestHandler
{
    DiaryHttpClient mDHCL = Globals.mDHCL;
    UserData mUser = Globals.mUser;
    Map<String, String> namesUrls;
    
    DiaryWebView mMessageBrowser;
    PullToRefreshListView mFolderBrowser;
    ProgressDialog pd;
    
    Handler mHandler, mUiHandler;
    Looper mLooper; // петля времени

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, WorkerCallback);
        mUiHandler = new Handler(UiCallback);
        
        setContentView(R.layout.umail_list_a);
        mMessageBrowser = (DiaryWebView) findViewById(R.id.umessage_browser);
        mMessageBrowser.setDefaultSettings();
        
        mFolderBrowser = (PullToRefreshListView) findViewById(R.id.ufolder_browser);
        
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
        returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(returnIntent);
        finish();
    }
    
    Handler.Callback UiCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            return false;
        }
    };
    
    
    Handler.Callback WorkerCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            return false;
        }
    };
    
    @Override
    public void handleBackground(int opCode, Object body)
    {
        pd = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data), true, true);
        mHandler.sendMessage(mHandler.obtainMessage(opCode, body));
    }

    @Override
    public void handleUi(int opCode, Object body)
    {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(opCode, body));
    }
}
