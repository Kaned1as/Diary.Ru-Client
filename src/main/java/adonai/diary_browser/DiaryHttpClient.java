package adonai.diary_browser;

import android.support.annotation.NonNull;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.AbortableHttpRequest;
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
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

public class DiaryHttpClient {
    private URI currentURL = URI.create("");

    HttpClient httpClient = new DefaultHttpClient();
    HttpContext localContext = new BasicHttpContext();
    CookieStore cookieStore = new BasicCookieStore();
    List<AbortableHttpRequest> runningRequests = new ArrayList<>();

    public DiaryHttpClient() {
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public void abort() {
        new Thread() {
            @Override
            public void run() {
                for(AbortableHttpRequest request : runningRequests) {
                    request.abort();
                }
                runningRequests.clear();
            }
        }.start();
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public String postPageToString(String url, HttpEntity data) throws IOException {
        String current = currentURL.resolve(url.trim().replace(" ", "")).toString(); // убиваем символ Non-breaking space
        HttpPost httpPost = new HttpPost(current);
        runningRequests.add(httpPost);
        if(data != null) {
            httpPost.setEntity(data);
        }
        HttpResponse response = httpClient.execute(httpPost, localContext);
        return EntityUtils.toString(response.getEntity());
    }

    public String postPageToString(HttpEntity data) throws IOException {
        return postPageToString("http://www.diary.ru/diary.php", data);
    }

    public String getPageAsString(String url) throws IOException {
        if (url.startsWith("file"))
            return null; // Не загружать локальные

        DefaultHttpClient asyncRetriever = new DefaultHttpClient();
        String current = currentURL.resolve(url).toString();
        HttpGet httpGet = new HttpGet(current);
        runningRequests.add(httpGet);
        HttpResponse response = asyncRetriever.execute(httpGet, localContext);
        if(response.getEntity() == null) {
            return null;
        }
        return EntityUtils.toString(response.getEntity());
    }

    public byte[] getPageAsByteArray(String url) {
        if (url.startsWith("file"))
            return null; // Не загружать локальные

        try {
            DefaultHttpClient asyncRetriever = new DefaultHttpClient();
            String current = currentURL.resolve(url).toString();
            HttpGet httpGet = new HttpGet(current);
            runningRequests.add(httpGet);
            HttpResponse response = asyncRetriever.execute(httpGet, localContext);

            if(response.getEntity() == null) {
                return null;
            }
            // getting bytes of image
            final InputStream is = response.getEntity().getContent();
            final byte[] buffer = new byte[8192];
            int bytesRead;
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytesRead = is.read(buffer)) != -1)
                output.write(buffer, 0, bytesRead);
            is.close();

            return output.toByteArray();
        } catch (Exception ignored) {
        } // stream close / timeout

        return null;
    }

    public String getPageAndContextAsString(String url) throws IOException {
        DefaultHttpClient asyncRetriever = new DefaultHttpClient();
        setCurrentURL(url);
        HttpGet httpGet = new HttpGet(currentURL);
        runningRequests.add(httpGet);
        HttpResponse response = asyncRetriever.execute(httpGet, localContext);
        if(response.getEntity() == null) {
            return null;
        }
        return EntityUtils.toString(response.getEntity());
    }

    /**
     * Manual page processing
     * Remember to disconnect connection!
     *
     * @param url url to fetch
     * @return connection for manual usage
     */
    public HttpResponse getPage(String url) throws IOException {
        try {
            DefaultHttpClient asyncRetriever = new DefaultHttpClient();
            String current = currentURL.resolve(url).toString();
            HttpGet httpGet = new HttpGet(current);
            runningRequests.add(httpGet);
    
            return asyncRetriever.execute(httpGet, localContext);
        } catch (SSLException e) {
            return null;
        }
    }

    public interface ProgressListener {
        void transferred(long transferredBytes);
    }

    static class CountingOutputStream extends FilterOutputStream {

        private final ProgressListener listener;
        private long transferred;

        CountingOutputStream(final OutputStream out, final ProgressListener listener)  {
            super(out);
            this.listener = listener;
            this.transferred = 0;
        }

        @Override
        public void write(@NonNull final byte[] b, final int off, final int len) throws IOException {
            //// NO, double-counting, as super.write(byte[], int, int) delegates to write(int).
            //super.write(b, off, len);
            out.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred);
        }

        @Override
        public void write(final int b) throws IOException {
            out.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred);
        }
    }

    static public class CountingFileBody extends FileBody {
        ProgressListener listener;
        CountingFileBody(File f, String str, ProgressListener progressListener) {
            super(f, str);
            listener = progressListener;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            super.writeTo(out instanceof CountingOutputStream ? out : new CountingOutputStream(out, listener));
        }
    }
    
    public void setCurrentURL(String url) {
        currentURL = currentURL.resolve(url);
    }
    
    public String getCurrentURL() {
        return currentURL.toString();
    }
}
