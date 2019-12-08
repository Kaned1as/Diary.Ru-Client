package adonai.diary_browser;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;

import adonai.diary_browser.database.DbProvider;
import adonai.diary_browser.entities.AutocompleteItem;
import adonai.diary_browser.entities.AutocompleteItem.AutocompleteType;
import adonai.diary_browser.pages.CommentsPage;
import adonai.diary_browser.pages.DiaryPage;

/**
 * Фрагмент основной активности дайри, отвечающий за обработку пунктов меню и кнопок {@link ActionBar}'a
 * <br/>
 * По историческим причинам логика всего контента, который присутствует в этом фрагменте, обрабатывается в активности.
 *
 * @see DiaryListActivity
 *
 * @author Адонай
 */
public class DiaryListFragment extends DiaryFragment {

    public final static int GROUP_PAGE_LINKS = 100;
    public final static int ITEM_PAGE_LINKS = 101;

    private final URLAutocompleteQueryListener mUrlListener = new URLAutocompleteQueryListener();
    private final URLAutocompleteSuggestionListener mUrlSuggestionListener = new URLAutocompleteSuggestionListener();
    private URLAutocompleteAdapter mUrlAdapter; // created on attach to activity

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mUrlAdapter = new URLAutocompleteAdapter(getActivity(), getPersistedUrlCompletions(""));


        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_diary_list, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.diary_list_a, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        UserData mUser = ((DiaryActivity) getActivity()).getUser();

        if (mUser == null) // not initialized, hide all
        {
            for (int i = 0; i < menu.size(); ++i)
                menu.getItem(i).setVisible(false);
            return;
        }

        // Только если это дневник
        if (mCurrentComponent == DiaryListActivity.PART_WEB && mUser.getCurrentDiaryPage().getClass().equals(DiaryPage.class)) {
            menu.findItem(R.id.menu_new_post).setVisible(true);
        } else {
            menu.findItem(R.id.menu_new_post).setVisible(false);
        }

        // Только если это пост
        if (mCurrentComponent == DiaryListActivity.PART_WEB && mUser.getCurrentDiaryPage().getClass().equals(CommentsPage.class))
            menu.findItem(R.id.menu_new_comment).setVisible(true);
        else
            menu.findItem(R.id.menu_new_comment).setVisible(false);


        // Для всех веб-страничек
        if (mCurrentComponent == DiaryListActivity.PART_WEB)
            menu.setGroupVisible(R.id.group_web, true);
        else
            menu.setGroupVisible(R.id.group_web, false);

        // Для всех списков
        if (mCurrentComponent == DiaryListActivity.PART_LIST) {
            menu.findItem(R.id.menu_share).setVisible(false);
            menu.findItem(R.id.menu_subscr_list).setVisible(true);
            menu.findItem(R.id.menu_manual_input).setVisible(true);

            SearchView searchView = (SearchView) menu.findItem(R.id.menu_manual_input).getActionView();
            searchView.setQueryHint(getString(R.string.url_hint));
            searchView.setSuggestionsAdapter(mUrlAdapter);
            searchView.setOnQueryTextListener(mUrlListener);
            searchView.setOnSuggestionListener(mUrlSuggestionListener);

            final ImageView v = (ImageView) searchView.findViewById(R.id.search_button);
            v.setImageResource(android.R.drawable.ic_menu_edit);

            /*
            //final EditText text = (EditText) searchView.findViewById(Resources.getSystem().getIdentifier("search_src_text", "id", "android"));
            final EditText text = (EditText) searchView.findViewById(R.id.search_src_text);
            text.setCursorVisible(false);
            text.setHintTextColor(Color.LTGRAY);
            text.setTextColor(Color.WHITE);
            */
        } else {
            menu.findItem(R.id.menu_share).setVisible(true);
            menu.findItem(R.id.menu_subscr_list).setVisible(false);
            menu.findItem(R.id.menu_manual_input).setVisible(false);
        }

        menu.removeGroup(GROUP_PAGE_LINKS);
        // добавляем ссылки дневника, если они есть
        if (mUser.getCurrentDiaryPage() instanceof DiaryPage) {
            final SubMenu linksMenu = menu.addSubMenu(GROUP_PAGE_LINKS, 0, 0, R.string.diary_links);
            for (String linkName : ((DiaryPage) mUser.getCurrentDiaryPage()).userLinks.keySet())
                linksMenu.add(ITEM_PAGE_LINKS, 0, 0, linkName);
        }
    }

    private String convertToSearchQuery(String somethingLikeURL) {
        try {
            if (!somethingLikeURL.startsWith("http")) {
                if (somethingLikeURL.contains("x.diary.ru"))
                    somethingLikeURL = "https://" + somethingLikeURL;
                else
                    somethingLikeURL = "https://x.diary.ru/search/?q=" + URLEncoder.encode(somethingLikeURL, "windows-1251");
            }
        } catch (UnsupportedEncodingException ignored) {
        }
        return somethingLikeURL;
    }

    private class URLAutocompleteAdapter extends CursorAdapter {

        public URLAutocompleteAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.panel_background, typedValue, true);
            view.setBackgroundColor(typedValue.data);
            TextView urlText = (TextView) view.findViewById(android.R.id.text1);
            TextView caption = (TextView) view.findViewById(android.R.id.text2);

            RuntimeExceptionDao<AutocompleteItem, Long> acDao = DbProvider.getHelper().getAutocompleteDao();
            AndroidDatabaseResults res = new AndroidDatabaseResults(cursor, acDao.getObjectCache());
            AutocompleteItem item = acDao.mapSelectStarRow(res);
            urlText.setText(item.getText());
            if (item.getTitle() != null)
                caption.setText(item.getTitle());
        }
    }

    private class URLAutocompleteQueryListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            getDiaryActivity().handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(convertToSearchQuery(query), false));
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (newText.length() > 0)
                mUrlAdapter.changeCursor(getPersistedUrlCompletions(newText));
            return true;
        }
    }

    @NonNull
    private Cursor getPersistedUrlCompletions(@NonNull String containingText) {
        try {
            RuntimeExceptionDao<AutocompleteItem, Long> dao = DbProvider.getHelper().getAutocompleteDao();
            CloseableIterator<AutocompleteItem> iterator = dao.queryBuilder()
                    .where()
                    .eq("type", AutocompleteType.URL)
                    .and()
                    .like("text", '%' + containingText + '%')
                    .iterator();
            AndroidDatabaseResults narrowed = (AndroidDatabaseResults) iterator.getRawResults();
            return narrowed.getRawCursor();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private class URLAutocompleteSuggestionListener implements SearchView.OnSuggestionListener {
        @Override
        public boolean onSuggestionSelect(int position) {
            return false;
        }

        @Override
        public boolean onSuggestionClick(int position) {
            try {
                final Cursor cur = (Cursor) mUrlAdapter.getItem(position);
                RuntimeExceptionDao<AutocompleteItem, Long> dao = DbProvider.getHelper().getAutocompleteDao();
                AutocompleteItem ai = dao.getSelectStarRowMapper().mapRow(new AndroidDatabaseResults(cur, dao.getObjectCache()));
                getDiaryActivity().handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(ai.getText(), false));
                return true;
            } catch (SQLException e) {
                Log.e("DB", "Exception mapping row " + position, e);
                return false;
            }
        }
    }
}
