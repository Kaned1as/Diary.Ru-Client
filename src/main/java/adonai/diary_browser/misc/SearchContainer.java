package adonai.diary_browser.misc;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import adonai.diary_browser.DiaryWebView;
import adonai.diary_browser.R;

/**
 * Логика поиска для {@link DiaryWebView}, для удобства вынесено в один класс
 */
public class SearchContainer {

    private final LinearLayout mSearchBar;
    private final WebView mWebContent;
    
    private EditText mSearchEdit;
    private ImageView mCloseSearchBar;

    public SearchContainer(LinearLayout searchLayout, final WebView mWebContent) {
        this.mSearchBar = searchLayout;
        this.mWebContent = mWebContent;
        
        mSearchEdit = (EditText) searchLayout.findViewById(R.id.search_edit);
        mCloseSearchBar = (ImageView) searchLayout.findViewById(R.id.close_search_bar);
        mCloseSearchBar.setOnClickListener(new SearchBarCloseAction());
        mSearchEdit.addTextChangedListener(new RequestSearchAction());
        mSearchEdit.setOnEditorActionListener(new SearchFurtherAction(mWebContent));
    }

    public void setVisibility(int visibility) {
        mSearchBar.setVisibility(visibility);

        Context context = mSearchBar.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (visibility == View.VISIBLE) {
            mSearchEdit.requestFocus();
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        } else {
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        }
    }

    public int getVisibility() {
        return mSearchBar.getVisibility();
    }

    private static class SearchFurtherAction implements TextView.OnEditorActionListener {
        private final WebView mWebContent;

        public SearchFurtherAction(WebView mWebContent) {
            this.mWebContent = mWebContent;
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            switch (actionId) {
                case EditorInfo.IME_ACTION_NEXT:
                    mWebContent.findNext(true);
                    return true;
                case EditorInfo.IME_ACTION_PREVIOUS:
                    mWebContent.findNext(false);
                    return true;
            }
            return false;
        }
    }

    /**
     * Closes the search bar
     */
    private class SearchBarCloseAction implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Context context = mSearchBar.getContext();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            mSearchBar.setVisibility(View.GONE);
            mWebContent.clearMatches();
        }
    }

    /**
     * Находим значение по изменению строки
     */
    private class RequestSearchAction implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @SuppressWarnings("deprecation")
        @Override
        public void afterTextChanged(Editable s) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mWebContent.findAllAsync(s.toString()); // API 16
            } else {
                mWebContent.findAll(s.toString());
            }
        }
    }
}
