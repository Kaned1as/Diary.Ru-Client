package adonai.diary_browser;

import adonai.diary_browser.entities.DiaryPage;
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
    
	public static int checkDiaryUrl(String response)
	{
		if(response.contains("tags_ul_all"))
			return DiaryPage.TAG_LIST;
		
		if(response.contains("commentsArea"))
			return DiaryPage.COMMENT_LIST;
		
		if(response.contains("postsArea"))
			return DiaryPage.POST_LIST;
		
		if(response.contains("table r"))
			return DiaryPage.PAGE_LIST;
		
		return DiaryPage.PAGE_NOT_RECOGNIZED; // not found
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
