package adonai.diary_browser.entities;

import java.util.List;

import adonai.diary_browser.R;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DiaryListArrayAdapter extends ArrayAdapter<Openable>
{
    
    public DiaryListArrayAdapter(Context context, int textViewResourceId, List<Openable> objects)
    {
        super(context, textViewResourceId, objects);
    }
    
    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {
        View view;
        Openable diary = getItem(pos);
        if (convertView == null)
            view = View.inflate(getContext(), R.layout.diary_list_item, null);
        else
            view = convertView;
        
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(diary.getTitle());
        TextView author = (TextView) view.findViewById(R.id.author);
        author.setText(diary.getAuthor());
        TextView last_post = (TextView) view.findViewById(R.id.last_post);
        last_post.setText(diary.getLastPost());

        if(diary instanceof Umail)
            title.setTextColor(((Umail)diary).isRead() ? Color.BLACK : Color.RED);
        
        return view;
    }
}