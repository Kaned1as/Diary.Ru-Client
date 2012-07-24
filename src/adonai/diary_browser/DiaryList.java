package adonai.diary_browser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;
import org.apache.http.util.EntityUtils;

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
import android.view.Menu;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;

public class DiaryList extends Activity {

	public static final int GET_U_BLOGS = 1;
	public static final int SET_HTTP_COOKIE = 2;
	
	SharedPreferences mSharedPrefs;
	ListView mFavoriteList;
	WebView mMainView;
	ProgressDialog pd;
	
	DiaryHttpClient dhcl;
	
	JMetaWeblogClient WMAClient;
	Object[] RPCResponse;
	
	static Handler mHandler, mUiHandler;
	Looper mLooper;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mSharedPrefs = getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
        
        try {
			WMAClient = new JMetaWeblogClient("http://www.diary.ru/client/mwa.php");
			WMAClient.setUsername(mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
			WMAClient.setPassword(mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, ""));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
        
        setContentView(R.layout.activity_diary_list_a);
        //mFavoriteList = (ListView)findViewById(R.id.FavoriteList);
        mMainView = (WebView)findViewById(R.id.main_view);
        	mMainView.setWebViewClient(new WebViewClient());
        
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
		mMainView.postUrl("http://www.diary.ru", null);
		//mHandler.sendEmptyMessage(GET_U_BLOGS);
	}
	
	Handler.Callback UiCallback = new Handler.Callback() {
		public boolean handleMessage(Message message)
		{
			switch (message.what) {
			case GET_U_BLOGS:
			case SET_HTTP_COOKIE:
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
					break;
				case SET_HTTP_COOKIE:
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			        nameValuePairs.add(new BasicNameValuePair("user_login", mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
			        nameValuePairs.add(new BasicNameValuePair("user_pass", mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));
			        nameValuePairs.add(new BasicNameValuePair("save_on", "1"));

			        dhcl = new DiaryHttpClient();
			        try {
			        	dhcl.postPage("http://www.diary.ru/login.php", new UrlEncodedFormEntity(nameValuePairs, "WINDOWS-1251"));
						String resultEntity = EntityUtils.toString(dhcl.response.getEntity());
						
						if(resultEntity.contains("Добро пожаловать")) { //login successful
							CookieSyncManager.createInstance(getApplicationContext());
							CookieManager cookieManager = CookieManager.getInstance();
							
							List<Cookie> cookies = dhcl.cookieStore.getCookies();
							for (Cookie cookie : cookies){
								String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
								cookieManager.removeSessionCookie();
				                cookieManager.setCookie("www.diary.ru", cookieString);
				                CookieSyncManager.getInstance().sync();
				            }
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
			        
			        mUiHandler.sendEmptyMessage(SET_HTTP_COOKIE);
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
