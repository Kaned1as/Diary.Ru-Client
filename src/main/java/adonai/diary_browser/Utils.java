package adonai.diary_browser;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiaryProfilePage;
import adonai.diary_browser.entities.DiscListPage;
import adonai.diary_browser.entities.SearchPage;
import adonai.diary_browser.entities.TagsPage;

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
    static final int HANDLE_PRELOAD_THEMES                          = 14;
    static final int HANDLE_REPOST                                  = 15;
    static final int HANDLE_QUERY_ONLINE                            = 16;
    static final int HANDLE_DELETE_POST_DRAFT                       = 17;
    static final int HANDLE_DELETE_TAG                              = 18;

    // Команды хэндлеру вида
    static final int HANDLE_IMAGE_CLICK                             = 20;
    static final int HANDLE_UPDATE_HEADERS                          = 21;
    static final int HANDLE_NAME_CLICK                              = 22;


    // команды U-mail
    static final int HANDLE_OPEN_FOLDER                             = 23;
    static final int HANDLE_OPEN_MAIL                               = 24;
    static final int HANDLE_DELETE_UMAILS                           = 25;
    static final int HANDLE_PRELOAD_UMAIL                           = 26;

    static final int HANDLE_START                                   = 101;
    static final int HANDLE_SERVICE_UPDATE                          = 102;
    static final int HANDLE_PROGRESS                                = 103;
    static final int HANDLE_PROGRESS_2                              = 104;
    static final int HANDLE_CONNECTIVITY_ERROR                      = 105;
    static final int HANDLE_SERVICE_ERROR                           = 106;
    static final int HANDLE_NOTFOUND_ERROR                          = 107;
    static final int HACKING_CLOUDFLARE                             = 108;
    static final int HANDLE_STEP_SEQUENCE                           = 109;

    static final int UMAIL_REPLY                                = 0;
    static final int UMAIL_FORWARD                              = 1;

    static final int ACTIVITY_ACTION_REQUEST_IMAGE              = 0;

    static String javascriptContent = "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/journal.js\"> </script>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/diary_client.js\"> </script>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/from_pages.js\"> </script>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/functions.js\"> </script>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/journal2.js\"> </script>";

    static Class<?> checkDiaryUrl(String response) {
        if (response.contains("class=\"tags_ul_all\""))
            return TagsPage.class;

        if (response.contains("id=\"addCommentArea\"") || response.contains("id=\"commentsArea\""))
            return CommentsPage.class;

        if (response.contains("id=\"postsArea\""))
            return DiaryPage.class;

        if (response.contains("class=\"table r\""))
            return DiaryListPage.class;

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
}
