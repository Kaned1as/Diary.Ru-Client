package adonai.diary_browser.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import adonai.diary_browser.R;
import adonai.diary_browser.pages.UmailListPage;
import com.afollestad.materialdialogs.MaterialDialog;

public class UmailListArrayAdapter extends ArrayAdapter<UmailListPage> {
    ArrayList<Long> checkedIds = new ArrayList<>();

    public UmailListArrayAdapter(Context context, int textViewResourceId, List<UmailListPage> objects) {
        super(context, textViewResourceId, objects);
    }

    public void addSelection(Long id) {
        checkedIds.add(id);
    }

    public void removeSelection(Long id) {
        checkedIds.remove(id);
    }

    public void clearSelections() {
        checkedIds.clear();
    }

    @Override
    public long getItemId(int position) {
        UmailListPage page = getItem(position);
        return  page.getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View view;
        final UmailListPage diary = getItem(pos);
        if (convertView == null)
            view = LayoutInflater.from(getContext()).inflate(R.layout.umail_list_item, parent, false);
        else
            view = convertView;

        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(diary.getTitle());
        TextView author = (TextView) view.findViewById(R.id.author);
        author.setText(diary.getAuthor());
        TextView lastPost = (TextView) view.findViewById(R.id.last_post);
        lastPost.setText(diary.getLastPost());
        ImageView preview = (ImageView) view.findViewById(R.id.preview_img);
        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(getContext())
                        .content(diary.getPageHint())
                        .show();
            }
        });

        if (!diary.isRead()) {
            title.setTypeface(null, Typeface.BOLD);
        } else {
            title.setTypeface(null, Typeface.NORMAL);
        }

        return view;
    }
}