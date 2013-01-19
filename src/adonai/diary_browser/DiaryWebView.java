package adonai.diary_browser;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import adonai.diary_browser.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
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

    IRequestHandler mActivity;
    UserData mUser;
    
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
        if(getContext() instanceof IRequestHandler) 
            mActivity = (IRequestHandler) getContext();
        mUser = Globals.mUser;
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
        getRefreshableView().setWebViewClient(new DiaryWebClient());
        setOnRefreshListener(new WebPageRefresher());
    }

    private class DiaryWebClient extends WebViewClient
    {
        // Override page so it's load on my view only
        @Override
        public boolean shouldOverrideUrlLoading(WebView  view, String  url)
        {
            mActivity.handleBackground(DiaryList.HANDLE_PICK_URL, new Pair<String, Boolean>(url, false));
            return true;
        }
    };
    
    public class WebPageRefresher implements OnRefreshListener<WebView>
    {

        public void onRefresh(PullToRefreshBase<WebView> refreshView)
        {
            switch (refreshView.getId())
            {
                case R.id.page_browser:
                    mActivity.handleBackground(DiaryList.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.currentDiaryPage.get_page_URL(), true));
                    break;
                case R.id.umessage_browser:
                    mActivity.handleBackground(UmailList.HANDLE_OPEN_MAIL, mUser.currentUmailPage.get_page_URL());
                    break;
            }
        }
    }
}
