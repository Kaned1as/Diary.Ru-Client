package adonai.diary_browser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;

public class CacheManager 
{

    public static long MAX_SIZE = 5 * 1048576L; // 5MB

    private CacheManager() 
    {

    }

    public static void cacheData(Context context, byte[] data, String name) throws IOException 
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

    public static byte[] retrieveData(Context context, String name) throws IOException 
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
    
    public static boolean hasData(Context context, String name) throws IOException 
    {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        // Data doesn't exist
        if (file.exists()) 
            return true;
        
        return false;
    }

    private static void cleanDir(File dir, long bytes) 
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

    private static long getDirSize(File dir) 
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