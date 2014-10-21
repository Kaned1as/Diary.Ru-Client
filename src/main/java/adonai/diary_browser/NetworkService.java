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
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiaryProfilePage;
import adonai.diary_browser.entities.DiscListPage;
import adonai.diary_browser.entities.DiscPage;
import adonai.diary_browser.entities.ListPage;
import adonai.diary_browser.entities.Post;
import adonai.diary_browser.entities.SearchPage;
import adonai.diary_browser.entities.TagsPage;
import adonai.diary_browser.entities.Umail;
import adonai.diary_browser.entities.UmailListPage;
import adonai.diary_browser.entities.UmailPage;
import adonai.diary_browser.entities.WebPage;

public class NetworkService extends Service implements Callback, OnSharedPreferenceChangeListener
{
    private static final int NOTIFICATION_ID = 3; // I SWEAR IT'S RANDOM!!11
    private static final int NEWS_NOTIFICATION_ID = 4;
    private static final int PENDING_INTENT_ID = 1408;  // I SWEAR IT'S RANDOM!!11
    public static final String CUSTOM_CSS_CACHED_FILE = "custom.css";

    private static NetworkService mInstance = null;
    private static boolean mIsStarting = false;

    private static String mCssContent;

    public UserData mUser = new UserData();
    public DiaryHttpClient mDHCL = new DiaryHttpClient();
    public SharedPreferences mPreferences;

    private PowerManager.WakeLock waker;

    private Handler mHandler;
    private Looper mLooper; // петля времени

    boolean load_images;
    boolean is_sticky;
    boolean notify_on_updates;
    boolean keep_device_on;
    boolean preload_themes;
    boolean preload_umail;
    int orientation;

    private List<DiaryActivity> mListeners = new ArrayList<>();
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
        is_sticky = mPreferences.getBoolean("service.always.running", false);
        notify_on_updates = mPreferences.getBoolean("service.notify.updates", false);
        keep_device_on = mPreferences.getBoolean("service.keep.device.on", false);
        preload_themes = mPreferences.getBoolean("preload.themes", true);
        preload_umail = mPreferences.getBoolean("preload.umail.quoting", true);
        orientation = Integer.parseInt(mPreferences.getString("screen.orientation", "-1")); // default to UNSPECIFIED

        final HandlerThread thr = new HandlerThread("ServiceThread");
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

