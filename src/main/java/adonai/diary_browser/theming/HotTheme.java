package adonai.diary_browser.theming;

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by adonai on 11.10.14.
 */
public class HotTheme {

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
                for (int i = elements.length - 1; i >= 1; i--) {
                    ThemedView tv = createThemedView(view, base, elements[i]);
                    tv.notifyChange();
                    HotTheme.sViews.add(tv);
                }
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


    private static ThemedView createThemedView(View v, final String base, String element) {
        ThemeType type = types.get(element);

        switch (type) {
            case TILED_BG:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        Bitmap bmp = decodeBitmap(base + "_bg");
                        BitmapDrawable bd = new BitmapDrawable(app().getResources(), bmp);
                        bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                        v.setBackgroundDrawable(bd);
                    }
                };
            case VIEW_COLOR_BG:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        int color = getColor(base + "_bg");
                        v.setBackgroundColor(color);
                        if (v instanceof ListView) {
                            // There is an android bug in setCacheColorHint
                            // That caused the IndexOutOfBoundsException
                            // look here:
                            // http://code.google.com/p/android/issues/detail?id=12840
                            //
                            // Moreover, that bug doesn't allow us to setDrawableCacheColor
                            // for recycled views. That's why we need to perform cleaning up
                            // via reflections
                            //
                            // Fixed in android 4.1.1_r1
                            try {
                                ((ListView) v).setCacheColorHint(color);
                            } catch (IndexOutOfBoundsException ex) {
                                try {
                                    Field mRecycler = AbsListView.class.getDeclaredField("mRecycler");
                                    mRecycler.setAccessible(true);
                                    Object recycler = mRecycler.get(v);
                                    Method m = recycler.getClass().getDeclaredMethod("clear");
                                    m.setAccessible(true);
                                    m.invoke(recycler);
                                } catch (Throwable t) {
                                    // No need to report this

                                }
                            }
                        }
                    }
                };
            case VIEW_IMAGE_BG:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        v.setBackgroundDrawable(decodeDrawable(base + "_bg"));
                    }
                };
            case IMAGE_FG:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        ((ImageView) v).setImageDrawable(decodeDrawable(base + "_bg"));
                    }
                };
            case TEXT_COLOR:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        final int color = getColor(base + "_fg");
                        if (v instanceof TextView) {
                            ((TextView) v).setTextColor(color);
                        }
                    }
                };
            case TEXT_HINT:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        final int color = getColor(base + "_hint_fg");
                        if (v instanceof TextView) {
                            ((TextView) v).setHintTextColor(color);
                        }
                    }
                };
            case DIVIDER:
                return new ThemedView(v) {
                    @Override
                    public void onChange(View v) {
                        int color = getColor(base + "_divider");
                        ListView lv = (ListView) v;
                        int h = lv.getDividerHeight();
                        lv.setDivider(new ColorDrawable(color));
                        lv.setDividerHeight(h);
                    }
                };
            case TABBUTTON_BG:
                return new ThemedView(v) {
                    @Override
                    void onChange(View v) {
                        StateListDrawable stateDrawable = new StateListDrawable();

                        Drawable selectedBd = decodeDrawable(base + "_selected");

                        stateDrawable.addState(new int[]{android.R.attr.state_selected}, selectedBd);
                        stateDrawable.addState(new int[]{android.R.attr.state_pressed}, selectedBd);
                        stateDrawable.addState(new int[]{}, decodeDrawable(base + "_unselected"));
                        v.setBackgroundDrawable(stateDrawable);
                    }
                };
            case EDITTEXT_COLOR:
                return new ThemedView(v) {
                    @Override
                    void onChange(View v) {
                        int color = getColor(base + "_fg");
                        EditText edit = (EditText) v;
                        edit.setTextColor(color);
                        int hintColor = getColor(base + "_disabled_fg");
                        edit.setHintTextColor(hintColor);
                    }
                };
            case GROUP_TINT:
                return new ThemedView(v) {
                    @Override
                    void onChange(View v) {
                        int tintColor = getColor(base + "_fg");
                        ImageView imageView = (ImageView) v;
                        imageView.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
                    }
                };
            default:
                throw new IllegalArgumentException("Error in layout: no such type \"" + element + "\" (" + base + ")");
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
