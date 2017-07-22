package adonai.diary_browser.misc;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import adonai.diary_browser.DiaryListActivity;
import adonai.diary_browser.R;

/**
 * Враппер для WebView
 * Необходим для добавления кнопки "цитировать"
 */
public class WebFrame extends WebView {

    public WebFrame(Context context) {
        super(context);
    }

    public WebFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        ActionMode.Callback wrapper = new ActionModeWrapper(callback);
        return super.startActionMode(wrapper);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        ActionMode.Callback wrapper = new ActionModeWrapper23(callback);
        return super.startActionMode(wrapper, type);
    }

    public static MenuItem findByTitle(Menu menu, String regex) {
        for(int i = 0; i < menu.size(); ++i) {
            String title = menu.getItem(i).getTitle().toString();
            if(title.matches(regex))
                return menu.getItem(i);
        }
        return null;
    }

    /**
     * Враппер для добавления кнопки "цитировать" - для Андроида версии M и выше
     */
    @TargetApi(Build.VERSION_CODES.M)
    private class ActionModeWrapper23 extends ActionMode.Callback2 {

        private final ActionMode.Callback mOriginalCallback;

        public ActionModeWrapper23(ActionMode.Callback cb) {
            mOriginalCallback = cb;
        }

        /**
         * Хак чтобы заставить работать систему перехвата копирования
         */
        @Override
        public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
            return mOriginalCallback.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            boolean result = mOriginalCallback.onPrepareActionMode(mode, menu);

            final MenuItem copyButton = findByTitle(menu, "(?i)Копировать|Copy|Копіювати"); // ugly hack, no better way
            if(copyButton == null) {
                return result;
            }

            TypedValue drawable = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.quote_menu_drawable, drawable, true);

            menu.add(copyButton.getGroupId(), Menu.NONE, 0, R.string.quote) // будет в начале
                    .setIcon(drawable.resourceId)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT)
                    .setOnMenuItemClickListener(new ClipboardGrab(this, mode, copyButton));

            return result;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mOriginalCallback.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mOriginalCallback.onDestroyActionMode(mode);
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if(mOriginalCallback instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) mOriginalCallback).onGetContentRect(mode, view, outRect);
            } else {
                super.onGetContentRect(mode, view, outRect);
            }
        }
    }

    /**
     * Враппер для добавления кнопки "цитировать" - для моделей Андроида < 23
     */
    private class ActionModeWrapper implements ActionMode.Callback {

        private final ActionMode.Callback mOriginalCallback;

        public ActionModeWrapper(ActionMode.Callback cb) {
            mOriginalCallback = cb;
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

            menu.add(copyButton.getGroupId(), Menu.NONE, 0, R.string.quote) // будет в начале
                    .setIcon(drawable.resourceId)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT)
                    .setOnMenuItemClickListener(new ClipboardGrab(this, mode, copyButton));

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

    private class ClipboardGrab implements MenuItem.OnMenuItemClickListener {
        private ActionMode.Callback callback;
        private final ActionMode mode;
        private final MenuItem copyButton;

        public ClipboardGrab(ActionMode.Callback cb, ActionMode mode, MenuItem copyButton) {
            this.callback = cb;
            this.mode = mode;
            this.copyButton = copyButton;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            callback.onActionItemClicked(mode, copyButton); // программное нажатие на кнопку "скопировать"
            // хак чтобы текст в клипборде точно попал к нам
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (!cm.hasPrimaryClip())
                        return;

                    ClipData cd = cm.getPrimaryClip();
                    if (cd.getItemCount() > 0) {
                        String copied = cd.getItemAt(0).getText().toString();
                        String quoted = "<span class='quote_text'>" + copied + "</span>";
                        if (getContext() instanceof DiaryListActivity) { // цитируем
                            ((DiaryListActivity) getContext()).handleMessagePaneAddText(quoted);
                        }
                    }
                }
            }, 500);
            return true;
        }
    }
}
