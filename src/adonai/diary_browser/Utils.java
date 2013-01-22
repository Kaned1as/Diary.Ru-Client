package adonai.diary_browser;

import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.TagsPage;
import android.app.AlertDialog;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Utils 
{
	// Команды хэндлерам
    public static final int DIARY_HANDLERS_MASK                           = 0x10000000;
    static final int HANDLE_AUTHORIZATION_ERROR 					=  -1 | DIARY_HANDLERS_MASK;
    static final int HANDLE_SET_HTTP_COOKIE 						= 	2 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_DIARIES_DATA 		             	= 	3 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_DIARY_PAGE_DATA 					= 	4 | DIARY_HANDLERS_MASK;
    static final int HANDLE_PICK_URL 								= 	5 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_DISCUSSIONS_DATA 					= 	6 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_DISCUSSION_LIST_DATA 				= 	7 | DIARY_HANDLERS_MASK;
    static final int HANDLE_GET_TAG_LIST_DATA 						= 	8 | DIARY_HANDLERS_MASK;
    
    public static final int UMAIL_HANDLERS_MASK                           = 0x20000000;
	static final int HANDLE_OPEN_FOLDER                             =   2 | UMAIL_HANDLERS_MASK;
	static final int HANDLE_OPEN_MAIL                               =   3 | UMAIL_HANDLERS_MASK;
	
	static final int HANDLE_START 									= 	1 | DIARY_HANDLERS_MASK | UMAIL_HANDLERS_MASK;
    static final int HANDLE_PROGRESS                                =   10 | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    static final int HANDLE_PROGRESS_2                              =   11 | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    static final int HANDLE_CONNECTIVITY_ERROR                      =  -20 | UMAIL_HANDLERS_MASK | DIARY_HANDLERS_MASK;
    
	
	
    public static String javascriptContent = "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/journal.js\"> </script>" +
                                             "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/diary_client.js\"> </script>" +
                                             "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/functions.js\"> </script>" +
                                             "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/select.js\"> </script>" +
                                             "<script type=\"text/javascript\" src=\"file:///android_asset/javascript/journal2.js\"> </script>";
    
	public static Class<?> checkDiaryUrl(String response)
	{
		if(response.contains("tags_ul_all"))
			return TagsPage.class;
		
		if(response.contains("commentsArea"))
			return CommentsPage.class;
		
		if(response.contains("postsArea"))
			return DiaryPage.class;
		
		if(response.contains("table r"))
			return DiaryListPage.class;
		
		return null; // not found
	}
	
	public static void showDevelSorry(Context ctx)
	{
		AlertDialog.Builder dlg = new AlertDialog.Builder(ctx);
		dlg.setTitle("Sorry :(");
		dlg.setMessage("This object is under development now, please, have a patience! ^_^");
		dlg.create().show();
	}
	
	public static class DiaryWebView extends WebViewClient
	{  //HERE IS THE MAIN CHANGE. 
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) 
        {
            return (false);
        }
	}
}
