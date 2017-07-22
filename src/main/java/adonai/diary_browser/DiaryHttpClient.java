package adonai.diary_browser;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.webkit.CookieManager;

import okhttp3.*;
import okhttp3.internal.Util;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

import adonai.diary_browser.misc.FileUtils;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

@SuppressWarnings("deprecation")
public class DiaryHttpClient {
    public final static String CLOUDFLARE_ANCHOR = "a = document.getElementById('jschl-answer');";
    public final static String FIXED_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36";

    private final static Pattern PASS_PATTERN = Pattern.compile("name=\"pass\" value=\"(.+?)\"");
    private final static Pattern CHALLENGE_PATTERN = Pattern.compile("name=\"jschl_vc\" value=\"(\\w+)\"");
    private final static Pattern OPERATION_PATTERN = Pattern.compile("setTimeout\\(function\\(\\)\\{\\s+(var s,t,o,p,b,r,e,a,k,i,n,g,f.+?\\r?\\n[\\s\\S]+?a\\.value =.+?)\\r?\\n");

    private URI currentUrl = URI.create("http://www.diary.ru");

    private final OkHttpClient httpClient;
    private java.net.CookieManager cookieManager = new java.net.CookieManager();
    private List<Call> runningRequests = new ArrayList<>();

    public DiaryHttpClient() {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        httpClient = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .addInterceptor(new UserAgentInterceptor())
                .build();
    }

    public class UserAgentInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", FIXED_USER_AGENT)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }

    public void abort() {
        new Thread() {
            @Override
            public void run() {
                for(Call request : runningRequests) {
                    request.cancel();
                }
                runningRequests.clear();
            }
        }.start();
    }

    public CookieStore getCookieStore() {
        return cookieManager.getCookieStore();
    }

    public boolean hasCookie(@NonNull String name) {
        for(HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
            if(cookie.getName().equals(name))
                return true;
        }
        return false;
    }

    public String postPageToString(@NonNull String url, @NonNull RequestBody data) {
        return postPageToString(url, data, null);
    }

    public String postPageToString(@NonNull String url, @NonNull RequestBody data, @Nullable Headers headers) {
        URI current = resolve(url);

        Request.Builder httpPost = new Request.Builder()
                .url(HttpUrl.get(current))
                .post(data);

        if(headers != null) {
            httpPost.headers(headers);
        }

        Call call = httpClient.newCall(httpPost.build());
        runningRequests.add(call);
        try {
            Response answer = call.execute();
            syncCookiesWithWebViews();
            return answer.body().string();
        } catch (IOException e) {
            return null;
        }
    }

    public String postPageToString(@NonNull String url, @NonNull List<Pair<String, String>> nameValuePairs) {
        URI current = resolve(url);
        FormBody.Builder fb = new FormBody.Builder();
        for(Pair<String, String> param : nameValuePairs) {
            try {
                fb.addEncoded(param.first, URLEncoder.encode(param.second, "windows-1251"));
            } catch (UnsupportedEncodingException ignored) {
            }
        }

        Request httpPost = new Request.Builder()
                .url(HttpUrl.get(current))
                .post(fb.build())
                .build();
        Call call = httpClient.newCall(httpPost);
        runningRequests.add(call);

        try {
            Response answer = call.execute();
            syncCookiesWithWebViews();
            return answer.body().string();
        } catch (IOException e) {
            return null;
        }
    }

    public String postPageToString(@NonNull RequestBody data) {
        return postPageToString("http://www.diary.ru/diary.php", data);
    }

    public String postPageToString(@NonNull List<Pair<String, String>> nameValuePairs) {
        return postPageToString("http://www.diary.ru/diary.php", nameValuePairs);
    }

    /**
     * Return page as string or null if failed
     * @param url
     * @return
     * @throws IOException
     */
    public String getPageAsString(@NonNull String url) {
        try {
            if (url.startsWith("file"))
                return null; // Не загружать локальные

            Call call = httpClient.newCall(new Request.Builder().url(HttpUrl.get(resolve(url))).get().build());
            runningRequests.add(call);
            Response answer = call.execute();
            syncCookiesWithWebViews();
            return answer.body().string();
        } catch (IOException e) {
            return null;
        }
    }

