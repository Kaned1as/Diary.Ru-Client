package adonai.diary_browser;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Vector;

import de.timroes.axmlrpc.XMLRPCException;

import adonai.metaweblog_client.JMetaWeblogClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.view.Menu;
import android.widget.ListView;

public class DiaryList extends Activity {

	public static final int GET_U_BLOGS = 1;
	
	ListView mFavoriteList;
	ProgressDialog pd;
	
	JMetaWeblogClient WMAClient;
	Object[] RPCResponse;
	Handler mHandler, mUiHandler;
	Looper mLooper;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
			SharedPreferences mSharedPrefs = getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
			WMAClient = new JMetaWeblogClient("http://www.diary.ru/client/mwa.php");
			WMAClient.setUsername(mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
			WMAClient.setPassword(mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, ""));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
        
        setContentView(R.layout.activity_diary_list_a);
        
        mFavoriteList = (ListView)findViewById(R.id.FavoriteList);
        
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start(); mLooper = thr.getLooper();
		mHandler = new Handler(mLooper, WorkerCallback);
		mUiHandler = new UiHandler();

    }

    @Override
	protected void onDestroy() {
		mLooper.quit();
		super.onDestroy();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_diary_list_a, menu);
        return true;
    }

	@Override
	protected void onStart() {
		super.onStart();
		pd = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.please_wait), true, true);
		mHandler.sendEmptyMessage(GET_U_BLOGS);
	}
	
	private class UiHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case GET_U_BLOGS:
				pd.dismiss();
				return;
			default:
				return;
			}
		}
	}
	
	Handler.Callback WorkerCallback = new Handler.Callback() {
		public boolean handleMessage(Message message)
		{
			try {
				switch (message.what) {
				case GET_U_BLOGS:
					
					RPCResponse = WMAClient.getUsersBlogs();
					pd.dismiss();
					
					if(RPCResponse == null)
						return false; 
					
					HashMap<?, ?> contentHash = new HashMap<Object, Object>();
					Vector<HashMap<?, ?>> contents = new Vector<HashMap<?, ?>>();

					for (int ctr = 0; ctr < RPCResponse.length; ctr++) {
						contentHash = (HashMap<?, ?>) RPCResponse[ctr];
						contents.add(ctr, contentHash);
					}
					
					mUiHandler.sendEmptyMessage(GET_U_BLOGS);
					break;
				default:
					return false;
				}
			} catch (XMLRPCException e) {
				e.printStackTrace();
			}
			
			return true;
		}
	};
}
