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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TabWidget;
import android.widget.TextView;

public class UmailList extends Activity implements IRequestHandler, OnClickListener
{
	public static final int TAB_INCOMING = 0;
	public static final int TAB_OUTCOMING = 1;
	
    DiaryHttpClient mDHCL = Globals.mDHCL;
    UserData mUser = Globals.mUser;
    Map<String, String> namesUrls;
    
    DiaryWebView mMessageBrowser;
    PullToRefreshListView mFolderBrowser;
    ProgressDialog pd;
    TabWidget mTabs;
    TextView mIncoming;
    TextView mOutcoming;
    
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
        mTabs = (TabWidget) findViewById(R.id.folder_selector);
        mIncoming = (TextView) findViewById(R.id.incoming);
        mOutcoming = (TextView) findViewById(R.id.outcoming);
        mIncoming.setOnClickListener(this);
        mOutcoming.setOnClickListener(this);
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
    
    public void handleBackground(int opCode, Object body)
    {
        pd = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data), true, true);
        mHandler.sendMessage(mHandler.obtainMessage(opCode, body));
    }

    public void handleUi(int opCode, Object body)
    {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(opCode, body));
    }

	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.incoming:
			mTabs.setCurrentTab(TAB_INCOMING);
			break;
		case R.id.outcoming:
			mTabs.setCurrentTab(TAB_OUTCOMING);
			break;
		}
	}
}
