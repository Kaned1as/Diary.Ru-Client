package adonai.diary_browser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class DiaryHttpClient 
{

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpContext localContext = new BasicHttpContext();
    CookieStore cookieStore = new BasicCookieStore();

    private HttpPost httpPost = null;
    String currentURL = "";

    public DiaryHttpClient() 
    {
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public void abort() 
    {

        try
        {
            if(httpClient != null && httpPost != null)
                httpPost.abort();

        } catch (Exception e)
        {
            System.out.println("HTTPHelp : Abort Exception : " + e);
        }
    }

    public HttpResponse postPage(String url, HttpEntity data) 
    {
        HttpResponse response = null;
        try
        {
            URI address = new URI(currentURL).resolve(url.trim().replace(" ", "")); // убиваем символ Non-breaking space
            httpPost = new HttpPost(address.toURL().toString());
            if(data != null)
                httpPost.setEntity(data);

            response = httpClient.execute(httpPost, localContext);
        }
        catch (Exception e) // неполадки в соединении
        {
            System.out.println("HTTPHelp : Null URL: " + e);
            e.printStackTrace();
        }

        return response;
    }

    public HttpResponse getPage(String url) 
    {
        if(url.startsWith("file"))
            return null; // Не загружать локальные

        DefaultHttpClient AsyncRetriever = new DefaultHttpClient();
        HttpResponse response = null;

        try
        {
            URI address = new URI(currentURL).resolve(url);
            HttpGet httpGet = new HttpGet(address.toURL().toString());
            response = AsyncRetriever.execute(httpGet, localContext);
        }
        catch (Exception e)
        {
            System.out.println("HTTPHelp : Null URL: " + e);
            e.printStackTrace();
        }

        return response;
    }

    public HttpResponse getPageAndContext(String url)
    {
        HttpResponse response = null;
        try
        {
            URI address = new URI(currentURL).resolve(url.trim().replace(" ", "")); // убиваем символ Non-breaking space
            HttpGet httpGet = new HttpGet(address.toURL().toString());
            currentURL = address.toURL().toString();

            response = httpClient.execute(httpGet, localContext);
        }
        catch (Exception e) // неполадки в соединении
        {
            System.out.println("HTTPHelp : Null URL: " + e);
            e.printStackTrace();
        }

        return response;
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
