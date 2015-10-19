package adonai.diary_browser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryLinkList;
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

public class NetworkService extends Service implements Callback, OnSharedPreferenceChangeListener {

    private static final int NOTIFICATION_ID = 3; // i swear it's random
    private static final int NEWS_NOTIFICATION_ID = 4;
    
    public static final String SHARED_PROP_IMAGES_AUTOLOAD = "images.autoload";
    public static final String SHARED_PROP_SERVICE_ALWAYS_RUNNING = "service.always.running";
    public static final String SHARED_PROP_SERVICE_NOTIFY_UPDATES = "service.notify.updates";
    public static final String SHARED_PROP_SERVICE_KEEP_DEVICE_ON = "service.keep.device.on";
    public static final String SHARED_PROP_PRELOAD_THEMES = "preload.themes";
    public static final String SHARED_PROP_PRELOAD_UMAIL_QUOTING = "preload.umail.quoting";
    public static final String SHARED_PROP_USE_TEXT_LINKS = "use.text.links";
    public static final String SHARED_PROP_SCREEN_ORIENTATION = "screen.orientation";
    public static final String SHARED_PROP_WEBVIEW_FONT_SIZE = "webview.font.size";
    public static final String SHARED_PROP_DEFAULT_TAB = "default.list.tab";

    // self state
    private static NetworkService mInstance = null;
    private static boolean mIsStarting = false;

    // diary data
    public DiaryHttpClient mNetworkClient = new DiaryHttpClient();
    private List<DiaryActivity> mListeners = new ArrayList<>(2);
    private SharedPreferences mPreferences;
    
    // settings
    private boolean mLoadImages;
    private boolean mIsStickyService;
    private boolean mNotifyOnUpdates;
    private boolean mKeepDeviceOn;
    boolean mPreloadThemes;
    boolean mPreloadUmails;
    boolean mUseTextInsteadOfImages;
    int mOrientation;
    
    // service data
    private CacheManager mCache = CacheManager.getInstance();
    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler;
    private Looper mLooper;
    private Bitmap mNotificationIcon;
    private NotifyState mCurrentLinkSet = new NotifyState();

    /*
    К сожалению, НЕТ другой возможности запустить сервис.
    Контекст способен к запуску сервиса только если цикл его главного потока выполняется.
    Поэтому НЕЛЬЗЯ остановить контекст, создающий сервис и подождать пока он запустится
    Из-за этого, в частности, и нужен механизм с HANDLE_APP_START.
    */
    public static NetworkService getInstance(Context context) {
        if (mInstance == null && !mIsStarting) {
            context.startService(new Intent(context, NetworkService.class));
            mIsStarting = true;
        }
        return mInstance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "diary.client");

        mLoadImages = mPreferences.getBoolean(SHARED_PROP_IMAGES_AUTOLOAD, false);
        mIsStickyService = mPreferences.getBoolean(SHARED_PROP_SERVICE_ALWAYS_RUNNING, false);
        mNotifyOnUpdates = mPreferences.getBoolean(SHARED_PROP_SERVICE_NOTIFY_UPDATES, false);
        mKeepDeviceOn = mPreferences.getBoolean(SHARED_PROP_SERVICE_KEEP_DEVICE_ON, false);
        mPreloadThemes = mPreferences.getBoolean(SHARED_PROP_PRELOAD_THEMES, true);
        mPreloadUmails = mPreferences.getBoolean(SHARED_PROP_PRELOAD_UMAIL_QUOTING, true);
        mUseTextInsteadOfImages = mPreferences.getBoolean(SHARED_PROP_USE_TEXT_LINKS, false);
        mOrientation = Integer.parseInt(mPreferences.getString(SHARED_PROP_SCREEN_ORIENTATION, "-1")); // default to UNSPECIFIED

