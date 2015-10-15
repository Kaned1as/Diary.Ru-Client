package adonai.diary_browser;

import android.support.v4.app.Fragment;

public abstract class DiaryFragment extends Fragment {
    int mCurrentComponent = 0;

    public DiaryActivity getDiaryActivity() {
        return (DiaryActivity) getActivity();
    }
}
