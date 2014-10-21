package adonai.diary_browser;

import android.app.Fragment;

public class DiaryFragment extends Fragment {
    int mCurrentComponent = 0;

    public DiaryActivity getDiaryActivity() {
        return (DiaryActivity) getActivity();
    }
}
