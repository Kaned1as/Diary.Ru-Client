package adonai.diary_browser;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;

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
        inflater.inflate(R.menu.diary_list_a, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        UserData mUser = ((DiaryActivity)getActivity()).getUser();

        if(mUser == null)
            return;

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
        }
        else
        {
            menu.findItem(R.id.menu_share).setVisible(true);
            menu.findItem(R.id.menu_subscr_list).setVisible(false);
        }

        menu.removeGroup(GROUP_PAGE_LINKS);
        // добавляем ссылки дневника, если они есть
        if(mUser.currentDiaryPage instanceof DiaryPage)
            for(String linkName : ((DiaryPage) mUser.currentDiaryPage).userLinks.keySet())
                menu.add(GROUP_PAGE_LINKS, 0, 0, linkName);
    }
}
