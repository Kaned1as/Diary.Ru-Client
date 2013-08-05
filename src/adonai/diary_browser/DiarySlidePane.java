package adonai.diary_browser;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DiarySlidePane extends SlidingPaneLayout
{
    private boolean slideRestrict = true;

    public DiarySlidePane(Context context)
    {
        super(context);
    }

    public DiarySlidePane(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public DiarySlidePane(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public boolean isSlideRestrict()
    {
        return slideRestrict;
    }

    @Override
    public boolean closePane()
    {
        slideRestrict = true;
        return super.closePane();
    }

    @Override
    public boolean openPane()
    {
        slideRestrict = false;
        return super.openPane();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        if(slideRestrict)
            return false;
        else
            return super.onInterceptTouchEvent(ev);
    }
}
