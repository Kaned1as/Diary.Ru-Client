package adonai.diary_browser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;


import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import de.timroes.axmlrpc.XMLRPCException;

import adonai.metaweblog_client.JMetaWeblogClient;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ListView;

public class DiaryList extends Activity implements OnClickListener {

	public static final int GET_U_BLOGS = 1;
	public static final int SET_HTTP_COOKIE = 2;
	protected static final int GET_DATA = 3;
	
	SharedPreferences mSharedPrefs;
	ListView mFavoriteList;
	WebView mMainView;
	ImageButton mExitButton;
	ProgressDialog pd;
	
	DiaryHttpClient mDHCL;
	UserData mUser;
	
	JMetaWeblogClient WMAClient;
	Object[] RPCResponse;
	
	static Handler mHandler, mUiHandler;
	Looper mLooper;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDHCL = new DiaryHttpClient();
        mUser = new UserData();
        
        mSharedPrefs = getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
        CookieSyncManager.createInstance(getApplicationContext());
        
        try {
			WMAClient = new JMetaWeblogClient("http://www.diary.ru/client/mwa.php");
			WMAClient.setUsername(mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
			WMAClient.setPassword(mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, ""));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
        
        setContentView(R.layout.activity_diary_list_a);
        //mFavoriteList = (ListView)findViewById(R.id.FavoriteList);
        mMainView = (WebView) findViewById(R.id.main_view);
        	mMainView.setWebViewClient(new WebViewClient());
        mExitButton = (ImageButton) findViewById(R.id.exit_button);	
        	mExitButton.setOnClickListener(this);
        
        
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start(); mLooper = thr.getLooper();
		mHandler = new Handler(mLooper, WorkerCallback);
		mUiHandler = new Handler(UiCallback);
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
		mHandler.sendEmptyMessage(SET_HTTP_COOKIE);
		//mHandler.sendEmptyMessage(GET_U_BLOGS);
	}
	
	Handler.Callback UiCallback = new Handler.Callback() {
		public boolean handleMessage(Message message)
		{
			switch (message.what) {
			case GET_U_BLOGS:
				break;
			case SET_HTTP_COOKIE:
				pd.setMessage(getString(R.string.loading_data));
				mHandler.sendEmptyMessage(GET_DATA);
				//pd.dismiss();
				//mMainView.loadUrl("http://diary.ru");
				break;
			case GET_DATA:
				pd.dismiss();
				break;
			default:
				return false;
			}
			return true;
		}
	};
	
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
					return true;
				case SET_HTTP_COOKIE:
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			        nameValuePairs.add(new BasicNameValuePair("user_login", mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
			        nameValuePairs.add(new BasicNameValuePair("user_pass", mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));
			        nameValuePairs.add(new BasicNameValuePair("save_on", "1"));

			        mDHCL.postPage("http://www.diary.ru/login.php", new UrlEncodedFormEntity(nameValuePairs, "WINDOWS-1251"));
					String loginScreen = EntityUtils.toString(mDHCL.response.getEntity());
					
					if(loginScreen.contains("Добро пожаловать")) { //login successful
						CookieManager cookieManager = CookieManager.getInstance();
						
						// Sharing cookies between webView and mDHCL
						List<Cookie> cookies = mDHCL.cookieStore.getCookies();
						cookieManager.removeSessionCookie();
						for (Cookie cookie : cookies){
							String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
			                cookieManager.setCookie("diary.ru", cookieString);
			            }
						Log.i("Cookie", cookieManager.getCookie("diary.ru"));
		                CookieSyncManager.getInstance().sync();
					}
			        
			        mUiHandler.sendEmptyMessage(SET_HTTP_COOKIE);
					return true;
				case GET_DATA:
					mDHCL.postPage("http://www.diary.ru", null);
					String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
					HtmlCleaner cleaner = new HtmlCleaner();
					cleaner.getProperties().setOmitComments(true);
					
					TagNode rootNode = cleaner.clean(dataPage);
					TagNode[] lists = rootNode.getElementsByAttValue("class", "sp", true, false);
					if(lists.length == 0)
						return false;
					
					for(TagNode child : lists[0].getChildTags())
						mUser.favorites.put(child.getText().toString(), child.getAttributeByName("href"));
					
					for(TagNode child : lists[1].getChildTags())
						mUser.communities.put(child.getText().toString(), child.getAttributeByName("href"));
					
					mUiHandler.sendEmptyMessage(GET_DATA);
					return true;
				default:
					return false;
				}
			} catch (XMLRPCException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			return true;
		}
	};

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.exit_button:
			Editor lysosome = mSharedPrefs.edit();
			lysosome.remove(AuthorizationForm.KEY_USERNAME);
			lysosome.remove(AuthorizationForm.KEY_PASSWORD);
			lysosome.commit();
			
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeSessionCookie();
			CookieSyncManager.getInstance().sync();
			mMainView.reload();
			break;
		default:
			return;
		}
		
	}
}
