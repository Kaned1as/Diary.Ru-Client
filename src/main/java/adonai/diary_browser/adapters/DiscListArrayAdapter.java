package adonai.diary_browser.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import adonai.diary_browser.R;
import adonai.diary_browser.pages.DiscListPage;
import adonai.diary_browser.pages.DiscPage;

public class DiscListArrayAdapter extends BaseExpandableListAdapter {
    DiscListPage discussions;
    Context context;

    public DiscListArrayAdapter(Context ctx, DiscListPage discussions) {
        this.discussions = discussions;
        this.context = ctx;
    }

    public int getGroupCount() {
        return discussions.size();
    }

    public int getChildrenCount(int groupPosition) {
        return discussions.get(groupPosition).getDiscussions().size();
    }

    public Object getGroup(int groupPosition) {
        return discussions.get(groupPosition);
    }

    public Object getChild(int groupPosition, int childPosition) {
        return discussions.get(groupPosition).getDiscussions().get(childPosition);
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 1000 + childPosition;
    }

    public boolean hasStableIds() {
        return false;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View view;
        DiscPage discussion = (DiscPage) getGroup(groupPosition);
        if (convertView == null)
            view = LayoutInflater.from(context).inflate(R.layout.discussion_item, null);
        else
            view = convertView;

        TextView title = (TextView) view.findViewById(R.id.discussion_title);
        title.setText(discussion.getTitle());
        TextView newPosts = (TextView) view.findViewById(R.id.discussion_last_post);
        newPosts.setText(discussion.getLastPost());
        if (!discussion.getLastPost().contains("/0"))
            newPosts.setTextColor(Color.RED);
        else
            newPosts.setTextColor(title.getCurrentTextColor());

        return view;
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View view;
        DiscPage.Discussion discussion = (DiscPage.Discussion) getChild(groupPosition, childPosition);
        if (convertView == null)
            view = LayoutInflater.from(context).inflate(R.layout.discussion_expanded_group_item, null);
        else
            view = convertView;

        TextView title = (TextView) view.findViewById(R.id.discussion_post_title);
        title.setText(discussion.title);
        TextView date = (TextView) view.findViewById(R.id.discussion_post_date);
        date.setText(discussion.date);

        return view;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


}