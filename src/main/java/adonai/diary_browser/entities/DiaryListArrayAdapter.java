package adonai.diary_browser.entities;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import adonai.diary_browser.R;

public class DiaryListArrayAdapter extends ArrayAdapter<ListPage>
{
    ArrayList<Long> checkedIds = new ArrayList<Long>();

    public DiaryListArrayAdapter(Context context, int textViewResourceId, List<ListPage> objects)
    {
        super(context, textViewResourceId, objects);
    }

    public void addSelection(Long id)
    {
        checkedIds.add(id);
    }

    public void removeSelection(Long id)
    {
        checkedIds.remove(id);
    }

    public void clearSelections()
    {
        checkedIds.clear();
    }

    @Override
    public long getItemId(int position)
    {
        ListPage page = getItem(position);
        if(page instanceof UmailListPage)
            return ((UmailListPage) page).getId();
        else
            return position;
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {
        View view;
        ListPage diary = getItem(pos);
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

        if(checkedIds.contains(getItemId(pos)))
            view.setBackgroundColor(getContext().getResources().getColor(R.color.selected));
        else
            view.setBackgroundResource(R.drawable.item_background);

        if(diary instanceof UmailListPage)
            title.setTextColor(((UmailListPage)diary).isRead() ? Color.BLACK : Color.RED);
        
        return view;
    }
}