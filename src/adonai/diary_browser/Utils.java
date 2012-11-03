package adonai.diary_browser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

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
	
	public static Pair<String, BitmapDrawable> loadImage(Context context, String url) 
    {
		return loadImage(context, url, true);
    }
	
	public static Pair<String, BitmapDrawable> loadImage(Context context, String url, boolean useCache) 
	{
		try 
        {
    		String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
    		String realName = URLUtil.guessFileName(url, null, fileExtenstion);
        	cache_and_load:
        	{
        		
        		if(useCache && CacheManager.getInstance().hasData(context.getApplicationContext(), realName))
        			break cache_and_load;
        		
        		
	            InputStream is = (InputStream) Globals.mDHCL.getPage(url).getEntity().getContent();
	            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	            int nRead;
	            while ((nRead = is.read()) != -1)
	            	  buffer.write(nRead);
	            buffer.flush();
	            CacheManager.getInstance().cacheData(context.getApplicationContext(), buffer.toByteArray(), realName);
	            buffer.close();
	            is.close();
        	}

            InputStream inPic = new ByteArrayInputStream(CacheManager.getInstance().retrieveData(context.getApplicationContext(), realName));
            Drawable drawable = Drawable.createFromStream(inPic, realName);
            inPic.close();
            if(drawable instanceof BitmapDrawable)
            	return new Pair<String, BitmapDrawable>(realName, (BitmapDrawable) drawable);
            
            return null;
        } 
        catch (Exception e) 
        {
            return null;
        }
	}
}