        final HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, this);

        if (mNotifyOnUpdates)
            mHandler.sendMessageDelayed(mHandler.obtainMessage(Utils.HANDLE_SERVICE_UPDATE), 300000);

        if (mKeepDeviceOn)
            mWakeLock.acquire();

        if (mIsStickyService)
            startForeground(NOTIFICATION_ID, createNotification(UserData.getInstance().getCurrentDiaryPage()));

        Bitmap appIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_inverted);
        Resources res = getResources();
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        mNotificationIcon = Bitmap.createScaledBitmap(appIcon, width, height, false);

        mInstance = this;
        mIsStarting = false;
    }

    @Override
    public void onDestroy() {
        mInstance = null;
        if (mWakeLock.isHeld())
            mWakeLock.release();
        mLooper.quit();

        // убираем значок
        stopForeground(true);

        // убираем вызовы регистрантам
        for (DiaryActivity listener : mListeners) {
            listener.mUiHandler.removeCallbacksAndMessages(null);
            listener.handleUi(Utils.HANDLE_SERVICE_ERROR, null);
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return mIsStickyService ? START_STICKY : START_NOT_STICKY;
    }

    public void handleRequest(int opcode, Object message) {
        mHandler.sendMessage(mHandler.obtainMessage(opcode, message));
    }

    public void addListener(DiaryActivity listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
            listener.handleFontChange(mPreferences.getString(SHARED_PROP_WEBVIEW_FONT_SIZE, "12"));
        }
    }

    public void removeListener(DiaryActivity listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners(int opCode, Object body) {
        for (DiaryActivity listener : mListeners)
            listener.handleUi(opCode, body);
    }

    private void notifyListeners(int opCode) {
        notifyListeners(opCode, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case Utils.HANDLE_SERVICE_UPDATE: { // уведомления о новых комментариях раз в 5 минут
                mHandler.sendMessageDelayed(mHandler.obtainMessage(Utils.HANDLE_SERVICE_UPDATE), 300000); // убедимся, что будем уведомлять и дальше

                final String dataPage = mNetworkClient.getPageAsString(UserData.getInstance().getFavoritesUrl()); // подойдет любая ссылка с дневников
                if (dataPage == null)
                    break;

                final Document rootNode = Jsoup.parse(dataPage);
                UserData.getInstance().updateData(rootNode);
                notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

                final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (UserData.getInstance().hasNotifications() && mCurrentLinkSet.shouldNotify(UserData.getInstance())) { // старые данные или нет?
                    mCurrentLinkSet.lastDiaryLink = UserData.getInstance().getNewDiaryLink(); // устанавливаем линки на новые значения
                    mCurrentLinkSet.lastDiscussionLink = UserData.getInstance().getNewDiscussLink();
                    mCurrentLinkSet.lastUmailLink = UserData.getInstance().getNewUmailLink();

                    NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
                    nBuilder.setContentTitle(getString(R.string.new_comments));
                    nBuilder.setContentText(
                            getString(R.string.my_diary) + ": " + UserData.getInstance().getNewDiaryCommentsNum() + " | " +
                            getString(R.string.discussions) + ": " + UserData.getInstance().getNewDiscussNum() + " | " +
                            getString(R.string.umail_activity_title) + ": " + UserData.getInstance().getNewUmailNum());
                    nBuilder.setSmallIcon(R.drawable.ic_launcher_status_icon);
                    nBuilder.setLargeIcon(mNotificationIcon);
                    nBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    nBuilder.setTicker(getString(R.string.new_comments) + ": " +
                            Integer.toString(UserData.getInstance().getNewDiaryCommentsNum() +
                                            UserData.getInstance().getNewDiscussNum() +
                                            UserData.getInstance().getNewUmailNum()));
                    nBuilder.setOnlyAlertOnce(true);
                    nBuilder.setAutoCancel(true);

                    final Intent intent = new Intent(this, DiaryListActivity.class); // при клике на уведомление открываем приложение
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    String newUrl = UserData.getInstance().getMostRecentNotification();
                    if(newUrl != null) { // we don't support U-Mails for now
                        intent.putExtra("url", newUrl);
                    }
                    
                    nBuilder.setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                    mNotificationManager.notify(NEWS_NOTIFICATION_ID, nBuilder.build()); // запускаем уведомление
                } else if (!UserData.getInstance().hasNotifications()) {
                    mNotificationManager.cancel(NEWS_NOTIFICATION_ID);
                }
                break;
            }
            case Utils.HANDLE_JUST_DO_GET: {
                if (mNetworkClient.getPageAsString(message.obj.toString()) != null)
                    notifyListeners(Utils.HANDLE_JUST_DO_GET);
                break;
            }
            case Utils.HANDLE_QUERY_ONLINE: {
                final String dataPage = mNetworkClient.getPageAsString("http://www.diary.ru");
                if (dataPage == null) { // no connection
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }
                HashMap<Integer, Spanned> onlineUsers = new HashMap<>(2);
                try {
                    Document rootNode = Jsoup.parse(dataPage);
                    UserData.getInstance().updateData(rootNode);
                    notifyListeners(Utils.HANDLE_UPDATE_HEADERS);
                    Element content = rootNode.getElementById("container");

                    Element favoritesOnline = content.select("span.sel:containsOwn(в том числе мои избранные) + div.sp").first();
                    onlineUsers.put(R.string.favourites_online, Html.fromHtml(favoritesOnline.html().replace("/member/", "http://www.diary.ru/member/")));

                    Element subscribersOnline = content.select("span.sel:containsOwn(и мои постоянные читатели) + div.sp").first();
                    onlineUsers.put(R.string.subscribers_online, Html.fromHtml(subscribersOnline.html().replace("/member/", "http://www.diary.ru/member/")));
                } catch (Exception ignored) {

                }
                notifyListeners(Utils.HANDLE_QUERY_ONLINE, onlineUsers);
                break;
            }
            case Utils.HANDLE_DELETE_UMAILS: {
                final Integer folderFrom = ((Pair<long[], Integer>) message.obj).second;
                final long[] ids = ((Pair<long[], Integer>) message.obj).first;

                final List<Pair<String, String>> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(Pair.create("act", "umail_move"));
                nameValuePairs.add(Pair.create("module", "umail"));
                nameValuePairs.add(Pair.create("move_from_folder", folderFrom.toString()));
                nameValuePairs.add(Pair.create("move_to_folder", "0"));
                nameValuePairs.add(Pair.create("signature", UserData.getInstance().getSignature()));
                nameValuePairs.add(Pair.create("delm", "Удалить отмеченные"));
                for (long id : ids)
                    nameValuePairs.add(Pair.create("umail_check[]", Long.toString(id)));

                mNetworkClient.postPageToString(nameValuePairs);
                notifyListeners(Utils.HANDLE_DELETE_UMAILS);
                break;
            }
            case Utils.HANDLE_AUTHORIZE: {
                final List<Pair<String, String>> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(Pair.create("user_login", mPreferences.getString(Utils.KEY_USERNAME, "")));
                nameValuePairs.add(Pair.create("user_pass", mPreferences.getString(Utils.KEY_PASSWORD, "")));
                nameValuePairs.add(Pair.create("save", "on"));

                String loginScreen = mNetworkClient.postPageToString(Utils.LOGIN_PAGE, nameValuePairs);

                if (loginScreen == null) { // no connection
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }
                
                if(loginScreen.contains(DiaryHttpClient.CLOUDFLARE_ANCHOR) && !mNetworkClient.hasCookie("cf_clearance")) {
                    notifyListeners(Utils.HACKING_CLOUDFLARE);
                    if(mNetworkClient.cloudFlareSolve(loginScreen)) {
                        loginScreen = mNetworkClient.postPageToString(Utils.LOGIN_PAGE, nameValuePairs);
                    } else { // couldn't solve
                        notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.captcha_error);
                        break;
                    }
                }

                if(loginScreen.contains("недоступен")) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.page_incorrect);
                    break;
                }
                
                if(loginScreen.contains("CAPTCHA")) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.captcha_error);
                    break;
                }

                if (!mNetworkClient.hasCookie("user_login") || !mNetworkClient.hasCookie("user_pass")) { // not authorized
                    notifyListeners(Utils.HANDLE_AUTHORIZATION_ERROR);
                    break;
                }

                UserData.getInstance().setAuthorized(true);
                String mainPage = mNetworkClient.getPageAsString(Utils.MAIN_PAGE);
                if(mainPage == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }
                UserData.getInstance().updateData(serializeMainPage(mainPage)); // get initial links

                notifyListeners(Utils.HANDLE_AUTHORIZE);
                break;
            }
            case Utils.HANDLE_GET_DISCUSSION_LIST_DATA: {
                final int pos = (Integer) ((ArrayList<?>) message.obj).get(0);
                final DiscPage dList = (DiscPage) ((ArrayList<?>) message.obj).get(1);
                final boolean onlyNew = (Boolean) ((ArrayList<?>) message.obj).get(2);

                String jsURL = dList.getURL();
                if (onlyNew)
                    jsURL = jsURL + "&new";

                final String dataPage = mNetworkClient.getPageAsString(jsURL);
                if (dataPage == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }
                serializeDiscussions(dataPage, dList.getDiscussions());

                notifyListeners(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, pos);
                break;
            }
            case Utils.HANDLE_PICK_URL: {
                final String URL = ((Pair<String, Boolean>) message.obj).first;
                boolean reload = ((Pair<String, Boolean>) message.obj).second;
                checkUrlAndHandle(URL, reload);
                break;
            }
            case Utils.HANDLE_OPEN_FOLDER: {
                String url = (String) message.obj;
                mNetworkClient.setCurrentUrl(mNetworkClient.resolve(url));
                final String uFolder = mNetworkClient.getPageAsString(url);
                if (uFolder == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }
                serializeUmailListPage(uFolder);

                notifyListeners(Utils.HANDLE_OPEN_FOLDER);
                break;
            }
            case Utils.HANDLE_OPEN_MAIL: {
                String url = (String) message.obj;
                mNetworkClient.setCurrentUrl(mNetworkClient.resolve(url));
                final String uMail = mNetworkClient.getPageAsString(url);
                if (uMail == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }
                serializeUmailPage(uMail);

                notifyListeners(Utils.HANDLE_OPEN_MAIL);
                break;
            }
            case Utils.HANDLE_DELETE_POST_DRAFT:
            case Utils.HANDLE_DELETE_POST: {
                final String id = (String) message.obj;
                final List<Pair<String, String>> postParams = new ArrayList<>();
                postParams.add(Pair.create("module", "journal"));
                postParams.add(Pair.create("act", "del_post_post"));
                postParams.add(Pair.create("post_id", id));
                postParams.add(Pair.create("yes", "Да"));

                if(message.what == Utils.HANDLE_DELETE_POST_DRAFT) { // удаляем черновик
                    postParams.add(Pair.create("draft", ""));
                }

                mNetworkClient.postPageToString(postParams);
                handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(mNetworkClient.getCurrentUrl(), true));
                break;
            }
            case Utils.HANDLE_REPOST: {
                final String URL = (String) message.obj;
                final String dataPage = mNetworkClient.getPageAsString(URL);
                if (dataPage == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }

                try {
                    final Post sendPost = serializePostEditPage(dataPage);
                    sendPost.diaryID = UserData.getInstance().getOwnProfileId();
                    notifyListeners(Utils.HANDLE_REPOST, sendPost);
                    break;
                } catch (NullPointerException ex) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.page_incorrect);
                    break;
                }
            }
            case Utils.HANDLE_DELETE_COMMENT: {
                final String id = (String) message.obj;
                mNetworkClient.getPageAsString(((DiaryPage) UserData.getInstance().getCurrentDiaryPage()).getDiaryUrl() + "?delcomment&commentid=" + id + "&js&signature=" + UserData.getInstance().getSignature());
                handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(mNetworkClient.getCurrentUrl(), true));
                break;
            }
            case Utils.HANDLE_DELETE_TAG: {
                final String referer = (String) message.obj;

                final List<Pair<String, String>> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(Pair.create("referer", referer));
                nameValuePairs.add(Pair.create("signature", UserData.getInstance().getSignature()));
                nameValuePairs.add(Pair.create("confirm", "Да"));
                
                mNetworkClient.postPageToString(referer, nameValuePairs);
                handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(mNetworkClient.getCurrentUrl(), true));
                break;
            }
            case Utils.HANDLE_EDIT_POST: {
                final String url = (String) message.obj;
                final String dataPage = mNetworkClient.getPageAsString(url);
                if (dataPage == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }

                try {
                    final Post sendPost = serializePostEditPage(dataPage);
                    sendPost.postID = url.substring(url.lastIndexOf("=") + 1);
                    sendPost.diaryID = ((DiaryPage) UserData.getInstance().getCurrentDiaryPage()).getDiaryId();
                    sendPost.postType = url.endsWith("draft") ? "draft" : "";
                    notifyListeners(Utils.HANDLE_EDIT_POST, sendPost);
                    break;
                } catch (NullPointerException ex) { // cannot serialize
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.page_incorrect);
                    break;
                }
            }
            case Utils.HANDLE_PRELOAD_THEMES: {
                // handle 'favorite' page case
                String currentUrl = ((DiaryPage) UserData.getInstance().getCurrentDiaryPage()).getDiaryUrl();
                String diaryUrl = currentUrl.contains("/")
                        ? currentUrl.substring(0, currentUrl.lastIndexOf('/') + 1)
                        : currentUrl;
                final String URL = diaryUrl + "?newpost";
                final String dataPage = mNetworkClient.getPageAsString(URL);
                if (dataPage == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }

                if (dataPage.contains("Нельзя опубликовать свою запись в чужом дневнике")) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.closed_error);
                    break;
                }

                final Post sendPost = serializePostEditPage(dataPage);
                if (sendPost == null) { // additional check due to nullptrs
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }

                sendPost.diaryID = ((DiaryPage) UserData.getInstance().getCurrentDiaryPage()).getDiaryId();

                notifyListeners(Utils.HANDLE_PRELOAD_THEMES, sendPost);
                break;
            }
            case Utils.HANDLE_PRELOAD_UMAIL: {
                final int type = (int) message.obj;
                final String umailId = UserData.getInstance().getCurrentUmailPage().getUmailID();
                final String URL = "http://www.diary.ru/u-mail/read/?" + (type == Utils.UMAIL_REPLY ? "reply" : "forward") + "&u_id=" + umailId;
                final String dataPage = mNetworkClient.getPageAsString(URL);
                if (dataPage == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }

                final Umail sendMail = serializeUmailEditPage(dataPage, type);
                if (sendMail == null) { // additional check due to nullptrs
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }

                notifyListeners(Utils.HANDLE_PRELOAD_UMAIL, sendMail);
                break;
            }
            case Utils.HANDLE_EDIT_COMMENT: {
                final String URL = (String) message.obj;
                final String dataPage = mNetworkClient.getPageAsString(URL);
                if (dataPage == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }

                try {
                    Comment sendComment = serializeCommentEditPage(dataPage);
                    sendComment.commentID = URL.substring(URL.lastIndexOf("=") + 1);
                    sendComment.postID = ((CommentsPage) UserData.getInstance().getCurrentDiaryPage()).getPostId();
                    notifyListeners(Utils.HANDLE_EDIT_COMMENT, sendComment);
                    break;
                } catch (NullPointerException ex) { // cannot serialize
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.page_incorrect);
                    break;
                }
            }
            case Utils.HANDLE_REQUEST_DIARY: {
                String diaryName = (String) message.obj;

                final List<Pair<String, String>> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(Pair.create("module", "registration"));
                nameValuePairs.add(Pair.create("act", "new_journal_post"));
                nameValuePairs.add(Pair.create("journal_act", "1"));
                nameValuePairs.add(Pair.create("diary_title", diaryName));
                nameValuePairs.add(Pair.create("community_title", ""));
                nameValuePairs.add(Pair.create("signature", UserData.getInstance().getSignature()));

                String response = mNetworkClient.postPageToString(nameValuePairs);
                if(response == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }
                
                String mainPage = mNetworkClient.getPageAsString(Utils.MAIN_PAGE);
                if(mainPage == null) {
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    break;
                }
                UserData.getInstance().updateData(serializeMainPage(mainPage)); // get initial links
                
                notifyListeners(Utils.HANDLE_REQUEST_DIARY, true);
                break;
            }
            default:
                return false;
        }

        return true;
    }

    private Post serializePostEditPage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        final Element rootNode = Jsoup.parse(dataPage).select("div.section").first(); // выбираем окошко с текстом

        final Post result = new Post();

        result.title = rootNode.select("input#postTitle.text").val();
        result.content = rootNode.select("textarea#message").text();

        final Elements communityThemes = rootNode.select("input[id^=favtg]");
        for (Element theme : communityThemes)
            result.predefinedTags.put(theme.val(), theme.hasAttr("checked"));

        result.themes += rootNode.select("input#tags.text").val();
        result.mood = rootNode.select("input#atMood.text").val();
        result.music = rootNode.select("input#atMusic.text").val();

        result.pollTitle = rootNode.select("input#queti.text").val();
        if (!result.pollTitle.isEmpty()) // если есть опрос
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
        if (!result.closeAccessMode.isEmpty()) {
            result.closeText = rootNode.select("textarea#close_text").text();
            if (result.closeAccessMode.equals("3"))
                result.closeAllowList = rootNode.select("textarea#access_list").text();
            if (result.closeAccessMode.equals("2"))
                result.closeDenyList = rootNode.select("textarea#access_list").text();
        }

        result.noComments = rootNode.select("input#nocomm").hasAttr("checked");

        return result;
    }

    private Umail serializeUmailEditPage(String dataPage, int type) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        final Element rootNode = Jsoup.parse(dataPage);
        if (rootNode == null)
            return null;

        final Umail result = new Umail();
        result.messageTheme = UserData.getInstance().getCurrentUmailPage().getMessageTheme();
        result.reMessage = rootNode.select("textarea#remessage").first().ownText();
        if (type == Utils.UMAIL_REPLY) {
            result.receiver = UserData.getInstance().getCurrentUmailPage().getSenderName();
        }

        return result;
    }

    private Element serializeMainPage(String dataPage) {
        Element rootNode = Jsoup.parse(dataPage).select("div#top").first(); // выбираем окошко с текстом
        return rootNode;
    }
    
    private Comment serializeCommentEditPage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Element rootNode = Jsoup.parse(dataPage).select("textarea#message").first(); // выбираем окошко с текстом
        Comment result = new Comment();

        result.content = rootNode.text();

        return result;
    }

    private void serializeDiaryListPage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        UserData.getInstance().setCurrentDiaries(new DiaryLinkList<>(mNetworkClient.getCurrentUrl()));

        Element table = rootNode.getElementsByAttributeValue("class", "table r").first();
        if (table == null) // Нет вообще никаких дневников, заканчиваем
            return;

        Element pages = rootNode.select("table.pages").first();
        if (pages != null)
            UserData.getInstance().getCurrentDiaries().setPageLinks(Html.fromHtml(pages.outerHtml()));

        Elements rows = table.select("tr:gt(0)");
        for (Element row : rows) {
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

            UserData.getInstance().getCurrentDiaries().add(diary);
        }

    }

    private void serializeDiaryPage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        DiaryPage scannedDiary = new DiaryPage(mNetworkClient.getCurrentUrl());

        Element diaryTag = rootNode.select("[id=authorName]").first();
        if (diaryTag != null) {
            String authorProfile = diaryTag.getElementsByTag("a").last().attr("href");
            scannedDiary.setDiaryId(authorProfile.substring(authorProfile.lastIndexOf("?") + 1));
            scannedDiary.userLinks.put(getString(R.string.author_diary), scannedDiary.getDiaryUrl());
            scannedDiary.userLinks.put(getString(R.string.author_profile), authorProfile);
        }


        // заполняем ссылки (пока что только какие можем обработать)
        // TODO: сделать generic-обработчик всех таких ссылок и вынести в новую процедуру (убрать tags)
        Elements userLinks = rootNode.select("li#tagslistlink, div#thisCommunityMember li, div#thisCommunity li, div#thisDiaryLinks li");
        for (Element link : userLinks)
            if (link.id().matches("communityMyPosts|communityFav|communityQuote|authorFav|authorQuot|tagslistlink"))
                scannedDiary.userLinks.put(link.child(0).text(), link.child(0).attr("href")); //they all contain <a> tag first

        notifyListeners(Utils.HANDLE_PROGRESS_2);
        Elements postsArea = rootNode.select("[id~=post\\d+], div.pageBar");

        Elements result = postsArea.clone();
        Document resultPage = Document.createShell(mNetworkClient.getCurrentUrl());
        resultPage.title(rootNode.title());
        for (Element to : result) {
            resultPage.body().appendChild(to);
        }
        mutateContent(resultPage);
        scannedDiary.setContent(resultPage.html());
        scannedDiary.setTitle(resultPage.title());

        UserData.getInstance().setCurrentDiaryPage(scannedDiary);
    }

    private void serializeSearchPage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        final Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        final SearchPage scannedSearch = new SearchPage(mNetworkClient.getCurrentUrl());

        notifyListeners(Utils.HANDLE_PROGRESS_2);
        final String searchText = rootNode.select("input[name=q]").val();
        final Elements postsArea = rootNode.select("[id~=post\\d+], div#pageBar");
        if (postsArea.isEmpty()) { // Нет вообще никаких постов, заканчиваем
            notifyListeners(Utils.HANDLE_NOTFOUND_ERROR);
            return;
        }


        final Elements result = postsArea.clone();
        final Document resultPage = Document.createShell(mNetworkClient.getCurrentUrl());
        resultPage.title(rootNode.title());
        for (final Element to : result)
            resultPage.body().appendChild(to);

        mutateContent(resultPage);
        scannedSearch.setContent(resultPage.html());
        scannedSearch.setTitle(resultPage.title() + searchText);

        UserData.getInstance().setCurrentDiaryPage(scannedSearch);
    }

    private void serializeCommentsPage(String dataPage) throws IOException {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        String diaryUrl = mNetworkClient.getCurrentUrl().substring(0, mNetworkClient.getCurrentUrl().lastIndexOf('/') + 1);
        CommentsPage scannedPost = new CommentsPage(diaryUrl);

        Element diaryTag = rootNode.select("#authorName").first();
        if (diaryTag != null) {
            String authorProfile = diaryTag.getElementsByTag("a").last().attr("href");
            scannedPost.setDiaryId(authorProfile.substring(authorProfile.lastIndexOf("?") + 1));
            scannedPost.userLinks.put(getString(R.string.author_diary), scannedPost.getDiaryUrl());
            scannedPost.userLinks.put(getString(R.string.author_profile), authorProfile);
        }

        Elements userLinks = rootNode.select("div#thisCommunityMember li, div#thisCommunity li, div#thisDiaryLinks li");
        for (Element link : userLinks)
            if (link.id().equals("communityMyPosts") || link.id().equals("communityFav") || link.id().equals("communityQuote") ||
                    link.id().equals("authorFav") || link.id().equals("authorQuot"))
                scannedPost.userLinks.put(link.text(), link.child(0).attr("href")); //they all contain <a> tag first


        notifyListeners(Utils.HANDLE_PROGRESS_2);
        Elements effectiveAreas = rootNode.select("[id~=post\\d+], [id~=comment\\d+], div.pageBar");
        if (effectiveAreas.isEmpty()) { // Нет вообще никаких постов, заканчиваем
            notifyListeners(Utils.HANDLE_NOTFOUND_ERROR);
            return;
        }

        Elements result = effectiveAreas.clone();
        Element urlNode = result.first().getElementsByClass("postLinksBackg").first();
        if (urlNode != null) {
            String postURL = urlNode.getElementsByTag("a").attr("href");
            scannedPost.setPostUrl(postURL);
            scannedPost.setPostId(postURL.substring(postURL.lastIndexOf('p') + 1, postURL.lastIndexOf('.')));
        }
        Document resultPage = Document.createShell(mNetworkClient.getCurrentUrl());
        resultPage.title(rootNode.title());
        for (Element to : result) {
            resultPage.body().appendChild(to);
        }

        mutateContent(resultPage);

        scannedPost.setContent(resultPage.html());
        scannedPost.setTitle(resultPage.title());
        UserData.getInstance().setCurrentDiaryPage(scannedPost);
    }

    private void serializeProfilePage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        DiaryProfilePage profilePage = new DiaryProfilePage(mNetworkClient.getCurrentUrl());

        Elements effectiveAreas = rootNode.select("div#contant");
        Elements result = effectiveAreas.clone();
        notifyListeners(Utils.HANDLE_PROGRESS_2);

        Document resultPage = Document.createShell(mNetworkClient.getCurrentUrl());
        resultPage.title(rootNode.title());
        for (Element to : result)
            resultPage.body().appendChild(to);

        // добавляем Umail к профилям
        Element userName = resultPage.select("h2 + p > b").first();
        Element contactsLine = resultPage.select("p:containsOwn(Контакты:)").first();
        if(userName != null && contactsLine != null) {
            try {
                String usernameEncoded = URLEncoder.encode(userName.text(), "windows-1251");
                String umailLink = "/u-mail/?new&username=" + usernameEncoded;
                contactsLine.append(String.format("<a href='%s'>U-Mail</a>", umailLink));
            } catch (UnsupportedEncodingException e) {
                // never happens
            }
        }

        mutateContent(resultPage);

        profilePage.setContent(resultPage.html());
        profilePage.setTitle(resultPage.title());
        UserData.getInstance().setCurrentDiaryPage(profilePage);
    }

    private void serializeTagsPage(String dataPage) throws IOException {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        TagsPage scannedTags = new TagsPage(mNetworkClient.getCurrentUrl().substring(0, mNetworkClient.getCurrentUrl().lastIndexOf('/') + 1));

        Element diaryTag = rootNode.select("#authorName").first();
        if (diaryTag != null) {
            String Id = diaryTag.getElementsByTag("a").last().attr("href");
            scannedTags.setDiaryId(Id.substring(Id.lastIndexOf("?") + 1));
        }

        notifyListeners(Utils.HANDLE_PROGRESS_2);
        Elements effectiveAreas = rootNode.select("ul.tags_ul_all");
        if (effectiveAreas.isEmpty()) // Нет вообще никаких тэгов, заканчиваем
            return;

        Elements result = effectiveAreas.clone();
        Document resultPage = Document.createShell(mNetworkClient.getCurrentUrl());
        resultPage.title(rootNode.title());
        for (Element to : result) {
            resultPage.body().appendChild(to);
        }

        mutateContent(resultPage);
        
        scannedTags.setContent(resultPage.html());
        scannedTags.setTitle(resultPage.title());
        UserData.getInstance().setCurrentDiaryPage(scannedTags);
    }

    private void serializeDiscussionsPage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        UserData.getInstance().getDiscussions().clear();
        UserData.getInstance().getDiscussions().setURL(mNetworkClient.getCurrentUrl());

        notifyListeners(Utils.HANDLE_PROGRESS_2);
        Element dIndex = rootNode.getElementById("all_bits");
        for (Element item : dIndex.getElementsByTag("h3")) {
            DiscPage currentList = new DiscPage();
            Element newPosts = item.getElementsByTag("em").first();
            currentList.setLastUpdate(newPosts.text());
            newPosts.remove();

            String link = item.getElementsByAttributeValueStarting("href", "/discussion/?open[]").attr("href") + "&js";
            currentList.setURL(link);
            String title = item.text();
            currentList.setTitle(title);

            UserData.getInstance().getDiscussions().add(currentList);
        }
    }

    private void serializeDiscussions(String dataPage, ArrayList<DiscPage.Discussion> destination) {
        destination.clear();
        dataPage = dataPage.replace("\\\"", "\"");
        Document rootNode = Jsoup.parse(dataPage);
        Elements tables = rootNode.getElementsByTag("em");
        tables.remove();

        for (Element discussion : rootNode.getElementsByTag("a")) {
            DiscPage.Discussion currentDisc = new DiscPage.Discussion();
            currentDisc.URL = discussion.attr("href");
            currentDisc.title = discussion.text();
            currentDisc.date = discussion.previousElementSibling().text();


            destination.add(currentDisc);
        }
    }

    private void serializeUmailListPage(String dataPage) {
        notifyListeners(Utils.HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        UserData.getInstance().setCurrentUmails(new DiaryLinkList<UmailListPage>(mNetworkClient.getCurrentUrl()));

        Element table = rootNode.select("table.table.l").first();
        if (table == null) // Нет вообще никаких сообщений, заканчиваем
            return;

        Element pages = rootNode.select("table.pages").first();
        if (pages != null)
            UserData.getInstance().getCurrentUmails().setPageLinks(Html.fromHtml(pages.outerHtml()));

        Elements rows = table.select("tr[id]");
        Element title = null, author = null, lastPost = null;
        for (Element row : rows) {
            if (title == null)
                title = row.getElementsByClass("withfloat").first();

            if (author == null)
                author = row.select("[target=_blank]").first();

            if (lastPost == null)
                lastPost = row.getElementsByClass("r").first();

            if (title != null && author != null && lastPost != null) {
                UmailListPage mail = new UmailListPage();
                mail.setTitle(title.getElementsByTag("b").text());
                mail.setURL(title.attr("href"));

                mail.setAuthor(author.text());
                String authorData = author.attr("href");
                mail.setAuthorURL(authorData);
                mail.setAuthorID(authorData.substring(authorData.lastIndexOf("?") + 1));

                mail.setLastUpdate(lastPost.text());
                mail.setLastUpdateURL(lastPost.attr("href"));
                
                Element preview = row.select("span.flwin").first();
                if(preview != null)
                    mail.setPageHint(preview.text());

                mail.setRead(!row.hasClass("not_readed_umail"));

                UserData.getInstance().getCurrentUmails().add(mail);
                title = author = lastPost = null;
            }
        }
    }

    private void serializeUmailPage(String dataPage) {
        UmailPage scannedUmail = new UmailPage();
        notifyListeners(Utils.HANDLE_PROGRESS);

        Document rootNode = Jsoup.parse(dataPage);
        UserData.getInstance().updateData(rootNode);
        notifyListeners(Utils.HANDLE_UPDATE_HEADERS);

        scannedUmail.setUmailURL(mNetworkClient.getCurrentUrl());
        scannedUmail.setUmailID(scannedUmail.getUmailURL().substring(scannedUmail.getUmailURL().lastIndexOf('=') + 1));
        notifyListeners(Utils.HANDLE_PROGRESS_2);

        Elements mailArea = rootNode.select("table.box, table.box + div");
        if (mailArea.isEmpty()) // Нет вообще никаких постов, заканчиваем
            return;

        Element sender = mailArea.select("div[style^=float:left] > b").first();
        if (sender != null)
            scannedUmail.setSenderName(sender.text());

        Element theme = mailArea.select("tbody td").last();
        if (theme != null)
            scannedUmail.setMessageTheme(theme.text());

        Elements result = mailArea.clone();
        Document resultPage = Document.createShell(mNetworkClient.getCurrentUrl());
        resultPage.title(rootNode.title());
        for (Element to : result)
            resultPage.body().appendChild(to);

        mutateContent(resultPage);

        scannedUmail.setContent(resultPage.html());
        scannedUmail.setTitle(resultPage.title());
        UserData.getInstance().setCurrentUmailPage(scannedUmail);
    }

    /**
     * Функция для применения модификаций ко всем загружаемым страницам дневников
     * Сюда вносятся правки страниц по просьбам пользователей
     * @param resultPage страница, которую нужно модифицировать
     */
    private void mutateContent(Document resultPage) {
        // страница будет иметь наш стиль
        String theme = mPreferences.getString("app.theme", "red");
        resultPage.head().append("<link rel=\"stylesheet\" href=\"file:///android_asset/css/" + theme + ".css\" type=\"text/css\" media=\"all\" title=\"Стандарт\"/>");

        // кнопка репоста указывает на нужную ссылку
        Elements shareLinks = resultPage.select(".postLinks li[class^=quote]");
        for (Element shareLi : shareLinks) {
            if (shareLi.childNodeSize() == 0)
                continue;

            Element repostLink = shareLi.child(0);
            Element diaryRepost = shareLi.select("div a[href*=newpost]").first();
            if (diaryRepost != null)
                repostLink.attr("href", diaryRepost.attr("href"));
        }

        // текст вместо кнопок правки
        if(mUseTextInsteadOfImages) {
            Elements postActionImages = resultPage.select("ul.postActionLinks img");
            for (Element img : postActionImages) { // переделываем на текст
                if (img.hasAttr("title")) {
                    Node text = new TextNode(img.attr("title"), resultPage.baseUri());
                    img.replaceWith(text);
                }
            }
        }

        // правка JS
        Elements jsElems = resultPage.getElementsByAttribute("onclick");
        for (Element js : jsElems) {
            String link = js.attr("href");
            if (!link.contains("#more") && !link.contains("subscribe") && !link.contains("showresult") &&
                !link.contains("up&signature=") && !link.contains("down&signature=") &&
                !link.contains("tag_showedit"))
                js.removeAttr("onclick"); // Убиваем весь яваскрипт кроме MORE, поднятия/опускания постов, результатов голосования и подписки
        }

        // смена картинок, если автозагрузка выключена
        if (!mLoadImages) {
            Elements images = resultPage.select("img[src^=http], a:has(img)");
            for (Element current : images) {
                if (current.tagName().equals("img")) {
                    String src = current.attr("src");
                    if (!src.contains("diary.ru") && !current.parent().className().equals("avatar") && !src.startsWith("/")) {
                        // все неподходящие под критерии изображения на странице будут заменены на кнопки, по клику на которые и будут открываться
                        String jsButton = "<input type='image' src='file:///android_asset/images/load_image.png' onclick='return handleIMGDown(this, \"" + src + "\")' />";

                        current.after(jsButton);
                        current.remove();
                    }
                }

                if (current.tagName().equals("a")) {
                    String src = current.getElementsByTag("img").attr("src");
                    if (!src.contains("diary.ru") && !current.parent().className().equals("avatar") && !src.startsWith("/")) {
                        // все неподходящие под критерии изображения на странице будут заменены на кнопки, по клику на которые и будут открываться
                        String jsButton = "<input type='image' src='file:///android_res/drawable/load_image.png' onclick='return handleADown(this, \"" + current.attr("href") + "\", \"" + src + "\")' />";

                        current.after(jsButton);
                        current.remove();
                    }
                }
            }
        }
        
        // включаем джаваскрипт
        resultPage.body().append(Utils.javascriptContent);
        // сигнатура должна быть видна методам JS
        resultPage.body().append("<script>var signature = '" + UserData.getInstance().getSignature() + "';</script>");
    }

    // TODO: превратить этот метод в нормальную фабрику!
    private void checkUrlAndHandle(String requestedUrl, boolean reload) {
        Class<?> handled;
        Object cachedPage = null;
        String dataPage = null;

        try {
            URI toLoad = mNetworkClient.resolve(requestedUrl);
            boolean isNewPage = shouldReloadUrl(toLoad);
            // загружаем из кэша, если это не новый коммент и не запрос на перезагрузку
            if (mCache.hasPage(toLoad) && !reload && !isNewPage) { 
                cachedPage = mCache.loadPageFromCache(toLoad);
                handled = cachedPage.getClass();
            } else {
                final Response page = mNetworkClient.getPage(toLoad);
                if(page == null || !page.isSuccessful()) { // no response, may be ssl error
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
                    return;
                }
                
                // проверим, не картинка ли это
                if (page.body().contentType().type().equals("image")) { // Just load image, no further processing
                    if (reload) { // reload - save
                        String srcName = page.header("Content-Disposition");
                        final String realName = URLUtil.guessFileName(requestedUrl, srcName, MimeTypeMap.getFileExtensionFromUrl(requestedUrl));
                        CacheManager.saveDataToSD(getApplicationContext(), realName, page.body().bytes());
                    } else // no reload - open
                        notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA, requestedUrl);
                    return;
                }

                dataPage = page.body().string();
                handled = Utils.checkDiaryUrl(dataPage);

                if (dataPage.contains("box-error") || dataPage.contains("попробовать что-нибудь еще")) { // неопознанная страничка
                    notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.closed_error); // если наткнулись на ошибку дневника
                    return;
                }
            }

            if (handled == null) { // Если это не страничка дайри
                notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.page_incorrect);
                return;
            }

            mNetworkClient.setCurrentUrl(toLoad);
            if (cachedPage != null) { // если страничка была в кэше или из числа новых комментов
                if (cachedPage instanceof DiaryLinkList) {
                    // umail-варианты сюда не попадают, их путь не идёт через checkDiaryUrl
                    UserData.getInstance().setCurrentDiaries((DiaryLinkList<ListPage>) cachedPage);
                    notifyListeners(Utils.HANDLE_GET_LIST_PAGE_DATA);
                }

                if (cachedPage instanceof DiscListPage) {
                    UserData.getInstance().setDiscussions((DiscListPage) cachedPage);
                    notifyListeners(Utils.HANDLE_GET_DISCUSSIONS_DATA);
                }

                if (cachedPage instanceof WebPage) {
                    UserData.getInstance().setCurrentDiaryPage((WebPage) cachedPage);
                    notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
                }
                
                return;
            } 
            
            // если нет такого кэша
            if (handled == DiaryPage.class) {
                serializeDiaryPage(dataPage);
                mCache.putPageToCache(toLoad, UserData.getInstance().getCurrentDiaryPage());
                notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
            } else if (handled == CommentsPage.class) {
                serializeCommentsPage(dataPage);
                mCache.putPageToCache(toLoad, UserData.getInstance().getCurrentDiaryPage());
                notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
            } else if (handled == TagsPage.class) {
                serializeTagsPage(dataPage);
                mCache.putPageToCache(toLoad, UserData.getInstance().getCurrentDiaryPage());
                notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
            } else if (handled == DiaryProfilePage.class) {
                serializeProfilePage(dataPage);
                mCache.putPageToCache(toLoad, UserData.getInstance().getCurrentDiaryPage());
                notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
            } else if (handled == DiaryLinkList.class) {
                serializeDiaryListPage(dataPage);
                mCache.putPageToCache(toLoad, UserData.getInstance().getCurrentDiaries());
                notifyListeners(Utils.HANDLE_GET_LIST_PAGE_DATA);
            } else if (handled == DiscListPage.class) {
                serializeDiscussionsPage(dataPage);
                mCache.putPageToCache(toLoad, UserData.getInstance().getDiscussions());
                notifyListeners(Utils.HANDLE_GET_DISCUSSIONS_DATA);
            } else if (handled == SearchPage.class) {
                serializeSearchPage(dataPage);
                mCache.putPageToCache(toLoad, UserData.getInstance().getCurrentDiaryPage());
                notifyListeners(Utils.HANDLE_GET_WEB_PAGE_DATA);
            }
            
        } catch (NullPointerException | IllegalArgumentException e) {
            notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.page_incorrect);
        } catch (InterruptedIOException e) {
            notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.canceled);
        } catch (IOException e) {
            if(e.getMessage() != null && e.getMessage().contains("closed")) {
                notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.canceled);
            } else {
                notifyListeners(Utils.HANDLE_CONNECTIVITY_ERROR, R.string.connection_error);
            }
        }
    }

    private boolean shouldReloadUrl(URI toLoad) {
        if(!UserData.getInstance().hasNotifications()) { // предварительная проверка
            return false;
        }
        
        if(UserData.getInstance().getNewDiscussNum() > 0) { // если у нас есть новая дискуссия, нужно перезагрузить её
            URI newDiscussion = mNetworkClient.resolve(UserData.getInstance().getNewDiscussLink());
            if(TextUtils.equals(newDiscussion.getPath(), toLoad.getPath())) {
                return true;
            }
        }

        if(UserData.getInstance().getNewDiaryCommentsNum() > 0) { // если у нас есть новый коммент в дневнике...
            URI newDiaryComment = mNetworkClient.resolve(UserData.getInstance().getNewDiaryLink());
            if(TextUtils.equals(newDiaryComment.getPath(), toLoad.getPath())) {
                return true;
            }
        }
        
        return false;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case SHARED_PROP_IMAGES_AUTOLOAD:
                mLoadImages = sharedPreferences.getBoolean(key, false);
                break;
            case SHARED_PROP_SERVICE_NOTIFY_UPDATES:
                mHandler.removeMessages(Utils.HANDLE_SERVICE_UPDATE);
                mNotifyOnUpdates = sharedPreferences.getBoolean(key, false);
                if (mNotifyOnUpdates)
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(Utils.HANDLE_SERVICE_UPDATE), 300000);
                break;
            case SHARED_PROP_SERVICE_KEEP_DEVICE_ON:
                if (mWakeLock.isHeld())
                    mWakeLock.release();

                mKeepDeviceOn = sharedPreferences.getBoolean(key, false);
                if (mKeepDeviceOn)
                    mWakeLock.acquire();
                break;
            case SHARED_PROP_SERVICE_ALWAYS_RUNNING:
                mIsStickyService = sharedPreferences.getBoolean(key, false);
                if (mIsStickyService)
                    startForeground(NOTIFICATION_ID, createNotification(UserData.getInstance().getCurrentDiaryPage()));
                else
                    stopForeground(true);
                break;
            case SHARED_PROP_WEBVIEW_FONT_SIZE:
                for (DiaryActivity current : mListeners)
                    current.handleFontChange(sharedPreferences.getString(SHARED_PROP_WEBVIEW_FONT_SIZE, "12"));
                break;
            case SHARED_PROP_PRELOAD_THEMES:
                mPreloadThemes = sharedPreferences.getBoolean("preload.themes", true);
                break;
            case SHARED_PROP_PRELOAD_UMAIL_QUOTING:
                mPreloadUmails = sharedPreferences.getBoolean("preload.umail.quoting", true);
                break;
            case SHARED_PROP_USE_TEXT_LINKS:
                mUseTextInsteadOfImages = sharedPreferences.getBoolean(SHARED_PROP_USE_TEXT_LINKS, false);
                break;
            case SHARED_PROP_SCREEN_ORIENTATION:
                mOrientation = Integer.parseInt(sharedPreferences.getString("screen.orientation", "-1"));
                break;
        }
    }

    // Создаем уведомление в статусной строке - для принудительно живого сервиса в Foreground-режиме
    private Notification createNotification(WebPage page) {
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setContentTitle(getString(R.string.service_notification));
        nBuilder.setContentText(page.getContent() != null && page.getTitle() != null ? page.getTitle() : "");
        nBuilder.setSmallIcon(R.drawable.ic_launcher_status_icon);
        nBuilder.setLargeIcon(mNotificationIcon);
        nBuilder.setOngoing(true);

        final Intent intent = new Intent(this, DiaryListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        nBuilder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0));
        return nBuilder.build();
    }

    public void newSession() {
        UserData.clear();
        mNetworkClient.getCookieStore().removeAll();
        mCache.clear();
    }
    
    private static class NotifyState {
        private String lastDiaryLink = "";
        private String lastDiscussionLink = "";
        private String lastUmailLink = "";
        
        public boolean shouldNotify(UserData user) {
            return !lastDiaryLink.equals(user.getNewDiaryLink()) || 
                   !lastDiscussionLink.equals(user.getNewDiscussLink()) || 
                   !lastUmailLink.equals(user.getNewUmailLink());
        }
    }
}
