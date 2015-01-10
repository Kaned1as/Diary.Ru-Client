package adonai.diary_browser.entities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import adonai.diary_browser.R;

public class DraftListArrayAdapter extends ArrayAdapter<Post> {

    public DraftListArrayAdapter(Context context, int resource, int textViewResourceId, List<Post> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View view;
        Post draft = getItem(pos);
        if (convertView == null)
            view = LayoutInflater.from(getContext()).inflate(R.layout.draft_list_item, null);
        else
            view = convertView;

        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(draft.title);
        TextView date = (TextView) view.findViewById(R.id.post_date);
        date.setText(draft.date);
        TextView content = (TextView) view.findViewById(R.id.message_content);
        content.setText(draft.content);

        return view;
    }
}
