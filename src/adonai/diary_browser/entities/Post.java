package adonai.diary_browser.entities;

import android.graphics.drawable.Drawable;
import android.text.Spanned;

public class Post
{
    public Post()
    {
        
    }
    
    private String _author = "";
    private String _author_URL = "";
    private String _community = "";
    private String _community_URL = "";
    private String _URL = "";
    private Spanned _text = null;
    private String _date;
    private String _title = "";
    private String _comment_count = "";
    private Drawable _author_avatar = null;
    
    /**
     * @return the _author
     */
    public String get_author()
    {
        return _author;
    }
    
    /**
     * @param _author
     *            the _author to set
     */
    public void set_author(String _author)
    {
        this._author = _author;
    }
    
    /**
     * @return the _URL
     */
    public String get_URL()
    {
        return _URL;
    }
    
    /**
     * @param _URL
     *            the _URL to set
     */
    public void set_URL(String _URL)
    {
        this._URL = _URL;
    }
    
    /**
     * @return the _text
     */
    public Spanned get_text()
    {
        return _text;
    }
    
    /**
     * @param spanned
     *            the _text to set
     */
    public void set_text(Spanned spanned)
    {
        this._text = spanned;
    }
    
    /**
     * @return the _date
     */
    public String get_date()
    {
        return _date;
    }
    
    /**
     * @param _date
     *            the _date to set
     */
    public void set_date(String _date)
    {
        this._date = _date;
    }
    
    /**
     * @return the _author_avatar
     */
    public Drawable get_author_avatar()
    {
        return _author_avatar;
    }
    
    /**
     * @param _author_avatar
     *            the _author_avatar to set
     */
    public void set_author_avatar(Drawable _author_avatar)
    {
        this._author_avatar = _author_avatar;
    }
    
    public String get_title()
    {
        return _title;
    }
    
    public void set_title(String _title)
    {
        this._title = _title;
    }

    public String get_author_URL()
    {
        return _author_URL;
    }

    public void set_author_URL(String _author_URL)
    {
        this._author_URL = _author_URL;
    }

    /**
     * @return the _comment_count
     */
    final public String get_comment_count()
    {
        return _comment_count;
    }

    /**
     * @param _comment_count the _comment_count to set
     */
    final public void set_comment_count(String _comment_count)
    {
        this._comment_count = _comment_count;
    }

	public String get_community() 
	{
		return _community;
	}

	public void set_community(String _community) 
	{
		this._community = _community;
	}

	final public String get_community_URL() 
	{
		return _community_URL;
	}

	final public void set_community_URL(String _community_URL) 
	{
		this._community_URL = _community_URL;
	}
}