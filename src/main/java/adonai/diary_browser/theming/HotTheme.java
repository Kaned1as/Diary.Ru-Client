package adonai.diary_browser.theming;

import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import adonai.diary_browser.DiaryActivity;
import adonai.diary_browser.database.DatabaseHandler;

/**
 * Created by adonai on 11.10.14.
 */
public class HotTheme {

    public enum ThemeType {
        BACKGROUND_COLOR,
        UP_COLOR,
        DOWN_COLOR,
        TEXT_COLOR,
        HINT_COLOR,
    }

    public enum ViewType {
        LAYOUT,
        BUTTON,
        LISTVIEW,
        EDIT,
        TEXT,
        ACTIONBAR
    }

    private static List<ThemedView> sViews = new ArrayList<>(50);

    public static void manage(View... views) {
        for (View v : views) {
            simpleManage(v);
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    manage(vg.getChildAt(i));
                }
            }
        }
    }

    public static void simpleManage(View view) {
        Object t = view.getTag();
        if (t instanceof String) {
            String tag = (String) t;
            if (tag.startsWith("!")) {
                tag = tag.substring(1);
                String[] elements = tag.split("\\|");
                String base = elements[0];
                String[] types = elements[1].split(",");
                ThemedView tv = createThemedView(view, base, types);
                tv.notifyChange();
                HotTheme.sViews.add(tv);
            }
        }
    }

    public static void setTheme() {
        for (Iterator<ThemedView> it = sViews.iterator(); it.hasNext(); ) {
            if (!it.next().notifyChange()) {
                it.remove();
            }
        }
    }


    private static ThemedView createThemedView(View v, final String base, String[] types) {


        return new ThemedView(v) {
            @Override
            public void onChange(View v) {
                DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                final Cursor themeRow = mDatabase.getThemeRow(base);
                if(themeRow != null) {

                }
            }
        };
    }

    private static abstract class ThemedView {

        private WeakReference<View> view;

        ThemedView(View v) {
            view = new WeakReference<>(v);
        }

        boolean notifyChange() {
            View v = view.get();
            if (v == null) {
                return false;
            }
            onChange(v);
            return true;
        }

        abstract void onChange(View v);

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ThemedView view1 = (ThemedView) o;

            View v1 = view.get();
            View v2 = view1.view.get();

            return (v1 != null ? v1.equals(v2) : v2 == null);

        }

        @Override
        public int hashCode() {
            if (view == null) {
                return 0;
            }
            View v = view.get();
            return v != null ? v.hashCode() : 0;
        }
    }
}
