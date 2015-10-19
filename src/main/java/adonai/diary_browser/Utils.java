package adonai.diary_browser;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryLinkList;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiaryProfilePage;
import adonai.diary_browser.entities.DiscListPage;
import adonai.diary_browser.entities.SearchPage;
import adonai.diary_browser.entities.TagsPage;

/**
 * Класс-свалка полезных функций и констант.
 * Один большой антипаттерн
 * 
 * @author не кто иной как Адонай!
 * 
 * //@TODO разгрести тут всё!
 */
public class Utils {
    public static final String KEY_USERNAME = "diary.username.key";
    public static final String KEY_PASSWORD = "diary.password.key";
    public static final String KEY_KEEP_AUTH = "diary.keep.auth";
    public static final String KEY_USERPASS_CACHE = "diary.password.cache";
    public static final String mPrefsFile = "diary.shared.prefs";

    public static final String CLOSING_PANE_BROADCAST = "CLOSING_PANE";
    public static final String OPENING_PANE_BROADCAST = "OPENING_PANE";

    static final int VIEW_SCROLL_UP = 1;
    static final int VIEW_SCROLL_DOWN = 2;

    // Команды хэндлерам
    static final int HANDLE_AUTHORIZATION_ERROR                     = 1;
    static final int HANDLE_AUTHORIZE                               = 2;
    static final int HANDLE_GET_LIST_PAGE_DATA                      = 3;
    static final int HANDLE_GET_WEB_PAGE_DATA                       = 4;
    static final int HANDLE_PICK_URL                                = 5;
    static final int HANDLE_GET_DISCUSSIONS_DATA                    = 6;
    static final int HANDLE_GET_DISCUSSION_LIST_DATA                = 7;
    static final int HANDLE_JUST_DO_GET                             = 8;
    static final int HANDLE_DELETE_POST                             = 9;
    static final int HANDLE_EDIT_POST                               = 10;
    static final int HANDLE_DELETE_COMMENT                          = 11;
    static final int HANDLE_EDIT_COMMENT                            = 12;
    static final int HANDLE_UPLOAD_FILE                             = 13;
    static final int HANDLE_UPLOAD_MUSIC                            = 14;
    static final int HANDLE_UPLOAD_GIF                              = 15;
    static final int HANDLE_PRELOAD_THEMES                          = 16;
    static final int HANDLE_REPOST                                  = 17;
    static final int HANDLE_QUERY_ONLINE                            = 18;
    static final int HANDLE_DELETE_POST_DRAFT                       = 19;
    static final int HANDLE_DELETE_TAG                              = 20;
    static final int HANDLE_REQUEST_DIARY                           = 21;

    // Команды хэндлеру вида
    static final int HANDLE_IMAGE_CLICK                             = 101;
    static final int HANDLE_UPDATE_HEADERS                          = 102;
    static final int HANDLE_NAME_CLICK                              = 103;


    // команды U-mail
    static final int HANDLE_OPEN_FOLDER                             = 301;
    static final int HANDLE_OPEN_MAIL                               = 302;
    static final int HANDLE_DELETE_UMAILS                           = 303;
    static final int HANDLE_PRELOAD_UMAIL                           = 304;

    static final int HANDLE_START                                   = 401;
    static final int HANDLE_SERVICE_UPDATE                          = 402;
    static final int HANDLE_PROGRESS                                = 403;
    static final int HANDLE_PROGRESS_2                              = 404;
    static final int HANDLE_CONNECTIVITY_ERROR                      = 405;
    static final int HANDLE_SERVICE_ERROR                           = 406;
    static final int HANDLE_NOTFOUND_ERROR                          = 407;
    static final int HACKING_CLOUDFLARE                             = 408;
    static final int HANDLE_STEP_SEQUENCE                           = 409;

    static final int UMAIL_REPLY                                = 0;
    static final int UMAIL_FORWARD                              = 1;

    static final int ACTIVITY_ACTION_REQUEST_IMAGE              = 0;
    static final int ACTIVITY_ACTION_REQUEST_MUSIC              = 1;
    static final int ACTIVITY_ACTION_REQUEST_GIF                = 2;
    
    public static final String MAIN_PAGE = "http://www.diary.ru";
    public static final String LOGIN_PAGE = "http://www.diary.ru/login.php";
    public static final String REGISTER_PAGE = "http://www.diary.ru/registration/";
    public static final String DISCUSSIONS_PAGE = "http://www.diary.ru/discussion/";
    public static final String FAVORITES_PAGE = "http://www.diary.ru/list/?act=show&fgroup_id=0";
    public static final String SUBSCRIBERS_PAGE = "http://www.diary.ru/list/?act=show&fgroup_id=-1";

    static String javascriptContent = "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/journal.js\"> </script>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/diary_client.js\"> </script>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/from_pages.js\"> </script>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/functions.js\"> </script>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/journal2.js\"> </script>";
    
    static final String IMGUR_CLIENT_AUTH = "Client-ID 860dc14aa7caf25";
    static final String IMGUR_API_ENDPOINT = "https://api.imgur.com/3/";

    static Class<?> checkDiaryUrl(String response) {
        if (response.contains("class=\"tags_ul_all\""))
            return TagsPage.class;

        if (response.contains("id=\"addCommentArea\"") || response.contains("id=\"commentsArea\""))
            return CommentsPage.class;

        if (response.contains("id=\"postsArea\""))
            return DiaryPage.class;

        if (response.contains("class=\"table r\""))
            return DiaryLinkList.class;

        if (response.contains("name=\"membershiplist\"") || response.contains("Профиль пользователя"))
            return DiaryProfilePage.class;

        if (response.contains("id=\"all_bits\""))
            return DiscListPage.class;

        if (response.contains("Поиск по дневникам") && response.contains("Что искать:"))
            return SearchPage.class;

        return null; // not found
    }

    static void showDevelSorry(Context ctx) {
        AlertDialogWrapper.Builder dlg = new AlertDialogWrapper.Builder(ctx);
        dlg.setTitle("Sorry :(");
        dlg.setMessage("This object is under development now, please, have a patience! ^_^");
        dlg.create().show();
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    public static void setupTheme(Activity activity) {
        // ставим тему до инстанцирования любых View
        SharedPreferences prefs = activity.getSharedPreferences(Utils.mPrefsFile, Context.MODE_PRIVATE);
        final String theme = prefs.getString("app.theme", "red");
        switch (theme) {
            case "red":
                activity.setTheme(R.style.DiaryThemeRed);
                break;
            case "light":
                activity.setTheme(R.style.DiaryThemeLight);
                break;
            case "dark":
                activity.setTheme(R.style.DiaryThemeDark);
                break;
            case "lilac":
                activity.setTheme(R.style.DiaryThemeLilac);
                break;
            case "dark_lilac":
                activity.setTheme(R.style.DiaryThemeDarkLilac);
                break;
            case "green":
                activity.setTheme(R.style.DiaryThemeGreen);
                break;
            case "pink":
                activity.setTheme(R.style.DiaryThemePink);
                break;
            case "greeny_brown":
                activity.setTheme(R.style.DiaryThemeGreenyBrown);
                break;
        }
    }

    public static String getStringFromInputStream(InputStream stream) throws IOException {
        int n = 0;
        char[] buffer = new char[1024 * 4];
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
        StringWriter writer = new StringWriter();
        while ((n = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, n);
        }
        return writer.toString();
    }
}
