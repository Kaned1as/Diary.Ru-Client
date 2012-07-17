package adonai.diary_browser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class DiaryHttpClient {

	DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpContext localContext = new BasicHttpContext();
    CookieStore cookieStore = new BasicCookieStore();
    private String ret;

    HttpResponse response = null;
    HttpPost httpPost = null;

    public void init() {
    	httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
    	localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }
    
    public void clearCookies() {
    	httpClient.getCookieStore().clear();
    }

    public void abort() {

    	try {
    		if(httpClient != null) {
    			System.out.println("Abort.");
    			httpPost.abort();
    		}
    	} catch (Exception e) {
    		System.out.println("HTTPHelp : Abort Exception : " + e);
    	}
    }

    public String postPage(String url, String data, boolean returnAddr) {
    	ret = null;

    	httpPost = new HttpPost(url);
    	response = null;

    	StringEntity tmp = null;		

    	httpPost.setHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux " + "i686; en-US; rv:1.8.1.6) Gecko/20061201 Firefox/2.0.0.6 (Ubuntu-feisty)");
    	httpPost.setHeader("Accept", "text/html,application/xml," + "application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
    	httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

    	try {
    		tmp = new StringEntity(data,"UTF-8");
    	} catch (UnsupportedEncodingException e) {
    		System.out.println("HTTPHelp : UnsupportedEncodingException : "+e);
    	}

    	httpPost.setEntity(tmp);

    	try {
    		response = httpClient.execute(httpPost,localContext);
    	} catch (ClientProtocolException e) {
    		System.out.println("HTTPHelp : ClientProtocolException : " + e);
    	} catch (IOException e) {
    		System.out.println("HTTPHelp : IOException : " + e);
    	} 
        ret = response.getStatusLine().toString();

        return ret;
    }
}
