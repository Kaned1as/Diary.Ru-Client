package adonai.diary_browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@SuppressLint("SetJavaScriptEnabled")
public class DiaryWebView extends PullToRefreshWebView
{
    public static final int MILLIS_TO_FAST_SCROLL = 200;

 // текущий контекст
    public static final int IMAGE_SAVE = 0;
    public static final int IMAGE_COPY_URL = 1;
    public static final int IMAGE_OPEN = 2;

    DiaryActivity mActivity;
    int scrolling = 0;

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
        settings.setUseWideViewPort(false);
        settings.setLightTouchEnabled(true);
        //settings.setBuiltInZoomControls(true);
        //settings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
        getRefreshableView().setWebViewClient(new DiaryWebClient());
        setOnRefreshListener(new WebPageRefresher());
        getRefreshableView().setOnTouchListener(new webTouchListener());
    }

    // Часть кода относится к кнопке быстрой промотки
    private class webTouchListener implements OnTouchListener
    {
        private GestureDetector mGestureDetector = new GestureDetector(getContext(), new webGestureDetector());
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            mGestureDetector.onTouchEvent(event);
            return false;
        }

        private class webGestureDetector extends GestureDetector.SimpleOnGestureListener
        {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
            {
                if(e1 != null && e2 != null && e2.getEventTime() - e1.getEventTime() < MILLIS_TO_FAST_SCROLL)
                {
                    if(distanceY > 90)
                    {
                        scrolling = Utils.VIEW_SCROLL_DOWN;
                        mActivity.handleScroll(Utils.VIEW_SCROLL_DOWN);
                    }
                    else if (distanceY < -90)
                    {
                        scrolling = Utils.VIEW_SCROLL_UP;
                        mActivity.handleScroll(Utils.VIEW_SCROLL_UP);
                    }
                }


                return false;
            }
        }
    }

    private class DiaryWebClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView  view, String  url)
        {
            if(url.contains("diary"))
            {
                if(url.contains("?delpost&postid=")) // удаление поста
                {
                    final String id = url.substring(url.lastIndexOf("=") + 1);
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setTitle(android.R.string.dialog_alert_title).setCancelable(false).setMessage(R.string.really_delete);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mActivity.handleBackground(Utils.HANDLE_DELETE_POST, id);
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    builder.create().show();
                    return true;
                }

                if(url.contains("?delcomment&commentid=")) // удаление коммента
                {
                    final String id = url.substring(url.lastIndexOf("=") + 1);
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setTitle(android.R.string.dialog_alert_title).setCancelable(false).setMessage(R.string.really_delete);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            mActivity.handleBackground(Utils.HANDLE_DELETE_COMMENT, id);
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    builder.create().show();
                    return true;
                }

                if(url.contains("?editpost&postid=")) // редактирование поста
                {
                    mActivity.handleBackground(Utils.HANDLE_EDIT_POST, url);
                    return true;
                }

                if(url.contains("?editcomment&commentid=")) // редактирование комментария
                {
                    mActivity.handleBackground(Utils.HANDLE_EDIT_COMMENT, url);
                    return true;
                }

                if(url.contains("u-mail/?new&username=")) // послать кому-то U-Mail
                {
                    try
                    {
                        Intent postIntent = new Intent(getContext(), MessageSender.class);
                        postIntent.putExtra("TypeId", "umailTo");
                        postIntent.putExtra("umailTo", URLDecoder.decode(url.substring(url.lastIndexOf("username=") + "username=".length()), "windows-1251"));
                        postIntent.putExtra("signature", mActivity.mUser.signature);
                        postIntent.putExtra("sendURL", "http://www.diary.ru/diary.php");
                        getContext().startActivity(postIntent);
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        Toast.makeText(getContext(), getContext().getString(R.string.codepage_missing), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }


                // а вот здесь будет обработчик того, что не смог сделать AJAX в яваскрипте дневников
                if(url.contains("?newquote&postid=") || url.contains("?delquote&postid=") || url.contains("up&signature="))
                {
                    mActivity.mService.handleRequest(Utils.HANDLE_JUST_DO_GET, url);
                    return true;
                }
            }
            mActivity.handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(url, url.equals(mActivity.mUser.currentDiaryPage.getPageURL())));
            return true;
        }
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
