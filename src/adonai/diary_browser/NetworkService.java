package adonai.diary_browser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.Html;
import android.util.Pair;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.RemoteViews;

import org.apache.http.Header;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiaryProfilePage;
import adonai.diary_browser.entities.DiscListPage;
import adonai.diary_browser.entities.DiscPage;
import adonai.diary_browser.entities.ListPage;
import adonai.diary_browser.entities.Post;
import adonai.diary_browser.entities.TagsPage;
import adonai.diary_browser.entities.Umail;
import adonai.diary_browser.entities.UmailPage;
import adonai.diary_browser.entities.WebPage;

public class NetworkService extends Service implements Callback, OnSharedPreferenceChangeListener
{
    private static final int NOTIFICATION_ID = 3; // Просто случайное число

    private static NetworkService mInstance = null;
    private static boolean mIsStarting = false;

    public UserData mUser = new UserData();
    public DiaryHttpClient mDHCL = new DiaryHttpClient();
    public SharedPreferences mPreferences;

    private CacheManager mCache = CacheManager.getInstance();
    private PowerManager.WakeLock waker;

    private Handler mHandler;
    private Looper mLooper; // петля времени

    boolean load_images;
    boolean load_cached;
    boolean is_sticky;
    boolean notify_on_updates;
    boolean keep_device_on;

    private List<DiaryActivity> mListeners = new ArrayList<DiaryActivity>();
    String[] lastLinks = {"", "", ""}; // дополнительная проверка, есть ли уже уведомление об этих ссылках