    public byte[] getPageAsByteArray(@NonNull String url) {
        if (url.startsWith("file"))
            return null; // Не загружать локальные

        try {
            Call call = httpClient.newCall(new Request.Builder().url(HttpUrl.get(resolve(url))).get().build());
            runningRequests.add(call);
            Response answer = call.execute();
            syncCookiesWithWebViews();
            return answer.body().bytes();
        } catch (Exception ignored) {
            return null;
        } // stream close / timeout
    }

    /**
     * Manual page processing
     * Remember to disconnect connection!
     *
     * @param url url to fetch
     * @return connection for manual usage
     */
    public Response getPage(@NonNull URI url) throws IOException {
        try {
            Call call = httpClient.newCall(new Request.Builder().url(HttpUrl.get(url)).get().build());
            runningRequests.add(call);

            Response result = call.execute();
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
    public Response getPage(@NonNull URI url, @NonNull Headers headers) throws IOException {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(HttpUrl.get(url))
                    .headers(headers)
                    .get();
            Call call = httpClient.newCall(builder.build());
            runningRequests.add(call);

            Response result = call.execute();
            syncCookiesWithWebViews();
            return result;
        } catch (SSLException e) {
            return null;
        }
    }

    public boolean cloudFlareSolve(@NonNull String responseString) {
        // initialize Rhino
        Context rhino = Context.enter();
        try {
            String domain = "www.diary.ru";
            getPage(URI.create("http://" + domain));

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
                    .replaceAll("a\\.value = (parseInt\\(.+?\\)).+", "$1")
                    .replaceAll("\\s{3,}[a-z](?: = |\\.).+", "");
            String js = operation.replace("\n", "");

            rhino.setOptimizationLevel(-1);
            Scriptable scope = rhino.initStandardObjects();
            int result = ((Double) rhino.evaluateString(scope, js, "CloudFlare JS Challenge", 1, null)).intValue();

            String answer = String.valueOf(result + domain.length());

            Headers headers = new Headers.Builder()
                    .add("Referer", "http://www.diary.ru/")
                    .build();

            String url = new HttpUrl.Builder()
                    .scheme("http")
                    .host("www.diary.ru")
                    .addPathSegment("cdn-cgi").addPathSegment("l").addPathSegment("chk_jschl")
                    .addEncodedQueryParameter("jschl_vc", URLEncoder.encode(challenge, "windows-1251"))
                    .addEncodedQueryParameter("pass", URLEncoder.encode(challengePass, "windows-1251"))
                    .addEncodedQueryParameter("jschl_answer", URLEncoder.encode(answer, "windows-1251"))
                    .build().toString();

            Response response = getPage(URI.create(url), headers);
            if(response.isSuccessful()) {
                response.request();
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

    public static class CountingFileRequestBody extends RequestBody {

        private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

        private final File file;
        private final ProgressListener listener;
        private final MediaType contentType;

        public CountingFileRequestBody(File file, ProgressListener listener) {
            this.file = file;
            this.contentType = MediaType.parse(FileUtils.getMimeType(file));
            this.listener = listener;
        }

        @Override
        public long contentLength() {
            return file.length();
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            Source source = null;
            try {
                source = Okio.source(file);
                long total = 0;
                long read;

                while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                    total += read;
                    sink.flush();
                    this.listener.transferred(total);

                }
            } finally {
                Util.closeQuietly(source);
            }
        }
    }

    private void syncCookiesWithWebViews() {
        List<HttpCookie> cookies = getCookieStore().getCookies();
        CookieManager cookieManager = CookieManager.getInstance();

        // to webviews
        for (HttpCookie cookie : cookies) {
            String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
            cookieManager.setCookie("diary.ru", cookieString);
        }
    }

    public void syncCookiesWithClient() {
        CookieStore store = getCookieStore();
        CookieManager cookieManager = CookieManager.getInstance();

        String cookies = cookieManager.getCookie("diary.ru");
        String[] cookieValues = cookies.split(";");

        HttpCookie cookie;
        URI diary = URI.create("http://diary.ru");
        for (String cookieValue : cookieValues) {
            String[] split = cookieValue.split("=");
            if (split.length == 2)
                cookie = new HttpCookie(split[0], split[1]);
            else
                cookie = new HttpCookie(split[0], null);

            cookie.setDomain("diary.ru");
            store.add(diary, cookie);
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
