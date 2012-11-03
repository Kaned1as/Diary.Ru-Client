package adonai.diary_browser.entities;

import java.util.ArrayList;

public class Diaries
{
	private ArrayList<Diary> mDiaries;
	private String parentURL;
	
	public Diaries(ArrayList<Diary> posts, String URL)
	{
		mDiaries = posts;
		parentURL = URL;
	}
	
	public ArrayList<Diary> getDiaryArray()
	{
		return mDiaries;
	}

	public String getParentURL() 
	{
		return parentURL;
	}
	
}
