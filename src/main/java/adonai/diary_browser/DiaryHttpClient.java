package adonai.diary_browser;

import android.support.annotation.NonNull;
import android.webkit.CookieManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

public class DiaryHttpClient {
    public final static String CLOUDFLARE_ANCHOR = "a = document.getElementById('jschl-answer');";
    public final static String FIXED_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36";
    
    private final static Pattern OPERATION_PATTERN = Pattern.compile("setTimeout\\(function\\(\\)\\{\\s+(var t,r,a,f.+?\\r?\\n[\\s\\S]+?a\\.value =.+?)\\r?\\n");
    private final static Pattern PASS_PATTERN = Pattern.compile("name=\"pass\" value=\"(.+?)\"");
    private final static Pattern CHALLENGE_PATTERN = Pattern.compile("name=\"jschl_vc\" value=\"(\\w+)\"");
    
    private URI currentUrl = URI.create("");

    HttpClient httpClient = new DefaultHttpClient();
    HttpContext localContext = new BasicHttpContext();
    CookieStore cookieStore = new BasicCookieStore();
    List<AbortableHttpRequest> runningRequests = new ArrayList<>();

    public DiaryHttpClient() {
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, FIXED_USER_AGENT);
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
    
    public boolean hasCookie(String name) {
        for(Cookie cookie : cookieStore.getCookies()) {
            if(cookie.getName().equals(name))
                return true;
        }
        return false;
    }

    public String postPageToString(String url, HttpEntity data) throws IOException {
        String current = resolve(url).toString();
        HttpPost httpPost = new HttpPost(current);
        runningRequests.add(httpPost);
        if(data != null) {
            httpPost.setEntity(data);
        }
        HttpResponse response = httpClient.execute(httpPost, localContext);
        syncCookiesWithWebViews();
        return EntityUtils.toString(response.getEntity());
    }

    public String postPageToString(HttpEntity data) throws IOException {
        return postPageToString("http://www.diary.ru/diary.php", data);
    }

    public String getPageAsString(String url) throws IOException {
        if (url.startsWith("file"))
            return null; // Не загружать локальные

        DefaultHttpClient asyncRetriever = new DefaultHttpClient();
        asyncRetriever.getParams().setParameter(CoreProtocolPNames.USER_AGENT, FIXED_USER_AGENT);
        String current = resolve(url).toString();
        HttpGet httpGet = new HttpGet(current);
        runningRequests.add(httpGet);
        HttpResponse response = asyncRetriever.execute(httpGet, localContext);
        syncCookiesWithWebViews();
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
            asyncRetriever.getParams().setParameter(CoreProtocolPNames.USER_AGENT, FIXED_USER_AGENT);
            String current = resolve(url).toString();
            HttpGet httpGet = new HttpGet(current);
            runningRequests.add(httpGet);
            HttpResponse response = asyncRetriever.execute(httpGet, localContext);
            syncCookiesWithWebViews();

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

    /**
     * Manual page processing
     * Remember to disconnect connection!
     *
     * @param url url to fetch
     * @return connection for manual usage
     */
    public HttpResponse getPage(URI url) throws IOException {
        try {
            HttpGet httpGet = new HttpGet(url);
            runningRequests.add(httpGet);
    
            HttpResponse result = httpClient.execute(httpGet, localContext);
            syncCookiesWithWebViews();
            return result;
        } catch (SSLException e) {
            return null;
        }
    }

    /**
     * Manual page processing
     * Remember to disconnect connection!
     *
     * @param url url to fetch
     * @return connection for manual usage
     */
    public HttpResponse getPage(URI url, HashMap<String, String> headers) throws IOException {
        try {
            HttpGet httpGet = new HttpGet(url);
            for(Map.Entry<String, String> key : headers.entrySet()) {
                httpGet.addHeader(key.getKey(), key.getValue());
            }
            runningRequests.add(httpGet);

            HttpResponse result = httpClient.execute(httpGet, localContext);
            syncCookiesWithWebViews();
            return result;
        } catch (SSLException e) {
            return null;
        }
    }

    public boolean cloudFlareSolve(String responseString) {
        // initialize Rhino
        Context rhino = Context.enter();
        try {
            String domain = "www.diary.ru";
            getPage(URI.create("http://" + domain), new HashMap<String, String>());
            
            // CF should wait
            Thread.sleep(5000);
            
            // extract the arithmetic operation
            Matcher operationSearch = OPERATION_PATTERN.matcher(responseString);
            Matcher challengeSearch = CHALLENGE_PATTERN.matcher(responseString);
            Matcher passSearch = PASS_PATTERN.matcher(responseString);
            if(!operationSearch.find() || !passSearch.find() || !challengeSearch.find())
                return false;
            
            String rawOperation = operationSearch.group(1);
            String challengePass = passSearch.group(1);
            String challenge = challengeSearch.group(1);
            
            
            String operation = rawOperation
                    .replaceAll("a\\.value =(.+?) \\+ .+?;", "$1")
                    .replaceAll("\\s{3,}[a-z](?: = |\\.).+", "");
            String js = operation.replace("\n", "");
            
            
            rhino.setOptimizationLevel(-1);
            Scriptable scope = rhino.initStandardObjects();
            int result = ((Double) rhino.evaluateString(scope, js, "CloudFlare JS Challenge", 1, null)).intValue();
            
            String answer = String.valueOf(result + domain.length());

            final List<NameValuePair> params = new ArrayList<>(3);
            params.add(new BasicNameValuePair("jschl_vc", challenge));
            params.add(new BasicNameValuePair("pass", challengePass));
            params.add(new BasicNameValuePair("jschl_answer", answer));
            
            HashMap<String, String> headers = new HashMap<>(1);
            headers.put("Referer", "http://www.diary.ru/");
            
            String url = "http://www.diary.ru/cdn-cgi/l/chk_jschl?" + URLEncodedUtils.format(params, "windows-1251");
            
            HttpResponse response = getPage(URI.create(url), headers);
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                response.getEntity().consumeContent();
                return true;
            }
        } catch (Exception e) {
            return false;
        } finally {
            Context.exit();
        }
        return false;
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
    
    private void syncCookiesWithWebViews() {
        List<Cookie> cookies = getCookieStore().getCookies();
        CookieManager cookieManager = CookieManager.getInstance();
        for (Cookie cookie : cookies) {
            String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
            cookieManager.setCookie("diary.ru", cookieString);
        }
    }

    public void setCurrentUrl(URI url) {
        currentUrl = url;
    }
    
    public URI resolve(String url) {
        return currentUrl.resolve(url);
    }

    public String getCurrentUrl() {
        return currentUrl.toString();
    }
}
