package adonai.diary_browser;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

public class DiaryFragment extends Fragment
{
    int mCurrentComponent = 0;
    UserData mUser;

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        assert(getActivity() instanceof  DiaryActivity);
        mUser = ((DiaryActivity)getActivity()).mUser;
    }
}
