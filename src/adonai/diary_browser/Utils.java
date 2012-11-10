package adonai.diary_browser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
    		BitmapDrawable drawable = null;
    		if(useCache)
    		{
    			drawable = CacheManager.getInstance().loadToReuse(realName);
    			if(drawable != null) // если картинка имеется в JavaHeap
    				return new Pair<String, BitmapDrawable>(realName, drawable);
    		}
    			
        	load_and_cache:
        	{
        		// Если картинка имеется в оффлайн-кэше
        		if(useCache && CacheManager.getInstance().hasData(context.getApplicationContext(), realName))
        			break load_and_cache;
        		
        		
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
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inDither = false;                     //Disable Dithering mode
            opts.inPurgeable = true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
            opts.inInputShareable = true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
            opts.inTempStorage = new byte[32 * 1024];
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            drawable = new BitmapDrawable(null, BitmapFactory.decodeStream(inPic, null, opts));
            
            // сохраняем слабую ссылку на изображение - для последующего использования
            CacheManager.getInstance().saveForReuse(realName, drawable);
            inPic.close();
            return new Pair<String, BitmapDrawable>(realName, drawable);
        } 
        catch (Exception e) 
        {
            return null;
        }
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
