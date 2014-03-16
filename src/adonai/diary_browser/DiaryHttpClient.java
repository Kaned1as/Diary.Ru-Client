package adonai.diary_browser;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class DiaryHttpClient 
{

    List<AbortableHttpRequest> runningRequests = new ArrayList<>();
    private final CookieManager manager;
    String currentURL = "";

    public DiaryHttpClient() 
    {
        manager = new CookieManager() {
            @Override
            public CookieStore getCookieStore() { // ugly hack for cookies best match
                final CookieStore in = super.getCookieStore();
                final List<HttpCookie> cookies = in.getCookies();
                for(HttpCookie cookie : cookies)
                    if(!cookie.getDomain().startsWith("."))
                        cookie.setDomain("." + cookie.getDomain());

                return in;
            }
        };
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
    }

    public CookieStore getCookieStore() {

        return manager.getCookieStore();
    }

    public void abort()
    {
        for(AbortableHttpRequest request : runningRequests)
            request.abort();

        runningRequests.clear();
    }

    public String postPageToString(String url, HttpEntity data)
    {
        try
        {
            final URI address = new URI(currentURL).resolve(url.trim().replace(" ", "")); // убиваем символ Non-breaking space
            final HttpURLConnection httpPost = (HttpURLConnection) address.toURL().openConnection();
            httpPost.setDoOutput(true);
            httpPost.setRequestProperty(HTTP.CONTENT_TYPE, data.getContentType().getValue());

            final OutputStream os = httpPost.getOutputStream();
            data.writeTo(os);
            os.flush();
            os.close();

            return getResponseString(httpPost);
        }
        catch (Exception e) // неполадки в соединении
        {
            System.out.println("HTTPHelp : Null URL: " + e);
        }

        return null;
    }

    public String getPageAsString(String url)
    {
        if(url.startsWith("file"))
            return null; // Не загружать локальные

        try
        {
            final URI address = new URI(currentURL).resolve(url);
            final HttpURLConnection httpGet = (HttpURLConnection) address.toURL().openConnection();

            return getResponseString(httpGet);

        }
        catch (Exception e)
        {
            System.out.println("HTTPHelp : Null URL: " + e);
        }

        return null;
    }

    public HttpURLConnection getPage(String url)
    {
        if(url.startsWith("file"))
            return null; // Не загружать локальные

        try
        {
            final URI address = new URI(currentURL).resolve(url);
            return (HttpURLConnection) address.toURL().openConnection();
        }
        catch (Exception e)
        {
            System.out.println("HTTPHelp : Null URL: " + e);
        }

        return null;
    }

    public String getPageAndContextAsString(String url)
    {
        try
        {
            final URI address = new URI(currentURL).resolve(url.trim().replace(" ", "")); // убиваем символ Non-breaking space
            currentURL = address.toURL().toString();
            final HttpURLConnection httpGet = (HttpURLConnection) address.toURL().openConnection();

            return getResponseString(httpGet);
        }
        catch (Exception e) // неполадки в соединении
        {
            System.out.println("HTTPHelp : Null URL: " + e);
        }

        return null;
    }

    public String getResponseString(HttpURLConnection httpGet) throws IOException {
        final InputStream is = httpGet.getInputStream();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) >= 0)
            stream.write(buffer, 0, bytesRead);
        is.close();

        return new String(stream.toByteArray(), "WINDOWS-1251");
    }

    public HttpURLConnection getPageAndContext(String url)
    {
        try
        {
            final URI address = new URI(currentURL).resolve(url.trim().replace(" ", "")); // убиваем символ Non-breaking space
            currentURL = address.toURL().toString();

            return (HttpURLConnection) address.toURL().openConnection();
        }
        catch (Exception e) // неполадки в соединении
        {
            System.out.println("HTTPHelp : Null URL: " + e);
        }

        return null;
    }

    public static interface ProgressListener
    {
        void transferred(long transferredBytes);
    }


    static class CountingOutputStream extends FilterOutputStream
    {

        private final ProgressListener listener;
        private long transferred;

        CountingOutputStream(final OutputStream out, final ProgressListener listener)
        {
            super(out);
            this.listener = listener;
            this.transferred = 0;
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException
        {
            //// NO, double-counting, as super.write(byte[], int, int) delegates to write(int).
            //super.write(b, off, len);
            out.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred);
        }

        @Override
        public void write(final int b) throws IOException
        {
            out.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred);
        }

    }

    static public class CountingFileBody extends FileBody
    {
        ProgressListener listener;
        CountingFileBody(File f, String str, ProgressListener progressListener)
        {
            super(f, str);
            listener = progressListener;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException
        {
            super.writeTo(out instanceof CountingOutputStream ? out : new CountingOutputStream(out, listener));
        }
    }
}
