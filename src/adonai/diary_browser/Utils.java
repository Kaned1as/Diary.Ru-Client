package adonai.diary_browser;

import android.app.AlertDialog;
import android.content.Context;

import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiaryProfilePage;
import adonai.diary_browser.entities.DiscListPage;
import adonai.diary_browser.entities.TagsPage;

public class Utils 
{
    public static final String KEY_USERNAME = "diary.username.key";
    public static final String KEY_PASSWORD = "diary.password.key";
    public static final String KEY_KEEP_AUTH = "diary.keep.auth";
    public static final String KEY_USERPASS_CACHE = "diary.password.cache";
    public static final String mPrefsFile = "diary.shared.prefs";

    static final int VIEW_SCROLL_UP                                 =   1;
    static final int VIEW_SCROLL_DOWN                               =   2;

    // Команды хэндлерам
    static final int DIARY_HANDLERS_MASK                            = 0x10000000;
    static final int HANDLE_AUTHORIZATION_ERROR                     =  -1 | DIARY_HANDLERS_MASK;
    static final int HANDLE_SET_HTTP_COOKIE                         =   2 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_LIST_PAGE_DATA                      =   3 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_WEB_PAGE_DATA                       =   4 | DIARY_HANDLERS_MASK;
    static final int HANDLE_PICK_URL                                =   5 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_DISCUSSIONS_DATA                    =   6 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_DISCUSSION_LIST_DATA                =   7 | DIARY_HANDLERS_MASK;
    static final int HANDLE_JUST_DO_GET                             =   8 | DIARY_HANDLERS_MASK;
    static final int HANDLE_DELETE_POST                             =   9 | DIARY_HANDLERS_MASK;
    static final int HANDLE_EDIT_POST                               =  10 | DIARY_HANDLERS_MASK;
    static final int HANDLE_DELETE_COMMENT                          =  11 | DIARY_HANDLERS_MASK;
    static final int HANDLE_EDIT_COMMENT                            =  12 | DIARY_HANDLERS_MASK;
    static final int HANDLE_UPLOAD_FILE                             =  13 | DIARY_HANDLERS_MASK;
    static final int HANDLE_PRELOAD_THEMES                          =  14 | DIARY_HANDLERS_MASK;

    // Команды хэндлеру вида
    static final int HANDLE_IMAGE_CLICK                             =  20 | DIARY_HANDLERS_MASK;
    static final int HANDLE_UPDATE_HEADERS                          =  21 | DIARY_HANDLERS_MASK;
    static final int HANDLE_NAME_CLICK                              =  22 | DIARY_HANDLERS_MASK;


    static final int UMAIL_HANDLERS_MASK                            = 0x20000000;
    static final int HANDLE_OPEN_FOLDER                             =   2 | UMAIL_HANDLERS_MASK;
    static final int HANDLE_OPEN_MAIL                               =   3 | UMAIL_HANDLERS_MASK;
    static final int HANDLE_DELETE_UMAILS                           =   4 | UMAIL_HANDLERS_MASK;

    static final int HANDLE_START                                   =   1  | DIARY_HANDLERS_MASK | UMAIL_HANDLERS_MASK;
    static final int HANDLE_PAGE_INCORRECT                          =   2  | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    static final int HANDLE_SERVICE_UPDATE                          =   3  | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    static final int HANDLE_PROGRESS                                =   10 | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    static final int HANDLE_PROGRESS_2                              =   11 | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    static final int HANDLE_CONNECTIVITY_ERROR                      =  -20 | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    static final int HANDLE_SERVICE_ERROR                           =  -30 | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    static final int HANDLE_CLOSED_ERROR                            =  -40 | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;

    static String javascriptContent =    "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/journal.js\"> </script>" +
                                         "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/diary_client.js\"> </script>" +
                                         "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/functions.js\"> </script>" +
                                         "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/journal2.js\"> </script>";

    static Class<?> checkDiaryUrl(String response)
    {
        if(response.contains("class=\"tags_ul_all\""))
            return TagsPage.class;

        if(response.contains("id=\"addCommentArea\"") || response.contains("id=\"commentsArea\""))
            return CommentsPage.class;

        if(response.contains("id=\"postsArea\""))
            return DiaryPage.class;

        if(response.contains("class=\"table r\""))
            return DiaryListPage.class;

        if(response.contains("name=\"membershiplist\""))
            return DiaryProfilePage.class;

        if(response.contains("id=\"all_bits\""))
            return DiscListPage.class;

        if(response.contains("Поиск по дневникам") && response.contains("Что искать:"))
            return DiaryPage.class;

        return null; // not found
    }

    static void showDevelSorry(Context ctx)
    {
        AlertDialog.Builder dlg = new AlertDialog.Builder(ctx);
        dlg.setTitle("Sorry :(");
        dlg.setMessage("This object is under development now, please, have a patience! ^_^");
        dlg.create().show();
    }
}
