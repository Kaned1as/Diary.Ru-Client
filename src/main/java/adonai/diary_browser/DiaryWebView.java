package adonai.diary_browser;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import adonai.diary_browser.entities.Umail;

@SuppressLint("SetJavaScriptEnabled")
public class DiaryWebView extends WebView {
    public static final int MILLIS_TO_FAST_SCROLL = 200;

    // текущий контекст
    public static final int IMAGE_SAVE          = 0;
    public static final int IMAGE_COPY_URL      = 1;
    public static final int IMAGE_OPEN_HERE     = 2;
    public static final int IMAGE_OPEN_EXTERNAL = 3;

    DiaryActivity mActivity;
    int scrolling = 0;
    private GestureDetector mGestureDetector = new GestureDetector(getContext(), new webGestureDetector());

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

    public void init() {
        if (getContext() instanceof DiaryActivity)
            mActivity = (DiaryActivity) getContext();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public void setDefaultSettings() {
        WebSettings settings = getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(DiaryHttpClient.FIXED_USER_AGENT);
        settings.setDefaultTextEncodingName("windows-1251");
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        setWebViewClient(new DiaryWebClient());
        setWebChromeClient(new WebChromeClient());

        // Lollipop blocks mixed content but we should load CSS from filesystem
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    private class webGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mActivity instanceof DiaryListActivity)
                if (e1 != null && e2 != null && e2.getEventTime() - e1.getEventTime() < MILLIS_TO_FAST_SCROLL) {
                    if (distanceY > 90) {
                        scrolling = Utils.VIEW_SCROLL_DOWN;
                        ((DiaryListActivity) mActivity).handleScroll(Utils.VIEW_SCROLL_DOWN);
                    } else if (distanceY < -90) {
                        scrolling = Utils.VIEW_SCROLL_UP;
                        ((DiaryListActivity) mActivity).handleScroll(Utils.VIEW_SCROLL_UP);
                    }
                }


            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Message msg = Message.obtain(mActivity.mUiHandler, Utils.HANDLE_NAME_CLICK);
            mActivity.mPageBrowser.requestFocusNodeHref(msg);

            return super.onSingleTapConfirmed(e);
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
}
