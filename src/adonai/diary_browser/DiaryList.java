package adonai.diary_browser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.diary.antic1tizen.R;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import adonai.diary_browser.entities.Diary;
import adonai.diary_browser.entities.DiaryListArrayAdapter;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiscussionList;
import adonai.diary_browser.entities.DiscussionListArrayAdapter;
import adonai.diary_browser.entities.Post;
import adonai.diary_browser.preferences.PreferencesScreen;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

public class DiaryList extends Activity implements OnClickListener, OnSharedPreferenceChangeListener, OnChildClickListener, OnGroupClickListener, OnRefreshListener<ListView>, OnItemLongClickListener
{
	public interface onUserDataParseListener
    {
        public void parseData(Element rootNode);
    }
    
    
    public void setUserDataListener(onUserDataParseListener listener)
    {
        this.mListener = listener;
    }
	
    // Команды хэндлерам
    static final int HANDLE_AUTHORIZATION_ERROR 					=  -1;
    static final int HANDLE_CONNECTIVITY_ERROR 						=  -2;
    static final int HANDLE_START 									= 	1;
    static final int HANDLE_SET_HTTP_COOKIE 						= 	2;
    static final int HANDLE_GET_DIARIES_DATA 		             	= 	3;
    static final int HANDLE_GET_DIARY_PAGE_DATA 					= 	4;
    static final int HANDLE_PROGRESS 								= 	7;
    static final int HANDLE_PROGRESS_2 								= 	8;
    static final int HANDLE_PICK_URL 								= 	9;
    static final int HANDLE_GET_DISCUSSIONS_DATA 					= 	11;
    static final int HANDLE_GET_DISCUSSION_LIST_DATA 				= 	12;
    
    // Команды хэндлеру вида
    static final int HANDLE_IMAGE_CLICK 							=   20;
    static final int HANDLE_UPDATE_HEADERS 							= 	21;
    
    
    // дополнительные команды хэндлерам
    
    // вкладки приложения
    public static final int TAB_FAVOURITES = 0;
    public static final int TAB_FAV_POSTS = 1;
    public static final int TAB_MY_DIARY = 2;
    public static final int TAB_MY_DIARY_NEW = 3;
    public static final int TAB_DISCUSSIONS = 4;
    public static final int TAB_DISCUSSIONS_NEW = 5;
    
    int mCurrentBrowser = 0;
    int mCurrentTab = 0;
    
    // Адаптеры типов
    DiaryListArrayAdapter mFavouritesAdapter;
    DiscussionListArrayAdapter mDiscussionsAdapter;
    
    // Видимые объекты
    TextView mLogin;
    TextView mDiscussNum;
    TextView mCommentsNum;
    PullToRefreshListView mDiaryBrowser;
    DiaryWebView mPageBrowser;
    ExpandableListView mDiscussionBrowser;
    
    ImageButton mExitButton;
    TabHost mTabHost;
    ProgressDialog pd;
    
    // Сервисные объекты
    DiaryHttpClient mDHCL = Globals.mDHCL;
    UserData mUser = Globals.mUser;
    onUserDataParseListener mListener;
    DisplayMetrics gMetrics;
    CacheManager mCache;
    
    SharedPreferences mPreferences;
    boolean load_images;
    boolean load_cached;
    
    Handler mHandler, mUiHandler;
    Looper mLooper; // петля времени
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Оповещаем остальных, что мы создались
        Globals.mMain = this;
        setUserDataListener(mUser);
        // Если был простой приложения
        if(Globals.mSharedPrefs == null)
        	Globals.mSharedPrefs = getApplicationContext().getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
        
