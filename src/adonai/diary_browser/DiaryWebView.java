package adonai.diary_browser;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import adonai.diary_browser.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("SetJavaScriptEnabled")
public class DiaryWebView extends PullToRefreshWebView
{
 // текущий контекст
    public static final int IMAGE_SAVE = 0;
    public static final int IMAGE_COPY_URL = 1;
    public static final int IMAGE_OPEN = 2;

    DiaryActivity mActivity;
    
    public DiaryWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public DiaryWebView(Context context, Mode mode)
    {
        super(context, mode);
        init();
    }

    public DiaryWebView(Context context)
    {
        super(context);
        init();
    }
    
    public void init()
    {
        if(getContext() instanceof DiaryActivity) 
            mActivity = (DiaryActivity) getContext();
    }
    
    public void setDefaultSettings()
    {
        WebSettings settings = getRefreshableView().getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("windows-1251");
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(false);
        settings.setLightTouchEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
        
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) 
        {
        	getRefreshableView().setWebViewClient(new WebViewClient()
	        {
	        	@Override
				public WebResourceResponse shouldInterceptRequest(WebView view, String url)
				{
	        		String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
					if(mime.contains("image"))
					{
						try
						{
							HttpResponse response = mActivity.mDHCL.getPage(url);
							if(response != null)
							{
								InputStream inImage = mActivity.mDHCL.getPage(url).getEntity().getContent();
								WebResourceResponse result = new WebResourceResponse(mime, response.getEntity().getContentEncoding().getValue(), inImage);
								
								return result;
							}
							
						} 
						catch (Exception e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return null;
					}
					return super.shouldInterceptRequest(view, url);
				}
	
				@Override
	            public boolean shouldOverrideUrlLoading(WebView  view, String  url)
	            {
	                mActivity.handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(url, false));
	                return true;
	            }
	        });
        }
        else
        {
        	getRefreshableView().setWebViewClient(new WebViewClient()
	        {
				@Override
	            public boolean shouldOverrideUrlLoading(WebView  view, String  url)
	            {
	                mActivity.handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(url, false));
	                return true;
	            }
	        });
        }
        setOnRefreshListener(new WebPageRefresher());
    }
    
    

    public class WebPageRefresher implements OnRefreshListener<WebView>
    {

        public void onRefresh(PullToRefreshBase<WebView> refreshView)
        {
            switch (refreshView.getId())
            {
                case R.id.page_browser:
                    mActivity.handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mActivity.mUser.currentDiaryPage.getPageURL(), true));
                    break;
                case R.id.umessage_browser:
                    mActivity.handleBackground(Utils.HANDLE_OPEN_MAIL, mActivity.mUser.currentUmailPage.getPageURL());
                    break;
            }
        }
    }
}
