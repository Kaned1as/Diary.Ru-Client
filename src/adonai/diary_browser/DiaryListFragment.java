package adonai.diary_browser;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryPage;

public class DiaryListFragment extends DiaryFragment
{
    public static int GROUP_PAGE_LINKS = 100;

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
            final ImageView v = (ImageView) searchView.findViewById(R.id.search_button);
            v.setImageResource(android.R.drawable.ic_menu_edit);

            final SearchView.SearchAutoComplete text = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text);
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
}