        mPreferences = Globals.mSharedPrefs;
        mCache = CacheManager.getInstance();
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        load_images = mPreferences.getBoolean("images.autoload", false);
        load_cached = mPreferences.getBoolean("images.autoload.cache", false);
        updateMaxCacheSize(mPreferences);
        
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, WorkerCallback);
        mUiHandler = new Handler(UiCallback);
        
        CookieSyncManager.createInstance(this);

        setContentView(R.layout.activity_diary_list_a);
        initializeUI();
    }
    
    public void initializeUI()
    {

        gMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(gMetrics);
        
        mLogin = (TextView) findViewById(R.id.login_name);
        mLogin.setText(mUser.userName);
        
        mExitButton = (ImageButton) findViewById(R.id.exit_button);
        mExitButton.setOnClickListener(this);
        
        mDiaryBrowser = (PullToRefreshListView) findViewById(R.id.diary_browser);
        mPageBrowser = (DiaryWebView) findViewById(R.id.page_browser);
        mPageBrowser.getRefreshableView().clearHistory();
        mPageBrowser.setDefaultSettings();
        registerForContextMenu(mPageBrowser);
        mDiscussionBrowser = (ExpandableListView) findViewById(R.id.discussion_browser);
        
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        
        // Когда мы добавляем несколько табов с одинаковым содержимым, необходимо в конце сделать нужную видимой.
        // Я открыл это странное свойство, когда копался в исходниках Андроида
        mTabHost.addTab(mTabHost.newTabSpec("tab_favourites").setIndicator(getString(R.string.favourites)).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_posts").setIndicator(getString(R.string.posts)).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_owndiary").setIndicator(getString(R.string.my_diary)).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_owndiary").setIndicator("").setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_discussions").setIndicator(getString(R.string.discussions)).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_discussions_newest").setIndicator("").setContent(android.R.id.tabcontent));
        mTabHost.getCurrentView().setVisibility(View.VISIBLE);
        
        // Дополнительные настройки для маленьких вкладок отображения новых комментариев
        mCommentsNum = (TextView) mTabHost.getTabWidget().getChildTabViewAt(TAB_MY_DIARY_NEW).findViewById(android.R.id.title);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mTabHost.getTabWidget().getChildTabViewAt(TAB_MY_DIARY_NEW).getLayoutParams();
		lp.weight = 0; lp.width = (int)(50 / gMetrics.density);
		mDiscussNum = (TextView) mTabHost.getTabWidget().getChildTabViewAt(TAB_DISCUSSIONS_NEW).findViewById(android.R.id.title);
		lp = (LinearLayout.LayoutParams) mTabHost.getTabWidget().getChildTabViewAt(TAB_DISCUSSIONS_NEW).getLayoutParams();
		lp.weight = 0; lp.width = (int)(50 / gMetrics.density);
        
        // UGLY HACK для более тонких табов
        for (int i = 0, count = mTabHost.getTabWidget().getTabCount(); i != count; ++i)
        {
            final View view = mTabHost.getTabWidget().getChildTabViewAt(i);
            view.setOnClickListener(this);
            view.setTag(i);
            view.getLayoutParams().height *= 0.50;
            view.setPadding((int)(10 / gMetrics.density), 0, (int)(10 / gMetrics.density), 0);

            final View textView = view.findViewById(android.R.id.title);
            if (textView instanceof TextView)
            {
                ((TextView) textView).setGravity(Gravity.CENTER);
                ((TextView) textView).setTypeface(Typeface.DEFAULT_BOLD);
                
                // explicitly set layout parameters
                textView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                textView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
        
        mFavouritesAdapter = new DiaryListArrayAdapter(this, android.R.layout.simple_list_item_1, mUser.currentDiaries);
        mDiaryBrowser.setAdapter(mFavouritesAdapter);
        mDiaryBrowser.setOnRefreshListener(this);
        
        mDiscussionsAdapter = new DiscussionListArrayAdapter(this, mUser.discussions);
        mDiscussionBrowser.setAdapter(mDiscussionsAdapter);
        mDiscussionBrowser.setOnChildClickListener(this);
        mDiscussionBrowser.setOnGroupClickListener(this);
        mDiscussionBrowser.setOnItemLongClickListener(this);
        
        mTabHost.setCurrentTab(mCurrentTab);
        setCurrentVisibleComponent(mCurrentBrowser);
        
        mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onDestroy()
    {
        mLooper.quit();
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.diary_list_a, menu);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
	public boolean onPrepareOptionsMenu(Menu menu) 
    {
        if(mUser.currentDiaryPage.getType() == DiaryPage.POST_LIST)
        	menu.findItem(R.id.menu_new_post).setVisible(true);
        else
        	menu.findItem(R.id.menu_new_post).setVisible(false);
        
        if(mUser.currentDiaryPage.getType() == DiaryPage.COMMENT_LIST)
        	menu.findItem(R.id.menu_new_comment).setVisible(true);
        else
        	menu.findItem(R.id.menu_new_comment).setVisible(false);
        
        if(mCurrentBrowser != DiaryPage.DIARY_LIST)
        	menu.findItem(R.id.menu_share).setVisible(true);
        else 
        	menu.findItem(R.id.menu_share).setVisible(false);
        
		return super.onPrepareOptionsMenu(menu);
	}

    // старые телефоны тоже должны работать
	@SuppressWarnings("deprecation")
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_new_post:
                newPostPost();
                return true;
            case R.id.menu_new_comment:
            	newCommentPost();
            	return true;
            case R.id.menu_settings:
            	startActivity(new Intent(this, PreferencesScreen.class));
            	return true;
            case R.id.menu_share:
            	android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            	switch(mUser.currentDiaryPage.getType())
            	{
            		case DiaryPage.POST_LIST:
            			Toast.makeText(getApplicationContext(), getString(R.string.copied) + " " + mUser.currentDiaryPage.get_diary_URL(), Toast.LENGTH_SHORT).show();
            			clipboard.setText(mUser.currentDiaryPage.get_diary_URL());
            		break;
            		case DiaryPage.COMMENT_LIST:
            			Toast.makeText(getApplicationContext(), getString(R.string.copied) + " " + mUser.currentDiaryPage.get_post_URL(), Toast.LENGTH_SHORT).show();
            			clipboard.setText(mUser.currentDiaryPage.get_post_URL());
            		break;
            		default:
            			return false;
            	}
            	return true;
            case R.id.menu_about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.about);
                View aboutContent = View.inflate(this, R.layout.about_d, null);
                TextView author = (TextView) aboutContent.findViewById(R.id.author_info);
                author.setText(Html.fromHtml(getString(R.string.author_description)));
                author.setMovementMethod(LinkMovementMethod.getInstance());
                TextView app = (TextView) aboutContent.findViewById(R.id.app_info);
                app.setText(Html.fromHtml(getString(R.string.application_description)));
                app.setMovementMethod(LinkMovementMethod.getInstance());
                builder.setView(aboutContent);
                builder.create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        if(v.getId() == R.id.page_browser)
        {
        	Message msg = Message.obtain(mUiHandler, HANDLE_IMAGE_CLICK);
        	mPageBrowser.getRefreshableView().requestImageRef(msg);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if(mUser.updateNeeded())
        	mUiHandler.sendEmptyMessage(HANDLE_START);
    }
    
    @Override
    protected void onNewIntent(Intent intent) 
    {
        super.onNewIntent(intent);

        if(intent != null && intent.getBooleanExtra("reloadContent", false))
        {
        	reloadContent();
        	return;
        }
    }
    
    Handler.Callback UiCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            switch (message.what)
            {
            	case HANDLE_START:
                    pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.please_wait), true, true);
                    mHandler.sendEmptyMessage(HANDLE_SET_HTTP_COOKIE);
            	break;
                case HANDLE_PROGRESS:
                    if(pd != null && pd.isShowing())
                        pd.setMessage(getString(R.string.parsing_data));
                break;
                case HANDLE_PROGRESS_2:
                    if(pd != null && pd.isShowing())
                        pd.setMessage(getString(R.string.sorting_data));
                break;
                case HANDLE_UPDATE_HEADERS:
                	// обрабатываем обновление контента
                	mLogin.setText(mUser.userName);
                    if(mUser.newDiaryCommentsNum != 0)
                    {
                    	mCommentsNum.setText(mUser.newDiaryCommentsNum.toString());
                    	mTabHost.getTabWidget().getChildTabViewAt(TAB_MY_DIARY_NEW).setEnabled(true);
                    }
                    else
                    {
                    	mCommentsNum.setText("");
                    	mTabHost.getTabWidget().getChildTabViewAt(TAB_MY_DIARY_NEW).setEnabled(false);
                    }
                    
                    if(mUser.newDiscussNum != 0)
                    {
                    	mDiscussNum.setText(mUser.newDiscussNum.toString());
                    	mTabHost.getTabWidget().getChildTabViewAt(TAB_DISCUSSIONS_NEW).setEnabled(true);
                    }
                    else
                    {
                    	mDiscussNum.setText("");
                    	mTabHost.getTabWidget().getChildTabViewAt(TAB_DISCUSSIONS_NEW).setEnabled(false);
                    }
                break;
                case HANDLE_SET_HTTP_COOKIE:
                    pd.setMessage(getString(R.string.getting_user_info));
                    mLogin.setText(Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>("http://www.diary.ru/list/?act=show&fgroup_id=0", true)));
                break;
                case HANDLE_GET_DIARIES_DATA:
                    setCurrentVisibleComponent(DiaryPage.DIARY_LIST);
                    mDiaryBrowser.setAdapter(null);
                    mDiaryBrowser.getRefreshableView().removeFooterView(mDiaryBrowser.getRefreshableView().findViewWithTag("footer"));
                    mFavouritesAdapter = new DiaryListArrayAdapter(DiaryList.this, android.R.layout.simple_list_item_1, mUser.currentDiaries);
                    if(mUser.currentDiaries.getPageLinks() != null)
                    {
                        LinearLayout LL = new LinearLayout(mDiaryBrowser.getContext());
                        LL.setTag("footer");
                        Spanned pageLinks = mUser.currentDiaries.getPageLinks();
                        URLSpan[] URLs = pageLinks.getSpans(0, pageLinks.length(), URLSpan.class);
                        for(URLSpan url : URLs)
                        {
                            Button click = new Button(LL.getContext());
                            click.setText(pageLinks.subSequence(pageLinks.getSpanStart(url), pageLinks.getSpanEnd(url)));
                            click.setTag(url.getURL());
                            click.setOnClickListener(DiaryList.this);
                            LL.addView(click);
                            
                            LayoutParams LP = (LayoutParams) click.getLayoutParams();
                            LP.width = LayoutParams.MATCH_PARENT;
                            LP.weight = 1.0f;
                        }
                        mDiaryBrowser.getRefreshableView().addFooterView(LL);
                    }
                    mDiaryBrowser.setAdapter(mFavouritesAdapter);
                    mDiaryBrowser.onRefreshComplete();
                    pd.dismiss();
                break;
                case HANDLE_GET_DIARY_PAGE_DATA:
                    setCurrentVisibleComponent(DiaryPage.POST_LIST);
                    switch(mUser.currentDiaryPage.getType())
                    {
                    case DiaryPage.POST_LIST: 
                        mPageBrowser.getRefreshableView().loadDataWithBaseURL(mUser.currentDiaryPage.get_diary_URL(), mUser.currentDiaryPage.get_content().html(), null, "utf-8", mUser.currentDiaryPage.get_diary_URL());
                    break;
                    case DiaryPage.COMMENT_LIST: 
                        mPageBrowser.getRefreshableView().loadDataWithBaseURL(mUser.currentDiaryPage.get_post_URL(), mUser.currentDiaryPage.get_content().html(), null, "utf-8", mUser.currentDiaryPage.get_post_URL());
                    break;
                    }
                    mPageBrowser.onRefreshComplete();
                    pd.dismiss();
                break;
                case HANDLE_GET_DISCUSSIONS_DATA:
                    mDiscussionsAdapter.notifyDataSetChanged();
                	setCurrentVisibleComponent(DiaryPage.DISCUSSION_LIST);
                	pd.dismiss();
                break;
                case HANDLE_AUTHORIZATION_ERROR:
                    pd.dismiss();
                    mPageBrowser.onRefreshComplete();
                    mDiaryBrowser.onRefreshComplete();
                    Toast.makeText(getApplicationContext(), "Not authorized, retry!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
                    finish();
                break;
                case HANDLE_GET_DISCUSSION_LIST_DATA:
                	int pos = (Integer) message.obj;
                	mDiscussionBrowser.expandGroup(pos);
                	pd.dismiss();
                break;
                case HANDLE_CONNECTIVITY_ERROR:
                    pd.dismiss();
                    Toast.makeText(getApplicationContext(), "Connection error", Toast.LENGTH_SHORT).show();
                break;
                case HANDLE_IMAGE_CLICK:
                {
                	final String src = message.getData().getString("url");
                	if(src == null) // нет картинки!
                		return false;
                	
                    ArrayList<String> itemsBuilder = new ArrayList<String>();
		        	itemsBuilder.add(getString(R.string.image_save));
		        	itemsBuilder.add(getString(R.string.image_copy_url));
		        	itemsBuilder.add(getString(R.string.image_open));

		        	final String[] items = itemsBuilder.toArray(new String[0]);
		        	AlertDialog.Builder builder = new AlertDialog.Builder(mPageBrowser.getContext());
		        	builder.setTitle(R.string.image_action);
		        	builder.setItems(items, new DialogInterface.OnClickListener() 
		        	{
		        	    @SuppressWarnings("deprecation")
						public void onClick(DialogInterface dialog, int item) 
		        	    {
		        	    	switch(item)
		        	    	{
		        	    		case DiaryWebView.IMAGE_SAVE: // save
		        	    		{	        	    			
		                            String hashCode = String.format("%08x", src.hashCode());
		                            File file = new File(new File(getCacheDir(), "webviewCache"), hashCode);
		                            if(file.exists())
		                            {
		                                String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(src);
		                                String realName = URLUtil.guessFileName(src, null, fileExtenstion);
		                                CacheManager.saveDataToSD(getApplicationContext(), realName, file);
		                            }
		        	    		}
		        	    		break;
		        	    		case DiaryWebView.IMAGE_COPY_URL: // copy
		        	    		{
		                            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		                            Toast.makeText(DiaryList.this, getString(R.string.copied) + " " + src, Toast.LENGTH_SHORT).show();
		                            clipboard.setText(src);
		        	    		}
		        	    		break;
		        	    		case DiaryWebView.IMAGE_OPEN: // open Link
		        	    		{
		                        	String hashCode = String.format("%08x", src.hashCode());
		                            File file = new File(new File(getCacheDir(), "webviewCache"), hashCode);
		                            if(file.exists())
		                            {
		                            	BitmapDrawable sendDrawable = (BitmapDrawable) BitmapDrawable.createFromPath(file.getAbsolutePath());
		                            	sendDrawable.setBounds(0, 0, sendDrawable.getIntrinsicWidth(), sendDrawable.getIntrinsicHeight());
		                            	Globals.tempDrawable = sendDrawable;
		                            	Intent intent = new Intent(getApplicationContext(), ImageViewer.class);
		                                startActivity(intent);
		                            }
		        	    		}
		        	    		break;
		        	    	}
		        	    }
		        	});
		        	AlertDialog alert = builder.create();
		        	alert.show();
		        }
            	break;
                default:
                    return false;
            }
            return true;
        }
    };
    
    Handler.Callback WorkerCallback = new Handler.Callback()
    {
        @SuppressWarnings("unchecked")
        public boolean handleMessage(Message message)
        {
            try
            {
                switch (message.what)
                {
                    case HANDLE_SET_HTTP_COOKIE:
                    {
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                        nameValuePairs.add(new BasicNameValuePair("user_login", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
                        nameValuePairs.add(new BasicNameValuePair("user_pass", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));
                        nameValuePairs.add(new BasicNameValuePair("save_on", "1"));
                        
                        HttpResponse page = mDHCL.postPage("http://www.diary.ru/login.php", new UrlEncodedFormEntity(nameValuePairs, "WINDOWS-1251"));
                        if(page == null)
                        {
                        	mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                        	return false;
                        }
                        	
                        String loginScreen = EntityUtils.toString(page.getEntity());
                        
                        if (loginScreen.contains("Если браузер не перенаправляет вас автоматически"))
                        { // login successful
                            CookieManager cookieManager = CookieManager.getInstance();
                            
                            // Sharing cookies between webView and mDHCL
                            List<Cookie> cookies = mDHCL.cookieStore.getCookies();
                            
                            // What the hell is with net Android devs?
                            // Why cookie operations are done through another thread
                            // without any clue like
                            // "onCookiePendingOperationsListener"??
                            // Don't uncomment this line! If you do, cookies'll be added
                            // and only then another thread'll delete them resulting in
                            // having no cookies at all.
                            // cookieManager.removeSessionCookie();
                            
                            for (Cookie cookie : cookies)
                            {
                                String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
                                cookieManager.setCookie("diary.ru", cookieString);
                            }
                            CookieSyncManager.getInstance().sync();
                        }
                        else
                        {
                            mUiHandler.sendEmptyMessage(HANDLE_AUTHORIZATION_ERROR);
                            return false;
                        }
                        
                        mUiHandler.sendEmptyMessage(HANDLE_SET_HTTP_COOKIE);
                        return true;
                    }
                    case HANDLE_GET_DIARIES_DATA:
                    // TODO: Исправить все к чертям!! Поставить строгое извлечение по
                    // столбцам таблицы, идиот!!
                    {
                        String URL = ((Pair<String, Boolean>) message.obj).first;
                        boolean reload = ((Pair<String, Boolean>) message.obj).second;
                        // Если страничка дневников есть в комментах
                        if(mCache.hasPage(URL) && !reload)
                        {
                            mUser.currentDiaries = (DiaryListPage) mCache.loadPageFromCache(URL);
                        }
                        else
                        {
                        	HttpResponse page = mDHCL.postPage(URL, null);
                        	if(page == null)
                            {
                            	mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                            	return false;
                            }
                            String favListPage = EntityUtils.toString(page.getEntity());
                            
                            serializeDiariesPage(favListPage);
                            mCache.putPageToCache(URL, mUser.currentDiaries);
                        }
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_DIARIES_DATA);
                        return true;
                    }
                    case HANDLE_GET_DIARY_PAGE_DATA:
                    {
                        String URL = ((Pair<String, Boolean>) message.obj).first;
                        boolean reload = ((Pair<String, Boolean>) message.obj).second;
                        // Если страничка постов есть в комментах
                        if(mCache.hasPage(URL) && !reload)
                        {
                        	mUser.currentDiaryPage = (DiaryPage) mCache.loadPageFromCache(URL);
                        }
                        else
                        {
                            HttpResponse page = mDHCL.postPage(URL, null);
                        	if(page == null)
                            {
                            	mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                            	return false;
                            }
                            String dataPage = EntityUtils.toString(page.getEntity());
                            int detected = Utils.checkDiaryUrl(dataPage);
                            if(detected == DiaryPage.PAGE_NOT_RECOGNIZED)
                            {
                                mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                                return false;
                            }
                            if(detected == DiaryPage.POST_LIST)
                                serializePostsPage(dataPage, null);
                            else if (detected == DiaryPage.COMMENT_LIST)
                                serializeCommentsPage(dataPage, null);
                            mCache.putPageToCache(Globals.currentURL, mUser.currentDiaryPage);
                        }
                        mUiHandler.sendEmptyMessage(HANDLE_GET_DIARY_PAGE_DATA);
                        return true;
                    }
                    case HANDLE_GET_DISCUSSIONS_DATA:
                    {
                    	HttpResponse page = mDHCL.postPage("http://www.diary.ru/discussion/", null);
                    	if(page == null)
                        {
                        	mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                        	return false;
                        }
                    	String dataPage = EntityUtils.toString(page.getEntity());
                    	serializeDiscussionsPage(dataPage, null); 
                    	
                    	mUiHandler.sendEmptyMessage(HANDLE_GET_DISCUSSIONS_DATA);
                    	return true;
                    }
                    case HANDLE_GET_DISCUSSION_LIST_DATA:
                    {
                    	int pos = ((Pair<Integer, Boolean>) message.obj).first;
                    	boolean onlyNew = ((Pair<Integer, Boolean>) message.obj).second;
                    	
                    	DiscussionList dList = (DiscussionList) mDiscussionBrowser.getExpandableListAdapter().getGroup(pos);
                    	String jsURL = dList.get_URL();
                    	if(onlyNew)
                    	    jsURL = jsURL + "&new";
                    	
                    	HttpResponse page = mDHCL.postPage(jsURL, null);
                        if(page == null)
                        {
                        	mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                        	return false;
                        }
                        String dataPage = EntityUtils.toString(page.getEntity());
                        serializeDiscussions(dataPage, dList.getDiscussions());
                        
                        mUiHandler.sendMessage(mUiHandler.obtainMessage(HANDLE_GET_DISCUSSION_LIST_DATA, pos));
                    	return true;
                    }
                    case HANDLE_PICK_URL:
                    {
                    	String URL = (String) message.obj;
                    	checkUrlAndHandle(URL);
                    	return true;
                    }
                    default:
                        return false;
                }
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            
            return true;
        }
    };
    
    public void onClick(View view)
    {	
        if (view == mExitButton)
        {
        	AlertDialog.Builder builder = new AlertDialog.Builder(mPageBrowser.getContext());
        	builder.setTitle(R.string.really_exit);
        	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() 
        	{
				public void onClick(DialogInterface dialog, int item) 
        	    {
					Editor lysosome = Globals.mSharedPrefs.edit();
		            lysosome.remove(AuthorizationForm.KEY_USERNAME);
		            lysosome.remove(AuthorizationForm.KEY_PASSWORD);
		            lysosome.commit();
		            
		            CookieManager cookieManager = CookieManager.getInstance();
		            cookieManager.removeSessionCookie();
		            CookieSyncManager.getInstance().sync();
		            Globals.mUser = new UserData();
		            
		            //TODO: просмотр без логина тоже еще не введен
		            startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
		            finish();
        	    }
        	});
        	builder.setNegativeButton(android.R.string.no, null);
        	builder.create().show();
        } 
        else if (view.getTag() != null)
        {
            if(view.getParent() instanceof TabWidget)
            {
                int i = (Integer) view.getTag();
                setCurrentTab(i);
            }
            if(view instanceof Button) // нижние панельки
            {
                pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>((String)view.getTag(), false)));
            }
        } else
            switch (view.getId())
            {
            	// Загружаем посты дневника
                case R.id.title:
                {
                    int pos = mDiaryBrowser.getRefreshableView().getPositionForView((View) view.getParent());
                    Diary diary = (Diary) mDiaryBrowser.getRefreshableView().getAdapter().getItem(pos);
                    
                    pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(diary.get_URL(), false)));
                }
                break;
                default:
                    Utils.showDevelSorry(this);
                break;
            }
    }
    

    // Загружаем дискуссии
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) 
	{
		pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
		String link = ((DiscussionList.Discussion) parent.getExpandableListAdapter().getChild(groupPosition, childPosition)).get_URL();
    	mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(link, false)));
		return true;
	}
	
	public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) 
	{
		if(parent.isGroupExpanded(groupPosition))
		{
			parent.collapseGroup(groupPosition);
			return true;
		}
		
		if(((DiscussionList)parent.getExpandableListAdapter().getGroup(groupPosition)).getDiscussions().isEmpty())
		{
			pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
			mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DISCUSSION_LIST_DATA, new Pair<Integer, Boolean>(groupPosition, false)));
		}
		else
			parent.expandGroup(groupPosition);
    	return true;
	}
	
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        if(parent instanceof ExpandableListView)
        {
            ExpandableListView elv = (ExpandableListView) parent;
            if(ExpandableListView.getPackedPositionType(id) != ExpandableListView.PACKED_POSITION_TYPE_GROUP)
                return false;
            
            int groupPosition = ExpandableListView.getPackedPositionGroup(id);
            if(elv.isGroupExpanded(groupPosition))
            {
                elv.collapseGroup(groupPosition);
                return true;
            }
            pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
            mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DISCUSSION_LIST_DATA, new Pair<Integer, Boolean>(groupPosition, true)));
            return true;
        }
        return false;
    }
    
    /*            is.close();
     * (non-Javadoc) Sets the contents to current tab and hides everything other. In addition, refreshes content on
     * page, if needed.
     */
    private void setCurrentTab(int index)
    {
        switch (index)
        {
            case TAB_FAVOURITES:
                pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>("http://www.diary.ru/list/?act=show&fgroup_id=0", false)));
            break;
            case TAB_FAV_POSTS:
                pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(mUser.ownDiaryURL + "?favorite", false)));
            break;
            case TAB_MY_DIARY:
                pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(mUser.ownDiaryURL, false)));
            break;
            case TAB_MY_DIARY_NEW:
            	pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(mUser.newDiaryLink, false)));
            break;
            case TAB_DISCUSSIONS:
            	pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DISCUSSIONS_DATA, null));
            break;
            case TAB_DISCUSSIONS_NEW:
            	pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(mUser.newDiscussLink, false)));
            break;
            default:
            	Utils.showDevelSorry(this);
            break;
        }
        
        mCurrentTab = index;
        mTabHost.setCurrentTab(index);
    }
    
    private void setCurrentVisibleComponent(int needed)
    {   
        mDiaryBrowser.setVisibility(needed == DiaryPage.DIARY_LIST ? View.VISIBLE : View.GONE);
        mPageBrowser.setVisibility(needed == DiaryPage.POST_LIST || needed == DiaryPage.COMMENT_LIST ? View.VISIBLE : View.GONE);
        //mAuthorBrowser.setVisibility(needed == AUTHOR_PAGE ? View.VISIBLE : View.GONE);
        mDiscussionBrowser.setVisibility(needed == DiaryPage.DISCUSSION_LIST ? View.VISIBLE : View.GONE);
        mCurrentBrowser = needed;
    }
    
    private void reloadContent()
    {
    	switch(mUser.currentDiaryPage.getType())
    	{
    	    case DiaryPage.DIARY_LIST:
    	        pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
    	        mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>("http://www.diary.ru/list/?act=show&fgroup_id=0", true)));
    		case DiaryPage.POST_LIST:
    			pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
    			mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(mUser.currentDiaryPage.get_diary_URL(), true)));
    			break;
    		case DiaryPage.COMMENT_LIST:
    			pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
    			mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_PAGE_DATA, new Pair<String, Boolean>(mUser.currentDiaryPage.get_post_URL(), true)));
    			break;
    	}
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        if(mCurrentBrowser == DiaryPage.POST_LIST || mCurrentBrowser == DiaryPage.COMMENT_LIST)
        {
        	WebBackForwardList browseHistory = mPageBrowser.getRefreshableView().copyBackForwardList();
        	ContextThemeWrapper ctw = new ContextThemeWrapper(this, android.R.style.Theme_Black);
        	ScrollView dialogView = new ScrollView(ctw);
        	LinearLayout LL = new LinearLayout(ctw);
        	LL.setOrientation(LinearLayout.VERTICAL);
        	
        	for(int i = 0; i < browseHistory.getSize(); i++)
        	{
        		String url = browseHistory.getItemAtIndex(i).getUrl();
        		if(url.equals("about:blank")) // ну да, это тупо. Но что делать?
        		    continue;
        		    
    			TextView tmpTxt = new TextView(ctw);
    			tmpTxt.setText(i + ") " + browseHistory.getItemAtIndex(i).getTitle());
    			tmpTxt.setTag(url);
    			tmpTxt.setMaxLines(1);
    			tmpTxt.setPadding(5, 5, 5, 5);
    			tmpTxt.setTextAppearance(ctw, android.R.style.TextAppearance_Large);
    			tmpTxt.setOnClickListener(new OnClickListener()
				{
					
					public void onClick(View v)
					{
						String url = (String) v.getTag();
	        	    	checkUrlAndHandle(url);
					}
				});
    			TextView tmpDescTxt = new TextView(ctw);
    			tmpDescTxt.setText(url);
    			LL.addView(tmpTxt);
    			LL.addView(tmpDescTxt);
        	}
        	dialogView.addView(LL);
        	AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
        	builder.setTitle(R.string.image_action);
        	builder.setView(dialogView);
        	builder.create().show();
        }
        else
        	super.onBackPressed();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSearchRequested()
     */
    public void serializeDiariesPage(String dataPage)
    {
        mUser.currentDiaries = new DiaryListPage();
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        if(mListener != null)
        {
            mListener.parseData(rootNode);
            mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
        }
            
        Element table = rootNode.getElementsByAttributeValue("class", "table r").first();
        if(table == null) // Нет вообще никаких дневников, заканчиваем
        	return;
        
        Element pages = rootNode.select("table.pages").first();
        if(pages != null)
            mUser.currentDiaries.setPageLinks(Html.fromHtml(pages.outerHtml()));
        
        Elements rows = table.getElementsByTag("td");
        Element title = null, author = null, last_post = null;
        for (int i = 0; i < rows.size(); ++i)
        {
            if (title == null && rows.get(i).hasClass("l"))
                title = rows.get(i).getElementsByClass("withfloat").first();

            if (author == null)
                author = rows.get(i).getElementsByAttributeValue("target", "_blank").first();
            
            if (last_post == null)
                if (rows.get(i).className().equals(""))
                    last_post = rows.get(i).getElementsByClass("withfloat").first();
            
            if (title != null && author != null && last_post != null)
            {
                Diary diary = new Diary();
                diary.set_title(title.getElementsByTag("b").text());
                diary.set_URL(title.attr("href"));
                
                diary.set_author(author.text());
                String authorData = author.attr("href");
                diary.set_author_URL(authorData);
                diary.set_ID(authorData.substring(authorData.lastIndexOf("?") + 1));
                
                diary.set_last_post(last_post.text());
                diary.set_last_post_URL(last_post.attr("href"));
                
                mUser.currentDiaries.add(diary);
                title = author = last_post = null;
            }
        }
    }
    
    @Override
    public boolean onSearchRequested()
    {
        int visibility = mTabHost.getTabWidget().getVisibility();
        if(visibility == View.GONE)
            mTabHost.getTabWidget().setVisibility(View.VISIBLE);
        else
            mTabHost.getTabWidget().setVisibility(View.GONE);
        
        return super.onSearchRequested();
    }
    
    public void serializePostsPage(String dataPage, List<Post> destination) throws IOException
    {
        mUser.currentDiaryPage = new DiaryPage();
        mUser.currentDiaryPage.setType(DiaryPage.POST_LIST);
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
    	Document rootNode = Jsoup.parse(dataPage);
    	
        if(mListener != null)
        {       	
        	mListener.parseData(rootNode);
            mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
        }
        
        mUser.currentDiaryPage.set_diary_URL(Globals.currentURL);
        Element diaryTag = rootNode.select("[id=authorName]").first();
        if(diaryTag != null)
        {
            String Id = diaryTag.getElementsByTag("a").last().attr("href");
            mUser.currentDiaryPage.set_diary_Id(Id.substring(Id.lastIndexOf("?") + 1));
        }
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS_2);
        Elements postsArea = rootNode.select("[id=postsArea] > [id=epigraph], [id=postsArea] > [id^=post], div.pageBar");
        if(postsArea == null) // Нет вообще никаких постов, заканчиваем
        	return;
        
        Elements result = postsArea.clone();
        Document resultPage = Document.createShell(Globals.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
        {
            resultPage.body().appendChild(to);
        }
        
        parseContent(resultPage);
        
        mUser.currentDiaryPage.set_content(resultPage);
    }
    
    private void parseContent(Document resultPage)
	{
        resultPage.head().append("<link rel=\"stylesheet\" href=\"file:///android_asset/css/journal.css\" type=\"text/css\" media=\"all\" title=\"Стандарт\"/>");
    	if(!load_images)
        {
            Elements images = resultPage.select("img[src^=http]");
            int i = 0;
            for(Element image : images)
            {
                //String width = image.attr("width");
                //String height = image.attr("height");
                String src = image.attr("src");
                if(!src.contains("diary.ru") && !image.parent().className().equals("avatar"))
                {
                    String jsButton = "<input type=\"image\" id=\"imageLoader" + i + "\" src=\"file:///android_res/drawable/load_image.png\" onclick=\"return handleIMGDown('" + i + "', '"+ image.attr("src") +"')\"/>";
                    
                    image.after(jsButton);
                    image.remove();

                    i++;
                }
            }
        }
    	resultPage.body().append(Utils.javascriptContent);
	}

	public void serializeCommentsPage(String dataPage, List<Post> destination) throws IOException
    {
    	mUser.currentDiaryPage = new DiaryPage();
    	mUser.currentDiaryPage.setType(DiaryPage.COMMENT_LIST);
    	
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        
        if(mListener != null)
        {
            mListener.parseData(rootNode);
            mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
        }
        
        mUser.currentDiaryPage.set_diary_URL(Globals.currentURL.substring(0, Globals.currentURL.lastIndexOf('/') + 1));
        Element diaryTag = rootNode.select("[id=authorName]").first();
        if(diaryTag != null)
        {
            String Id = diaryTag.getElementsByTag("a").last().attr("href");
            mUser.currentDiaryPage.set_diary_Id(Id.substring(Id.lastIndexOf("?") + 1));
        }
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS_2);
        Elements effectiveAreas = rootNode.select("[id=postsArea] > [id^=post], [id=commentsArea] > [id^=comment], div.pageBar");
        if(effectiveAreas == null) // Нет вообще никаких постов, заканчиваем
            return;
        
        Elements result = effectiveAreas.clone();
        Element urlNode = result.first().getElementsByClass("postLinksBackg").first();
        if (urlNode != null)
        {
            String postURL = urlNode.getElementsByTag("a").attr("href");
            mUser.currentDiaryPage.set_post_URL(postURL);
            mUser.currentDiaryPage.set_post_ID(postURL.substring(postURL.lastIndexOf('p') + 1, postURL.lastIndexOf('.')));
        }
        Document resultPage = Document.createShell(Globals.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
        {
            resultPage.body().appendChild(to);
        }
        
        parseContent(resultPage);
        
        mUser.currentDiaryPage.set_content(resultPage);
    }
    
    public void serializeDiscussionsPage(String dataPage, List<DiscussionList> destination)
    {
    	mUser.discussions.clear();
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        
        if(mListener != null)
        {
            mListener.parseData(rootNode);
            mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
        }
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS_2);
        Element dIndex = rootNode.getElementById("all_bits");
        for (Element item : dIndex.getElementsByTag("h3"))
        {
        	DiscussionList currentList = new DiscussionList();
        	Element newPosts = item.getElementsByTag("em").first();
        	currentList.set_last_post(newPosts.text());
        	newPosts.remove();
        	
        	String link = item.getElementsByAttributeValueStarting("href", "/discussion/?open[]").attr("href") + "&js";
        	currentList.set_URL(link);
        	String title = item.text();
        	currentList.set_title(title);
        	
        	mUser.discussions.add(currentList);
        }
    }
    
    public void serializeDiscussions(String dataPage, ArrayList<DiscussionList.Discussion> destination)
    {
    	destination.clear();
    	dataPage = dataPage.replace("\\\"", "\"");
        Document rootNode = Jsoup.parse(dataPage);
        Elements tables = rootNode.getElementsByTag("em");
        tables.remove();
        
        for (Element discussion : rootNode.getElementsByTag("a"))
        {
        	DiscussionList.Discussion currentDisc = new DiscussionList.Discussion();
        	String link = discussion.attr("href");
        	currentDisc.set_URL(link);
        	String title = discussion.text();
        	currentDisc.set_title(title);
        	String date = discussion.previousElementSibling().text();
        	currentDisc.set_date(date);
        	
        	
        	destination.add(currentDisc);
        }
    }
    
    
    
    public void newPostPost()
    {
        if(mUser.currentDiaryPage.get_diary_Id().equals(""))
            return;
        
        Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
        postIntent.putExtra("DiaryId", mUser.currentDiaryPage.get_diary_Id());
        postIntent.putExtra("signature", mUser.signature);
        
        startActivity(postIntent);
    }
    
    public void newCommentPost()
    {
    	if(mUser.currentDiaryPage.get_post_ID().equals(""))
            return;
    	
    	Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
    	
        postIntent.putExtra("PostId", mUser.currentDiaryPage.get_post_ID());
        postIntent.putExtra("signature", mUser.signature);
        
        startActivity(postIntent);
    }
    
    public void checkUrlAndHandle(String URL)
    {   
    	int handled = DiaryPage.PAGE_NOT_PROCESSED;
    	DiaryPage cachedPage = null;
    	String dataPage = null;
    	
    	try 
    	{
	    	if(mCache.hasPage(URL))
	    	{
	    		cachedPage = (DiaryPage) mCache.loadPageFromCache(URL);
	    		handled = cachedPage.getType();
	    	}
	    	else
	    	{
		    	HttpResponse page = mDHCL.postPage(URL, null);
		    	if(page == null)
		    	{
		    		mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
		    		return;
		    	}
		    	dataPage = EntityUtils.toString(page.getEntity());
				handled = Utils.checkDiaryUrl(dataPage);
	    	}

			if(handled != DiaryPage.PAGE_NOT_RECOGNIZED) // Если это страничка дайри
	    	{
	    		switch(handled)
	    		{
	    			case DiaryPage.DIARY_LIST:
	    			break;
	    			case DiaryPage.POST_LIST:
	    				if(cachedPage != null)
                        	mUser.currentDiaryPage = (DiaryPage) mCache.loadPageFromCache(URL);
	    				else
	    				{
		    				serializePostsPage(dataPage, null);
		    				mCache.putPageToCache(Globals.currentURL, mUser.currentDiaryPage);
	    				}
	    				mUiHandler.sendEmptyMessage(HANDLE_GET_DIARY_PAGE_DATA);
	    			break;
	    			case DiaryPage.COMMENT_LIST:
	    				if(cachedPage != null)
                        	mUser.currentDiaryPage = (DiaryPage) mCache.loadPageFromCache(URL);
	    				else
	    				{
		    				serializeCommentsPage(dataPage, null);
		    				mCache.putPageToCache(Globals.currentURL, mUser.currentDiaryPage);
	    				}
	    				mUiHandler.sendEmptyMessage(HANDLE_GET_DIARY_PAGE_DATA);
	    			break;
	    		}
	    	}
			else
			{
				pd.dismiss();
				Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
				//startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)));
				startActivity(sendIntent);
			}
		} 
    	catch (Exception e) 
		{
    		mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
			e.printStackTrace();
		}
    	
    	return;
    }
    
    public void updateMaxCacheSize(SharedPreferences prefs)
    {
    	try
        {
        	CacheManager.MAX_SIZE = Integer.valueOf(prefs.getString("cache.size", "5")) * 1048576L;
        }
        catch (NumberFormatException e)
        {
        	CacheManager.MAX_SIZE = 5 * 1048576L;
        }
    }

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
	{
		if(key.equals("images.autoload"))
		{
			load_images = sharedPreferences.getBoolean(key, false);
		}
		else if(key.equals("cache.size"))
		{
			updateMaxCacheSize(sharedPreferences);
		}
		else if(key.equals("images.autoload.cache"))
		{
			load_cached = sharedPreferences.getBoolean(key, false);
		}
	}

	public void onRefresh(PullToRefreshBase<ListView> refreshView)
    {
        switch (refreshView.getId())
        {
            case R.id.diary_browser:
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>("http://www.diary.ru/list/?act=show&fgroup_id=0", true)));
            break;
        }
    }
}
