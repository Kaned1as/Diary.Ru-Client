package adonai.diary_browser.entities;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class UmailListArrayAdapter extends ArrayAdapter<Diary>
{
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// TODO Auto-generated method stub
		return super.getView(position, convertView, parent);
	}

	public UmailListArrayAdapter(Context context, int textViewResourceId, List<Diary> objects)
	{
		super(context, textViewResourceId, objects);
	}

	
}