    private void notifyListeners(int opCode)
    {
        notifyListeners(opCode, null);
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

                    final String dataPage = mDHCL.getPageAsString(mUser.favoritesURL); // подойдет любая ссылка с дневников
                    if(dataPage == null)
                        break;

                    final Document rootNode = Jsoup.parse(dataPage);
                    mUser.parseData(rootNode);

                    final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if(mUser.newDiaryCommentsNum + mUser.newDiscussNum + mUser.newUmailNum > 0 && (!lastLinks[0].equals(mUser.newDiaryLink) || !lastLinks[1].equals(mUser.newDiscussLink) || !lastLinks[2].equals(mUser.newUmailLink))) // старые данные или нет?
                    {
                        lastLinks[0] = mUser.newDiaryLink; // устанавливаем линки на новые значения
                        lastLinks[1] = mUser.newDiscussLink;
                        lastLinks[2] = mUser.newUmailLink;

                        final RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);

                        views.setTextViewText(R.id.notification_title, getString(R.string.new_comments));
                        views.setTextViewText(R.id.notification_text, getString(R.string.my_diary) + ": " + mUser.newDiaryCommentsNum + " | " +
                                                                      getString(R.string.discussions) + ": " + mUser.newDiscussNum + " | " +
                                                                      getString(R.string.umail_activity_title) + ": " + mUser.newUmailNum);

                        final Notification notification = new Notification();
                        notification.contentView = views;
                        notification.icon = R.drawable.ic_launcher_inverted; // иконка
                        notification.ledOnMS = 1000;
                        notification.ledOffMS = 10000;
                        notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        notification.ledARGB = Color.parseColor("#FFD8BD");
                        notification.tickerText = getString(R.string.new_comments) + ": " + Integer.toString(mUser.newDiaryCommentsNum + mUser.newDiscussNum + mUser.newUmailNum);
                        notification.flags |= Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_AUTO_CANCEL;

                        final Intent intent = new Intent(this, DiaryListActivity.class); // при клике на уведомление открываем приложение
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        notification.contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
                        mNotificationManager.notify(NEWS_NOTIFICATION_ID, notification); // запускаем уведомление
                    }
                    else if (mUser.newDiscussNum + mUser.newDiaryCommentsNum + mUser.newUmailNum == 0)
                        mNotificationManager.cancel(NEWS_NOTIFICATION_ID);
                    break;
                }
                case Utils.HANDLE_JUST_DO_GET:
                {
                    if(mDHCL.getPageAsString(message.obj.toString()) != null)
                        notifyListeners(Utils.HANDLE_JUST_DO_GET);
                    break;
                }
                case Utils.HANDLE_DELETE_UMAILS:
                {
                    final Integer folderFrom = ((Pair<long[], Integer>) message.obj).second;
                    final long[] ids = ((Pair<long[], Integer>) message.obj).first;

                    final List<NameValuePair> nameValuePairs = new ArrayList<>();
                    nameValuePairs.add(new BasicNameValuePair("act", "umail_move"));
                    nameValuePairs.add(new BasicNameValuePair("module", "umail"));
                    nameValuePairs.add(new BasicNameValuePair("move_from_folder", folderFrom.toString()));
                    nameValuePairs.add(new BasicNameValuePair("move_to_folder", "0"));
                    nameValuePairs.add(new BasicNameValuePair("signature", mUser.signature));
                    nameValuePairs.add(new BasicNameValuePair("delm", "Удалить отмеченные"));
                    for(long id : ids)
                        nameValuePairs.add(new BasicNameValuePair("umail_check[]", Long.toString(id)));

                    mDHCL.postPageToString("http://www.diary.ru/diary.php", new UrlEncodedFormEntity(nameValuePairs, "WINDOWS-1251"));

                    notifyListeners(Utils.HANDLE_DELETE_UMAILS);
                    break;
                }
                case Utils.HANDLE_SET_HTTP_COOKIE:
                {
                    final List<NameValuePair> nameValuePairs = new ArrayList<>();
                    nameValuePairs.add(new BasicNameValuePair("user_login", mPreferences.getString(Utils.KEY_USERNAME, "")));
                    nameValuePairs.add(new BasicNameValuePair("user_pass", mPreferences.getString(Utils.KEY_PASSWORD, "")));
                    nameValuePairs.add(new BasicNameValuePair("save_on", "1"));

                    final String loginScreen = mDHCL.postPageToString("http://www.diary.ru/login.php", new UrlEncodedFormEntity(nameValuePairs, "WINDOWS-1251"));
                    final List<HttpCookie> cookies = mDHCL.getCookieStore().getCookies();

                    if(loginScreen == null)
                    { // no connection
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }

                    boolean user = false, password = false;
                    for(HttpCookie cookie : cookies)
                    {
                        switch (cookie.getName())
                        {
                            case "user_login":
                                user = true;
                                break;
                            case "user_pass":
                                password = true;
                                break;
                        }
                    }

                    if(!(user && password)) // not authorised
                    {
                        notifyListeners(Utils.HANDLE_AUTHORIZATION_ERROR);
                        break;
                    }

                    final CookieManager cookieManager = CookieManager.getInstance();
                    // cookieManager.removeSessionCookie();

                    for (HttpCookie cookie : cookies)
                    {
                        String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
                        cookieManager.setCookie("diary.ru", cookieString);
                    }
                    CookieSyncManager.getInstance().sync();
                    mUser.isAuthorised = true;

                    if(message.obj != null) // возвращаемся к загрузке
                        handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(message.obj, false));

                    notifyListeners(Utils.HANDLE_SET_HTTP_COOKIE);
                    break;
                }
                case Utils.HANDLE_GET_DISCUSSION_LIST_DATA:
                {

                    final int pos = (Integer) ((ArrayList<?>) message.obj).get(0);
                    final DiscPage dList = (DiscPage) ((ArrayList<?>) message.obj).get(1);
                    final boolean onlyNew = (Boolean) ((ArrayList<?>) message.obj).get(2);

                    String jsURL = dList.getURL();
                    if(onlyNew)
                        jsURL = jsURL + "&new";

                    final String dataPage = mDHCL.getPageAndContextAsString(jsURL);
                    if(dataPage == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }
                    serializeDiscussions(dataPage, dList.getDiscussions());

                    notifyListeners(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, pos);
                    break;
                }
                case Utils.HANDLE_PICK_URL:
                {
                    final String URL = ((Pair<String, Boolean>) message.obj).first;
                    boolean reload = ((Pair<String, Boolean>) message.obj).second;

                    if(!mUser.isAuthorised)
                        handleRequest(Utils.HANDLE_SET_HTTP_COOKIE, URL);
                    else
                        checkUrlAndHandle(URL, reload);

                    break;
                }
                case Utils.HANDLE_OPEN_FOLDER:
                {
                    final String uFolder = mDHCL.getPageAndContextAsString((String) message.obj);
                    if(uFolder == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }
                    serializeUmailListPage(uFolder);

                    notifyListeners(Utils.HANDLE_OPEN_FOLDER);
                    break;
                }
                case Utils.HANDLE_OPEN_MAIL:
                {
                    final String uMail = mDHCL.getPageAndContextAsString((String) message.obj);
                    if(uMail == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }
                    serializeUmailPage(uMail);

                    notifyListeners(Utils.HANDLE_OPEN_MAIL);
                    break;
                }
                case Utils.HANDLE_DELETE_POST:
                {
                    final String id = (String) message.obj;
                    final List<NameValuePair> postParams = new ArrayList<>();
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("act", "del_post_post"));
                    postParams.add(new BasicNameValuePair("post_id", id));
                    postParams.add(new BasicNameValuePair("yes", "Да"));
                    mDHCL.postPageToString(((DiaryPage) mUser.currentDiaryPage).getDiaryURL() + "diary.php", new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));

                    handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(mUser.currentDiaryPage.getPageURL(), true));
                    break;
                }
                case Utils.HANDLE_REPOST:
                {
                    final String URL = (String) message.obj;
                    final String dataPage = mDHCL.getPageAsString(URL);
                    if(dataPage == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }

                    try
                    {
                        final Post sendPost = serializePostEditPage(dataPage);
                        sendPost.diaryID = mUser.ownProfileID;
                        notifyListeners(Utils.HANDLE_REPOST, sendPost);
                        break;
                    }
                    catch (NullPointerException ex) // cannot serialize
                    {
                        notifyListeners(Utils.HANDLE_PAGE_INCORRECT);
                        break;
                    }
                }
                case Utils.HANDLE_DELETE_COMMENT:
                {
                    final String id = (String) message.obj;
                    mDHCL.getPageAsString(((DiaryPage) mUser.currentDiaryPage).getDiaryURL() + "?delcomment&commentid=" + id + "&js&signature=" + mUser.signature);

                    handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(mUser.currentDiaryPage.getPageURL(), true));
                    break;
                }
                case Utils.HANDLE_EDIT_POST:
                {
                    final String URL = (String) message.obj;
                    final String dataPage = mDHCL.getPageAsString(URL);
                    if(dataPage == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }

                    try
                    {
                        final Post sendPost = serializePostEditPage(dataPage);
                        sendPost.postID = URL.substring(URL.lastIndexOf("=") + 1);
                        sendPost.diaryID = ((DiaryPage)mUser.currentDiaryPage).getDiaryID();
                        notifyListeners(Utils.HANDLE_EDIT_POST, sendPost);
                        break;
                    }
                    catch (NullPointerException ex) // cannot serialize
                    {
                        notifyListeners(Utils.HANDLE_PAGE_INCORRECT);
                        break;
                    }
                }
                case Utils.HANDLE_PRELOAD_THEMES:
                {
                    final String URL = ((DiaryPage)mUser.currentDiaryPage).getDiaryURL() + "?newpost";
                    final String dataPage = mDHCL.getPageAsString(URL);
                    if(dataPage == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }

                    if(dataPage.contains("Нельзя опубликовать свою запись в чужом дневнике"))
                    {
                        notifyListeners(Utils.HANDLE_CLOSED_ERROR);
                        break;
                    }

                    final Post sendPost = serializePostEditPage(dataPage);
                    if(sendPost == null) // additional check due to nullptrs
                    {
                        notifyListeners(Utils.HANDLE_CLOSED_ERROR);
                        break;
                    }

                    sendPost.diaryID = ((DiaryPage)mUser.currentDiaryPage).getDiaryID();

                    notifyListeners(Utils.HANDLE_PRELOAD_THEMES, sendPost);
                    break;
                }
                case Utils.HANDLE_PRELOAD_UMAIL:
                {
                    final int type = (int) message.obj;
                    final String umailId = mUser.currentUmailPage.getUmailID();
                    final String URL = "http://www.diary.ru/u-mail/read/?" + (type == Utils.UMAIL_REPLY ? "reply" : "forward") + "&u_id=" + umailId;
                    final String dataPage = mDHCL.getPageAsString(URL);
                    if(dataPage == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }

                    final Umail sendMail = serializeUmailEditPage(dataPage, type);
                    if(sendMail == null) // additional check due to nullptrs
                    {
                        notifyListeners(Utils.HANDLE_CLOSED_ERROR);
                        break;
                    }

                    notifyListeners(Utils.HANDLE_PRELOAD_UMAIL, sendMail);
                    break;
                }
                case Utils.HANDLE_EDIT_COMMENT:
                {
                    final String URL = (String) message.obj;
                    final String dataPage = mDHCL.getPageAndContextAsString(URL);
                    if(dataPage == null)
                    {
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                        break;
                    }

                    try
                    {
                        Comment sendComment = serializeCommentEditPage(dataPage);
                        sendComment.commentID = URL.substring(URL.lastIndexOf("=") + 1);
                        sendComment.postID = ((CommentsPage)mUser.currentDiaryPage).getPostID();
                        notifyListeners(Utils.HANDLE_EDIT_COMMENT, sendComment);
                        break;
                    }
                    catch (NullPointerException ex) // cannot serialize
                    {
                        notifyListeners(Utils.HANDLE_PAGE_INCORRECT);
                        break;
                    }
                }
                default:
                    return false;
            }
        }
        catch (IOException ignored)
        {
            // all URLs are valid
        }

        return true;
    }

    private Post serializePostEditPage(String dataPage)
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        final Element rootNode = Jsoup.parse(dataPage).select("div.section").first(); // выбираем окошко с текстом
        if(rootNode == null)
            return null;

        final Post result = new Post();

        result.title = rootNode.select("input#postTitle.text").val();
        result.content = rootNode.select("textarea#message").text();

        final Elements communityThemes = rootNode.select("input[id^=favtg]");
        for(Element theme : communityThemes)
            result.predefinedTags.put(theme.val(), theme.hasAttr("checked"));

        result.themes += rootNode.select("input#tags.text").val();
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

        result.noComments = rootNode.select("input#nocomm").hasAttr("checked");

        return result;
    }

    private Umail serializeUmailEditPage(String dataPage, int type)
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        final Element rootNode = Jsoup.parse(dataPage);
        if(rootNode == null)
            return null;

        final Umail result = new Umail();
        result.messageTheme = mUser.currentUmailPage.getMessageTheme();
        result.reMessage = rootNode.select("textarea#remessage").first().ownText();
        if(type == Utils.UMAIL_REPLY) {
            result.receiver = mUser.currentUmailPage.getSenderName();
        }

        return result;
    }

    private Comment serializeCommentEditPage(String dataPage)
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Element rootNode = Jsoup.parse(dataPage).select("textarea#message").first(); // выбираем окошко с текстом
        Comment result = new Comment();

        result.content = rootNode.text();

        return result;
    }

    private void serializeDiaryListPage(String dataPage)
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        mUser.currentDiaries = new DiaryListPage(mDHCL.currentURL);

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

    private void serializeDiaryPage(String dataPage)
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        DiaryPage scannedDiary = new DiaryPage(mDHCL.currentURL);

        Element diaryTag = rootNode.select("[id=authorName]").first();
        if(diaryTag != null)
        {
            String authorProfile = diaryTag.getElementsByTag("a").last().attr("href");
            scannedDiary.setDiaryID(authorProfile.substring(authorProfile.lastIndexOf("?") + 1));
            scannedDiary.userLinks.put(getString(R.string.author_diary), scannedDiary.getDiaryURL());
            scannedDiary.userLinks.put(getString(R.string.author_profile), authorProfile);
        }


        // заполняем ссылки (пока что только какие можем обработать)
        // TODO: сделать generic-обработчик всех таких ссылок и вынести в новую процедуру (убрать tags)
        Elements userLinks = rootNode.select("div#thisCommunityMember li, div#thisCommunity li, div#thisDiaryLinks li");
        for(Element link : userLinks)
            if(link.id().equals("communityMyPosts") || link.id().equals("communityFav") || link.id().equals("communityQuote") ||
               link.id().equals("authorFav") || link.id().equals("authorQuot"))
                scannedDiary.userLinks.put(link.text(), link.child(0).attr("href")); //they all contain <a> tag first

        notifyListeners(Utils.HANDLE_PROGRESS_2);
        Elements postsArea = rootNode.select("[id~=post\\d+], div.pageBar");
        if(postsArea.isEmpty()) { // Нет вообще никаких постов, заканчиваем
            notifyListeners(Utils.HANDLE_NOTFOUND_ERROR);
            return;
        }

        Elements result = postsArea.clone();
        Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
        {
            resultPage.body().appendChild(to);
        }
        parseContent(resultPage);
        scannedDiary.setContent(resultPage.html());
        scannedDiary.setTitle(resultPage.title());

        mUser.currentDiaryPage = scannedDiary;
    }

    private void serializeSearchPage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        final Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        final SearchPage scannedSearch = new SearchPage(mDHCL.currentURL);

        notifyListeners(Utils.HANDLE_PROGRESS_2);
        final String searchText = rootNode.select("input[name=q]").val();
        final Elements postsArea = rootNode.select("[id~=post\\d+], div#pageBar");
        if(postsArea.isEmpty()) { // Нет вообще никаких постов, заканчиваем
            notifyListeners(Utils.HANDLE_NOTFOUND_ERROR);
            return;
        }


        final Elements result = postsArea.clone();
        final Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(final Element to : result)
            resultPage.body().appendChild(to);

        parseContent(resultPage);
        scannedSearch.setContent(resultPage.html());
        scannedSearch.setTitle(resultPage.title() + searchText);

        mUser.currentDiaryPage = scannedSearch;
    }

    private void serializeCommentsPage(String dataPage) throws IOException
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        CommentsPage scannedPost = new CommentsPage(mDHCL.currentURL.substring(0, mDHCL.currentURL.lastIndexOf('/') + 1));

        Element diaryTag = rootNode.select("#authorName").first();
        if(diaryTag != null)
        {
            String authorProfile = diaryTag.getElementsByTag("a").last().attr("href");
            scannedPost.setDiaryID(authorProfile.substring(authorProfile.lastIndexOf("?") + 1));
            scannedPost.userLinks.put(getString(R.string.author_diary), scannedPost.getDiaryURL());
            scannedPost.userLinks.put(getString(R.string.author_profile), authorProfile);
        }

        Elements userLinks = rootNode.select("div#thisCommunityMember li, div#thisCommunity li, div#thisDiaryLinks li");
        for(Element link : userLinks)
            if(link.id().equals("communityMyPosts") || link.id().equals("communityFav") || link.id().equals("communityQuote") ||
               link.id().equals("authorFav") || link.id().equals("authorQuot"))
                scannedPost.userLinks.put(link.text(), link.child(0).attr("href")); //they all contain <a> tag first



        notifyListeners(Utils.HANDLE_PROGRESS_2);
        Elements effectiveAreas = rootNode.select("[id~=post\\d+], [id~=comment\\d+], div.pageBar");
        if(effectiveAreas.isEmpty()) { // Нет вообще никаких постов, заканчиваем
            notifyListeners(Utils.HANDLE_NOTFOUND_ERROR);
            return;
        }

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

        scannedPost.setContent(resultPage.html());
        scannedPost.setTitle(resultPage.title());
        mUser.currentDiaryPage = scannedPost;
    }


    private void serializeProfilePage(String dataPage)
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        DiaryProfilePage profilePage = new DiaryProfilePage(mDHCL.currentURL);

        Elements effectiveAreas = rootNode.select("div#contant");
        Elements result = effectiveAreas.clone();
        notifyListeners(Utils.HANDLE_PROGRESS_2);

        Document resultPage = Document.createShell(mDHCL.currentURL);
        resultPage.title(rootNode.title());
        for(Element to : result)
            resultPage.body().appendChild(to);

        parseContent(resultPage);

        profilePage.setContent(resultPage.html());
        profilePage.setTitle(resultPage.title());
        mUser.currentDiaryPage = profilePage;
    }

    private void serializeTagsPage(String dataPage) throws IOException
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        TagsPage scannedTags = new TagsPage(mDHCL.currentURL.substring(0, mDHCL.currentURL.lastIndexOf('/') + 1));

        Element diaryTag = rootNode.select("#authorName").first();
        if(diaryTag != null)
        {
            String Id = diaryTag.getElementsByTag("a").last().attr("href");
            scannedTags.setDiaryID(Id.substring(Id.lastIndexOf("?") + 1));
        }

        notifyListeners(Utils.HANDLE_PROGRESS_2);
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
        scannedTags.setContent(resultPage.html());
        scannedTags.setTitle(resultPage.title());
        mUser.currentDiaryPage = scannedTags;
    }

    private void serializeDiscussionsPage(String dataPage)
    {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        mUser.discussions.clear();
        mUser.discussions.setURL(mDHCL.currentURL);

        notifyListeners(Utils.HANDLE_PROGRESS_2);
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
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        mUser.currentUmails = new DiaryListPage(mDHCL.currentURL);

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
                UmailListPage mail = new UmailListPage();
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
        notifyListeners(Utils.HANDLE_PROGRESS);

        Document rootNode = Jsoup.parse(dataPage);
        mUser.parseData(rootNode);

        scannedUmail.setUmailURL(mDHCL.currentURL);
        scannedUmail.setUmailID(scannedUmail.getUmailURL().substring(scannedUmail.getUmailURL().lastIndexOf('=') + 1));
        notifyListeners(Utils.HANDLE_PROGRESS_2);

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

        scannedUmail.setContent(resultPage.html());
        scannedUmail.setTitle(resultPage.title());
        mUser.currentUmailPage = scannedUmail;
    }

    private void parseContent(Document resultPage)
    {
        // страница будет иметь наш стиль
        resultPage.outputSettings().prettyPrint(false).escapeMode(Entities.EscapeMode.none);
        //getAssets().open("css/journal.css");
        //resultPage.head().append("<link rel=\"stylesheet\" href=\"file:///android_asset/css/journal.css\" type=\"text/css\" media=\"all\" title=\"Стандарт\"/>");
        if(!getCssContent(this).isEmpty()) {
            resultPage.head().append("<style type=\"text/css\" media=screen>" + mCssContent +"</style>");
        }

        // кнопка репоста указывает на нужную ссылку
        Elements shareLinks = resultPage.select(".postLinks li[class^=quote]");
        for(Element shareLi : shareLinks) {
            if(shareLi.childNodeSize() == 0)
                continue;

            Element repostLink = shareLi.child(0);
            Element diaryRepost = shareLi.select("div a[href*=newpost]").first();
            if(diaryRepost != null)
                repostLink.attr("href", diaryRepost.attr("href"));
        }

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
                        // все неподходящие под критерии изображения на странице будут заменены на кнопки, по клику на которые и будут открываться
                        String jsButton = "<input type='image' src='file:///android_asset/images/load_image.png' onclick='return handleIMGDown(this, \"" + src + "\")' />";

                        current.after(jsButton);
                        current.remove();
                    }
                }

                if(current.tagName().equals("a"))
                {
                    String src = current.getElementsByTag("img").attr("src");
                    if(!src.contains("diary.ru") && !current.parent().className().equals("avatar") && !src.startsWith("/"))
                    {
                        // все неподходящие под критерии изображения на странице будут заменены на кнопки, по клику на которые и будут открываться
                        String jsButton = "<input type='image' src='file:///android_res/drawable/load_image.png' onclick='return handleADown(this, \"" + current.attr("href") + "\", \"" + src + "\")' />";

                        current.after(jsButton);
                        current.remove();
                    }
                }
            }
        }
        resultPage.body().append(Utils.javascriptContent);
    }

    private void checkUrlAndHandle(String requestedUrl, boolean reload)
    {   
        Class<?> handled;
        Object cachedPage = null;
        String dataPage = null;

        try
        {
            if(CacheManager.getInstance().hasPage(requestedUrl) && !reload)
            {
                cachedPage = CacheManager.getInstance().loadPageFromCache(requestedUrl);
                handled = cachedPage.getClass();
            }
            else
            {
                final HttpURLConnection page = mDHCL.getPageAndContext(requestedUrl);
                if(page == null)
                {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                    return;
                }

                if(page.getContentType() != null && page.getContentType().contains("image")) // Just load image, no further processing
                {
                    if(reload) // reload - save
                    {
                        final String srcName = page.getHeaderField("Content-Disposition");
                        final String realName = URLUtil.guessFileName(requestedUrl, srcName != null ? srcName : null, MimeTypeMap.getFileExtensionFromUrl(requestedUrl));
                        CacheManager.saveDataToSD(getApplicationContext(), realName, mDHCL.getResponseBytes(page));
                    }
                    else // no reload - open
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA, requestedUrl);
                    return;
                }

                dataPage = mDHCL.getResponseString(page);
                handled = Utils.checkDiaryUrl(dataPage);
            }

            if(handled != null) // Если это страничка дайри
            {
                if(cachedPage != null) // если страничка была в кэше
                {
                    mDHCL.currentURL = requestedUrl;

                    if(cachedPage instanceof DiaryListPage)
                    {
                        mUser.currentDiaries = (DiaryListPage) cachedPage;
                        notifyListeners(Utils.HANDLE_GET_LIST_PAGE_DATA);
                    }

                    if(cachedPage instanceof DiscListPage)
                    {
                        mUser.discussions = (DiscListPage) cachedPage;
                        notifyListeners(Utils.HANDLE_GET_DISCUSSIONS_DATA);
                    }

                    if(cachedPage instanceof WebPage)
                    {
                        mUser.currentDiaryPage = (WebPage) cachedPage;
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
                    }
                }
                else // если нет такого кэша
                {
                    if(handled == DiaryPage.class)
                    {
                        serializeDiaryPage(dataPage);
                        CacheManager.getInstance().putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
                    }
                    else if (handled == CommentsPage.class)
                    {
                        serializeCommentsPage(dataPage);
                        CacheManager.getInstance().putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
                    }
                    else if(handled == TagsPage.class)
                    {
                        serializeTagsPage(dataPage);
                        CacheManager.getInstance().putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
                    }
                    else if(handled == DiaryProfilePage.class)
                    {
                        serializeProfilePage(dataPage);
                        CacheManager.getInstance().putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
                    }
                    else if(handled == DiaryListPage.class)
                    {
                        serializeDiaryListPage(dataPage);
                        CacheManager.getInstance().putPageToCache(requestedUrl, mUser.currentDiaries);
                        notifyListeners(Utils.HANDLE_GET_LIST_PAGE_DATA);
                    }
                    else if(handled == DiscListPage.class)
                    {
                        serializeDiscussionsPage(dataPage);
                        CacheManager.getInstance().putPageToCache(mUser.discussionsURL, mUser.discussions);
                        notifyListeners(Utils.HANDLE_GET_DISCUSSIONS_DATA);
                    }
                    else if(handled == SearchPage.class)
                    {
                        serializeSearchPage(dataPage);
                        CacheManager.getInstance().putPageToCache(mDHCL.currentURL, mUser.currentDiaryPage);
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
                    }
                }
            }
            else // неопознанная страничка
            {
                assert(cachedPage == null);
                if(requestedUrl.contains("diary.ru") && dataPage.contains("закрыт") || dataPage.contains("попробовать что-нибудь еще")) // если наткнулись на ошибку дневника
                    notifyListeners(Utils.HANDLE_CLOSED_ERROR);
                else
                {
                    final Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestedUrl));
                    // createChooser создает новый Intent из предыдущего, флаги нужно присоединять уже к нему!
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
                }
            }
        }
        catch (NullPointerException e)
        {
            notifyListeners(Utils.HANDLE_PAGE_INCORRECT);
        }
        catch (Exception e)
        {
            notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR);
        }

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        switch (key)
        {
            case "images.autoload":
                load_images = sharedPreferences.getBoolean(key, false);
                break;
            case "service.notify.updates":
                mHandler.removeMessages(Utils.HANDLE_SERVICE_UPDATE);
                notify_on_updates = sharedPreferences.getBoolean(key, false);
                if (notify_on_updates)
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(Utils.HANDLE_SERVICE_UPDATE), 300000);
                break;
            case "service.keep.device.on":
                if (waker.isHeld())
                    waker.release();

                keep_device_on = sharedPreferences.getBoolean(key, false);
                if (keep_device_on)
                    waker.acquire();
                break;
            case "service.always.running":
                is_sticky = sharedPreferences.getBoolean(key, false);
                if (is_sticky)
                    startForeground(NOTIFICATION_ID, createNotification(mUser.currentDiaryPage));
                else
                    stopForeground(true);
                break;
            case "webview.font.size":
                for (DiaryActivity current : mListeners)
                    current.handleFontChange(sharedPreferences.getString("webview.font.size", "8"));
                break;
            case "preload.themes":
                preload_themes = sharedPreferences.getBoolean("preload.themes", true);
                break;
            case "preload.umail.quoting":
                preload_umail = sharedPreferences.getBoolean("preload.umail.quoting", true);
                break;
            case "screen.orientation":
                orientation = Integer.parseInt(sharedPreferences.getString("screen.orientation", "-1"));
                break;
        }
    }

    // Создаем уведомление в статусной строке - для принудительно живого сервиса в Foreground-режиме
    private Notification createNotification(WebPage page)
    {
        final RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
        views.setTextViewText(R.id.notification_text, page.getContent() != null && page.getTitle() != null ? page.getTitle() : "");


        final Notification notification = new Notification();
        notification.contentView = views;
        notification.icon = R.drawable.ic_launcher_inverted;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        final Intent intent = new Intent(this, DiaryListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        notification.contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        return notification;
    }

    public void newSession()
    {
        mUser = new UserData();
        mDHCL.getCookieStore().removeAll();
        CacheManager.getInstance().clear();
    }

    private static String getCssContent(Context ctx) {
        if(mCssContent == null) {
            mCssContent = retrieveCss(ctx);
        }
        return mCssContent;
    }

    private static void setCssContent(Context ctx, String content) {
        mCssContent = content;
    }

    /**
     * Retrieves current css content from cached file or android assets (on first launch)
     * @param ctx context to live in
     * @return String representing file contents
     */
    private static String retrieveCss(Context ctx) {
        try {
            if(CacheManager.getInstance().hasData(ctx, CUSTOM_CSS_CACHED_FILE)) {
                return new String(CacheManager.getInstance().retrieveData(ctx, CUSTOM_CSS_CACHED_FILE));
            } else {
                InputStream is = ctx.getAssets().open("css/journal.css");
                String contents = Utils.getStringFromInputStream(is);
                CacheManager.getInstance().cacheData(ctx, contents.getBytes(), CUSTOM_CSS_CACHED_FILE);
                return contents;
            }
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Css file parsed to retrieve colors
     * @param ctx context to live in
     * @return mapping of colors like (Name:color)
     */
    public static Map<String, String> getCssColors(Context ctx) {
        String regex = ":\\s?(#[\\w\\d]+);?\\s+/\\*\\s(.*?)\\s\\*/";
        Pattern parser = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = parser.matcher(getCssContent(ctx));
        Map<String, String> result = new HashMap<>();
        while (matcher.find()) {
            result.put(matcher.group(2), matcher.group(1));
        }
        return result;
    }

    /**
     * Css string is replaced with colors put in
     * @param ctx context to live in
     * @param replacements  mapping of colors like (Name:color)
     */
    public static void replaceCssColors(Context ctx, Map<String, String> replacements) {
        String css = getCssContent(ctx);
        String regex = ":\\s?(#[\\w\\d]+);?\\s+/\\*\\s(.*?)\\s\\*/";
        Pattern parser = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = parser.matcher(css);
        while (matcher.find()) {
            if (replacements.containsKey(matcher.group(2))) {
                css = css.replace(matcher.group(), ": " + replacements.get(matcher.group(2)) + ";   /* " + matcher.group(2) + " */");
                replacements.remove(matcher.group(2));
                matcher = parser.matcher(css);
            }
        }
        try {
            setCssContent(ctx, css);
            CacheManager.getInstance().cacheData(ctx, css.getBytes(), CUSTOM_CSS_CACHED_FILE);
        } catch (IOException e) {
            Toast.makeText(ctx, R.string.io_error, Toast.LENGTH_SHORT).show();
        }
    }

    public static void resetCssColors(Context ctx) {
        try {
            CacheManager.getInstance().dropData(ctx, CUSTOM_CSS_CACHED_FILE);
            mCssContent = null;
        } catch (IOException e) {
            Toast.makeText(ctx, R.string.io_error, Toast.LENGTH_SHORT).show();
        }
    }
}
