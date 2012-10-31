package adonai.diary_browser.entities;

import java.util.ArrayList;
import adonai.diary_browser.R;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class DiscussionListArrayAdapter extends BaseExpandableListAdapter
{
	ArrayList<DiscussionList> discussions;
	Context context;
    
    public DiscussionListArrayAdapter(Context ctx, ArrayList<DiscussionList> discussions)
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
        DiscussionList discussion = (DiscussionList) getGroup(groupPosition);
        if (convertView == null)
            view = View.inflate(context, R.layout.discussion_list_item, null);
        else
            view = convertView;
        
        TextView title = (TextView) view.findViewById(R.id.discussion_title);
        title.setText(discussion.get_title());
        TextView author = (TextView) view.findViewById(R.id.discussion_author);
        author.setText(discussion.get_author());
        
        return view;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) 
	{
		View view;
        DiscussionList.Discussion discussion = (DiscussionList.Discussion) getChild(groupPosition, childPosition);
        if (convertView == null)
            view = View.inflate(context, R.layout.discussion_item, null);
        else
            view = convertView;
        
        TextView title = (TextView) view.findViewById(R.id.discussion_post_title);
        title.setText(discussion.get_title());
        TextView author = (TextView) view.findViewById(R.id.discussion_post_date);
        author.setText(discussion.get_author());
        
        return view;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) 
	{
		return true;
	}
    
	
}