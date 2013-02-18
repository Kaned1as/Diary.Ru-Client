package adonai.diary_browser.entities;

import java.util.ArrayList;
import adonai.diary_browser.R;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class DiscListArrayAdapter extends BaseExpandableListAdapter
{
	ArrayList<DiscList> discussions;
	Context context;
    
    public DiscListArrayAdapter(Context ctx, ArrayList<DiscList> discussions)
    {
    	this.discussions = discussions;
    	this.context = ctx;
    }

	public int getGroupCount() 
	{
		return discussions.size();
	}

	public int getChildrenCount(int groupPosition) 
	{
		return discussions.get(groupPosition).getDiscussions().size();
	}

	public Object getGroup(int groupPosition) 
	{
		return discussions.get(groupPosition);
	}

	public Object getChild(int groupPosition, int childPosition) 
	{
		return discussions.get(groupPosition).getDiscussions().get(childPosition);
	}

	public long getGroupId(int groupPosition) 
	{
		return groupPosition;
	}

	public long getChildId(int groupPosition, int childPosition) 
	{
		return groupPosition * 1000 + childPosition;
	}

	public boolean hasStableIds() 
	{
		// TODO Auto-generated method stub
		return false;
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) 
	{
		View view;
        DiscList discussion = (DiscList) getGroup(groupPosition);
        if (convertView == null)
            view = View.inflate(context, R.layout.discussion_list_item, null);
        else
            view = convertView;
        
        TextView title = (TextView) view.findViewById(R.id.discussion_title);
        title.setText(discussion.getTitle());
        TextView newPosts = (TextView) view.findViewById(R.id.discussion_last_post);
        newPosts.setText(discussion.getLastPost());
        if(!discussion.getLastPost().contains("/0"))
        	newPosts.setTextColor(Color.RED);
        else
            newPosts.setTextColor(Color.BLACK);
        
        return view;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) 
	{
		View view;
        DiscList.Discussion discussion = (DiscList.Discussion) getChild(groupPosition, childPosition);
        if (convertView == null)
            view = View.inflate(context, R.layout.discussion_item, null);
        else
            view = convertView;
        
        TextView title = (TextView) view.findViewById(R.id.discussion_post_title);
        title.setText(discussion.title);
        TextView date = (TextView) view.findViewById(R.id.discussion_post_date);
        date.setText(discussion.date);
        
        return view;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) 
	{
		return true;
	}
    
	
}