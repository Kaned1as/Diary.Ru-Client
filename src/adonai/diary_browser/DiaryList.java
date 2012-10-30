package adonai.diary_browser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.FastHtmlSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.CommentListArrayAdapter;
import adonai.diary_browser.entities.Diary;
import adonai.diary_browser.entities.DiaryListArrayAdapter;
import adonai.diary_browser.entities.Post;
import adonai.diary_browser.entities.PostListArrayAdapter;
import adonai.diary_browser.preferences.PreferencesScreen;
import adonai.diary_browser.tags.MoreTag;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Pair;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

public class DiaryList extends Activity implements OnClickListener, OnSharedPreferenceChangeListener
{
    public interface onUserDataParseListener
    {
        public void parseData(Element rootNode);
        public boolean updateNeeded();
        public void updateCurrentDiary(Element element);
        public void updateCurrentPost(Post post);
    }
    
    
    public void setUserDataListener(onUserDataParseListener listener)
    {
        this.listener = listener;
    }
	
    // Команды хэндлерам
    private static final int HANDLE_AUTHORIZATION_ERROR = 0;
    private static final int HANDLE_START = 1;
    private static final int HANDLE_SET_HTTP_COOKIE = 2;
    private static final int HANDLE_GET_FAVORITES_COMMUNITIES_DATA = 3;
    private static final int HANDLE_GET_DIARY_POSTS_DATA = 4;
    private static final int HANDLE_GET_POST_COMMENTS_DATA = 5;
    private static final int HANDLE_GET_FAVORITE_POSTS_DATA = 6;
    private static final int HANDLE_PROGRESS = 7;
    private static final int HANDLE_PROGRESS_2 = 8;
    private static final int HANDLE_PICK_URL = 9;
    private static final int HANDLE_UPDATE_HEADERS = 10;
    
    // дополнительные команды хэндлерам
    private static final int HANDLE_SERVICE_RELOAD_CONTENT = 100;
    
    // вкладки приложения
    public static final int TAB_FAVOURITES = 0;
    public static final int TAB_FAV_POSTS = 1;
    public static final int TAB_MY_DIARY = 2;
    public static final int TAB_MY_DIARY_NEW = 3;
    public static final int TAB_DISCUSSIONS = 4;
    public static final int TAB_DISCUSSIONS_NEW = 5;
    
    // текущий контекст
    public static final int DIARY_LIST = 0;
    public static final int POST_LIST = 1;
    public static final int COMMENT_LIST = 2;
    public static final int AUTHOR_PAGE = 3;
    
    // TODO: доделать обновление текущего контента по запросу
    boolean mNeedsRefresh = true;
    int mCurrentBrowser = 0;
    int mCurrentTab = 0;
    
    // Адаптеры типов
    DiaryListArrayAdapter mFavouritesAdapter;
    PostListArrayAdapter mPostListAdapter;
    PostListArrayAdapter mFavPostListAdapter;
    PostListArrayAdapter mOwnDiaryPostListAdapter;
    CommentListArrayAdapter mCommentListAdapter;
    
    // Видимые объекты
    TextView mLogin;
    TextView mDiscussNum;
    TextView mCommentsNum;
    ListView mFavouriteBrowser;
    ListView mPostBrowser;
    ListView mCommentBrowser;
    
    ImageButton mExitButton;
    TabHost mTabHost;
    ProgressDialog pd;
    
    // Сервисные объекты
    DiaryHttpClient mDHCL = Globals.mDHCL;
    HtmlCleaner postCleaner;
    UserData mUser = Globals.mUser;
    onUserDataParseListener listener;
    DisplayMetrics gMetrics;
    Object[] RPCResponse;
    
    SharedPreferences mPreferences;
    boolean load_images;
    boolean load_cached;
    
