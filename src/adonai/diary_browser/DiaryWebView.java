package adonai.diary_browser;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("SetJavaScriptEnabled")
public class DiaryWebView extends PullToRefreshWebView
{

    DiaryList mActivity;
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
        mActivity = Globals.mMain;
        mUser = Globals.mUser;
    }
    
    public void setDefaultSettings()
    {
        WebSettings settings = getRefreshableView().getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(true);
        settings.setDefaultTextEncodingName("utf-8");
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        getRefreshableView().setWebViewClient(DiaryWebClient);
        getRefreshableView().setBackgroundColor(0x00000000);
        setOnRefreshListener(new WebPageRefresher());
    }

    WebViewClient DiaryWebClient = new WebViewClient()
    {
        // Override page so it's load on my view only
        @Override
        public boolean shouldOverrideUrlLoading(WebView  view, String  url)
        {
         if ( url.contains("diary.ru"))
         {
            mActivity.pd = ProgressDialog.show(view.getContext(), mActivity.getString(R.string.loading), mActivity.getString(R.string.loading_data), true, true);
            mActivity.mHandler.sendMessage(mActivity.mHandler.obtainMessage(DiaryList.HANDLE_PICK_URL, url));
            return true;
         }
          
         // Return true to override url loading (In this case do nothing).
         return true;
        }
    };
    
    public class WebPageRefresher implements OnRefreshListener<WebView>
    {

        public void onRefresh(PullToRefreshBase<WebView> refreshView)
        {
            switch (refreshView.getId())
            {
                case R.id.post_browser:
                    mActivity.mHandler.sendMessage(mActivity.mHandler.obtainMessage(DiaryList.HANDLE_GET_DIARY_POSTS_DATA, new Pair<String, Boolean>(mUser.currentDiaryPosts.get_diary_URL(), true)));
                break;
                case R.id.comment_browser:
                    mActivity.mHandler.sendMessage(mActivity.mHandler.obtainMessage(DiaryList.HANDLE_GET_POST_COMMENTS_DATA, new Pair<String, Boolean>(mUser.currentPostComments.get_post_URL(), true)));
                break;
            }
        }
        
    }
}
