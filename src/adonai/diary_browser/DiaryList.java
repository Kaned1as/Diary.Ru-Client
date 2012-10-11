package adonai.diary_browser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;

import de.timroes.axmlrpc.XMLRPCException;

import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.Diary;
import adonai.diary_browser.entities.Post;
import adonai.metaweblog_client.JMetaWeblogClient;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
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
import android.widget.BaseAdapter;
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
    private static final int HANDLE_GET_U_BLOGS = 1;
    private static final int HANDLE_SET_HTTP_COOKIE = 2;
    private static final int HANDLE_GET_USER_PARAMS = 3;
    private static final int HANDLE_GET_FAVORITES_COMMUNITIES_DATA = 4;
    private static final int HANDLE_GET_DIARY_POSTS_DATA = 5;
    private static final int HANDLE_GET_POST_COMMENTS_DATA = 6;
    private static final int HANDLE_GET_FAVORITE_POSTS_DATA = 7;
    private static final int HANDLE_GET_OWNDIARY_POSTS_DATA = 8;
    
    // дополнительные команды хэндлерам
    private static final int HANDLE_SERVICE_LOAD_IMAGE = 10;
    
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
    
    // Адаптеры типов
    DiaryListArrayAdapter mFavouritesAdapter;
    PostListArrayAdapter mPostListAdapter;
    PostListArrayAdapter mFavPostListAdapter;
    PostListArrayAdapter mOwnDiaryPostListAdapter;
    CommentListArrayAdapter mCommentListAdapter;
    
    // Настройки (пока нужны только для добавления логина и пароля)
    SharedPreferences mSharedPrefs;
    
    // Видимые объекты
    TextView mLogin;
    ListView mFavouriteBrowser;
    ListView mPostBrowser;
    ListView mCommentBrowser;
    
    WebView mMainView;
    ImageButton mExitButton;
    TabHost mTabHost;
    ProgressDialog pd;
    
    // Сервисные объекты
    DiaryHttpClient mDHCL;
    HtmlCleaner postCleaner;
    UserData mUser;
    
    JMetaWeblogClient WMAClient;
    Object[] RPCResponse;
    
    static Handler mHandler, mUiHandler;
    Looper mLooper; // петля времени
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        postCleaner = new HtmlCleaner();
        postCleaner.getProperties().setOmitComments(true);
        postCleaner.getProperties().setRecognizeUnicodeChars(true);
        
        mDHCL = new DiaryHttpClient();
        mUser = new UserData();
        
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, WorkerCallback);
        mUiHandler = new Handler(UiCallback);
        
        mSharedPrefs = getSharedPreferences(AuthorizationForm.mPrefsFile, MODE_PRIVATE);
        CookieSyncManager.createInstance(this);
        
        // Возможно, устаревший код. Оставлен для возможного будущего использования
        try
        {
            WMAClient = new JMetaWeblogClient("http://www.diary.ru/client/mwa.php");
            WMAClient.setUsername(mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
            WMAClient.setPassword(mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, ""));
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_diary_list_a);
        mFavouriteBrowser = (ListView) findViewById(R.id.favourite_browser);
        mPostBrowser = (ListView) findViewById(R.id.post_browser);
        mCommentBrowser = (ListView) findViewById(R.id.comment_browser);
        
        mLogin = (TextView) findViewById(R.id.login_name);
        
        // Также устаревший код, оставлен в целях тестирования
        mMainView = (WebView) findViewById(R.id.main_view);
        mMainView.setWebViewClient(new WebViewClient());
        
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
            
            // reduce height of the tab
            view.getLayoutParams().height *= 0.50;
            
            // get title text view
            final View textView = view.findViewById(android.R.id.title);
            if (textView instanceof TextView)
            {
                // just in case check the type
                
                // center text
                ((TextView) textView).setGravity(Gravity.CENTER);
                ((TextView) textView).setTypeface(Typeface.DEFAULT_BOLD);
                
                // explicitly set layout parameters
                textView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                textView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
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
        getMenuInflater().inflate(R.menu.activity_diary_list_a, menu);
        return true;
    }
    
    @Override
    protected void onStart()
    {
        super.onStart();
        if(pd == null)
        {
            pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.please_wait), true, true);
            mHandler.sendEmptyMessage(HANDLE_SET_HTTP_COOKIE);
        }
    }
    
    Handler.Callback UiCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            switch (message.what)
            {
                case HANDLE_SERVICE_LOAD_IMAGE:
                    ((PostListArrayAdapter) mPostBrowser.getAdapter()).notifyDataSetChanged();
                    mCommentListAdapter.notifyDataSetChanged();
                break;
                case HANDLE_GET_U_BLOGS:
                    pd.dismiss();
                break;
                case HANDLE_SET_HTTP_COOKIE:
                    pd.setMessage(getResources().getString(R.string.getting_user_info));
                    mLogin.setText(mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, ""));
                    mHandler.sendEmptyMessage(HANDLE_GET_USER_PARAMS);
                break;
                case HANDLE_GET_USER_PARAMS:
                    pd.dismiss();
                    setCurrentTab(TAB_FAVOURITES);
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
                case HANDLE_GET_OWNDIARY_POSTS_DATA:
                    setCurrentVisibleComponent(POST_LIST);
                    mPostBrowser.setAdapter(mOwnDiaryPostListAdapter);
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
                    case HANDLE_SERVICE_LOAD_IMAGE:
                    {
                        Pair<Spannable, ImageSpan> pair = (Pair<Spannable, ImageSpan>)message.obj;
                        final int start = pair.first.getSpanStart(pair.second);
                        final int end = pair.first.getSpanEnd(pair.second);
                        if(start == -1 || end == -1) // уже удалена
                            return false;
                        
                        Drawable loadedPicture = loadImage(pair.second.getSource());
                        loadedPicture.setBounds(new Rect(0, 0, loadedPicture.getIntrinsicWidth(), loadedPicture.getIntrinsicWidth()));
                                                
                        pair.first.removeSpan(pair.second);
                        for(ClickableSpan spanToPurge : pair.first.getSpans(start, end, ClickableSpan.class))
                            pair.first.removeSpan(spanToPurge);
                        
                        pair.first.setSpan(new ImageSpan(loadedPicture, ImageSpan.ALIGN_BASELINE), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        mUiHandler.sendMessage(mUiHandler.obtainMessage(HANDLE_SERVICE_LOAD_IMAGE));                        
                    }
                    break;
                    case HANDLE_GET_U_BLOGS:
                        
                        RPCResponse = WMAClient.getUsersBlogs();
                        
                        if (RPCResponse == null)
                            return false;
                        
                        HashMap<?, ?> contentHash = new HashMap<Object, Object>();
                        Vector<HashMap<?, ?>> contents = new Vector<HashMap<?, ?>>();
                        
                        for (int ctr = 0; ctr < RPCResponse.length; ctr++)
                        {
                            contentHash = (HashMap<?, ?>) RPCResponse[ctr];
                            contents.add(ctr, contentHash);
                        }
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_U_BLOGS);
                        return true;
                    case HANDLE_SET_HTTP_COOKIE:
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                        nameValuePairs.add(new BasicNameValuePair("user_login", mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
                        nameValuePairs.add(new BasicNameValuePair("user_pass", mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));
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
                    case HANDLE_GET_USER_PARAMS:
                    {
                        mDHCL.postPage("http://www.diary.ru", null);
                        String homeScreen = EntityUtils.toString(mDHCL.response.getEntity());
                        
                        TagNode rootNode = postCleaner.clean(homeScreen);
                        TagNode[] nodes = rootNode.getAllElements(true);
                        for(TagNode node : nodes)
                        {
                            if(node.getText().toString().equals("Мой дневник"))
                                mUser.ownDiaryURL = node.getAttributeByName("href");
                            if(node.getText().toString().equals(mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")))
                                mUser.ownProfileURL = node.getAttributeByName("href");
                        }
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_USER_PARAMS);
                        return true;
                    }
                    case HANDLE_GET_FAVORITES_COMMUNITIES_DATA:
                    // TODO: Исправить все к чертям!! Поставить строгое извлечение по
                    // столбцам таблицы, идиот!!
                    {
                        mDHCL.postPage("http://www.diary.ru/list/?act=show&fgroup_id=0", null);
                        String favListPage = EntityUtils.toString(mDHCL.response.getEntity());
                        
                        TagNode rootNode = postCleaner.clean(favListPage);
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
                                mUser.favorites.add(new Diary(title.findElementByName("b", false).getText().toString(), title.getAttributeByName("href"), author.getText().toString(), author.getAttributeByName("href"), last_post.getText().toString(), last_post.getAttributeByName("href")));
                                title = author = last_post = null;
                            }
                        }
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_FAVORITES_COMMUNITIES_DATA);
                        return true;
                    }
                    case HANDLE_GET_DIARY_POSTS_DATA:
                    {
                        Diary diary = (Diary) message.obj;
                        String URL = diary.getDiaryUrl();
                        
                        serializePostsPage(URL, null);
                        
                        mUiHandler.sendEmptyMessage(HANDLE_GET_DIARY_POSTS_DATA);
                        return true;
                    }
                    case HANDLE_GET_POST_COMMENTS_DATA:
                    {
                    	mUser.currentPostComments.clear();
                        Post parsingPost = (Post) message.obj;
                        String URL = parsingPost.get_URL();
                        
                        mDHCL.postPage(URL, null);
                        String dataPage = EntityUtils.toString(mDHCL.response.getEntity());
                        
                        mUser.currentPostComments.add(parsingPost);
                        TagNode rootNode = postCleaner.clean(dataPage);
                        TagNode commentsArea = rootNode.findElementByAttValue("id", "commentsArea", true, true);
                        if(commentsArea == null)
                        {
                            mUiHandler.sendEmptyMessage(HANDLE_GET_POST_COMMENTS_DATA);
                            return true;
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
                                	SimpleHtmlSerializer serializer = new SimpleHtmlSerializer(postCleaner.getProperties());
                                	SpannableStringBuilder SB = new SpannableStringBuilder(Html.fromHtml(serializer.getAsString(contentNode)));
                                	formatText(SB);
                                	currentPost.set_text(SB);
                                }
                                TagNode urlNode = comment.findElementByAttValue("class", "postLinksBackg", false, true);
                                if (urlNode != null)
                                {
                                	currentPost.set_URL(urlNode.findElementByName("a", true).getAttributeByName("href"));
                                }
                                mUser.currentPostComments.add(currentPost);  
                            }   
                        }
                          
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
                    case HANDLE_GET_OWNDIARY_POSTS_DATA:
                    {
                        mUser.ownDiaryPosts.clear();
                        String URL = mUser.ownDiaryURL;
                        
                        serializePostsPage(URL, mUser.ownDiaryPosts);

                        mUiHandler.sendEmptyMessage(HANDLE_GET_OWNDIARY_POSTS_DATA);
                        return true;
                    }
                    default:
                        return false;
                }
            }
            catch (XMLRPCException e)
            {
                e.printStackTrace();
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
    
    // форматируем текст перед выведением в TextView в списках
    private void formatText(final SpannableStringBuilder spannable)
    {
    	ImageSpan[] imageSpans = spannable.getSpans(0, spannable.length(), ImageSpan.class);
    	
        for (final ImageSpan span : imageSpans)
        {
            
            final String image_src = span.getSource();
            
            // Если это смайлик или системное изображение
            // загрузка изображений обрабатывается в сервисном потоке - обязательно!
            
            // Временно отключено - большая потеря памяти
            //if(image_src.contains("static") && !image_src.contains("userdir"))
            //    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_SERVICE_LOAD_IMAGE, new Pair<Spannable, ImageSpan>(spannable, span)));
            
            final int start = spannable.getSpanStart(span);
            final int end = spannable.getSpanEnd(span);
            
            // делаем каждую картинку кликабельной
            ClickableSpan click_span = new ClickableSpan()
            {
                
                @Override
                public void onClick(View widget)
                {
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_SERVICE_LOAD_IMAGE, new Pair<Spannable, ImageSpan>(spannable, span)));
                    Toast.makeText(DiaryList.this, "Image Clicked " + image_src, Toast.LENGTH_SHORT).show();
                }
                
            };
            
            ClickableSpan[] click_spans = spannable.getSpans(start, end, ClickableSpan.class);
            for (ClickableSpan c_span : click_spans)
            {
                spannable.removeSpan(c_span);
            }
            
            spannable.setSpan(click_span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
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
            TextView title = (TextView) view.findViewById(R.id.post_title);
            title.setText(post.get_title());
            title.setOnClickListener(DiaryList.this);
            TextView author = (TextView) view.findViewById(R.id.post_author);
            author.setText(post.get_author());
            author.setOnClickListener(DiaryList.this);
            TextView post_date = (TextView) view.findViewById(R.id.post_date);
            post_date.setText(post.get_date(), TextView.BufferType.SPANNABLE);
            TextView comment_count = (TextView) view.findViewById(R.id.comments_number);
            comment_count.setText(getResources().getString(R.string.comments) + " " + post.get_comment_count());
            TextView post_content = (TextView) view.findViewById(R.id.post_content);
            post_content.setText(post.get_text());
            
            post_content.setMovementMethod(LinkMovementMethod.getInstance());
                        
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
            post_date.setText(post.get_date(), TextView.BufferType.SPANNABLE);
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
            
            /* ImageButton delete = (ImageButton)view.findViewById(R.id.p_delete); */
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
            Editor lysosome = mSharedPrefs.edit();
            lysosome.remove(AuthorizationForm.KEY_USERNAME);
            lysosome.remove(AuthorizationForm.KEY_PASSWORD);
            lysosome.commit();
            
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeSessionCookie();
            CookieSyncManager.getInstance().sync();
            
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
                    mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DIARY_POSTS_DATA, diary));
                }
                break;
                case R.id.post_title:
                {
                    ListView ownerListView = (ListView)((View) view.getParent()).getParent();
                	int pos = ownerListView.getPositionForView((View) view.getParent());
                	Post post = (Post) mPostBrowser.getAdapter().getItem(pos);
                	
                	 pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.loading_data), true, true);
                     mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_POST_COMMENTS_DATA, post));
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
                    mHandler.sendEmptyMessage(HANDLE_GET_OWNDIARY_POSTS_DATA);
                break;
                default:
                break;
            }
        
        mTabHost.setCurrentTab(index);
    }
    
    private void setCurrentVisibleComponent(int needed)
    {   
        mFavouriteBrowser.setVisibility(needed == FAVOURITE_LIST ? View.VISIBLE : View.GONE);
        mPostBrowser.setVisibility(needed == POST_LIST ? View.VISIBLE : View.GONE);
        mCommentBrowser.setVisibility(needed == COMMENT_LIST ? View.VISIBLE : View.GONE);
        //mAuthorBrowser.setVisibility(needed == AUTHOR_PAGE ? View.VISIBLE : View.GONE);
    }
    
    public static Drawable loadImage(String url) 
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


    /* (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        if (mPostBrowser.getVisibility() == View.VISIBLE || mTabHost.getCurrentTab() == TAB_FAVOURITES)
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
        
        TagNode rootNode = postCleaner.clean(dataPage);
        TagNode postsArea = rootNode.findElementByAttValue("id", "postsArea", true, true);
        for (TagNode post : postsArea.getAllElements(false))
        {
            if (post.getAttributeByName("class") != null && post.getAttributeByName("class").contains("singlePost"))
            {
                Post currentPost = new Post();
                TagNode headerNode = post.findElementByAttValue("class", "postTitle header", false, true);
                if (headerNode != null)
                {
                    currentPost.set_title(headerNode.findElementByName("h2", false).getText().toString());
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
                TagNode contentNode = post.findElementByAttValue("class", "paragraph", true, true);
                if(contentNode != null)
                {
                    SimpleHtmlSerializer serializer = new SimpleHtmlSerializer(postCleaner.getProperties());
                    SpannableStringBuilder SB = new SpannableStringBuilder(Html.fromHtml(serializer.getAsString(contentNode)));
                    formatText(SB);
                    currentPost.set_text(SB);
                }
                TagNode urlNode = post.findElementByAttValue("class", "postLinksBackg", false, true);
                if (urlNode != null)
                {
                    currentPost.set_URL(urlNode.findElementByName("a", true).getAttributeByName("href"));
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
}
