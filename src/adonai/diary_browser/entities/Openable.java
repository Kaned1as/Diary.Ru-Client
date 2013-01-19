package adonai.diary_browser.entities;

public class Openable
{
    private String _title = "", _url = "";
    private String _author = "", _author_url = "";
    private String _last_post = "", _last_post_url = "";
    private String _author_diary_ID = "";
    private String _hint = "";
    
    public Openable(String title, String url, String author, String author_url, String last_post, String last_post_url)
    {
        _title = title;
        _url = url;
        _author = author;
        _author_url = author_url;
        _last_post = last_post;
        _last_post_url = last_post_url;
    }
    
    public Openable() 
    {
	}
    
    public void set_title(String title)
    {
        _title = title;
    }
    
    public String get_title()
    {
        return _title;
    }
    
    public void set_URL(String url)
    {
        _url = url;
    }
    
    public String get_URL()
    {
        return _url;
    }
    
    public void set_author(String author)
    {
        _author = author;
    }
    
    public String get_author()
    {
        return _author;
    }
    
    public void set_author_URL(String author_url)
    {
        _author_url = author_url;
    }
    
    public String get_author_URL()
    {
        return _author_url;
    }
    
    public void set_last_post(String last_post)
    {
        _last_post = last_post;
    }
    
    public String get_last_post()
    {
        return _last_post;
    }
    
    public void set_last_post_URL(String last_post_url)
    {
        _last_post_url = last_post_url;
    }
    
    public String get_last_post_URL()
    {
        return _last_post_url;
    }

	public String get_author_ID() 
	{
		return _author_diary_ID;
	}

	public void set_author_ID(String _ID) 
	{
		this._author_diary_ID = _ID;
	}

    public String get_hint()
    {
        return _hint;
    }

    public void set_hint(String _hint)
    {
        this._hint = _hint;
    }
}