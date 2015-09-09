package adonai.diary_browser.misc;

import android.app.Activity;
import android.content.Context;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;

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

    public float getPosition() {
        return super.getProgress();
    }
}
