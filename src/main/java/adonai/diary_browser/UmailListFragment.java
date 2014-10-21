package adonai.diary_browser;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import adonai.diary_browser.theming.HotLayoutInflater;

public class UmailListFragment extends DiaryFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCurrentComponent = 1;
        this.setHasOptionsMenu(true);
        return HotLayoutInflater.wrap(inflater).inflate(R.layout.fragment_umail, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.umail_list_a, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        UserData mUser = ((DiaryActivity) getActivity()).getUser();
        // Только если это письмо из папки входящих
        if (mCurrentComponent == UmailListActivity.PART_WEB && mUser.currentUmails.getURL().equals(UmailListActivity.inFolderAddress)) // Если мы в папке "входящие"
        {
            menu.findItem(R.id.menu_reply_umail).setVisible(true);
            menu.findItem(R.id.menu_forward_umail).setVisible(true);
        } else {
            menu.findItem(R.id.menu_reply_umail).setVisible(false);
            menu.findItem(R.id.menu_forward_umail).setVisible(false);
        }

        super.onPrepareOptionsMenu(menu);
    }
}
