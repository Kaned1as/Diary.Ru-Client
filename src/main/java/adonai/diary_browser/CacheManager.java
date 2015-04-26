package adonai.diary_browser;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CacheManager {
    public static long MAX_SIZE = 5 * 1048576L; // 5MB
    private static CacheManager mInstance;
    // загруженные странички
    private Map<URI, Object> browseCache = new HashMap<>();

    public static CacheManager getInstance() {
        if (mInstance == null)
            mInstance = new CacheManager();
        return mInstance;
    }

    public static File saveDataToSD(Context context, String name, InputStream inStream) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;

        File SD = Environment.getExternalStorageDirectory();
        File externalDir = new File(SD, "Diary.Ru");
        if (!externalDir.exists())
            externalDir.mkdir();

        File toFile = new File(externalDir, name);

        try {
            final OutputStream out = new FileOutputStream(toFile);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = inStream.read(buf)) > 0)
                out.write(buf, 0, len);
            inStream.close();
            out.close();
            Toast.makeText(context, context.getResources().getString(R.string.saved_to) + toFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return toFile;
        } catch (IOException e) {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public static File saveDataToSD(Context context, String name, byte[] bytes) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;

        File SD = Environment.getExternalStorageDirectory();
        File externalDir = new File(SD, "Diary.Ru");
        if (!externalDir.exists())
            externalDir.mkdir();

        File toFile = new File(externalDir, name);

        try {
            final OutputStream out = new FileOutputStream(toFile);
            final InputStream inStream = new ByteArrayInputStream(bytes);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = inStream.read(buf)) > 0)
                out.write(buf, 0, len);
            inStream.close();
            out.close();
            Toast.makeText(context, context.getResources().getString(R.string.saved_to) + toFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return toFile;
        } catch (IOException e) {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public Object loadPageFromCache(URI url) {
        return browseCache.get(url);
    }

    public boolean hasPage(URI url) {
        return browseCache.containsKey(url);
    }

    public void putPageToCache(URI url, Object page) {
        browseCache.put(url, page);
    }

    public void clear() {
        browseCache.clear();
    }

    public void cacheData(Context context, byte[] data, String name) throws IOException {

        File cacheDir = context.getCacheDir();
        long size = getDirSize(cacheDir);
        long newSize = data.length + size;

        if (newSize > MAX_SIZE)
            cleanDir(cacheDir, newSize - MAX_SIZE);

        File file = new File(cacheDir, name);
        FileOutputStream os = new FileOutputStream(file);
        try {
            os.write(data);
        } finally {
            os.flush();
            os.close();
        }
    }

    public byte[] retrieveData(Context context, String name) throws IOException {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        // Data doesn't exist
        if (!file.exists())
            return null;

        byte[] data = new byte[(int) file.length()];
        FileInputStream is = new FileInputStream(file);
        try {
            is.read(data);
        } finally {
            is.close();
        }

        return data;
    }

    public boolean dropData(Context context, String name) throws IOException {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        return file.exists() && file.delete();
    }

    public boolean hasData(Context context, String name) {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        // Data doesn't exist
        return file.exists();
    }

    private void cleanDir(File dir, long bytes) {

        long bytesDeleted = 0;
        File[] files = dir.listFiles();

        for (File file : files) {
            bytesDeleted += file.length();
            file.delete();

            if (bytesDeleted >= bytes)
                break;
        }
    }

    private long getDirSize(File dir) {

        long size = 0;
        File[] files = dir.listFiles();

        for (File file : files)
            if (file.isFile())
                size += file.length();

        return size;
    }
}