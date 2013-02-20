package adonai.diary_browser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
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

    private HttpPost httpPost = null;
    private HttpGet httpGet = null;
    String currentURL = "";

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
    		if(httpClient != null && httpPost != null)
    		{
    			httpPost.abort();
    		}
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
    		URI address = new URI(currentURL).resolve(url);
        	httpPost = new HttpPost(address.toURL().toString());
        	if(data != null)
        		httpPost.setEntity(data);
        	else
            	currentURL = address.toURL().toString();
        	
    		response = httpClient.execute(httpPost, localContext);
    	}
    	catch (IllegalStateException isex)
    	{
    		System.out.println("HTTPHelp : no such host : " + isex);
    	}
    	catch (IllegalArgumentException e)
    	{
    	    System.out.println("Illegal argument: " + e);
    	}
    	catch (ClientProtocolException e) 
    	{
    		System.out.println("HTTPHelp : ClientProtocolException : " + e);
    	} 
    	catch (IOException e) 
    	{
    		System.out.println("HTTPHelp : IOException : " + e);
    	} 
    	catch (URISyntaxException e) 
    	{
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
        	httpGet = new HttpGet(address.toURL().toString());
    		response = AsyncRetriever.execute(httpGet, localContext);
    	} 
    	catch (ClientProtocolException e) 
    	{
    		System.out.println("HTTPHelp : ClientProtocolException : " + e);
    	} 
    	catch (IOException e) 
    	{
    		System.out.println("HTTPHelp : IOException : " + e);
    	} catch (URISyntaxException e) 
    	{
			e.printStackTrace();
		} 
    	
  		return response;
    }
}
