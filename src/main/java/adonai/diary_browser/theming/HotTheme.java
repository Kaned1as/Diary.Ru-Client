package adonai.diary_browser.theming;

import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import adonai.diary_browser.DiaryActivity;
import adonai.diary_browser.database.DatabaseHandler;

/**
 * Created by adonai on 11.10.14.
 */
public class HotTheme {

    private static Set<ThemedView> sViews = new HashSet<>(50);

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

    public static void updateTheme() {
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
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final HashMap<DatabaseHandler.ThemeField, Object> themeRow = mDatabase.getThemeRow(base);
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.BACKGROUND_COLOR)) {
                            v.setBackgroundColor((Integer) themeRow.get(DatabaseHandler.ThemeField.BACKGROUND_COLOR));
                        }
                    }
                };
            case LISTVIEW:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final HashMap<DatabaseHandler.ThemeField, Object> themeRow = mDatabase.getThemeRow(base);
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.BACKGROUND_COLOR)) {
                            v.setBackgroundColor((Integer) themeRow.get(DatabaseHandler.ThemeField.BACKGROUND_COLOR));
                        }
                    }
                };
            case LISTITEM:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final HashMap<DatabaseHandler.ThemeField, Object> themeRow = mDatabase.getThemeRow(base);
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.BACKGROUND_COLOR)) {
                            RoundRectShape rect = new RoundRectShape(new float[]{10, 10, 10, 10, 10, 10, 10, 10}, null, null);
                            ShapeDrawable bg = new ShapeDrawable(rect);
                            bg.getPaint().setColor((Integer) themeRow.get(DatabaseHandler.ThemeField.BACKGROUND_COLOR));
                            v.setBackgroundDrawable(bg);
                        }
                    }
                };
            case BUTTON:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final HashMap<DatabaseHandler.ThemeField, Object> themeRow = mDatabase.getThemeRow(base);
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.DOWN_COLOR) && themeRow.containsKey(DatabaseHandler.ThemeField.UP_COLOR) && themeRow.containsKey(DatabaseHandler.ThemeField.TEXT_COLOR)) {
                            StateListDrawable sld = new StateListDrawable();
                            sld.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable((Integer) themeRow.get(DatabaseHandler.ThemeField.DOWN_COLOR)));
                            sld.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable((Integer) themeRow.get(DatabaseHandler.ThemeField.DOWN_COLOR)));
                            sld.addState(new int[]{}, new ColorDrawable((Integer) themeRow.get(DatabaseHandler.ThemeField.UP_COLOR)));
                            if (v instanceof Button) {
                                ((Button) v).setTextColor((Integer) themeRow.get(DatabaseHandler.ThemeField.TEXT_COLOR));
                            }
                            v.setBackgroundDrawable(sld);
                        }
                    }
                };
            case EDIT:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final HashMap<DatabaseHandler.ThemeField, Object> themeRow = mDatabase.getThemeRow(base);
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.BACKGROUND_COLOR)) {
                            v.getBackground().setColorFilter((Integer) themeRow.get(DatabaseHandler.ThemeField.BACKGROUND_COLOR), PorterDuff.Mode.MULTIPLY);
                        }
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.TEXT_COLOR)) {
                            ((EditText) v).setTextColor((Integer) themeRow.get(DatabaseHandler.ThemeField.TEXT_COLOR));
                        }
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.HINT_COLOR)) {
                            ((EditText) v).setHintTextColor((Integer) themeRow.get(DatabaseHandler.ThemeField.HINT_COLOR));
                        }
                    }
                };
            case TEXT:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final HashMap<DatabaseHandler.ThemeField, Object> themeRow = mDatabase.getThemeRow(base);
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.BACKGROUND_COLOR)) {
                            v.setBackgroundColor((Integer) themeRow.get(DatabaseHandler.ThemeField.BACKGROUND_COLOR));
                        }
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.TEXT_COLOR)) {
                            ((TextView) v).setTextColor((Integer) themeRow.get(DatabaseHandler.ThemeField.TEXT_COLOR));
                        }
                    }
                };
            case ACTIONBAR:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final HashMap<DatabaseHandler.ThemeField, Object> themeRow = mDatabase.getThemeRow(base);
                        if (themeRow.containsKey(DatabaseHandler.ThemeField.BACKGROUND_COLOR)) {
                            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{(int) themeRow.get(DatabaseHandler.ThemeField.BACKGROUND_COLOR), 0xFFA0A0A0 & (Integer) themeRow.get(DatabaseHandler.ThemeField.BACKGROUND_COLOR)});
                            v.setBackgroundDrawable(gd);
                            if(themeRow.containsKey(DatabaseHandler.ThemeField.TEXT_COLOR)) {
                                traverse(themeRow, v);
                            }
                        }
                    }

                    private void traverse(HashMap<DatabaseHandler.ThemeField, Object> themeRow, View... views) {
                        for (View v : views) {
                            if (v instanceof ViewGroup) {
                                ViewGroup vg = (ViewGroup) v;
                                for (int i = 0; i < vg.getChildCount(); i++) {
                                    traverse(themeRow, vg.getChildAt(i));
                                }
                            }

                            if (v instanceof TextView) {
                                ((TextView) v).setTextColor((Integer) themeRow.get(DatabaseHandler.ThemeField.TEXT_COLOR));
                                if(v.getBackground() != null)
                                    v.setBackground(null);
                            }
                        }
                    }
                };
            case CHECKBOX:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        DatabaseHandler mDatabase = ((DiaryActivity) v.getContext()).getDatabase();
                        final HashMap<DatabaseHandler.ThemeField, Object> themeRow = mDatabase.getThemeRow(base);

                        try {
                            Field f = v.getClass().getSuperclass().getDeclaredField("mButtonDrawable");
                            f.setAccessible(true);
                            Drawable drawable = (Drawable) f.get(v);
                            if (drawable != null && themeRow.containsKey(DatabaseHandler.ThemeField.BACKGROUND_COLOR)) {
                                drawable.setColorFilter((Integer) themeRow.get(DatabaseHandler.ThemeField.BACKGROUND_COLOR), PorterDuff.Mode.MULTIPLY);
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                };
            default:
                throw new RuntimeException("No such view type implemented!");
        }
    }


    public enum ViewType {
        LAYOUT,
        BUTTON,
        LISTVIEW,
        LISTITEM,
        CHECKBOX,
        EDIT,
        TEXT,
        ACTIONBAR
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
