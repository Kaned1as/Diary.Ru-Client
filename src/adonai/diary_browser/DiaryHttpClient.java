package adonai.diary_browser;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class DiaryHttpClient 
{

	DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpContext localContext = new BasicHttpContext();
    CookieStore cookieStore = new BasicCookieStore();

    HttpResponse response = null;
    HttpPost httpPost = null;
    String lastURL = "";

    public DiaryHttpClient() 
    {
    	httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
    	localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }
    
    public void clearCookies()
    {
    	httpClient.getCookieStore().clear();
    }

    public void abort() 
    {

    	try 
    	{
    		if(httpClient != null)
    		{
    			System.out.println("Abort.");
    			httpPost.abort();
    		}
    	} catch (Exception e) 
    	{
    		System.out.println("HTTPHelp : Abort Exception : " + e);
    	}
    }

    public HttpResponse postPage(String url, HttpEntity data) {

    	httpPost = new HttpPost(url);
    	response = null;
    	
    	if(data != null)
    		httpPost.setEntity(data);
    	else
        	lastURL = url;

    	try 
    	{
    		response = httpClient.execute(httpPost,localContext);
    	} catch (ClientProtocolException e) {
    		System.out.println("HTTPHelp : ClientProtocolException : " + e);
    	} catch (IOException e) {
    		System.out.println("HTTPHelp : IOException : " + e);
    	} 
  		return response;
    }
}
