package adonai.diary_browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import adonai.diary_browser.misc.SearchContainer;
import adonai.diary_browser.pages.Umail;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

@SuppressLint("SetJavaScriptEnabled")
public class DiaryWebView extends FrameLayout implements View.OnClickListener {

    private static final int VIEW_SCROLL_DOWN       = 2;
    private static final int VIEW_SCROLL_UP         = 1;
    private static final int MIN_TRIGGER_DISTANCE   = 90;
    private static final int MILLIS_TO_FAST_SCROLL  = 200;

    // текущий контекст
    public static final int IMAGE_SAVE              = 0;
    public static final int IMAGE_COPY_URL          = 1;
    public static final int IMAGE_OPEN_HERE         = 2;
    public static final int IMAGE_OPEN_EXTERNAL     = 3;

    private final GestureDetector mGestureDetector = new GestureDetector(getContext(), new ClickScrollDetector());
    private final Runnable fadeAnimation = new FadeAnimation();

    private DiaryActivity mActivity;
    private WebView mWebContent;
    private SearchContainer mSearchBar;

    private LinearLayout mButtonPanel;
    private ImageButton mScrollButton;
    private ImageButton mSearchButton;

    private PositionTracker mListener;

    private int mScrollDirection = 0;


    /**
     * Стандартные конструкторы
     */

    public DiaryWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DiaryWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public DiaryWebView(Context context) {
        super(context);
        init();
    }

    /**
     * Делегируем наверх методы WebView
     */

