package adonai.diary_browser;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import adonai.diary_browser.entities.Umail;

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

    private DiaryActivity mActivity;
    private WebView mWebContent;
    private ImageButton mScrollButton;

    private int mScrollDirection = 0;
    private GestureDetector mGestureDetector = new GestureDetector(getContext(), new ClickScrollDetector());


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
        mScrollButton = (ImageButton) layout.findViewById(R.id.updown_button);
        mActivity = (DiaryActivity) getContext();
        
        mWebContent.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
        mScrollButton.setOnClickListener(this);
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

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        ActionMode.Callback wrapper = new ActionModeWrapper(callback);
        return super.startActionMode(wrapper);
    }

    /**
     * Враппер для добавления кнопки "цитировать"
     */
    private class ActionModeWrapper implements ActionMode.Callback {

        private final ActionMode.Callback mOriginalCallback;
        
        public ActionModeWrapper(ActionMode.Callback cb) {
            mOriginalCallback = cb;
        }
        
        public MenuItem findByTitle(Menu menu, String regex) {
            for(int i = 0; i < menu.size(); ++i) {
                String title = menu.getItem(i).getTitle().toString();
                if(title.matches(regex))
                    return menu.getItem(i);
            }
            return null;
        }

        /**
         * Хак чтобы заставить работать систему перехвата копирования
         */
        @Override
        public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
            boolean result = mOriginalCallback.onCreateActionMode(mode, menu);
            final MenuItem copyButton = findByTitle(menu, "Копировать|Copy|Копіювати"); // ugly hack, no better way
            if(copyButton == null) {
                return result;
            }

            TypedValue drawable = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.quote_menu_drawable, drawable, true);

            menu.add(copyButton.getGroupId(), Menu.NONE, copyButton.getOrder(), R.string.quote) // будет в начале
                .setIcon(drawable.resourceId)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        onActionItemClicked(mode, copyButton); // программное нажатие на кнопку "скопировать"
                        // хак чтобы текст в клипборде точно попал к нам
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                                if (!cm.hasPrimaryClip())
                                    return;

                                ClipData cd = cm.getPrimaryClip();
                                if (cd.getItemCount() > 0) {
                                    String copied = cd.getItemAt(0).getText().toString();
                                    String quoted = "<span class='quote_text'>" + copied + "</span>";
                                    if (mActivity instanceof DiaryListActivity) { // цитируем
                                        ((DiaryListActivity) mActivity).handleMessagePaneAddText(quoted);
                                    }
                                }
                            }
                        }, 500);
                        return true;
                    }
                });

            return result;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mOriginalCallback.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mOriginalCallback.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mOriginalCallback.onDestroyActionMode(mode);
        }
    }

    private class DiaryWebClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            if (mActivity instanceof DiaryListActivity) {
                final Integer pos = ((DiaryListActivity) mActivity).browserHistory.getPosition();
                if (pos > 0)
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollTo(0, pos);
                        }
                    }, 1000);

            }
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

            ((DiaryListActivity) mActivity).browserHistory.setPosition(getScrollY());
            
            if (url.contains("?delpost&postid=")) { // удаление поста
                final String id = url.substring(url.lastIndexOf("=") + 1);
                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(view.getContext());
                builder.setTitle(android.R.string.dialog_alert_title).setCancelable(false).setMessage(R.string.really_delete);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.handleBackground(Utils.HANDLE_DELETE_POST, id);
                    }
                }).setNegativeButton(android.R.string.no, null);

                builder.create().show();
                return true;
            }

            if (url.contains("newpost&quote_")) { // кнопка репоста
                mActivity.handleBackground(Utils.HANDLE_REPOST, url);
                return true;
            }

            if (url.contains("?delcomment&commentid=")) { // удаление коммента
                final String id = url.substring(url.lastIndexOf("=") + 1);
                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(view.getContext());
                builder.setTitle(android.R.string.dialog_alert_title).setCancelable(false).setMessage(R.string.really_delete);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.handleBackground(Utils.HANDLE_DELETE_COMMENT, id);
                    }
                }).setNegativeButton(android.R.string.no, null);

                builder.create().show();
                return true;
            }

            if(url.contains("?tag_delete")) { // удаление тэга
                final String confirmed = url;
                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(view.getContext());
                builder.setTitle(android.R.string.dialog_alert_title).setCancelable(false).setMessage(R.string.really_delete);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.handleBackground(Utils.HANDLE_DELETE_TAG, confirmed);
                    }
                }).setNegativeButton(android.R.string.no, null);

                builder.create().show();
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

            mActivity.handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(url, url.equals(mActivity.getUser().getCurrentDiaryPage().getPageURL())));
            return true;
        }
    }

    // Часть кода относится к кнопке быстрой промотки
    private void handleScroll() {
        mScrollButton.setVisibility(View.VISIBLE);
        mScrollButton.removeCallbacks(fadeAnimation);
        mScrollButton.clearAnimation();
        mScrollButton.postDelayed(fadeAnimation, 2000);
        switch (mScrollDirection) {
            case VIEW_SCROLL_DOWN:
                mScrollButton.setImageResource(R.drawable.overscroll_button_down);
                break;
            case VIEW_SCROLL_UP:
                mScrollButton.setImageResource(R.drawable.overscroll_button_up);
                break;
        }

    }

    // Часть кода относится к кнопке быстрой промотки
    private Runnable fadeAnimation = new Runnable() {
        @Override
        public void run() {
            Animation animation = AnimationUtils.loadAnimation(mScrollButton.getContext(), android.R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mScrollButton.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mScrollButton.startAnimation(animation);
        }
    };
}
