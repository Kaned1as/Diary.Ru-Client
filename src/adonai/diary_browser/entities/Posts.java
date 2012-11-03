package adonai.diary_browser.entities;

import java.util.ArrayList;

public class Posts
{
	private ArrayList<Post> mPosts;
	private String parentURL;
	
	public Posts(ArrayList<Post> posts, String URL)
	{
		mPosts = posts;
		parentURL = URL;
	}
	
	public ArrayList<Post> getPostArray()
	{
		return mPosts;
	}

	public String getParentURL() 
	{
		return parentURL;
	}
	
}
