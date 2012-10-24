package adonai.diary_browser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.Diary;
import adonai.diary_browser.entities.Post;
import adonai.diary_browser.tags.MoreTag;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
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
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

public class DiaryList extends Activity implements OnClickListener
{
    // Команды хэндлерам
    private static final int HANDLE_AUTHORIZATION_ERROR = 0;
    private static final int HANDLE_START = 1;
    private static final int HANDLE_SET_HTTP_COOKIE = 2;
    private static final int HANDLE_GET_FAVORITES_COMMUNITIES_DATA = 4;
    private static final int HANDLE_GET_DIARY_POSTS_DATA = 5;
    private static final int HANDLE_GET_POST_COMMENTS_DATA = 6;
    private static final int HANDLE_GET_FAVORITE_POSTS_DATA = 7;
    private static final int HANDLE_PROGRESS = 8;
    private static final int HANDLE_PROGRESS_2 = 9;
    
    // дополнительные команды хэндлерам
    private static final int HANDLE_SERVICE_RELOAD_CONTENT = 10;
    
    // вкладки приложения
    public static final int TAB_FAVOURITES = 0;
    public static final int TAB_FAV_POSTS = 1;
    public static final int TAB_MY_DIARY = 2;
    
    // текущий контекст
    private static final int FAVOURITE_LIST = 0;
    private static final int POST_LIST = 1;
    private static final int COMMENT_LIST = 2;
    private static final int AUTHOR_PAGE = 3;
    
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
    
    static Handler mHandler, mUiHandler;
    Looper mLooper; // петля времени
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        postCleaner = new HtmlCleaner();
        postCleaner.getProperties().setOmitComments(true);

