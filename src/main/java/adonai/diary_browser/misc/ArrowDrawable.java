package adonai.diary_browser.misc;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.ViewCompat;

/**
 * Скопировано почти полностью с support lib 22
 */
public class ArrowDrawable extends DrawerArrowDrawable {

    private final Activity mActivity;

    public ArrowDrawable(Activity activity, Context themedContext) {
        super(themedContext);
        mActivity = activity;
    }

    public void setPosition(float position) {
        if (position == 1f) {
            setVerticalMirror(true);
        } else if (position == 0f) {
            setVerticalMirror(false);
        }
        super.setProgress(position);
    }

    @Override
    boolean isLayoutRtl() {
        return ViewCompat.getLayoutDirection(mActivity.getWindow().getDecorView())
                == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    public float getPosition() {
        return super.getProgress();
    }
}
