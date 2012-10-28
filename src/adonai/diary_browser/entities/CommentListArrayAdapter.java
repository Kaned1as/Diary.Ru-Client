package adonai.diary_browser.entities;

import java.util.List;

import adonai.diary_browser.R;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CommentListArrayAdapter extends ArrayAdapter<Post>
{
    
    public CommentListArrayAdapter(Context context, int textViewResourceId, List<Post> objects)
    {
        super(context, textViewResourceId, objects);
    }
    
    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {
        View view;
        Post post = getItem(pos);
        if (convertView == null)
            view = View.inflate(getContext(), R.layout.post_list_item, null);
        else
            view = convertView;
        
        /* ImageButton delete = (ImageButton)view.findViewById(R.id.p_delete); */
        TextView title = (TextView) view.findViewById(R.id.post_title);
        title.setText(post.get_title());
        TextView author = (TextView) view.findViewById(R.id.post_author);
        author.setText(post.get_author());
        TextView post_date = (TextView) view.findViewById(R.id.post_date);
        post_date.setText(post.get_date());
        TextView post_content = (TextView) view.findViewById(R.id.post_content);
        post_content.setText(post.get_text());
        
        post_content.setMovementMethod(LinkMovementMethod.getInstance());
        
        return view;
    }
    
}