        setUserDataListener(mUser);
        
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, WorkerCallback);
        mUiHandler = new Handler(UiCallback);
        
        CookieSyncManager.createInstance(this);

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
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
        mTabHost.addTab(mTabHost.newTabSpec("tab_favourites").setIndicator(getString(R.string.favourites)).setContent(R.id.generic_tab_content));
        mTabHost.addTab(mTabHost.newTabSpec("tab_posts").setIndicator(getString(R.string.posts)).setContent(R.id.generic_tab_content));
        mTabHost.addTab(mTabHost.newTabSpec("tab_owndiary").setIndicator(getString(R.string.my_diary)).setContent(R.id.generic_tab_content));
        mTabHost.getCurrentView().setVisibility(View.VISIBLE);
        
        // UGLY HACK для более тонких табов
        for (int i = 0, count = mTabHost.getTabWidget().getTabCount(); i != count; ++i)
        {
            final View view = mTabHost.getTabWidget().getChildTabViewAt(i);
            view.setOnClickListener(this);
            view.setTag(i);
            
            view.getLayoutParams().height *= 0.50;

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

    
    @Override
    protected void onResume()
    {
    	 super.onResume();
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
                        pd.setMessage(getResources().getString(R.string.parsing_data));
                break;
                case HANDLE_PROGRESS_2:
                    if(pd != null && pd.isShowing())
                        pd.setMessage(getResources().getString(R.string.sorting_data));
                break;
                case HANDLE_SERVICE_RELOAD_CONTENT:
                    ((PostListArrayAdapter) mPostBrowser.getAdapter()).notifyDataSetChanged();
                    mCommentListAdapter.notifyDataSetChanged();
                break;
                case HANDLE_SET_HTTP_COOKIE:
                    pd.setMessage(getResources().getString(R.string.getting_user_info));
                    mLogin.setText(Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
                    mHandler.sendEmptyMessage(HANDLE_GET_FAVORITES_COMMUNITIES_DATA);
                break;
                case HANDLE_GET_FAVORITES_COMMUNITIES_DATA:
                    setCurrentVisibleComponent(FAVOURITE_LIST);
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
                        
                        Drawable loadedPicture = loadImage(pair.second.getSource());
                        if(loadedPicture == null) // нет такой картинки
                            return false;
                        
                        if(loadedPicture.getIntrinsicWidth() > gMetrics.widthPixels)
                            loadedPicture.setBounds(0, 0, gMetrics.widthPixels, loadedPicture.getIntrinsicHeight() * gMetrics.widthPixels / loadedPicture.getIntrinsicWidth());
                        else
                            loadedPicture.setBounds(0, 0, loadedPicture.getIntrinsicWidth(), loadedPicture.getIntrinsicHeight());
                                                
                        pair.first.removeSpan(pair.second);
                        pair.first.setSpan(new ImageSpan(loadedPicture, ImageSpan.ALIGN_BASELINE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        
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
                        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
                        String favListPage = EntityUtils.toString(mDHCL.response.getEntity());
                        
                        TagNode rootNode = postCleaner.clean(favListPage);
                        
                        if(listener != null && listener.updateNeeded())
                            listener.parseData(rootNode);
                            
                        TagNode table = rootNode.findElementByAttValue("class", "table r", true, false);
                        TagNode[] rows = table.getElementsByName("td", true);
                        mUser.favorites.clear();
                        TagNode title = null, author = null, last_post = null;
                        for (int i = 0; i < rows.length; ++i)
                        {
                            if (title == null && rows[i].getAttributeByName("class").equals("l"))
                                title = rows[i].findElementByAttValue("class", "withfloat", true, false);
                            if (author == null)
                                author = rows[i].findElementByAttValue("target", "_blank", true, false);
                            if (last_post == null)
                                if (rows[i].getAttributeByName("class") == null)
                                    last_post = rows[i].findElementByAttValue("class", "withfloat", true, false);
                            
                            if (title != null && author != null && last_post != null)
                            {
                                mUser.favorites.add(new Diary(Html.fromHtml(title.findElementByName("b", false).getText().toString()).toString(), title.getAttributeByName("href"), author.getText().toString(), author.getAttributeByName("href"), last_post.getText().toString(), last_post.getAttributeByName("href")));
                                title = author = last_post = null;
                            }
                        }
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_FAVORITES_COMMUNITIES_DATA);
                        return true;
                    }
                    case HANDLE_GET_DIARY_POSTS_DATA:
                    {
                        String URL = (String) message.obj;
                        
                        serializePostsPage(URL, null);
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_DIARY_POSTS_DATA);
                        return true;
                    }
                    case HANDLE_GET_POST_COMMENTS_DATA:
                    {
                    	mUser.currentPostComments.clear();
                        Post parsingPost = (Post) message.obj;
                        
                        serializeCommentsPage(parsingPost, mUser.currentPostComments);
                          
                        mUiHandler.sendEmptyMessage(HANDLE_GET_POST_COMMENTS_DATA);
                    	return true;
                    }
                    case HANDLE_GET_FAVORITE_POSTS_DATA:
                    {
                        mUser.favoritePosts.clear();
                        String URL = mUser.ownDiaryURL + "?favorite";
                        
                        serializePostsPage(URL, mUser.favoritePosts);       
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_FAVORITE_POSTS_DATA);
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
    
    private PostContentBuilder makeContent(TagNode contentNode) throws IOException
    {
     // То, чем будем выстраивать контент
        //SimpleHtmlSerializer serializer = new SimpleHtmlSerializer(postCleaner.getProperties());
        FastHtmlSerializer serializer = new FastHtmlSerializer(postCleaner.getProperties());
        
        // Ищем тэги MORE
        MoreTag moreTag = null;
        // Есть
        if(contentNode.findElementHavingAttribute("ondblclick", true) != null)
            moreTag = makeMoreTag(contentNode, null);
        
        PostContentBuilder post = new PostContentBuilder(Html.fromHtml(serializer.getAsString(contentNode), new DiaryImageGetter(), null), moreTag);
        formatText(post);
        
        return post;
    }
    
    private MoreTag makeMoreTag(TagNode contentNode, MoreTag parent) throws IOException
    {
        SimpleHtmlSerializer serializer = new SimpleHtmlSerializer(postCleaner.getProperties());
        MoreTag result = new MoreTag(parent);
        for(; ;)
        {
            TagNode moreNode = contentNode.findElementHavingAttribute("ondblclick", true);
            if (moreNode == null)
                break;
            
            if(moreNode.findElementHavingAttribute("ondblclick", true) != null)
                result.addChild(makeMoreTag(moreNode, result));
                
            result.add(Html.fromHtml(serializer.getAsString(moreNode), new DiaryImageGetter(), null));
            moreNode.removeFromTree();
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
        }
        
    	ImageSpan[] imageSpans = contentPart.getSpans(0, contentPart.length(), ImageSpan.class);
        for (final ImageSpan span : imageSpans)
        {            
            final String image_src = span.getSource();
            
            // Если это смайлик или системное изображение
            // загрузка изображений обрабатывается в сервисном потоке - обязательно!
            
            // Временно отключено - большая потеря памяти
            if(image_src.contains("static") && !image_src.contains("userdir") && image_src.endsWith("gif"))
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_SERVICE_RELOAD_CONTENT, new Pair<Spannable, ImageSpan>(contentPart.getRealContainer(), span)));
            
            final int start = contentPart.getSpanStart(span);
            final int end = contentPart.getSpanEnd(span);
            
            // делаем каждую картинку кликабельной
            ClickableSpan image_span = new ClickableSpan()
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
                        ImageSpan[] loadedSpans = container.getSpans(start, end, ImageSpan.class);
                        for(ImageSpan loadedSpan : loadedSpans)
                        {
                        	
                        	// Если картинка не нуждается в увеличении...
                        	if(loadedSpan.getDrawable().getIntrinsicWidth() < gMetrics.widthPixels)
                        		return;
                        	
                        	Intent intent = new Intent(getApplicationContext(), ImageViewer.class);
                        	Drawable sendDrawable = loadedSpan.getDrawable().getConstantState().newDrawable();
                        	sendDrawable.setBounds(0, 0, sendDrawable.getIntrinsicWidth(), sendDrawable.getIntrinsicHeight());
                            Globals.tempDrawable = sendDrawable;
                            startActivity(intent);
                        }
                    }
                }   
            };
            
            ClickableSpan[] click_spans = contentPart.getSpans(start, end, ClickableSpan.class);
            for (ClickableSpan c_span : click_spans)
                contentPart.removeSpan(c_span);
            
            contentPart.setSpan(image_span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
        }
    }
    
    private class PostListArrayAdapter extends ArrayAdapter<Post>
    {
        
        public PostListArrayAdapter(Context context, int textViewResourceId, List<Post> objects)
        {
            super(context, textViewResourceId, objects);
        }
        
        @Override
        public View getView(int pos, View convertView, ViewGroup parent)
        {
            View view;
            Post post = getItem(pos);
            if (convertView == null)
                view = View.inflate(getContext(), R.layout.post_list_item, null);
            else
                view = convertView;
            
            /* ImageButton delete = (ImageButton)view.findViewById(R.id.p_delete); */
            TextView community = (TextView) view.findViewById(R.id.post_community);
            community.setText(post.get_community());
            TextView author = (TextView) view.findViewById(R.id.post_author);
            author.setText(post.get_author());
            author.setOnClickListener(DiaryList.this);
            TextView post_date = (TextView) view.findViewById(R.id.post_date);
            post_date.setText(post.get_date());
            TextView post_content = (TextView) view.findViewById(R.id.post_content);
            post_content.setText(post.get_text());
            post_content.setMovementMethod(LinkMovementMethod.getInstance());
            
            TextView title = (TextView) view.findViewById(R.id.post_title);
            TextView comment_count = (TextView) view.findViewById(R.id.comments_number);
            if(!post.isEpigraph())
            {
            	comment_count.setVisibility(View.VISIBLE);
	            comment_count.setText(getResources().getString(R.string.comments) + " " + post.get_comment_count());
	            comment_count.setOnClickListener(DiaryList.this);
	            
	            title.setVisibility(View.VISIBLE);
	            title.setText(post.get_title());
	            title.setOnClickListener(DiaryList.this);
            }
            else
            {
            	comment_count.setVisibility(View.GONE);
            	title.setVisibility(View.GONE);
            }
            	
                        
            return view;
        }
        
    }
    
    private class CommentListArrayAdapter extends ArrayAdapter<Post>
    {
        
        public CommentListArrayAdapter(Context context, int textViewResourceId, List<Post> objects)
        {
            super(context, textViewResourceId, objects);
        }
        
        @Override
        public View getView(int pos, View convertView, ViewGroup parent)
        {
            View view;
            Post post = getItem(pos);
            if (convertView == null)
                view = View.inflate(getContext(), R.layout.post_list_item, null);
            else
                view = convertView;
            
            /* ImageButton delete = (ImageButton)view.findViewById(R.id.p_delete); */
            TextView title = (TextView) view.findViewById(R.id.post_title);
            title.setText(post.get_title());
            TextView author = (TextView) view.findViewById(R.id.post_author);
            author.setText(post.get_author());
            TextView post_date = (TextView) view.findViewById(R.id.post_date);
            post_date.setText(post.get_date());
            TextView post_content = (TextView) view.findViewById(R.id.post_content);
            post_content.setText(post.get_text());
            
            post_content.setMovementMethod(LinkMovementMethod.getInstance());
            
            return view;
        }
        
    }
    
    private class DiaryListArrayAdapter extends ArrayAdapter<Diary>
    {
        
        public DiaryListArrayAdapter(Context context, int textViewResourceId, List<Diary> objects)
        {
            super(context, textViewResourceId, objects);
        }
        
        @Override
        public View getView(int pos, View convertView, ViewGroup parent)
        {
            View view;
            Diary diary = getItem(pos);
            if (convertView == null)
                view = View.inflate(getContext(), R.layout.diary_list_item, null);
            else
                view = convertView;
            
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(diary.getTitle());
            title.setOnClickListener(DiaryList.this);
            TextView author = (TextView) view.findViewById(R.id.author);
            author.setText(diary.getAuthor());
            author.setOnClickListener(DiaryList.this);
            TextView last_post = (TextView) view.findViewById(R.id.last_post);
            last_post.setText(diary.getLastPost());
            last_post.setOnClickListener(DiaryList.this);
            
            return view;
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
                		mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_POST_COMMENTS_DATA, post));
                	}
                }
                break;
                default:
                    Log.i("TODO", "Sorry, this click action is not yet implemented");
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
                default:
                break;
            }
        
        mCurrentTab = index;
        mTabHost.setCurrentTab(index);
    }
    
    private void setCurrentVisibleComponent(int needed)
    {   
        mFavouriteBrowser.setVisibility(needed == FAVOURITE_LIST ? View.VISIBLE : View.GONE);
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
    			mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_POSTS_DATA, mDHCL.lastURL));
    			break;
    		case COMMENT_LIST:
    			pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
    			mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_POST_COMMENTS_DATA, mUser.currentPostComments.get(0)));
    			break;
    	}
    }
    
    private static Drawable loadImage(String url) 
    {
        try 
        {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "InTextImage");
            return d;
        } 
        catch (Exception e) 
        {
            return null;
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
            setCurrentVisibleComponent(FAVOURITE_LIST);
        else if (mCommentBrowser.getVisibility() == View.VISIBLE)
            setCurrentVisibleComponent(POST_LIST);
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
    
    public void serializePostsPage(String URL, List<Post> destination) throws IOException
    {
        mUser.currentDiaryPosts.clear();
        
        mDHCL.postPage(URL, null);
        String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
        
        TagNode rootNode = postCleaner.clean(dataPage);
        
        if(listener != null)
        {
        	listener.updateIDs(rootNode);
        	
        	if(listener.updateNeeded())
        		listener.parseData(rootNode);
        }
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS_2);
        TagNode postsArea = rootNode.findElementByAttValue("id", "postsArea", true, true);
        for (TagNode post : postsArea.getAllElements(false))
        {
            if (post.getAttributeByName("class") != null && post.getAttributeByName("class").contains("singlePost"))
            {
                Post currentPost = new Post();
                
            	currentPost.setIsEpigraph(post.getAttributeByName("id") != null && post.getAttributeByName("id").equals("epigraph"));
                TagNode headerNode = post.findElementByAttValue("class", "postTitle header", false, true);
                if (headerNode != null)
                {
                    currentPost.set_title(Html.fromHtml(headerNode.findElementByName("h2", false).getText().toString()).toString());
                    if(currentPost.get_title().equals(""))
                        currentPost.set_title(getResources().getString(R.string.without_title));
                    currentPost.set_date(headerNode.findElementByName("span", false).getAttributeByName("title"));
                }
                TagNode authorNode = post.findElementByAttValue("class", "authorName", false, true);
                if(authorNode != null)
                {
                    currentPost.set_author(authorNode.findElementByName("a", false).getText().toString());
                    currentPost.set_author_URL(authorNode.findElementByName("a", false).getAttributeByName("href"));
                }
                TagNode communityNode = post.findElementByAttValue("class", "communityName", false, true);
                if(communityNode != null)
                {
                    currentPost.set_community(Html.fromHtml(communityNode.findElementByName("a", false).getText().toString()).toString());
                    currentPost.set_community_URL(communityNode.findElementByName("a", false).getAttributeByName("href"));
                }
                
                TagNode contentNode = post.findElementByAttValue("class", "paragraph", true, true);
                if(contentNode != null)
                {
                    currentPost.set_text(makeContent(contentNode));
                }
                TagNode urlNode = post.findElementByAttValue("class", "postLinksBackg", false, true);
                if (urlNode != null)
                {
                	String postURL = urlNode.findElementByName("a", true).getAttributeByName("href");
                    currentPost.set_URL(postURL);
                    currentPost.set_ID(postURL.substring(postURL.lastIndexOf('p') + 1, postURL.lastIndexOf('.')));
                    TagNode comment_count = urlNode.findElementByAttValue("class", "comments_count_link", true, true);
                    if(comment_count != null)
                        currentPost.set_comment_count(comment_count.getText().toString());
                    else 
                        currentPost.set_comment_count(getResources().getString(R.string.comments_disabled));
                }
                
                if(destination != null)
                    destination.add(currentPost);
                
                // Всегда заполняем текущие посты
                mUser.currentDiaryPosts.add(currentPost);
            }
        }
    }
    
    public void serializeCommentsPage(Post originalPost, List<Post> destination) throws IOException
    {
        String URL = originalPost.get_URL();
        
    	mDHCL.postPage(URL, null);
        String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
        
        mUser.currentPostComments.add(originalPost);
        TagNode rootNode = postCleaner.clean(dataPage);
        
        if(listener != null && listener.updateNeeded())
            listener.parseData(rootNode);
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS_2);
        TagNode commentsArea = rootNode.findElementByAttValue("id", "commentsArea", true, true);
        if(commentsArea == null)
        {
            mUiHandler.sendEmptyMessage(HANDLE_GET_POST_COMMENTS_DATA);
            return;
        }
        
        for (TagNode comment : commentsArea.getAllElements(false))
        {
            if (comment.getAttributeByName("class") != null && comment.getAttributeByName("class").contains("singleComment"))
            {
                Comment currentPost = new Comment();
                TagNode headerNode = comment.findElementByAttValue("class", "postTitle header", false, true);
                if (headerNode != null)
                {
                    currentPost.set_title(headerNode.findElementByName("h2", false).getText().toString());
                    currentPost.set_date(headerNode.findElementByName("span", false).getAttributeByName("title"));
                }
                TagNode authorNode = comment.findElementByAttValue("class", "authorName", false, true);
                if(authorNode != null)
                {
                    currentPost.set_author(authorNode.findElementByName("a", false).getText().toString());
                    currentPost.set_author_URL(authorNode.findElementByName("a", false).getAttributeByName("href"));
                }
                TagNode contentNode = comment.findElementByAttValue("class", "paragraph", true, true);
                if(contentNode != null)
                {
                	currentPost.set_text(makeContent(contentNode));
                }
                TagNode urlNode = comment.findElementByAttValue("class", "postLinksBackg", false, true);
                if (urlNode != null)
                {
                	currentPost.set_URL(urlNode.findElementByName("a", true).getAttributeByName("href"));
                }
                mUser.currentPostComments.add(currentPost);
            }   
        }
    }
    
    public void setUserDataListener(onUserDataParseListener listener)
    {
        this.listener = listener;
    }
    
    public interface onUserDataParseListener
    {
        public void parseData(TagNode tag);
        public boolean updateNeeded();
        public void updateIDs(TagNode tag);
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
    	if(mUser.currentDiaryId.equals(""))
            return;
    	
    	Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
    	
        postIntent.putExtra("PostId", mUser.currentPostComments.get(0).get_ID());
        postIntent.putExtra("signature", mUser.signature);
        
        startActivity(postIntent);
    }
}
