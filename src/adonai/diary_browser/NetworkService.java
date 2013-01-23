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

import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiaryWebPage;
import adonai.diary_browser.entities.DiscList;
import adonai.diary_browser.entities.Openable;
import adonai.diary_browser.entities.TagsPage;
import adonai.diary_browser.entities.Umail;
import adonai.diary_browser.entities.UmailPage;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.util.Pair;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import adonai.diary_browser.Utils;

public class NetworkService extends Service implements Callback, OnSharedPreferenceChangeListener
{
	private static NetworkService mInstance = null;
	private static boolean mIsStarting = false;
	
	public UserData mUser = new UserData();
	public DiaryHttpClient mDHCL = new DiaryHttpClient();
	public SharedPreferences mPreferences;
	private CacheManager mCache = CacheManager.getInstance();
	
	private Handler mHandler;
    private Looper mLooper; // петля времени
    
    boolean load_images;
    boolean load_cached;
    
    private List<DiaryActivity> mListeners = new ArrayList<DiaryActivity>();
	
    
	public static NetworkService getInstance(Context context)
	{
		if(mInstance == null && !mIsStarting)
		{
			context.startService(new Intent(context, NetworkService.class));
			mIsStarting = true;
		}
		return mInstance;
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
        mPreferences = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        load_images = mPreferences.getBoolean("images.autoload", false);
        load_cached = mPreferences.getBoolean("images.autoload.cache", false);
        updateMaxCacheSize(mPreferences);
		
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
		mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, this);
		