    static Handler mHandler, mUiHandler;
    Looper mLooper; // петля времени
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);      
        postCleaner = new HtmlCleaner();
        postCleaner.getProperties().setOmitComments(true);
        
        setUserDataListener(mUser);
        
        // Если был простой приложения
        if(Globals.mSharedPrefs == null)
        	Globals.mSharedPrefs = getApplicationContext().getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
        
        mPreferences = Globals.mSharedPrefs;
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
        
        mFavouriteBrowser = (ListView) findViewById(R.id.favourite_browser);
        mPostBrowser = (ListView) findViewById(R.id.post_browser);
        mCommentBrowser = (ListView) findViewById(R.id.comment_browser);
        
        mLogin = (TextView) findViewById(R.id.login_name);
        mLogin.setText(mUser.userName);
        
        // Также устаревший код, оставлен в целях тестирования
        
        mExitButton = (ImageButton) findViewById(R.id.exit_button);
        mExitButton.setOnClickListener(this);
        
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        
        // Когда мы добавляем несколько табов с одинаковым содержимым, необходимо в конце сделать нужную видимой.
        // Я открыл это странное свойство, когда копался в исходниках Андроида
        mTabHost.addTab(mTabHost.newTabSpec("tab_favourites").setIndicator(getString(R.string.favourites)).setContent(R.id.generic_tab_content));
        mTabHost.addTab(mTabHost.newTabSpec("tab_posts").setIndicator(getString(R.string.posts)).setContent(R.id.generic_tab_content));
        mTabHost.addTab(mTabHost.newTabSpec("tab_owndiary").setIndicator(getString(R.string.my_diary)).setContent(R.id.generic_tab_content));
        mTabHost.addTab(mTabHost.newTabSpec("tab_owndiary").setIndicator("").setContent(R.id.generic_tab_content));
        mTabHost.addTab(mTabHost.newTabSpec("tab_discussions").setIndicator(getString(R.string.discussions)).setContent(R.id.generic_tab_content));
        mTabHost.addTab(mTabHost.newTabSpec("tab_discussions_newest").setIndicator("").setContent(R.id.generic_tab_content));
        mTabHost.getCurrentView().setVisibility(View.VISIBLE);
        
        // Дополнительные настройки для маленьких вкладок отображения новых комментариев
        mCommentsNum = (TextView) mTabHost.getTabWidget().getChildTabViewAt(TAB_MY_DIARY_NEW).findViewById(android.R.id.title);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mTabHost.getTabWidget().getChildTabViewAt(TAB_MY_DIARY_NEW).getLayoutParams();
		lp.weight = 0; lp.width = (int)(30 / gMetrics.density);
		mDiscussNum = (TextView) mTabHost.getTabWidget().getChildTabViewAt(TAB_DISCUSSIONS_NEW).findViewById(android.R.id.title);
		lp = (LinearLayout.LayoutParams) mTabHost.getTabWidget().getChildTabViewAt(TAB_DISCUSSIONS_NEW).getLayoutParams();
		lp.weight = 0; lp.width = (int)(30 / gMetrics.density);
        
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
        
        mFavouritesAdapter = new DiaryListArrayAdapter(this, android.R.layout.simple_list_item_1, mUser.favorites);
        mFavouriteBrowser.setAdapter(mFavouritesAdapter);
        mPostListAdapter = new PostListArrayAdapter(this, android.R.layout.simple_list_item_1, mUser.currentDiaryPosts);
        mFavPostListAdapter = new PostListArrayAdapter(this, android.R.layout.simple_list_item_1, mUser.favoritePosts);
        mOwnDiaryPostListAdapter = new PostListArrayAdapter(this, android.R.layout.simple_list_item_1, mUser.ownDiaryPosts);
        mPostBrowser.setAdapter(mPostListAdapter);
        mCommentListAdapter = new CommentListArrayAdapter(this, android.R.layout.simple_list_item_1, mUser.currentPostComments);
        mCommentBrowser.setAdapter(mCommentListAdapter);
        
        mTabHost.setCurrentTab(mCurrentTab);
        setCurrentVisibleComponent(mCurrentBrowser);
        
        mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_diary_list_a);
        initializeUI();
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
	public boolean onPrepareOptionsMenu(Menu menu) {
        if(mCurrentBrowser == POST_LIST)
        	menu.findItem(R.id.menu_new_post).setVisible(true);
        else
        	menu.findItem(R.id.menu_new_post).setVisible(false);
        
        if(mCurrentBrowser == COMMENT_LIST)
        	menu.findItem(R.id.menu_new_comment).setVisible(true);
        else
        	menu.findItem(R.id.menu_new_comment).setVisible(false);
        
		return super.onPrepareOptionsMenu(menu);
	}

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
            default:
                return super.onOptionsItemSelected(item);
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
                case HANDLE_SERVICE_RELOAD_CONTENT:
                    ((PostListArrayAdapter) mPostBrowser.getAdapter()).notifyDataSetChanged();
                    mCommentListAdapter.notifyDataSetChanged();
                break;
                case HANDLE_SET_HTTP_COOKIE:
                    pd.setMessage(getString(R.string.getting_user_info));
                    mLogin.setText(Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
                    mHandler.sendEmptyMessage(HANDLE_GET_FAVORITES_COMMUNITIES_DATA);
                break;
                case HANDLE_GET_FAVORITES_COMMUNITIES_DATA:
                    setCurrentVisibleComponent(DIARY_LIST);
                    mFavouritesAdapter.notifyDataSetChanged();
                    pd.dismiss();
                break;
                case HANDLE_GET_DIARY_POSTS_DATA:
                    setCurrentVisibleComponent(POST_LIST);
                    mPostBrowser.setAdapter(mPostListAdapter);
                    pd.dismiss();
                break;
                case HANDLE_GET_POST_COMMENTS_DATA:
                    setCurrentVisibleComponent(COMMENT_LIST);
                	mCommentListAdapter.notifyDataSetChanged();
                    pd.dismiss();
                break;
                case HANDLE_GET_FAVORITE_POSTS_DATA:
                    setCurrentVisibleComponent(POST_LIST);
                    mPostBrowser.setAdapter(mFavPostListAdapter);
                    pd.dismiss();
                break;
                case HANDLE_AUTHORIZATION_ERROR:
                    pd.dismiss();
                    Toast.makeText(getApplicationContext(), "Not authorized, retry!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
                    finish();
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
                    case HANDLE_SERVICE_RELOAD_CONTENT:
                    {
                        Pair<Spannable, ImageSpan> pair = (Pair<Spannable, ImageSpan>)message.obj;
                        final int start = pair.first.getSpanStart(pair.second);
                        final int end = pair.first.getSpanEnd(pair.second);
                        if(start == -1 || end == -1) // удалена
                            return false;
                        
                        Pair<String, BitmapDrawable> result = Utils.loadImage(DiaryList.this, pair.second.getSource());
                        if(result == null) // нет такой картинки
                            return false;
                        
                        Drawable loadedPicture = result.second;
                        String fileName = result.first;
                        // Масштабируем под экран
                        if(loadedPicture.getIntrinsicWidth() > gMetrics.widthPixels)
                            loadedPicture.setBounds(0, 0, gMetrics.widthPixels, loadedPicture.getIntrinsicHeight() * gMetrics.widthPixels / loadedPicture.getIntrinsicWidth());
                        else
                            loadedPicture.setBounds(0, 0, loadedPicture.getIntrinsicWidth(), loadedPicture.getIntrinsicHeight());
                                                
                        pair.first.removeSpan(pair.second);
                        pair.first.setSpan(new ImageSpan(loadedPicture, fileName, ImageSpan.ALIGN_BASELINE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        
                        // Если обрабатываем последний запрос
                        if(!mHandler.hasMessages(HANDLE_SERVICE_RELOAD_CONTENT))
                        	mUiHandler.sendMessage(mUiHandler.obtainMessage(HANDLE_SERVICE_RELOAD_CONTENT));
                    }
                    break;
                    case HANDLE_SET_HTTP_COOKIE:
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                        nameValuePairs.add(new BasicNameValuePair("user_login", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
                        nameValuePairs.add(new BasicNameValuePair("user_pass", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));
                        nameValuePairs.add(new BasicNameValuePair("save_on", "1"));
                        
                        mDHCL.postPage("http://www.diary.ru/login.php", new UrlEncodedFormEntity(nameValuePairs, "WINDOWS-1251"));
                        String loginScreen = EntityUtils.toString(mDHCL.response.getEntity());
                        
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
                    case HANDLE_GET_FAVORITES_COMMUNITIES_DATA:
                    // TODO: Исправить все к чертям!! Поставить строгое извлечение по
                    // столбцам таблицы, идиот!!
                    {
                        mDHCL.postPage("http://www.diary.ru/list/?act=show&fgroup_id=0", null);
                        String favListPage = EntityUtils.toString(mDHCL.response.getEntity());
                        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
                        Document rootNode = Jsoup.parse(favListPage);
                        if(listener != null)
                        {
                            listener.parseData(rootNode);
                            mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
                        }
                            
                        Element table = rootNode.getElementsByAttributeValue("class", "table r").first();
                        Elements rows = table.getElementsByTag("td");
                        mUser.favorites.clear();
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
                            	diary.setTitle(title.getElementsByTag("b").text());
                                diary.setDiaryUrl(title.attr("href"));
                                
                                diary.setAuthor(author.text());
                                String authorData = author.attr("href");
                                diary.setAuthorUrl(authorData);
                                diary.set_ID(authorData.substring(authorData.lastIndexOf("?") + 1));
                                
                                diary.setLastPost(last_post.text());
                                diary.setLastPostUrl(last_post.attr("href"));
                                
                                mUser.favorites.add(diary);
                                title = author = last_post = null;
                            }
                        }
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_FAVORITES_COMMUNITIES_DATA);
                        return true;
                    }
                    case HANDLE_GET_DIARY_POSTS_DATA:
                    {
                        String URL = (String) message.obj;
                        mDHCL.postPage(URL, null);
                        String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
                        
                        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
                        
                        serializePostsPage(dataPage, null);
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_DIARY_POSTS_DATA);
                        return true;
                    }
                    case HANDLE_GET_POST_COMMENTS_DATA:
                    {
                        String URL = (String)message.obj;
                        
                    	mDHCL.postPage(URL, null);
                        String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
                        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
                        
                        serializeCommentsPage(dataPage, null);
                          
                        mUiHandler.sendEmptyMessage(HANDLE_GET_POST_COMMENTS_DATA);
                    	return true;
                    }
                    case HANDLE_GET_FAVORITE_POSTS_DATA:
                    {
                        mUser.favoritePosts.clear();
                        String URL = mUser.ownDiaryURL + "?favorite";
                        mDHCL.postPage(URL, null);
                        String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
                        
                        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
                        
                        serializePostsPage(dataPage, mUser.favoritePosts);       
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_FAVORITE_POSTS_DATA);
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
    
    private PostContentBuilder makeContent(Element contentNode) throws IOException
    {
        // Ищем тэги MORE
        MoreTag moreTag = null;
        // Есть
        if(contentNode.getElementsByAttribute("ondblclick").size() > 0)
            moreTag = makeMoreTag(contentNode, null);
        
        PostContentBuilder post = new PostContentBuilder(Html.fromHtml(contentNode.html(), new DiaryImageGetter(), null), moreTag);
        formatText(post);
        
        return post;
    }
    
    private MoreTag makeMoreTag(Element contentNode, MoreTag parent) throws IOException
    {
        MoreTag result = new MoreTag(parent);
        for(; ;)
        {
        	Element moreNode = contentNode.children().select("[ondblclick]").first();
            if (moreNode == null)
                break;
            
            if(moreNode.children().select("[ondblclick]").first() != null)
                result.addChild(makeMoreTag(moreNode, result));
                
            result.add(Html.fromHtml(moreNode.html(), new DiaryImageGetter(), null));
            moreNode.remove();
        }
        
        
        return result;
    }
    
    // форматируем текст перед выведением в TextView в списках
    private void formatText(final PostContentBuilder contentPart)
    {
        URLSpan[] urlSpans = contentPart.getSpans(0, contentPart.length(), URLSpan.class);
        
        // индекс тэга в списке нужных (т.е. только тех, где есть #more)
        int effective_index = 0;
        for (URLSpan span : urlSpans)
        {
            // Если это действительно нужный тэг
            if(span.getURL().contains("#more"))
            {
            	final int i = effective_index;
                int url_start = contentPart.getSpanStart(span);
                int url_end = contentPart.getSpanEnd(span);
                ClickableSpan more_span = new ClickableSpan()
                {
                    @Override
                    public void onClick(View widget)
                    {
                    	PostContentBuilder onscreenText = contentPart.getRealContainer();
                        int start = onscreenText.getSpanStart(this);
                        int end = onscreenText.getSpanEnd(this);
                        MoreTag content = contentPart.getMore();
                        if(content == null)
                            return;
                        
                        PostContentBuilder hiddenText = new PostContentBuilder(content.get(i), content.getChild(i));
                        formatText(hiddenText);
                        
                        // вставляем содержимое тэга после его названия
                        onscreenText.insert(end, hiddenText);
                        // удаляем кликабельный текст
                        onscreenText.removeSpan(this);
                        // удаляем текст тэга
                        onscreenText.delete(start, end);
                        mUiHandler.sendEmptyMessage(HANDLE_SERVICE_RELOAD_CONTENT);
                    }
                };
                contentPart.removeSpan(span);
                contentPart.setSpan(more_span, url_start, url_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            	effective_index++;
            }
            else // если обычный URL
            {
                int url_start = contentPart.getSpanStart(span);
                int url_end = contentPart.getSpanEnd(span);
                URLSpan url_span = new URLSpan(span.getURL())
                {
                    @Override
                    public void onClick(View widget)
                    {
                    	pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                        mHandler.sendMessage(mHandler.obtainMessage(HANDLE_PICK_URL, getURL()));
                    }
                };
                contentPart.removeSpan(span);
                contentPart.setSpan(url_span, url_start, url_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(span.getURL().contains("diary.ru")) // предположительно, наша страничка
                	contentPart.setSpan(new ForegroundColorSpan(Color.RED), url_start, url_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        
    	ImageSpan[] imageSpans = contentPart.getSpans(0, contentPart.length(), ImageSpan.class);
        for (final ImageSpan span : imageSpans)
        {            
            final String image_src = span.getSource();
            final int start = contentPart.getSpanStart(span);
            final int end = contentPart.getSpanEnd(span);
            
            // Если это смайлик или системное изображение
            // загрузка изображений обрабатывается в сервисном потоке - обязательно!
            
            // Временно отключено - большая потеря памяти
            String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(image_src);
    		String realName = URLUtil.guessFileName(image_src, null, fileExtenstion);
            if (load_images ||  																				// Если включена автозагрузка изображений
            	load_cached && CacheManager.hasData(getApplicationContext(), realName) || 						// если включена автозагрузка из кэша и файл имеется там
            	image_src.contains("static") && !image_src.contains("userdir") && image_src.endsWith("gif")) 	// если это смайлик
            {						/* Тогда качаем картинку сразу */
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_SERVICE_RELOAD_CONTENT, new Pair<Spannable, ImageSpan>(contentPart.getRealContainer(), span)));
            }
            
            // делаем каждую картинку кликабельной
            ClickableSpan imageAction = new ClickableSpan()
            {       
                @Override
                public void onClick(View widget)
                {
                    PostContentBuilder container = contentPart.getRealContainer();
                    if(container.getSpanStart(span) != -1) // если картинка - образец присутствует
                    {
                        mHandler.sendMessage(mHandler.obtainMessage(HANDLE_SERVICE_RELOAD_CONTENT, new Pair<Spannable, ImageSpan>(container, span)));
                        Toast.makeText(DiaryList.this, R.string.loading, Toast.LENGTH_SHORT).show();
                    }
                    else // если картинки уже нет
                    {
                    	// вообще она будет одна, но на всякий случай я оставляю цикл
                    	// Мы назначаем картинку туда же, где размещен текущий спан клика, соответственно, это способ ее разыскать.
                        ImageSpan[] loadedSpans = container.getSpans(container.getSpanStart(this), container.getSpanEnd(this), ImageSpan.class);
                        for(final ImageSpan loadedSpan : loadedSpans)
                        {
                        	ArrayList<String> itemsBuilder = new ArrayList<String>();
                        	itemsBuilder.add(getString(R.string.image_save));
                        	itemsBuilder.add(getString(R.string.image_open));
                        	
                        	// Ищем, ссылается ли картинка куда-нибудь
                        	final URLSpan[] imageURL = container.getSpans(container.getSpanStart(this), container.getSpanEnd(this), URLSpan.class);
                        	if(imageURL.length > 0)
                        		itemsBuilder.add(getString(R.string.image_open_link));
                        		
                        	final String[] items = itemsBuilder.toArray(new String[0]);
                        	AlertDialog.Builder builder = new AlertDialog.Builder(DiaryList.this);
                        	builder.setTitle(R.string.image_action);
                        	builder.setItems(items, new DialogInterface.OnClickListener() 
                        	{
                        	    public void onClick(DialogInterface dialog, int item) 
                        	    {
                        	    	switch(item)
                        	    	{
                        	    		case 0: // save
                        	    			String filename = loadedSpan.getSource();
                        	    			CacheManager.saveDataToSD(getApplicationContext(), filename);
                        	    		break;
                        	    		case 1: // open
                        	    			// Если картинка не нуждается в увеличении...
                                        	if(loadedSpan.getDrawable().getIntrinsicWidth() < gMetrics.widthPixels)
                                        	{
                                        		Toast.makeText(DiaryList.this, getString(R.string.image_fullsize), Toast.LENGTH_SHORT).show();
                                        		return;
                                        	}
                                        	
                                        	Intent intent = new Intent(getApplicationContext(), ImageViewer.class);
                                        	BitmapDrawable sendDrawable = (BitmapDrawable) loadedSpan.getDrawable().getConstantState().newDrawable();
                                        	sendDrawable.setBounds(0, 0, sendDrawable.getIntrinsicWidth(), sendDrawable.getIntrinsicHeight());
                                            Globals.tempDrawable = sendDrawable;
                                            startActivity(intent);
                        	    		break;
                        	    		case 2: // open Link
                        	    			imageURL[0].onClick(getCurrentFocus());
                        	    		break;
                        	    	}
                        	    	dialog.dismiss();
                        	    }
                        	});
                        	AlertDialog alert = builder.create();
                        	alert.show();
                        }
                    }
                }   
            };
            
            // Это хак. Делаем наш кликабельный спэн первым в списке, т.е. более приоритетным
            ClickableSpan[] click_spans = contentPart.getSpans(start, end, ClickableSpan.class);
            for (ClickableSpan c_span : click_spans)
                contentPart.removeSpan(c_span);
            
            contentPart.setSpan(imageAction, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            for (ClickableSpan c_span : click_spans)
                contentPart.setSpan(c_span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
        }
    }
    
    public void onClick(View view)
    {	
        if (view == mExitButton)
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
        else if (view.getTag() != null && view.getParent() instanceof TabWidget)
        {
            int i = (Integer) view.getTag();
            setCurrentTab(i);
        } else
            switch (view.getId())
            {
                case R.id.title:
                {
                    int pos = mFavouriteBrowser.getPositionForView((View) view.getParent());
                    Diary diary = mUser.favorites.get(pos);
                    
                    pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_POSTS_DATA, diary.getDiaryUrl()));
                }
                break;
                case R.id.post_title:
                case R.id.comments_number:
                {
                	int pos = mPostBrowser.getPositionForView((View) view.getParent());
                	Post post = (Post) mPostBrowser.getAdapter().getItem(pos);
                	
                	if(!post.isEpigraph())
                	{
                		pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                		mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_POST_COMMENTS_DATA, post.get_URL()));
                	}
                }
                break;
                default:
                    Utils.showDevelSorry(this);
                break;
            }
    }
    
    /*            is.close();
     * (non-Javadoc) Sets the contents to current tab and hides everything other. In addition, refreshes content on
     * page, if needed.
     */
    private void setCurrentTab(int index)
    {
        if (mNeedsRefresh)
            switch (index)
            {
                case TAB_FAVOURITES:
                    pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                    mHandler.sendEmptyMessage(HANDLE_GET_FAVORITES_COMMUNITIES_DATA);
                break;
                case TAB_FAV_POSTS:
                    pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                    mHandler.sendEmptyMessage(HANDLE_GET_FAVORITE_POSTS_DATA);
                break;
                case TAB_MY_DIARY:
                    pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_POSTS_DATA, mUser.ownDiaryURL));
                break;
                case TAB_MY_DIARY_NEW:
                	pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_POST_COMMENTS_DATA, mUser.newDiaryLink));
                break;
                case TAB_DISCUSSIONS_NEW:
                	pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_POST_COMMENTS_DATA, mUser.newDiscussLink));
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
        mFavouriteBrowser.setVisibility(needed == DIARY_LIST ? View.VISIBLE : View.GONE);
        mPostBrowser.setVisibility(needed == POST_LIST ? View.VISIBLE : View.GONE);
        mCommentBrowser.setVisibility(needed == COMMENT_LIST ? View.VISIBLE : View.GONE);
        //mAuthorBrowser.setVisibility(needed == AUTHOR_PAGE ? View.VISIBLE : View.GONE);
        mCurrentBrowser = needed;
    }
    
    private void reloadContent()
    {
    	switch(mCurrentBrowser)
    	{
    		case POST_LIST:
    			pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
    			mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_POSTS_DATA, mUser.currentDiaryURL));
    			break;
    		case COMMENT_LIST:
    			pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
    			mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_POST_COMMENTS_DATA, mUser.currentPostURL));
    			break;
    	}
    }

    public class DiaryImageGetter implements Html.ImageGetter
	{
		public Drawable getDrawable(String source)
		{
			Drawable loader = getResources().getDrawable(R.drawable.load_image);
            loader.setBounds(0, 0, loader.getIntrinsicWidth(), loader.getIntrinsicHeight());

			return loader;
		}
		
	}


    /* (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        if (mPostBrowser.getVisibility() == View.VISIBLE && mTabHost.getCurrentTab() == TAB_FAVOURITES)
            setCurrentVisibleComponent(DIARY_LIST);
        else if (mCommentBrowser.getVisibility() == View.VISIBLE)
        {
        	// TODO: исправить когда придумается что-то получше
        	// Если мы сразу зашли в список комментов, список постов пустой.
        	if(mUser.currentDiaryPosts.isEmpty()) 
        		setCurrentTab(TAB_FAVOURITES);
        	else
        		setCurrentVisibleComponent(POST_LIST);
        }
        else
            super.onBackPressed();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSearchRequested()
     */
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
        mUser.currentDiaryPosts.clear();
    	Document rootNode = Jsoup.parse(dataPage);

    	
        if(listener != null)
        {
        	listener.updateCurrentDiary(rootNode.select("[id=authorName]").first());
        	
        	listener.parseData(rootNode);
            mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
        }
        
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS_2);
        //TagNode postsArea = rootNode.findElementByAttValue("id", "postsArea", true, true);
        Element postsArea = rootNode.select("[id=postsArea]").first();
        for (Element post : postsArea.children())
        {
            // не парсим пока что ссылки RSS
        	// TODO: Сделать переходы по страницам (и для комментов в том числе)
            if (post.hasAttr("id") && post.className().contains("singlePost"))
            {
                Post currentPost = new Post();
                
            	currentPost.setIsEpigraph(post.id().equals("epigraph"));
            	
            	Element headerNode = post.getElementsByAttributeValue("class", "postTitle header").first();
                if (headerNode != null)
                {
                    currentPost.set_title(headerNode.getElementsByTag("h2").text());
                    if(currentPost.get_title().equals(""))
                        currentPost.set_title(getString(R.string.without_title));
                    currentPost.set_date(headerNode.getElementsByTag("span").attr("title"));
                }
                Element authorNode = post.getElementsByClass("authorName").first();
                if(authorNode != null)
                {
                    currentPost.set_author(authorNode.getElementsByTag("a").last().text());
                    currentPost.set_author_URL(authorNode.getElementsByTag("a").last().attr("href"));
                }
                Element communityNode = post.getElementsByClass("communityName").first();
                if(communityNode != null)
                {
                    currentPost.set_community(communityNode.getElementsByTag("a").text());
                    currentPost.set_community_URL(communityNode.getElementsByTag("a").attr("href"));
                }
                
                Element contentNode = post.getElementsByClass("paragraph").first();
                if(contentNode != null)
                {
                    currentPost.set_text(makeContent(contentNode));
                }
                Element urlNode = post.getElementsByClass("postLinksBackg").first();
                if (urlNode != null)
                {
                	String postURL = urlNode.getElementsByTag("a").attr("href");
                    currentPost.set_URL(postURL);
                    currentPost.set_ID(postURL.substring(postURL.lastIndexOf('p') + 1, postURL.lastIndexOf('.')));
                    Element comment_count = urlNode.getElementsByClass("comments_count_link").first();
                    if(comment_count != null)
                        currentPost.set_comment_count(comment_count.text());
                    else
                        currentPost.set_comment_count(getString(R.string.comments_disabled));
                }
                
                if(destination != null)
                    destination.add(currentPost);
                
                // Всегда заполняем текущие посты
                mUser.currentDiaryPosts.add(currentPost);
            }
        }
    }
    
    public void serializeCommentsPage(String dataPage, List<Post> destination) throws IOException
    {
    	mUser.currentPostComments.clear();
        Document rootNode = Jsoup.parse(dataPage);
        
        if(listener != null)
        {
            listener.parseData(rootNode);
            mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
        }
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS_2);
        
        // сначала получаем первый пост
        
        Element postsArea = rootNode.getElementsByAttributeValue("id", "postsArea").first();
        if(postsArea != null)
        {
            Post currentPost = new Post();
            Element headerNode = postsArea.getElementsByClass("postTitle header").first();
	        if (headerNode != null)
	        {
	            currentPost.set_title(headerNode.getElementsByTag("h2").text());
	            if(currentPost.get_title().equals(""))
	                currentPost.set_title(getString(R.string.without_title));
	            currentPost.set_date(headerNode.getElementsByTag("span").attr("title"));
	        }
	        Element authorNode = postsArea.getElementsByAttributeValue("class", "authorName").first();
	        if(authorNode != null)
	        {
	            currentPost.set_author(authorNode.getElementsByTag("a").last().text());
	            currentPost.set_author_URL(authorNode.getElementsByTag("a").last().attr("href"));
	        }
	        
	        Element contentNode = postsArea.getElementsByAttributeValue("class", "paragraph").first();
	        if(contentNode != null)
	        {
	            currentPost.set_text(makeContent(contentNode));
	        }
	        Element urlNode = postsArea.getElementsByClass("postLinksBackg").first();
            if (urlNode != null)
            {
            	String postURL = urlNode.getElementsByTag("a").attr("href");
                currentPost.set_URL(postURL);
                currentPost.set_ID(postURL.substring(postURL.lastIndexOf('p') + 1, postURL.lastIndexOf('.')));
            }
            if(destination != null)
                destination.add(currentPost);
            
            if(listener != null)
	        	listener.updateCurrentPost(currentPost);
            
	        mUser.currentPostComments.add(currentPost);
        }
        
        // теперь получаем его комменты
        
        Element commentsArea = rootNode.getElementsByAttributeValue("id", "commentsArea").first();
        if(commentsArea == null)
        {
            mUiHandler.sendEmptyMessage(HANDLE_GET_POST_COMMENTS_DATA);
            return;
        }
        
        for (Element comment : commentsArea.getElementsByAttributeValueContaining("class", "singleComment"))
        {
            Comment currentComment = new Comment();
            Element headerNode = comment.getElementsByAttributeValue("class", "postTitle header").first();
            if (headerNode != null)
            {
                currentComment.set_title(headerNode.getElementsByTag("h2").text());
                currentComment.set_date(headerNode.getElementsByTag("span").attr("title"));
            }
            Element authorNode = comment.getElementsByClass("authorName").first();
            if(authorNode != null)
            {
                currentComment.set_author(authorNode.getElementsByTag("a").last().text());
                currentComment.set_author_URL(authorNode.getElementsByTag("a").last().attr("href"));
            }
            Element contentNode = comment.getElementsByClass("paragraph").first();
            if(contentNode != null)
            {
            	currentComment.set_text(makeContent(contentNode));
            }
            Element urlNode = comment.getElementsByClass("postLinksBackg").first();
            if (urlNode != null)
            {
            	currentComment.set_URL(urlNode.getElementsByTag("a").attr("href"));
            }
            
            if(destination != null)
                destination.add(currentComment);
            
            mUser.currentPostComments.add(currentComment);
        }
    }
    
    public void newPostPost()
    {
        if(mUser.currentDiaryId.equals(""))
            return;
        
        Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
        postIntent.putExtra("DiaryId", mUser.currentDiaryId);
        postIntent.putExtra("signature", mUser.signature);
        
        startActivity(postIntent);
    }
    
    public void newCommentPost()
    {
    	if(mUser.currentPostId.equals(""))
            return;
    	
    	Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
    	
        postIntent.putExtra("PostId", mUser.currentPostId);
        postIntent.putExtra("signature", mUser.signature);
        
        startActivity(postIntent);
    }
    
    public void checkUrlAndHandle(String URL)
    {
    	if(mDHCL.postPage(URL, null) == null)
    	{
    		pd.dismiss();
    		return;
    	}
    	
    	int handled = -1;
    	try 
    	{
			String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
			handled = Utils.checkDiaryUrl(dataPage);
			
			if(handled != -1) // Если это страничка дайри
	    	{
	    		switch(handled)
	    		{
	    			case DIARY_LIST:
	    			break;
	    			case POST_LIST:
	    				serializePostsPage(dataPage, null);
	    				mUiHandler.sendEmptyMessage(HANDLE_GET_DIARY_POSTS_DATA);
	    			break;
	    			case COMMENT_LIST:
	    				serializeCommentsPage(dataPage, null);
	    				mUiHandler.sendEmptyMessage(HANDLE_GET_POST_COMMENTS_DATA);
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
}
