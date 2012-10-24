package adonai.diary_browser.entities;

import java.util.ArrayList;


public class Diary
{
    private String _title = "", _url = "";
    private String _author = "", _author_url = "";
    private String _last_post = "", _last_post_url = "";
    private String _ID;
    private ArrayList<Post> _posts = new ArrayList<Post>();
    
    public Diary(String title, String url, String author, String author_url, String last_post, String last_post_url)
    {
        _title = title;
        _url = url;
        _author = author;
        _author_url = author_url;
        _last_post = last_post;
        _last_post_url = last_post_url;
    }
    
    void setPosts(ArrayList<Post> posts)
    {
        _posts = posts;
    }
    
    ArrayList<Post> getPosts()
    {
        return _posts;
    }
    
    void setTitle(String title)
    {
        _title = title;
    }
    
    public String getTitle()
    {
        return _title;
    }
    
    void setDiaryUrl(String url)
    {
        _url = url;
    }
    
    public String getDiaryUrl()
    {
        return _url;
    }
    
    void setAuthor(String author)
    {
        _author = author;
    }
    
    public String getAuthor()
    {
        return _author;
    }
    
    void setAuthorUrl(String author_url)
    {
        _author_url = author_url;
    }
    
    String getAuthorUrl()
    {
        return _author_url;
    }
    
    void setLastPost(String last_post)
    {
        _last_post = last_post;
    }
    
    public String getLastPost()
    {
        return _last_post;
    }
    
    void setLastPostUrl(String last_post_url)
    {
        _last_post_url = last_post_url;
    }
    
    String getLastPostUrl()
    {
        return _last_post_url;
    }

	public String get_ID() 
	{
		return _ID;
	}

	public void set_ID(String _ID) 
	{
		this._ID = _ID;
	}
}