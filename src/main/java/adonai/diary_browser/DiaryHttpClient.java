package adonai.diary_browser;

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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DiaryHttpClient {
    String currentURL = "";

    HttpClient httpClient = new DefaultHttpClient();
    HttpContext localContext = new BasicHttpContext();
    CookieStore cookieStore = new BasicCookieStore();
    List<AbortableHttpRequest> runningRequests = new ArrayList<>();

    public DiaryHttpClient() {
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public void abort() {
        for(AbortableHttpRequest request : runningRequests) {
            request.abort();
        }

        runningRequests.clear();
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public String postPageToString(String url, HttpEntity data) throws IOException {
        try {
            URI address = new URI(currentURL).resolve(url.trim().replace(" ", "")); // убиваем символ Non-breaking space
            HttpPost httpPost = new HttpPost(address.toURL().toString());
            runningRequests.add(httpPost);
            if(data != null) {
                httpPost.setEntity(data);
            }
            HttpResponse response = httpClient.execute(httpPost, localContext);
            return EntityUtils.toString(response.getEntity());
        } catch (URISyntaxException e) {
            throw new IOException(String.format("Wrong URL: %s", url));
        }
    }

    public String getPageAsString(String url) throws IOException {
        if (url.startsWith("file"))
            return null; // Не загружать локальные

        try {
            DefaultHttpClient AsyncRetriever = new DefaultHttpClient();
            URI address = new URI(currentURL).resolve(url);
            HttpGet httpGet = new HttpGet(address.toURL().toString());
            runningRequests.add(httpGet);
            HttpResponse response = AsyncRetriever.execute(httpGet, localContext);
            return EntityUtils.toString(response.getEntity());
        } catch (URISyntaxException e) {
            throw new IOException(String.format("Wrong URL: %s", url));
        }
    }

    public byte[] getPageAsByteArray(String url) {
        if (url.startsWith("file"))
            return null; // Не загружать локальные

        try {
            DefaultHttpClient asyncRetriever = new DefaultHttpClient();
            final URI address = new URI(currentURL).resolve(url);
            HttpGet httpGet = new HttpGet(address.toURL().toString());
            runningRequests.add(httpGet);
            HttpResponse response = asyncRetriever.execute(httpGet, localContext);

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
        try {
            DefaultHttpClient asyncRetriever = new DefaultHttpClient();
            URI address = new URI(currentURL).resolve(url);
            currentURL = address.toString();
            HttpGet httpGet = new HttpGet(address.toURL().toString());
            runningRequests.add(httpGet);
            HttpResponse response = asyncRetriever.execute(httpGet, localContext);

            return EntityUtils.toString(response.getEntity());
        } catch (URISyntaxException e) {
            throw new IOException(String.format("Wrong URL: %s", url));
        }
    }

    /**
     * Manual page processing
     * Remember to disconnect connection!
     *
     * @param url url to fetch
     * @return connection for manual usage
     */
    public HttpResponse getPageAndContext(String url) throws IOException {
        try {
            DefaultHttpClient asyncRetriever = new DefaultHttpClient();
            URI address = new URI(currentURL).resolve(url);
            currentURL = address.toString();
            HttpGet httpGet = new HttpGet(address.toURL().toString());
            runningRequests.add(httpGet);

            return asyncRetriever.execute(httpGet, localContext);
        } catch (URISyntaxException e) {
            throw new IOException(String.format("Wrong URL: %s", url));
        }
    }

    public static interface ProgressListener {
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
        public void write(final byte[] b, final int off, final int len) throws IOException {
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
