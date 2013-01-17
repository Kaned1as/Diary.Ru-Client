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
