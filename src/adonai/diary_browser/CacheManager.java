package adonai.diary_browser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import adonai.diary_browser.R;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.widget.Toast;

public class CacheManager 
{
	private static CacheManager mInstance;

    public static long MAX_SIZE = 5 * 1048576L; // 5MB
    // загруженные странички
    private Map<String, Object> browseCache = new HashMap<String, Object>();
    // загруженные изображения
    private Map<String, SoftReference<BitmapDrawable>> imageCache = new HashMap<String, SoftReference<BitmapDrawable>>();
    
    public void saveForReuse(String URL, BitmapDrawable drawable)
    {
    	imageCache.put(URL, new SoftReference<BitmapDrawable>(drawable));
    }
    
    public BitmapDrawable loadToReuse(String URL)
    {
    	if(imageCache.containsKey(URL) && imageCache.get(URL).get() != null)
    	{
    		return imageCache.get(URL).get();
    	}
    	return null;
    }
    
    public Object loadPageFromCache(String URL)
    {
    	Object toPage = browseCache.get(URL);
	    return toPage;
    }
    
    public boolean hasPage(String URL)
    {
    	return browseCache.containsKey(URL);
    }
    
    public void putPageToCache(String URL, Object page)
    {
    	browseCache.put(URL, page);
    }

    private CacheManager() 
    {
    }
    
    public static CacheManager getInstance()
    {
    	if(mInstance == null)
    		mInstance = new CacheManager();
    	return mInstance;
    }

    public void cacheData(Context context, byte[] data, String name) throws IOException 
    {

        File cacheDir = context.getCacheDir();
        long size = getDirSize(cacheDir);
        long newSize = data.length + size;

        if (newSize > MAX_SIZE)
            cleanDir(cacheDir, newSize - MAX_SIZE);

        File file = new File(cacheDir, name);
        FileOutputStream os = new FileOutputStream(file);
        try 
        {
            os.write(data);
        }
        finally 
        {
            os.flush();
            os.close();
        }
    }

    public byte[] retrieveData(Context context, String name) throws IOException 
    {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        // Data doesn't exist
        if (!file.exists()) 
            return null;

        byte[] data = new byte[(int) file.length()];
        FileInputStream is = new FileInputStream(file);
        try 
        {
            is.read(data);
        }
        finally 
        {
            is.close();
        }

        return data;
    }
    
    public boolean hasData(Context context, String name) 
    {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        // Data doesn't exist
        if (file.exists()) 
            return true;
        
        return false;
    }
    
    public static void saveDataToSD(Context context, String name, File internalFile)
    {
    	if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			return;
    	
		File SD = Environment.getExternalStorageDirectory();
		File externalDir = new File(SD, "Diary.Ru"); 
		if(!externalDir.exists())
			externalDir.mkdir();
    	
		File toFile = new File(externalDir, name);
		
		try 
		{
			InputStream in = new FileInputStream(internalFile);
			OutputStream out = new FileOutputStream(toFile);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			Toast.makeText(context, context.getResources().getString(R.string.saved_to) + toFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
		}  
		catch (IOException e) 
		{
			Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
		}
    }

    private void cleanDir(File dir, long bytes) 
    {

        long bytesDeleted = 0;
        File[] files = dir.listFiles();

        for (File file : files) 
        {
            bytesDeleted += file.length();
            file.delete();

            if (bytesDeleted >= bytes) 
                break;
        }
    }

    private long getDirSize(File dir) 
    {

        long size = 0;
        File[] files = dir.listFiles();

        for (File file : files) 
        {
            if (file.isFile())
                size += file.length();
        }

        return size;
    }
}