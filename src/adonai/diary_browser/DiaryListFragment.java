package adonai.diary_browser;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SearchView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import adonai.diary_browser.database.DatabaseHandler;
import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryPage;

public class DiaryListFragment extends DiaryFragment
{
    public final static int GROUP_PAGE_LINKS = 100;
    private final URLAutocompleteAdapter mUrlAdapter = new URLAutocompleteAdapter(getActivity(), getDiaryActivity().mDatabase.getAutocompleteCursor(DatabaseHandler.AutocompleteType.URL, ""));
    private final URLAutocompleteQueryListener mUrlListener = new URLAutocompleteQueryListener();
    private final URLAutocompleteSuggestionListener mUrlSuggestionListener = new URLAutocompleteSuggestionListener();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_diary_list, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.diary_list_a, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        UserData mUser = ((DiaryActivity)getActivity()).getUser();

        if(mUser == null) // not initialized, hide all
        {
            for(int i = 0; i < menu.size(); ++i)
                menu.getItem(i).setVisible(false);
            return;
        }

        // Только если это дневник
        if(mCurrentComponent == DiaryListActivity.PART_WEB && mUser.currentDiaryPage.getClass().equals(DiaryPage.class))
        {
            menu.findItem(R.id.menu_tags).setVisible(true);
            menu.findItem(R.id.menu_new_post).setVisible(true);
        }
        else
        {
            menu.findItem(R.id.menu_tags).setVisible(false);
            menu.findItem(R.id.menu_new_post).setVisible(false);
        }

        // Только если это пост
        if(mCurrentComponent == DiaryListActivity.PART_WEB && mUser.currentDiaryPage.getClass().equals(CommentsPage.class))
            menu.findItem(R.id.menu_new_comment).setVisible(true);
        else
            menu.findItem(R.id.menu_new_comment).setVisible(false);


        // Для всех веб-страничек
        if(mCurrentComponent == DiaryListActivity.PART_WEB)
            menu.setGroupVisible(R.id.group_web, true);
        else
            menu.setGroupVisible(R.id.group_web, false);

        // Для всех списков
        if(mCurrentComponent == DiaryListActivity.PART_LIST)
        {
            menu.findItem(R.id.menu_share).setVisible(false);
            menu.findItem(R.id.menu_subscr_list).setVisible(true);
            menu.findItem(R.id.menu_manual_input).setVisible(true);

            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_manual_input));
            searchView.setBackgroundColor(Color.parseColor("#33ffff"));
            searchView.setSuggestionsAdapter(mUrlAdapter);
            searchView.setOnQueryTextListener(mUrlListener);
            searchView.setOnSuggestionListener(mUrlSuggestionListener);

            final ImageView v = (ImageView) searchView.findViewById(R.id.search_button);
            v.setImageResource(android.R.drawable.ic_menu_edit);
        }
        else
        {
            menu.findItem(R.id.menu_share).setVisible(true);
            menu.findItem(R.id.menu_subscr_list).setVisible(false);
            menu.findItem(R.id.menu_manual_input).setVisible(false);
        }

        menu.removeGroup(GROUP_PAGE_LINKS);
        // добавляем ссылки дневника, если они есть
        if(mUser.currentDiaryPage instanceof DiaryPage)
            for(String linkName : ((DiaryPage) mUser.currentDiaryPage).userLinks.keySet())
                menu.add(GROUP_PAGE_LINKS, 0, 0, linkName);
    }

    private class URLAutocompleteAdapter extends CursorAdapter {

        public URLAutocompleteAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return View.inflate(context, android.R.layout.simple_list_item_2, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView urlText = (TextView) view.findViewById(android.R.id.text1);
            final TextView caption = (TextView) view.findViewById(android.R.id.text2);

            urlText.setText(cursor.getString(0));
            if(!cursor.isNull(1))
                caption.setText(cursor.getString(1));
        }
    }

    private class URLAutocompleteQueryListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            getDiaryActivity().handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(query, false));
            getDiaryActivity().mDatabase.addAutocompleteText(DatabaseHandler.AutocompleteType.URL, query);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if(newText.length() > 0)
                mUrlAdapter.changeCursor(getDiaryActivity().mDatabase.getAutocompleteCursor(DatabaseHandler.AutocompleteType.URL, newText));
            return true;
        }
    }

    private class URLAutocompleteSuggestionListener implements SearchView.OnSuggestionListener {
        @Override
        public boolean onSuggestionSelect(int position) {
            return false;
        }

        @Override
        public boolean onSuggestionClick(int position) {
            final Cursor cur = (Cursor) mUrlAdapter.getItem(position);
            final String url = cur.getString(0);
            getDiaryActivity().handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(url, false));
            return true;
        }
    }
}
