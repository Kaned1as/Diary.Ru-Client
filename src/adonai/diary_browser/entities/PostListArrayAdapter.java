package adonai.diary_browser.entities;

import java.util.List;

import adonai.diary_browser.R;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PostListArrayAdapter extends ArrayAdapter<Post>
{

	public PostListArrayAdapter(Context context, int textViewResourceId, List<Post> objects)
    {
        super(context, textViewResourceId, objects);
    }
    
    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {	
        View view;
        if (convertView == null)
            view = View.inflate(getContext(), R.layout.post_list_item, null);
        else
            view = convertView;
    	
        if(pos >= getCount())
        	return view;
        
        Post post = getItem(pos);
        TextView community = (TextView) view.findViewById(R.id.post_community);
        community.setText(post.get_community());
        TextView author = (TextView) view.findViewById(R.id.post_author);
        author.setText(post.get_author());
        author.setOnClickListener((OnClickListener) getContext());
        TextView post_date = (TextView) view.findViewById(R.id.post_date);
        post_date.setText(post.get_date());
        TextView post_content = (TextView) view.findViewById(R.id.post_content);
        post_content.setText(post.get_text());
        post_content.setMovementMethod(LinkMovementMethod.getInstance());
        
        TextView title = (TextView) view.findViewById(R.id.post_title);
        TextView comment_count = (TextView) view.findViewById(R.id.comments_number);
        if(!post.isEpigraph())
        {
        	comment_count.setVisibility(View.VISIBLE);
            comment_count.setText(getContext().getString(R.string.comments) + " " + post.get_comment_count());
            comment_count.setOnClickListener((OnClickListener) getContext());
            
            title.setVisibility(View.VISIBLE);
            title.setText(post.get_title());
            title.setOnClickListener((OnClickListener) getContext());
        }
        else
        {
        	comment_count.setVisibility(View.GONE);
        	title.setVisibility(View.GONE);
        }
        	
                    
        return view;
    }
    
}