    public void loadUrl(String url) {
        mWebContent.loadUrl(url);
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        mWebContent.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    public WebSettings getSettings() {
        return mWebContent.getSettings();
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return mWebContent.canScrollVertically(direction);
    }

    /**
     * Далее наша логика
     */

    public void init() {
        final View layout = LayoutInflater.from(getContext()).inflate(R.layout.diary_web_view, this, true);
        mWebContent = (WebView) layout.findViewById(R.id.web_content);

        mSearchBar = new SearchContainer((LinearLayout) layout.findViewById(R.id.search_bar_layout), mWebContent);

        mButtonPanel = (LinearLayout) layout.findViewById(R.id.page_button_panel);
        mScrollButton = (ImageButton) mButtonPanel.findViewById(R.id.page_updown_button);
        mSearchButton = (ImageButton) mButtonPanel.findViewById(R.id.page_search_button);

        mActivity = (DiaryActivity) getContext();

        mWebContent.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
        mScrollButton.setOnClickListener(this);
        mSearchButton.setOnClickListener(this);
    }

    public void setDefaultSettings() {
        WebSettings settings = mWebContent.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(DiaryHttpClient.FIXED_USER_AGENT);
        settings.setDefaultTextEncodingName("windows-1251");
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        mWebContent.setWebViewClient(new DiaryWebClient());
        mWebContent.setWebChromeClient(new WebChromeClient());

        // Lollipop blocks mixed content but we should load CSS from filesystem
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    public void setPositionTracker(PositionTracker listener) {
        this.mListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (v == mScrollButton) {
            // Офигительная штука, документации по которой нет.
            // Устанавливает начальную скорость скролла даже если в данный момент уже происходит скроллинг
            if (mScrollDirection == VIEW_SCROLL_DOWN)
                mWebContent.flingScroll(0, 100000);
            else
                mWebContent.flingScroll(0, -100000);
        }

        if(v == mSearchButton) {
            boolean wasHidden = mSearchBar.getVisibility() == View.GONE;
            mSearchBar.setVisibility(wasHidden ? View.VISIBLE : View.GONE);
        }
    }

    // Часть кода относится к панели кнопок
    private void handleScroll() {
        mButtonPanel.setVisibility(View.VISIBLE);
        mButtonPanel.removeCallbacks(fadeAnimation);
        mButtonPanel.clearAnimation();
        mButtonPanel.postDelayed(fadeAnimation, 2000);

        // для кнопки быстрой промотки
        switch (mScrollDirection) {
            case VIEW_SCROLL_DOWN:
                mScrollButton.setImageResource(R.drawable.overscroll_button_down);
                break;
            case VIEW_SCROLL_UP:
                mScrollButton.setImageResource(R.drawable.overscroll_button_up);
                break;
        }

    }

    /**
     * Трекер ответственен за хранение состояния и отслеживание страницы
     */
    public interface PositionTracker {

        /**
         * Сохраняет текущую позицию страницы
         */
        void savePosition(String url, int position);

        /**
         * Восстанавливает текущую позицию
         */
        int restorePosition(String url);
    }

    private class ClickScrollDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 != null && e2 != null && e2.getEventTime() - e1.getEventTime() < MILLIS_TO_FAST_SCROLL) {
                if (distanceY > MIN_TRIGGER_DISTANCE) {
                    mScrollDirection = VIEW_SCROLL_DOWN;
                    handleScroll();
                } else if (distanceY < -MIN_TRIGGER_DISTANCE) {
                    mScrollDirection = VIEW_SCROLL_UP;
                    handleScroll();
                }
            }

            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Message msg = Message.obtain(mActivity.mUiHandler, Utils.HANDLE_NAME_CLICK);
            mWebContent.requestFocusNodeHref(msg);

            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Message msg = Message.obtain(mActivity.mUiHandler, Utils.HANDLE_IMAGE_CLICK);
            mWebContent.requestImageRef(msg);
        }
    }

    private class DiaryWebClient extends WebViewClient {

        @Override
        public void onPageFinished(final WebView view, String url) {
            if(mListener == null)
                return;

            final Integer pos = mListener.restorePosition(url);
            if (pos > 0)
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.scrollTo(0, pos);
                    }
                }, 1000);

        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!url.contains("diary.ru") || !(mActivity instanceof DiaryListActivity)) {
                // не обрабатываем никакие ссылки кроме дневниковых, остальные отправляем в селектор
                // не открываем ссылки из активности U-Mail, посылаем обратно
                final Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                // createChooser создает новый Intent из предыдущего, флаги нужно присоединять уже к нему!
                mActivity.startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)));

                return true;
            }

            if(mListener != null) {
                // сохраним позицию перед загрузкой
                mListener.savePosition(view.getUrl(), view.getScrollY());
            }

            if (url.contains("?delpost&postid=")) { // удаление поста
                final String id = url.substring(url.lastIndexOf("=") + 1);
                new MaterialDialog.Builder(view.getContext())
                        .title(android.R.string.dialog_alert_title)
                        .content(R.string.really_delete)
                        .positiveText(android.R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                mActivity.handleBackground(Utils.HANDLE_DELETE_POST, id);
                            }
                        })
                        .negativeText(android.R.string.no)
                        .cancelable(false)
                        .show();
                return true;
            }

            if (url.contains("newpost&quote_")) { // кнопка репоста
                mActivity.handleBackground(Utils.HANDLE_REPOST, url);
                return true;
            }

            if (url.contains("?delcomment&commentid=")) { // удаление коммента
                final String id = url.substring(url.lastIndexOf("=") + 1);
                new MaterialDialog.Builder(view.getContext())
                        .title(android.R.string.dialog_alert_title)
                        .content(R.string.really_delete)
                        .positiveText(android.R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                mActivity.handleBackground(Utils.HANDLE_DELETE_COMMENT, id);
                            }
                        })
                        .negativeText(android.R.string.no)
                        .cancelable(false)
                        .show();
                return true;
            }

            if(url.contains("?tag_delete")) { // удаление тэга
                final String confirmed = url;
                new MaterialDialog.Builder(view.getContext())
                        .title(android.R.string.dialog_alert_title)
                        .content(R.string.really_delete)
                        .cancelable(false)
                        .positiveText(android.R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                mActivity.handleBackground(Utils.HANDLE_DELETE_TAG, confirmed);
                            }
                        })
                        .negativeText(android.R.string.no)
                        .show();
                return true;
            }

            if (url.contains("?editpost&postid=")) { // редактирование поста
                mActivity.handleBackground(Utils.HANDLE_EDIT_POST, url);
                return true;
            }

            if (url.contains("?editcomment&commentid=")) { // редактирование комментария
                mActivity.handleBackground(Utils.HANDLE_EDIT_COMMENT, url);
                return true;
            }

            if (url.contains("u-mail/?new&username=")) { // послать кому-то U-Mail
                try {
                    Umail withAddress = new Umail();
                    withAddress.receiver = URLDecoder.decode(url.substring(url.lastIndexOf("username=") + "username=".length()), "windows-1251");
                    mActivity.messagePane.prepareFragment(mActivity.getUser().getSignature(), withAddress);
                    mActivity.slider.openPane();
                } catch (UnsupportedEncodingException e) {
                    Toast.makeText(getContext(), getContext().getString(R.string.codepage_missing), Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            // а вот здесь будет обработчик того, что не смог сделать AJAX в яваскрипте дневников
            if (url.contains("?newquote&postid=") || url.contains("?delquote&postid=") ||
                    url.contains("up&signature=") || url.contains("down&signature=") ||
                    url.contains("?fav_add&userid=") || url.contains("?fav_del&userid=")) {
                mActivity.mService.handleRequest(Utils.HANDLE_JUST_DO_GET, url);
                return true;
            }

            mActivity.handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(url, url.equals(mActivity.getUser().getCurrentDiaryPage().getPageUrl())));
            return true;
        }
    }

    // Часть кода относится к кнопке быстрой промотки
    private class FadeAnimation implements Runnable {
        @Override
        public void run() {
            Animation animation = AnimationUtils.loadAnimation(mButtonPanel.getContext(), android.R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mButtonPanel.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mButtonPanel.startAnimation(animation);
        }
    }
}