		mInstance = this;
		mIsStarting = false;
	}
	
	@Override
	public void onDestroy()
	{
		mInstance = null;
		mLooper.quit();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return START_NOT_STICKY;
	}
	
	public void handleRequest(int opcode, Object message)
	{
		mHandler.sendMessage(mHandler.obtainMessage(opcode, message));
	}
	
	public void addListener(DiaryActivity listener)
	{
		if(!mListeners.contains(listener))
			mListeners.add(listener);
	}
	
	public void removeListener(DiaryActivity listener)
	{
		mListeners.remove(listener);
	}
	
	public void notifyListeners(int opCode, Object body)
	{
		for (DiaryActivity listener : mListeners)
			listener.handleUi(opCode, body);
	}
	
	

	@SuppressWarnings("unchecked")
	@Override
	public boolean handleMessage(Message message)
	{
		try
        {
            switch (message.what)
            {
                case Utils.HANDLE_SET_HTTP_COOKIE:
                {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                    nameValuePairs.add(new BasicNameValuePair("user_login", mPreferences.getString(Utils.KEY_USERNAME, "")));
                    nameValuePairs.add(new BasicNameValuePair("user_pass", mPreferences.getString(Utils.KEY_PASSWORD, "")));
                    nameValuePairs.add(new BasicNameValuePair("save_on", "1"));
                    
                    HttpResponse page = mDHCL.postPage("http://www.diary.ru/login.php", new UrlEncodedFormEntity(nameValuePairs, "WINDOWS-1251"));
                    if(page == null)
                    {
                    	notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
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
                    	notifyListeners(Utils.HANDLE_AUTHORIZATION_ERROR, null);
                        return false;
                    }
                    
                    notifyListeners(Utils.HANDLE_SET_HTTP_COOKIE, null);
                    return true;
                }
                case Utils.HANDLE_GET_DIARIES_DATA:
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
                    		notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                        	return false;
                        }
                        String favListPage = EntityUtils.toString(page.getEntity());
                        
                        serializeDiaryListPage(favListPage);
                        mCache.putPageToCache(URL, mUser.currentDiaries);
                    }
                    
                    notifyListeners(Utils.HANDLE_GET_DIARIES_DATA, null);
                    return true;
                }
                case Utils.HANDLE_GET_DISCUSSIONS_DATA:
                {
                	HttpResponse page = mDHCL.postPage("http://www.diary.ru/discussion/", null);
                	if(page == null)
                    {
                		notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                    	return false;
                    }
                	String dataPage = EntityUtils.toString(page.getEntity());
                	serializeDiscussionsPage(dataPage, null); 
                	
                	notifyListeners(Utils.HANDLE_GET_DISCUSSIONS_DATA, null);
                	return true;
                }
                case Utils.HANDLE_GET_DISCUSSION_LIST_DATA:
                {
                	
                	int pos = (Integer) ((ArrayList<?>) message.obj).get(0);
                	DiscList dList = (DiscList) ((ArrayList<?>) message.obj).get(1);
                	boolean onlyNew = (Boolean) ((ArrayList<?>) message.obj).get(2);
                	
                	String jsURL = dList.getURL();
                	if(onlyNew)
                	    jsURL = jsURL + "&new";
                	
                	HttpResponse page = mDHCL.postPage(jsURL, null);
                    if(page == null)
                    {
                    	notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                    	return false;
                    }
                    String dataPage = EntityUtils.toString(page.getEntity());
                    serializeDiscussions(dataPage, dList.getDiscussions());
                    
                    notifyListeners(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, pos);
                	return true;
                }
                case Utils.HANDLE_PICK_URL:
                {
                	String URL = ((Pair<String, Boolean>) message.obj).first;
                    boolean reload = ((Pair<String, Boolean>) message.obj).second;
                	checkUrlAndHandle(URL, reload);
                	return true;
                }
                case Utils.HANDLE_OPEN_FOLDER:
                {
                    HttpResponse page = mDHCL.postPage((String)message.obj, null);
                    if(page == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                        return false;
                    }
                    String uFolder = EntityUtils.toString(page.getEntity());
                    serializeUmailListPage(uFolder);
                    
                    notifyListeners(Utils.HANDLE_OPEN_FOLDER, null);
                    return true;
                }
                case Utils.HANDLE_OPEN_MAIL:
                {
                    HttpResponse page = mDHCL.postPage((String)message.obj, null);
                    if(page == null)
                    {
                    	notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                        return false;
                    }
                    String uMail = EntityUtils.toString(page.getEntity());
                    serializeUmailPage(uMail);
                    
                    notifyListeners(Utils.HANDLE_OPEN_MAIL, null);
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

	/* (non-Javadoc)
     * @see android.app.Activity#onSearchRequested()
     */
    public void serializeDiaryListPage(String dataPage)
    {
        mUser.currentDiaries = new DiaryListPage();
        mUser.currentDiaries.setURL(mDHCL.currentURL);
        
        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);
            
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
                Openable diary = new Openable();
                diary.setTitle(title.getElementsByTag("b").text());
                diary.setURL(title.attr("href"));
                
                diary.setAuthor(author.text());
                String authorData = author.attr("href");
                diary.setAuthorURL(authorData);
                diary.setAuthorID(authorData.substring(authorData.lastIndexOf("?") + 1));
                
                diary.setLastUpdate(last_post.text());
                diary.setLastUpdateURL(last_post.attr("href"));
                
                mUser.currentDiaries.add(diary);
                title = author = last_post = null;
            }
        }
    }
	
	public void serializeDiaryPage(String dataPage) throws IOException
    {
        DiaryPage scannedDiary = new DiaryPage();
        
        notifyListeners(Utils.HANDLE_PROGRESS, null);
    	Document rootNode = Jsoup.parse(dataPage);
    	mUser.parseData(rootNode);
        
        scannedDiary.setDiaryURL(mDHCL.currentURL);
        Element diaryTag = rootNode.select("[id=authorName]").first();
        if(diaryTag != null)
        {
            String Id = diaryTag.getElementsByTag("a").last().attr("href");
            scannedDiary.setDiaryID(Id.substring(Id.lastIndexOf("?") + 1));
        }
        
        notifyListeners(Utils.HANDLE_PROGRESS_2, null);
        Elements postsArea = rootNode.select("[id=postsArea] > [id=epigraph], [id=postsArea] > [id^=post], div.pageBar");
        if(postsArea.isEmpty()) // Нет вообще никаких постов, заканчиваем
        	return;
        
        Elements result = postsArea.clone();
        Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
        {
            resultPage.body().appendChild(to);
        }
        parseContent(resultPage);
        scannedDiary.setContent(resultPage);
        
        mUser.currentDiaryPage = scannedDiary;
    }
    
    private void parseContent(Document resultPage)
	{
        resultPage.head().append("<link rel=\"stylesheet\" href=\"file:///android_asset/css/journal.css\" type=\"text/css\" media=\"all\" title=\"Стандарт\"/>");
    	if(!load_images)
        {
            Elements images = resultPage.select("img[src^=http], a:has(img)");
            for(Element current : images)
            {
                if(current.tagName().equals("img"))
                {
                    String src = current.attr("src");
                    if(!src.contains("diary.ru") && !current.parent().className().equals("avatar") && !src.startsWith("/"))
                    {
                    	if(load_cached)
                    	{
    	                	String hashCode = String.format("%08x", src.hashCode());
    	                    File file = new File(new File(getCacheDir(), "webviewCache"), hashCode);
    	                    if(file.exists())
    	                    	continue;
                    	}
                        
                        String jsButton = "<input type=\"image\" src=\"file:///android_res/drawable/load_image.png\" onclick=\"return handleIMGDown(this, '" + src + "')\"/>";
                        
                        current.after(jsButton);
                        current.remove();
                    }
                }
                
                if(current.tagName().equals("a"))
                {
                    String src = current.getElementsByTag("img").attr("src");
                    if(!src.contains("diary.ru") && !current.parent().className().equals("avatar") && !src.startsWith("/"))
                    {
                        if(load_cached)
                        {
                            String hashCode = String.format("%08x", src.hashCode());
                            File file = new File(new File(getCacheDir(), "webviewCache"), hashCode);
                            if(file.exists())
                                continue;
                        }
                        
                        String jsButton = "<input type=\"image\" src=\"file:///android_res/drawable/load_image.png\" onclick=\"return handleADown(this, '" + current.attr("href") + "', '" + src + "')\"/>";
                        
                        current.after(jsButton);
                        current.remove();
                    }
                }
            }
        }
    	resultPage.body().append(Utils.javascriptContent);
	}

	public void serializeCommentsPage(String dataPage) throws IOException
    {
	    CommentsPage scannedPost = new CommentsPage();
    	
        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);
        
        scannedPost.setDiaryURL(mDHCL.currentURL.substring(0, mDHCL.currentURL.lastIndexOf('/') + 1));
        Element diaryTag = rootNode.select("[id=authorName]").first();
        if(diaryTag != null)
        {
            String Id = diaryTag.getElementsByTag("a").last().attr("href");
            scannedPost.setDiaryID(Id.substring(Id.lastIndexOf("?") + 1));
        }
        
        notifyListeners(Utils.HANDLE_PROGRESS_2, null);
        Elements effectiveAreas = rootNode.select("[id=postsArea] > [id^=post], [id=commentsArea] > [id^=comment], div.pageBar");
        if(effectiveAreas.isEmpty()) // Нет вообще никаких постов, заканчиваем
            return;
        
        Elements result = effectiveAreas.clone();
        Element urlNode = result.first().getElementsByClass("postLinksBackg").first();
        if (urlNode != null)
        {
            String postURL = urlNode.getElementsByTag("a").attr("href");
            scannedPost.setPostURL(postURL);
            scannedPost.setPostID(postURL.substring(postURL.lastIndexOf('p') + 1, postURL.lastIndexOf('.')));
        }
        Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
        {
            resultPage.body().appendChild(to);
        }
        
        parseContent(resultPage);
        
        scannedPost.setContent(resultPage);
        mUser.currentDiaryPage = scannedPost;
    }
	
	public void serializeTagsPage(String dataPage) throws IOException
    {
	    TagsPage scannedTags = new TagsPage();
    	
	    notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);
        
        scannedTags.setDiaryURL(mDHCL.currentURL.substring(0, mDHCL.currentURL.lastIndexOf('/') + 1));
        Element diaryTag = rootNode.select("[id=authorName]").first();
        if(diaryTag != null)
        {
            String Id = diaryTag.getElementsByTag("a").last().attr("href");
            scannedTags.setDiaryID(Id.substring(Id.lastIndexOf("?") + 1));
        }
        
        notifyListeners(Utils.HANDLE_PROGRESS_2, null);
        Elements effectiveAreas = rootNode.select("ul.tags_ul_all");
        if(effectiveAreas.isEmpty()) // Нет вообще никаких тэгов, заканчиваем
            return;
        
        Elements result = effectiveAreas.clone();
        result.select("input[type=checkbox]").remove();
        
        Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
        {
            resultPage.body().appendChild(to);
        }
        
        resultPage.head().append("<link rel=\"stylesheet\" href=\"file:///android_asset/css/journal.css\" type=\"text/css\" media=\"all\" title=\"Стандарт\"/>");
        scannedTags.setContent(resultPage);
        mUser.currentDiaryPage = scannedTags;
    }
    
    public void serializeDiscussionsPage(String dataPage, List<DiscList> destination)
    {
    	mUser.discussions.clear();
    	notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);
        
        notifyListeners(Utils.HANDLE_PROGRESS_2, null);
        Element dIndex = rootNode.getElementById("all_bits");
        for (Element item : dIndex.getElementsByTag("h3"))
        {
        	DiscList currentList = new DiscList();
        	Element newPosts = item.getElementsByTag("em").first();
        	currentList.setLastUpdate(newPosts.text());
        	newPosts.remove();
        	
        	String link = item.getElementsByAttributeValueStarting("href", "/discussion/?open[]").attr("href") + "&js";
        	currentList.setURL(link);
        	String title = item.text();
        	currentList.setTitle(title);
        	
        	mUser.discussions.add(currentList);
        }
    }
    
    public void serializeDiscussions(String dataPage, ArrayList<DiscList.Discussion> destination)
    {
    	destination.clear();
    	dataPage = dataPage.replace("\\\"", "\"");
        Document rootNode = Jsoup.parse(dataPage);
        Elements tables = rootNode.getElementsByTag("em");
        tables.remove();
        
        for (Element discussion : rootNode.getElementsByTag("a"))
        {
        	DiscList.Discussion currentDisc = new DiscList.Discussion();
        	String link = discussion.attr("href");
        	currentDisc.set_URL(link);
        	String title = discussion.text();
        	currentDisc.set_title(title);
        	String date = discussion.previousElementSibling().text();
        	currentDisc.set_date(date);
        	
        	
        	destination.add(currentDisc);
        }
    }
    
    void serializeUmailListPage(String dataPage)
    {
        mUser.currentUmails = new DiaryListPage();
        mUser.currentUmails.setURL(mDHCL.currentURL);
        
        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);
            
        Element table = rootNode.getElementsByAttributeValue("class", "table l").first();
        if(table == null) // Нет вообще никаких сообщений, заканчиваем
            return;
        
        Element pages = rootNode.select("table.pages").first();
        if(pages != null)
            mUser.currentUmails.setPageLinks(Html.fromHtml(pages.outerHtml()));
        
        Elements rows = table.select("tr[id]");
        Element title = null, author = null, last_post = null;
        for (Element row : rows)
        {
            if (title == null)
                title = row.getElementsByClass("withfloat").first();

            if(author == null)
                author = row.select("[target=_blank]").first();
            
            if (last_post == null)
                last_post = row.getElementsByClass("r").first();
            
            if (title != null && author != null && last_post != null)
            {
                Umail mail = new Umail();
                mail.setTitle(title.getElementsByTag("b").text());
                mail.setURL(title.attr("href"));
                
                mail.setAuthor(author.text());
                String authorData = author.attr("href");
                mail.setAuthorURL(authorData);
                mail.setAuthorID(authorData.substring(authorData.lastIndexOf("?") + 1));
                
                mail.setLastUpdate(last_post.text());
                mail.setLastUpdateURL(last_post.attr("href"));
                
                mail.setRead(!row.hasClass("not_readed_umail"));
                
                mUser.currentUmails.add(mail);
                title = author = last_post = null;
            }
        }
    }
    
    public void serializeUmailPage(String dataPage) throws IOException
    {
        UmailPage scannedUmail = new UmailPage();
        notifyListeners(Utils.HANDLE_PROGRESS, null);
        
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);
        
        scannedUmail.setUmail_URL(mDHCL.currentURL);
        scannedUmail.setUmail_ID(scannedUmail.getUmail_URL().substring(scannedUmail.getUmail_URL().lastIndexOf('=') + 1));
        notifyListeners(Utils.HANDLE_PROGRESS_2, null);
        
        Elements mailArea = rootNode.select("table.box, table.box + div");
        if(mailArea.isEmpty()) // Нет вообще никаких постов, заканчиваем
            return;
        
        Element sender = mailArea.select("div[style^=float:left] > b").first();
        if(sender != null)
            scannedUmail.setSender_Name(sender.text());
        
        Elements result = mailArea.clone();
        Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
            resultPage.body().appendChild(to);
        
        scannedUmail.setContent(resultPage);
        mUser.currentUmailPage = scannedUmail;
    }
    
    public void checkUrlAndHandle(String URL, boolean reload)
    {   
    	Class<?> handled = null;
    	DiaryWebPage cachedPage = null;
    	String dataPage = null;
    	
    	try 
    	{
	    	if(mCache.hasPage(URL) && !reload)
	    	{
	    	    // Особый обработчик для случая с списками
	    	    if(mCache.loadPageFromCache(URL) instanceof DiaryListPage)
	    	    {
	    	        mDHCL.currentURL = URL;
	    	        mHandler.sendMessage(mHandler.obtainMessage(Utils.HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>(URL, false)));
	    	        return;
	    	    }
	    	    
	    		cachedPage = (DiaryWebPage) mCache.loadPageFromCache(URL);
	    		handled = cachedPage.getClass();
	    	}
	    	else
	    	{
		    	HttpResponse page = mDHCL.postPage(URL, null);
		    	if(page == null)
		    	{
		    		notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
		    		return;
		    	}
		    	dataPage = EntityUtils.toString(page.getEntity());
				handled = Utils.checkDiaryUrl(dataPage);
	    	}

			if(handled != null) // Если это страничка дайри
	    	{
			    if(cachedPage != null && !reload)
			    {
			        mDHCL.currentURL = URL;
                    mUser.currentDiaryPage = (DiaryWebPage) mCache.loadPageFromCache(URL);
			    }
			    else if(handled == DiaryPage.class)
    			{
    				serializeDiaryPage(dataPage);
    				mCache.putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
    			}
	    		else if (handled == CommentsPage.class)
    			{
    				serializeCommentsPage(dataPage);
    				mCache.putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
    			}
	    		else if(handled == TagsPage.class)
    			{
    				serializeTagsPage(dataPage);
    				mCache.putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
    			}
	    		else
	    		    return;
			    
			    notifyListeners(Utils.HANDLE_GET_DIARY_PAGE_DATA, null);
	    	}
			else
			{
				notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
				Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
				//startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)));
				startActivity(sendIntent);
			}
		} 
    	catch (Exception e) 
		{
    		notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
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