    /*
    К сожалению, НЕТ другой возможности запустить сервис.
    Контекст способен к запуску сервиса только если цикл его главного потока выполняется.
    Поэтому НЕЛЬЗЯ остановить контекст, создающий сервис и подождать пока он запустится
    Из-за этого, в частности, и нужен механизм HANDLE_APP_START.
    SPICE MUST FLOW!
    */
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
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        waker = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "diary.client");

        load_images = mPreferences.getBoolean("images.autoload", false);
        load_cached = mPreferences.getBoolean("images.autoload.cache", false);
        is_sticky = mPreferences.getBoolean("service.always.running", false);
        notify_on_updates = mPreferences.getBoolean("service.notify.updates", false);
        keep_device_on = mPreferences.getBoolean("service.keep.device.on", false);

        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, this);

        if(notify_on_updates)
            mHandler.sendMessageDelayed(mHandler.obtainMessage(Utils.HANDLE_SERVICE_UPDATE), 300000);

        if(keep_device_on)
            waker.acquire();

        if(is_sticky)
            startForeground(NOTIFICATION_ID, createNotification(mUser.currentDiaryPage));

        mInstance = this;
        mIsStarting = false;
    }

    @Override
    public void onDestroy()
    {
        mInstance = null;
        if(waker.isHeld())
            waker.release();
        mLooper.quit();

        // убираем значок
        stopForeground(true);

        // убираем вызовы регистрантам
        for (DiaryActivity listener : mListeners)
        {
            listener.mUiHandler.removeCallbacksAndMessages(null);
            listener.handleUi(Utils.HANDLE_SERVICE_ERROR, null);
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return is_sticky ? START_STICKY : START_NOT_STICKY;
    }

    public void handleRequest(int opcode, Object message)
    {
        mHandler.sendMessage(mHandler.obtainMessage(opcode, message));
    }

    public void addListener(DiaryActivity listener)
    {
        if(!mListeners.contains(listener))
        {
            mListeners.add(listener);
            listener.handleFontChange(mPreferences.getString("webview.font.size", "8"));
        }
    }

    public void removeListener(DiaryActivity listener)
    {
        mListeners.remove(listener);
    }

    private void notifyListeners(int opCode, Object body)
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
                case Utils.HANDLE_SERVICE_UPDATE: // уведомления о новых комментариях раз в 5 минут
                {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(Utils.HANDLE_SERVICE_UPDATE), 300000); // убедимся, что будем уведомлять и дальше

                    HttpResponse page = mDHCL.postPage(mUser.favoritesURL, null); // подойдет любая ссылка с дневников
                    if(page == null)
                        return false;

                    String dataPage = EntityUtils.toString(page.getEntity());
                    Document rootNode = Jsoup.parse(dataPage);
                    mUser.parseData(rootNode);

                    if(mUser.newDiaryCommentsNum + mUser.newDiscussNum + mUser.newUmailNum > 0 && (!lastLinks[0].equals(mUser.newDiaryLink) || !lastLinks[1].equals(mUser.newDiscussLink) || !lastLinks[2].equals(mUser.newUmailLink))) // старые данные или нет?
                    {
                        lastLinks[0] = mUser.newDiaryLink; // устанавливаем линки на новые значения
                        lastLinks[1] = mUser.newDiscussLink;
                        lastLinks[2] = mUser.newUmailLink;

                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);

                        views.setTextViewText(R.id.notification_title, getString(R.string.new_comments));
                        views.setTextViewText(R.id.notification_text, getString(R.string.my_diary) + ": " + mUser.newDiaryCommentsNum + " | " +
                                                                      getString(R.string.discussions) + ": " + mUser.newDiscussNum + " | " +
                                                                      getString(R.string.umail_activity_title) + ": " + mUser.newUmailNum);

                        Notification notification = new Notification();
                        notification.contentView = views;
                        notification.icon = R.drawable.ic_launcher_inverted; // иконка
                        notification.ledOnMS = 1000;
                        notification.ledOffMS = 10000;
                        notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        notification.ledARGB = Color.parseColor("#FFD8BD");
                        notification.tickerText = getString(R.string.new_comments) + ": " + Integer.toString(mUser.newDiaryCommentsNum + mUser.newDiscussNum + mUser.newUmailNum);
                        notification.flags |= Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_AUTO_CANCEL;

                        Intent intent = new Intent(this, DiaryListActivity.class); // при клике на уведомление открываем приложение
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        notification.contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
                        mNotificationManager.notify(NOTIFICATION_ID + 1, notification); // запускаем уведомление
                    }
                    break;
                }
                case Utils.HANDLE_JUST_DO_GET:
                {
                    if(mDHCL.getPage(message.obj.toString()) != null)
                        notifyListeners(Utils.HANDLE_JUST_DO_GET, null);
                    break;
                }
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
                        mUser.isAuthorised = true;
                    }
                    else
                    {
                        notifyListeners(Utils.HANDLE_AUTHORIZATION_ERROR, null);
                        return false;
                    }

                    notifyListeners(Utils.HANDLE_SET_HTTP_COOKIE, null);
                    return true;
                }
                case Utils.HANDLE_GET_LIST_PAGE_DATA:
                // TODO: Исправить все к чертям!! Поставить строгое извлечение по
                // столбцам таблицы, идиот!!
                {
                    String URL = ((Pair<String, Boolean>) message.obj).first;
                    boolean reload = ((Pair<String, Boolean>) message.obj).second;
                    // Если страничка дневников есть в комментах
                    if(mCache.hasPage(URL) && !reload)
                        mUser.currentDiaries = (DiaryListPage) mCache.loadPageFromCache(URL);
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

                    notifyListeners(Utils.HANDLE_GET_LIST_PAGE_DATA, null);
                    return true;
                }
                case Utils.HANDLE_GET_DISCUSSIONS_DATA:
                {
                    String URL = ((Pair<String, Boolean>) message.obj).first;
                    boolean reload = ((Pair<String, Boolean>) message.obj).second;

                    if(mCache.hasPage(URL) && !reload)
                        mUser.discussions = (DiscListPage) mCache.loadPageFromCache(URL);
                    else
                    {
                        HttpResponse page = mDHCL.postPage(URL, null);
                        if(page == null)
                        {
                            notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                            return false;
                        }
                        String dataPage = EntityUtils.toString(page.getEntity());
                        serializeDiscussionsPage(dataPage);
                        mCache.putPageToCache(mUser.discussionsURL, mUser.discussions);
                    }

                    notifyListeners(Utils.HANDLE_GET_DISCUSSIONS_DATA, null);
                    return true;
                }
                case Utils.HANDLE_GET_DISCUSSION_LIST_DATA:
                {

                    int pos = (Integer) ((ArrayList<?>) message.obj).get(0);
                    DiscPage dList = (DiscPage) ((ArrayList<?>) message.obj).get(1);
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
                case Utils.HANDLE_GET_IMAGE:
                {
                    String src = ((Pair<String, Boolean>) message.obj).first;
                    boolean openOnLoad = ((Pair<String, Boolean>) message.obj).second;
                    HttpResponse response = mDHCL.getPage(src);
                    if(response == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                        return false;
                    }
                    Header srcName = response.getFirstHeader("Content-Disposition");
                    String realName = URLUtil.guessFileName(src, srcName != null ? srcName.getValue() : null, MimeTypeMap.getFileExtensionFromUrl(src));
                    File newFile = CacheManager.saveDataToSD(getApplicationContext(), realName, response.getEntity().getContent());
                    if(openOnLoad && newFile != null)
                    {
                        Intent intent = new Intent(getApplicationContext(), ImageViewer.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("image_file", newFile.getCanonicalPath());
                        startActivity(intent);
                    }
                    return true;
                }
                case Utils.HANDLE_DELETE_POST:
                {
                    String id = (String) message.obj;
                    List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("act", "del_post_post"));
                    postParams.add(new BasicNameValuePair("post_id", id));
                    postParams.add(new BasicNameValuePair("yes", "Да"));
                    mDHCL.postPage(((DiaryPage)mUser.currentDiaryPage).getDiaryURL() + "diary.php", new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));

                    handleRequest(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.currentDiaryPage.getPageURL(), true));
                    return true;
                }
                case Utils.HANDLE_DELETE_COMMENT:
                {
                    String id = (String) message.obj;
                    mDHCL.getPage(((DiaryPage)mUser.currentDiaryPage).getDiaryURL() + "?delcomment&commentid=" + id + "&js&signature=" + mUser.signature);

                    handleRequest(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.currentDiaryPage.getPageURL(), true));
                    return true;
                }
                case Utils.HANDLE_EDIT_POST:
                {
                    String URL = (String) message.obj;
                    HttpResponse page = mDHCL.postPage(URL, null);
                    if(page == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                        return false;
                    }
                    String dataPage = EntityUtils.toString(page.getEntity());
                    Post sendPost = serializePostEditPage(dataPage);
                    sendPost.postID = URL.substring(URL.lastIndexOf("=") + 1);
                    sendPost.diaryID = ((DiaryPage)mUser.currentDiaryPage).getDiaryID();

                    notifyListeners(Utils.HANDLE_EDIT_POST, sendPost);
                    return true;
                }
                case Utils.HANDLE_EDIT_COMMENT:
                {
                    String URL = (String) message.obj;
                    HttpResponse page = mDHCL.postPage(URL, null);
                    if(page == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                        return false;
                    }
                    String dataPage = EntityUtils.toString(page.getEntity());
                    Comment sendComment = serializeCommentEditPage(dataPage);
                    sendComment.commentID = URL.substring(URL.lastIndexOf("=") + 1);
                    sendComment.postID = ((CommentsPage)mUser.currentDiaryPage).getPostID();

                    notifyListeners(Utils.HANDLE_EDIT_COMMENT, sendComment);
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

    private Post serializePostEditPage(String dataPage)
    {
        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Element rootNode = Jsoup.parse(dataPage).select("div.section").first(); // выбираем окошко с текстом
        Post result = new Post();

        result.title = rootNode.select("input#postTitle.text").val();
        result.content = rootNode.select("textarea#message").text();

        result.themes = rootNode.select("input#tags.text").val();
        result.mood = rootNode.select("input#atMood.text").val();
        result.music = rootNode.select("input#atMusic.text").val();

        result.pollTitle = rootNode.select("input#queti.text").val();
        if(!result.pollTitle.equals("")) // если есть опрос
        {
            result.pollAnswer1 = rootNode.select("input#answer1i.text.poll_answer").val();
            result.pollAnswer2 = rootNode.select("input#answer2i.text.poll_answer").val();
            result.pollAnswer3 = rootNode.select("input#answer3i.text.poll_answer").val();
            result.pollAnswer4 = rootNode.select("input#answer4i.text.poll_answer").val();
            result.pollAnswer5 = rootNode.select("input#answer5i.text.poll_answer").val();
            result.pollAnswer6 = rootNode.select("input#answer6i.text.poll_answer").val();
            result.pollAnswer7 = rootNode.select("input#answer7i.text.poll_answer").val();
            result.pollAnswer8 = rootNode.select("input#answer8i.text.poll_answer").val();
            result.pollAnswer9 = rootNode.select("input#answer9i.text.poll_answer").val();
            result.pollAnswer10 = rootNode.select("input#answer10i.text.poll_answer").val();
        }

        result.closeAccessMode = rootNode.select("[id^=closeaccessmode]").select("[checked]").val();
        if(!result.closeAccessMode.equals(""))
        {
            result.closeText = rootNode.select("textarea#close_text").text();
            if(result.closeAccessMode.equals("3"))
                result.closeAllowList = rootNode.select("textarea#access_list").text();
            if(result.closeAccessMode.equals("2"))
                result.closeDenyList = rootNode.select("textarea#access_list").text();
        }

        return result;
    }

    private Comment serializeCommentEditPage(String dataPage)
    {
        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Element rootNode = Jsoup.parse(dataPage).select("textarea#message").first(); // выбираем окошко с текстом
        Comment result = new Comment();

        result.content = rootNode.text();

        return result;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSearchRequested()
     */
    private void serializeDiaryListPage(String dataPage)
    {
        mUser.currentDiaries = new DiaryListPage(mDHCL.currentURL);

        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        Element table = rootNode.getElementsByAttributeValue("class", "table r").first();
        if(table == null) // Нет вообще никаких дневников, заканчиваем
            return;

        Element pages = rootNode.select("table.pages").first();
        if(pages != null)
            mUser.currentDiaries.setPageLinks(Html.fromHtml(pages.outerHtml()));

        Elements rows = table.select("tr:gt(0)");
        for (Element row : rows)
        {
            ListPage diary = new ListPage();
            Elements columns = row.children();
            diary.setTitle(columns.get(1).getElementsMatchingOwnText(".+").first().text());
            diary.setPageHint(columns.get(1).getElementsByTag("em").text());
            diary.setURL(columns.get(1).children().attr("href"));

            diary.setAuthor(columns.get(2).text());
            String authorData = columns.get(2).children().attr("href");
            diary.setAuthorURL(authorData);
            diary.setAuthorID(authorData.substring(authorData.lastIndexOf("?") + 1));

            diary.setLastUpdate(columns.get(5).text());
            diary.setLastUpdateURL(columns.get(5).children().attr("href"));

            mUser.currentDiaries.add(diary);
        }

    }

    private void serializeDiaryPage(String dataPage) throws IOException
    {
        DiaryPage scannedDiary = new DiaryPage(mDHCL.currentURL);

        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

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
        // страница будет иметь наш стиль
        resultPage.head().append("<link rel=\"stylesheet\" href=\"file:///android_asset/css/journal.css\" type=\"text/css\" media=\"all\" title=\"Стандарт\"/>");

        Elements jsElems = resultPage.getElementsByAttribute("onclick");
        for(Element js : jsElems)
            if(!js.attr("href").contains("#more") && !js.attr("href").contains("subscribe") && !js.attr("href").contains("showresult") && !js.attr("href").contains("up&signature=") && !js.attr("href").contains("down&signature="))
                js.removeAttr("onclick"); // Убиваем весь яваскрипт кроме MORE, поднятия/опускания постов, результатов голосования и подписки

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
                        // все неподходящие под критерии изображения на странице будут заменены на кнопки, по клику на которые и будут открываться
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
                        // все неподходящие под критерии изображения на странице будут заменены на кнопки, по клику на которые и будут открываться
                        String jsButton = "<input type=\"image\" src=\"file:///android_res/drawable/load_image.png\" onclick=\"return handleADown(this, '" + current.attr("href") + "', '" + src + "')\"/>";

                        current.after(jsButton);
                        current.remove();
                    }
                }
            }
        }
        resultPage.body().append(Utils.javascriptContent);
    }

    private void serializeCommentsPage(String dataPage) throws IOException
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


    private void serializeProfilePage(String dataPage)
    {
        DiaryProfilePage profilePage = new DiaryProfilePage(mDHCL.currentURL);
        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        Elements effectiveAreas = rootNode.select("div#contant");
        Elements result = effectiveAreas.clone();
        notifyListeners(Utils.HANDLE_PROGRESS_2, null);

        Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
        {
            resultPage.body().appendChild(to);
        }

        parseContent(resultPage);

        profilePage.setContent(resultPage);
        mUser.currentDiaryPage = profilePage;
    }

    private void serializeTagsPage(String dataPage) throws IOException
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

    private void serializeDiscussionsPage(String dataPage)
    {
        mUser.discussions.clear();
        mUser.discussions.setURL(mDHCL.currentURL);

        notifyListeners(Utils.HANDLE_PROGRESS, null);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        notifyListeners(Utils.HANDLE_PROGRESS_2, null);
        Element dIndex = rootNode.getElementById("all_bits");
        for (Element item : dIndex.getElementsByTag("h3"))
        {
            DiscPage currentList = new DiscPage();
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

    private void serializeDiscussions(String dataPage, ArrayList<DiscPage.Discussion> destination)
    {
        destination.clear();
        dataPage = dataPage.replace("\\\"", "\"");
        Document rootNode = Jsoup.parse(dataPage);
        Elements tables = rootNode.getElementsByTag("em");
        tables.remove();

        for (Element discussion : rootNode.getElementsByTag("a"))
        {
            DiscPage.Discussion currentDisc = new DiscPage.Discussion();
            currentDisc.URL = discussion.attr("href");
            currentDisc.title = discussion.text();
            currentDisc.date = discussion.previousElementSibling().text();


            destination.add(currentDisc);
        }
    }

    private void serializeUmailListPage(String dataPage)
    {
        mUser.currentUmails = new DiaryListPage(mDHCL.currentURL);

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

    private void serializeUmailPage(String dataPage) throws IOException
    {
        UmailPage scannedUmail = new UmailPage();
        notifyListeners(Utils.HANDLE_PROGRESS, null);

        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        scannedUmail.setUmailURL(mDHCL.currentURL);
        scannedUmail.setUmailID(scannedUmail.getUmailURL().substring(scannedUmail.getUmailURL().lastIndexOf('=') + 1));
        notifyListeners(Utils.HANDLE_PROGRESS_2, null);

        Elements mailArea = rootNode.select("table.box, table.box + div");
        if(mailArea.isEmpty()) // Нет вообще никаких постов, заканчиваем
            return;

        Element sender = mailArea.select("div[style^=float:left] > b").first();
        if(sender != null)
            scannedUmail.setSenderName(sender.text());

        Element theme = mailArea.select("tbody td").last();
        if(theme != null)
            scannedUmail.setMessageTheme(theme.text());

        Elements result = mailArea.clone();
        Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
            resultPage.body().appendChild(to);

        scannedUmail.setContent(resultPage);
        mUser.currentUmailPage = scannedUmail;
    }

    private void checkUrlAndHandle(String URL, boolean reload)
    {   
        Class<?> handled;
        Object cachedPage = null;
        String dataPage = null;

        try
        {
            if(mCache.hasPage(URL) && !reload)
            {
                cachedPage = mCache.loadPageFromCache(URL);
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
                if(cachedPage != null) // если страничка была в кэше
                {
                    mDHCL.currentURL = URL;

                    // Особый обработчик для случая с списками
                    if(cachedPage instanceof DiaryListPage)
                    {
                        mUser.currentDiaries = (DiaryListPage) cachedPage;
                        notifyListeners(Utils.HANDLE_GET_LIST_PAGE_DATA, null);
                    }

                    if(cachedPage instanceof DiscListPage)
                    {
                        mUser.discussions = (DiscListPage) cachedPage;
                        notifyListeners(Utils.HANDLE_GET_DISCUSSIONS_DATA, null);
                    }

                    if(cachedPage instanceof WebPage)
                    {
                        mUser.currentDiaryPage = (WebPage) cachedPage;
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA, null);
                    }
                }
                else // если нет такого кэша
                {
                    if(handled == DiaryPage.class)
                    {
                        serializeDiaryPage(dataPage);
                        mCache.putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA, null);
                    }
                    else if (handled == CommentsPage.class)
                    {
                        serializeCommentsPage(dataPage);
                        mCache.putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA, null);
                    }
                    else if(handled == TagsPage.class)
                    {
                        serializeTagsPage(dataPage);
                        mCache.putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA, null);
                    }
                    else if(handled == DiaryProfilePage.class)
                    {
                        serializeProfilePage(dataPage);
                        mCache.putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA, null);
                    }
                    else if(handled == DiaryListPage.class)
                    {
                        serializeDiaryListPage(dataPage);
                        mCache.putPageToCache(URL, mUser.currentDiaries);
                        notifyListeners(Utils.HANDLE_GET_LIST_PAGE_DATA, null);
                    }
                }
            }
            else // неопознанная страничка
            {
                assert(cachedPage == null);
                if(dataPage.contains("закрыт") || dataPage.contains("попробовать что-нибудь еще")) // если наткнулись на ошибку дневника
                    notifyListeners(Utils.HANDLE_CLOSED_ERROR, null);
                else
                {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
                    Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
                    // createChooser создает новый Intent из предыдущего, флаги нужно присоединять уже к нему!
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }
        }
        catch (Exception e)
        {
            notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, null);
            e.printStackTrace();
        }

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
    {
        if(key.equals("images.autoload"))
        {
            load_images = sharedPreferences.getBoolean(key, false);
        }
        else if(key.equals("images.autoload.cache"))
        {
            load_cached = sharedPreferences.getBoolean(key, false);
        }
        else if(key.equals("service.notify.updates"))
        {
            mHandler.removeMessages(Utils.HANDLE_SERVICE_UPDATE);
            notify_on_updates = sharedPreferences.getBoolean(key, false);
            if(notify_on_updates)
                mHandler.sendMessageDelayed(mHandler.obtainMessage(Utils.HANDLE_SERVICE_UPDATE), 300000);
        }
        else if(key.equals("service.keep.device.on"))
        {
            if(waker.isHeld())
                waker.release();

            keep_device_on = sharedPreferences.getBoolean(key, false);
            if(keep_device_on)
                waker.acquire();
        }
        else if(key.equals("service.always.running"))
        {
            is_sticky = sharedPreferences.getBoolean(key, false);
            if(is_sticky)
                startForeground(NOTIFICATION_ID, createNotification(mUser.currentDiaryPage));
            else
                stopForeground(true);
        }
        else if(key.equals("webview.font.size"))
        {
            for(DiaryActivity current : mListeners)
                current.handleFontChange(sharedPreferences.getString("webview.font.size", "8"));
        }
    }

    // Создаем уведомление в статусной строке - для принудительно живого сервиса в Foregound-режиме
    private Notification createNotification(WebPage page)
    {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
        views.setTextViewText(R.id.notification_text, page.getContent() != null && page.getContent().title() != null ? page.getContent().title() : "");


        Notification notification = new Notification();
        notification.contentView = views;
        notification.icon = R.drawable.ic_launcher_inverted;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        Intent intent = new Intent(this, DiaryListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        notification.contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        return notification;
    }
}
