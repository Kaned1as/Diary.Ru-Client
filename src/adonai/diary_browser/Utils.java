package adonai.diary_browser;

import android.app.AlertDialog;
import android.content.Context;

public class Utils 
{
	public static int checkDiaryUrl(String response)
	{
		if(response.contains("commentsArea"))
			return DiaryList.COMMENT_LIST;
		
		if(response.contains("postsArea"))
			return DiaryList.POST_LIST;
		
		if(response.contains("table r"))
			return DiaryList.DIARY_LIST;
		
		return -1;
	}
	
	public static void showDevelSorry(Context ctx)
	{
		AlertDialog.Builder dlg = new AlertDialog.Builder(ctx);
		dlg.setTitle("Sorry :(");
		dlg.setMessage("This object is under development now, please, have a patience! ^_^");
		dlg.create().show();
	}
}
