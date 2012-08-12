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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;

public class DiaryList extends Activity implements OnClickListener {

	public static final int GET_U_BLOGS = 1;
	public static final int SET_HTTP_COOKIE = 2;
	protected static final int GET_FAVORITES_COMMUNITIES_DATA = 3;
	
	public static final int TAB_FAVOURITES = 0;
	public static final int TAB_COMMUNITIES = 1;
	public static final int TAB_MY_DIARY = 2;
	
	boolean mNeedsRefresh = true;
	MediaArrayAdapter mFavouritesAdapter;
	
	SharedPreferences mSharedPrefs;
	ListView mFavouriteList;
	WebView mMainView;
	ImageButton mExitButton;
	TabHost mTabHost;
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
        
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start(); mLooper = thr.getLooper();
		mHandler = new Handler(mLooper, WorkerCallback);
		mUiHandler = new Handler(UiCallback);
        
        mSharedPrefs = getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
        CookieSyncManager.createInstance(this);
        
        try {
			WMAClient = new JMetaWeblogClient("http://www.diary.ru/client/mwa.php");
			WMAClient.setUsername(mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
			WMAClient.setPassword(mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, ""));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_diary_list_a);
        mFavouriteList = (ListView)findViewById(R.id.favourite_list);
        mMainView = (WebView) findViewById(R.id.main_view);
        	mMainView.setWebViewClient(new WebViewClient());
        mExitButton = (ImageButton) findViewById(R.id.exit_button);	
        	mExitButton.setOnClickListener(this);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        	mTabHost.setup();
        	mTabHost.addTab(mTabHost.newTabSpec("tab_favourites").setIndicator(getString(R.string.favourites)).setContent(R.id.favourite_list));
        	mTabHost.addTab(mTabHost.newTabSpec("tab_communities").setIndicator(getString(R.string.communities)).setContent(R.id.communities_list));
        	mTabHost.addTab(mTabHost.newTabSpec("tab_owndiary").setIndicator(getString(R.string.my_diary)).setContent(R.id.owndiary_list));
        	for(int i = 0, count = mTabHost.getTabWidget().getTabCount(); i != count; ++i) {
        		mTabHost.getTabWidget().getChildTabViewAt(i).setOnClickListener(this);
        		mTabHost.getTabWidget().getChildTabViewAt(i).setTag(i);
        	}
        mFavouritesAdapter = new MediaArrayAdapter(this, android.R.layout.simple_list_item_1, mUser.favorites); 
        mFavouriteList.setAdapter(mFavouritesAdapter);	
        //setCurrentTab(0);
        	
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
		pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.please_wait), true, true);
		mHandler.sendEmptyMessage(SET_HTTP_COOKIE);
	}
	
	Handler.Callback UiCallback = new Handler.Callback() {
		public boolean handleMessage(Message message)
		{
			switch (message.what) {
			case GET_U_BLOGS:
				pd.dismiss();
				break;
			case SET_HTTP_COOKIE:
				pd.dismiss();
				setCurrentTab(TAB_FAVOURITES);
				break;
			case GET_FAVORITES_COMMUNITIES_DATA:
				mFavouritesAdapter.notifyDataSetChanged();
				pd.dismiss();
				//mMainView.loadUrl("http://www.diary.ru");
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
					
					if (loginScreen.contains("Добро пожаловать")) { //login successful
						CookieManager cookieManager = CookieManager.getInstance();
						
						// Sharing cookies between webView and mDHCL
						List<Cookie> cookies = mDHCL.cookieStore.getCookies();
						
						//What the hell is with net Android devs? 
						//Why cookie operations are done through another thread without any clue like "onCookiePendingOperationsListener"??
						//Don't uncomment this line! If you do, cookies'll be added and only then another thread'll delete them resulting in having no cookies at all.
						//cookieManager.removeSessionCookie();
						
						for (Cookie cookie : cookies){
							String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
			                cookieManager.setCookie("diary.ru", cookieString);
			            }
						CookieSyncManager.getInstance().sync();
					}
			        
			        mUiHandler.sendEmptyMessage(SET_HTTP_COOKIE);
					return true;
				case GET_FAVORITES_COMMUNITIES_DATA:
					// TODO: Исправить все к чертям!! Поставить строгое извлечение по столбцам таблицы, идиот!!
					
					mDHCL.postPage("http://www.diary.ru/list/?act=show&fgroup_id=0", null);
					String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
					HtmlCleaner cleaner = new HtmlCleaner();
					cleaner.getProperties().setOmitComments(true);
					
					TagNode rootNode = cleaner.clean(dataPage);
					TagNode table = rootNode.findElementByAttValue("class", "table r", true, false);
					TagNode[] rows = table.getElementsByName("td", true);
					TagNode title = null, author = null, last_post = null;
					for(int i = 0; i < rows.length; ++i) {
						if (title == null)
							title = rows[i].findElementByAttValue("class", "withfloat", true, false);
						if (author == null)
							author = rows[i].findElementByAttValue("target", "_blank", true, false);
						if (last_post == null)
							if(rows[i].getAttributeByName("class") == null)
								last_post = rows[i].findElementByAttValue("class", "withfloat", true, false);
						
						if(title != null && author != null && last_post != null) {
							mUser.favorites.add(mUser.new Diary(title.findElementByName("b", false).getText().toString(), title.getAttributeByName("href"), 
																					  author.getText().toString(), author.getAttributeByName("href"), 
																					  last_post.getText().toString(), last_post.getAttributeByName("href")));
							title = author = last_post = null;
						}
					}
						
					mUiHandler.sendEmptyMessage(GET_FAVORITES_COMMUNITIES_DATA);
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
	
	private class MediaArrayAdapter extends ArrayAdapter<UserData.Diary> {
		
		public MediaArrayAdapter(Context context, int textViewResourceId,
				List<UserData.Diary> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup parent)
		{
			View view;
			if (convertView == null)
				view = View.inflate(getContext(), R.layout.tab_list_item, null);
			else
				view = convertView;
			
			/*ImageButton delete = (ImageButton)view.findViewById(R.id.p_delete);
			TextView title = (TextView)view.findViewById(R.id.p_title);
			TextView artist = (TextView)view.findViewById(R.id.p_artist);
			TextView album = (TextView)view.findViewById(R.id.p_album);
			
			ImageButton up = (ImageButton)view.findViewById(R.id.p_upbutton);
			ImageButton down = (ImageButton)view.findViewById(R.id.p_downbutton);
			up.setOnClickListener(ListItemButtonClickListener);
			down.setOnClickListener(ListItemButtonClickListener);
			
			delete.setOnClickListener(ListItemButtonClickListener);
			title.setText(getItem(pos).title);
			artist.setText(getItem(pos).artist);
			album.setText(getItem(pos).album); */
			
			return view;
		}
		
	}
	
	/*private OnClickListener ListItemButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) 
		{
			int pos = mSongList.getPositionForView((View)view.getParent());
			Song song = mAdapter.getItem(pos);
			switch (view.getId()) {
			case R.id.p_delete:
				if(mID == -1)
					mAdapter.remove(song);
				else {
					String[] selected = { String.format("%d", song.id) };
					Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", mID);
					getContentResolver().delete(uri, "AUDIO_ID = ?", selected);
					mAdapter.remove(song);
				}
				break;
			case R.id.p_upbutton:
				if(pos > 0) {
					mAdapter.remove(song);
					mAdapter.insert(song, pos - 1);
				}
				break;
			case R.id.p_downbutton:
				if(pos < mAdapter.getCount() - 1) {
					mAdapter.remove(song);
					mAdapter.insert(song, pos + 1);
				}
				break;
			}
		}
	}; */
	
	public void onClick(View view) {
		
		if (view == mExitButton) {
			Editor lysosome = mSharedPrefs.edit();
			lysosome.remove(AuthorizationForm.KEY_USERNAME);
			lysosome.remove(AuthorizationForm.KEY_PASSWORD);
			lysosome.commit();
			
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeSessionCookie();
			CookieSyncManager.getInstance().sync();
			mMainView.reload();
		} else if (view.getTag() != null && view.getParent() instanceof TabWidget) {
			int i = (Integer) view.getTag();
			setCurrentTab(i);
		}
	}
	
	/* (non-Javadoc)
	 * Sets the contents to current tab and hides everything other.
	 * In addition, refreshes content on page, if needed.
	 */
	public void setCurrentTab(int index) {
		if (mNeedsRefresh)
			switch (index) {
			case TAB_FAVOURITES:
				pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
				mHandler.sendEmptyMessage(GET_FAVORITES_COMMUNITIES_DATA);
				break;
			case TAB_COMMUNITIES:
				pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
				
				break;
			case TAB_MY_DIARY:
				break;
			default:
				break;
			}
		
		mTabHost.setCurrentTab(index);
	}

}
