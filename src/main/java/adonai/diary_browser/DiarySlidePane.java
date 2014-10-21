package adonai.diary_browser;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DiarySlidePane extends SlidingPaneLayout {
    private boolean slideRestrict = true;

    public DiarySlidePane(Context context) {
        super(context);
    }

    public DiarySlidePane(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DiarySlidePane(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean closePane() {
        slideRestrict = true;
        return super.closePane();
    }

    @Override
    public boolean openPane() {
        slideRestrict = false;
        return super.openPane();
    }

    public boolean isDouble() {
        return !slideRestrict;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (slideRestrict)
            return false;
        else
            return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        // Хак для маленьких экранов! Делаем форму отправки сообщений всегда видной полностью
        LayoutParams childLP = (LayoutParams) getChildAt(0).getLayoutParams();
        if (changed && childLP.width > getWidth())
            childLP.width = LayoutParams.MATCH_PARENT;

        super.onLayout(changed, l, t, r, b);
    }
}
