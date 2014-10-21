package adonai.diary_browser.theming;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by adonai on 11.10.14.
 */
public class HotLayoutInflater {

    private LayoutInflater inflater;

    private HotLayoutInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public static HotLayoutInflater wrap(LayoutInflater layoutInflater) {
        return new HotLayoutInflater(layoutInflater);
    }

    public static HotLayoutInflater from(Context context) {
        return new HotLayoutInflater(LayoutInflater.from(context));
    }

    public View inflate(int resource, ViewGroup root, boolean attachToRoot) {
        View v = inflater.inflate(resource, root, attachToRoot);
        HotTheme.manage(v);
        return v;
    }

    public View inflate(int resource, ViewGroup root) {
        View v = inflater.inflate(resource, root);
        HotTheme.manage(v);
        return v;
    }

}