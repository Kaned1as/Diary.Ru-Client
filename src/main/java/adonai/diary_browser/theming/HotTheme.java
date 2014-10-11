package adonai.diary_browser.theming;

import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import adonai.diary_browser.DiaryActivity;
import adonai.diary_browser.database.DatabaseHandler;

import static adonai.diary_browser.database.DatabaseHandler.ThemeFields;

/**
 * Created by adonai on 11.10.14.
 */
public class HotTheme {

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
                String type = elements[1];
                ThemedView tv = createThemedView(view, base, type);
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


    private static ThemedView createThemedView(View v, final String base, String type) {
        ViewType typeOfView = ViewType.valueOf(type);
        switch (typeOfView) {
            case LAYOUT:
            case LISTVIEW:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final Cursor themeRow = mDatabase.getThemeRow(base);
                        if(themeRow != null) {
                            int rowNum = ThemeFields.BACKGROUND_COLOR.ordinal();
                            if(!themeRow.isNull(rowNum)) {
                                v.setBackgroundColor(themeRow.getInt(rowNum));
                            }
                        }
                    }
                };
            case BUTTON:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final Cursor themeRow = mDatabase.getThemeRow(base);
                        if(themeRow != null) {
                            int downColorIndex = ThemeFields.DOWN_COLOR.ordinal();
                            int upColorIndex = ThemeFields.UP_COLOR.ordinal();
                            int textColorIndex = ThemeFields.TEXT_COLOR.ordinal();
                            if(!themeRow.isNull(downColorIndex) && !themeRow.isNull(upColorIndex) && !themeRow.isNull(textColorIndex)) {
                                StateListDrawable sld = new StateListDrawable();
                                sld.addState(new int[] {android.R.attr.state_pressed, android.R.attr.state_selected}, new ColorDrawable(themeRow.getInt(downColorIndex)));
                                sld.addState(new int[] {}, new ColorDrawable(themeRow.getInt(upColorIndex)));
                                ((Button) v).setTextColor(themeRow.getInt(textColorIndex));
                                v.setBackground(sld);
                            }
                        }
                    }
                };
            case EDIT:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final Cursor themeRow = mDatabase.getThemeRow(base);
                        if(themeRow != null) {
                            int rowNum = ThemeFields.BACKGROUND_COLOR.ordinal();
                            if(!themeRow.isNull(rowNum)) {
                                v.setBackgroundColor(themeRow.getInt(rowNum));
                            }
                            rowNum = ThemeFields.TEXT_COLOR.ordinal();
                            if(!themeRow.isNull(rowNum)) {
                                ((EditText) v).setTextColor(themeRow.getInt(rowNum));
                            }
                            rowNum = ThemeFields.HINT_COLOR.ordinal();
                            if(!themeRow.isNull(rowNum)) {
                                ((EditText) v).setHintTextColor(themeRow.getInt(rowNum));
                            }
                        }
                    }
                };
            case TEXT:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final Cursor themeRow = mDatabase.getThemeRow(base);
                        if(themeRow != null) {
                            int rowNum = ThemeFields.BACKGROUND_COLOR.ordinal();
                            if(!themeRow.isNull(rowNum)) {
                                v.setBackgroundColor(themeRow.getInt(rowNum));
                            }
                            rowNum = ThemeFields.TEXT_COLOR.ordinal();
                            if(!themeRow.isNull(rowNum)) {
                                ((EditText) v).setTextColor(themeRow.getInt(rowNum));
                            }
                        }
                    }
                };
            case ACTIONBAR:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final Cursor themeRow = mDatabase.getThemeRow(base);
                        if(themeRow != null) {
                            int rowNum = ThemeFields.BACKGROUND_COLOR.ordinal();
                            if(!themeRow.isNull(rowNum)) {
                                v.setBackgroundColor(themeRow.getInt(rowNum));
                            }
                        }
                    }
                };
            default:
                throw new RuntimeException("No such view type implemented!");
        }
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
