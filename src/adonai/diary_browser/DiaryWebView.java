package adonai.diary_browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

// не боимся и не печалимся, запускаем приложение с флагом -Dflag_no_pichal
// Это была шутка.
// Если что, я не хотел обидеть никаких печальных разработчиков
@SuppressLint("SetJavaScriptEnabled")
public class DiaryWebView extends WebView
{
    public static final int MILLIS_TO_FAST_SCROLL = 200;

 // текущий контекст
    public static final int IMAGE_SAVE = 0;
    public static final int IMAGE_COPY_URL = 1;
    public static final int IMAGE_OPEN = 2;

    DiaryActivity mActivity;
    int scrolling = 0;

    public PullToRefreshAttacher.OnRefreshListener refresher = new PullToRefreshAttacher.OnRefreshListener()
    {
        @Override
        public void onRefreshStarted(View view)
        {
            if(mActivity == null)
                return;

            if(mActivity instanceof DiaryListActivity)
                mActivity.handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mActivity.mUser.currentDiaryPage.getPageURL(), true));
            else if(mActivity instanceof UmailListActivity)
                mActivity.handleBackground(Utils.HANDLE_OPEN_MAIL, mActivity.mUser.currentUmailPage.getPageURL());
        }
    };

    public DiaryWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public DiaryWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private GestureDetector mGestureDetector = new GestureDetector(getContext(), new webGestureDetector());

    private class webGestureDetector extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if(mActivity instanceof DiaryListActivity)
                if(e1 != null && e2 != null && e2.getEventTime() - e1.getEventTime() < MILLIS_TO_FAST_SCROLL)
                {
                    if(distanceY > 90)
                    {
                        scrolling = Utils.VIEW_SCROLL_DOWN;
                        ((DiaryListActivity)mActivity).handleScroll(Utils.VIEW_SCROLL_DOWN);
                    }
                    else if (distanceY < -90)
                    {
                        scrolling = Utils.VIEW_SCROLL_UP;
                        ((DiaryListActivity)mActivity).handleScroll(Utils.VIEW_SCROLL_UP);
                    }
                }


            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            Message msg = Message.obtain(mActivity.mUiHandler, Utils.HANDLE_NAME_CLICK);
            mActivity.mPageBrowser.requestFocusNodeHref(msg);

            return super.onSingleTapConfirmed(e);
        }
    }

    public void setDefaultSettings()
    {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("windows-1251");
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUseWideViewPort(false);
        settings.setLightTouchEnabled(true);
        setWebViewClient(new DiaryWebClient());
        setWebChromeClient(new WebChromeClient());
    }

    private class DiaryWebClient extends WebViewClient
    {
        @Override
        public void onPageFinished(WebView view, String url)
        {
            if(mActivity instanceof DiaryListActivity)
            {
                final Integer pos = ((DiaryListActivity) mActivity).browserHistory.getPosition();
                if(pos > 0)
                    view.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            scrollTo(0, pos);
                        }
                    }, 1000);

            }
        }

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
                    }).setNegativeButton(android.R.string.no, null);

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
                    }).setNegativeButton(android.R.string.no, null);

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
                        mActivity.messagePane.prepareFragment(mActivity.mUser.signature, "http://www.diary.ru/diary.php", "umailTo", URLDecoder.decode(url.substring(url.lastIndexOf("username=") + "username=".length()), "windows-1251"), null);
                        mActivity.slider.openPane();
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        Toast.makeText(getContext(), getContext().getString(R.string.codepage_missing), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }


                // а вот здесь будет обработчик того, что не смог сделать AJAX в яваскрипте дневников
                if(url.contains("?newquote&postid=") || url.contains("?delquote&postid=") || url.contains("up&signature=") || url.contains("down&signature=") || url.contains("?fav_add&userid=") || url.contains("?fav_del&userid="))
                {
                    mActivity.mService.handleRequest(Utils.HANDLE_JUST_DO_GET, url);
                    return true;
                }
            }
            if(mActivity instanceof DiaryListActivity)
                ((DiaryListActivity)mActivity).browserHistory.setPosition(getScrollY());

            mActivity.handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(url, url.equals(mActivity.mUser.currentDiaryPage.getPageURL())));
            return true;
        }
    }
}
