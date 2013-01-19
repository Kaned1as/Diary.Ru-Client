package adonai.diary_browser.entities;

public class Openable
{
    private String pageTitle = "", pageUrl = "";
    private String pageAuthor = "", pageAuthorURL = "";
    private String lastUpdate = "", lastUpdateURL = "";
    private String authorDiaryID = "";
    private String pageHint = "";
    
    public Openable(String title, String url, String author, String author_url, String last_post, String last_post_url)
    {
        pageTitle = title;
        pageUrl = url;
        pageAuthor = author;
        pageAuthorURL = author_url;
        lastUpdate = last_post;
        lastUpdateURL = last_post_url;
    }
    
    public Openable() 
    {
	}
    
    public void setTitle(String title)
    {
        pageTitle = title;
    }
    
    public String getTitle()
    {
        return pageTitle;
    }
    
    public void setURL(String url)
    {
        pageUrl = url;
    }
    
    public String getURL()
    {
        return pageUrl;
    }
    
    public void setAuthor(String author)
    {
        pageAuthor = author;
    }
    
    public String getAuthor()
    {
        return pageAuthor;
    }
    
    public void setAuthorURL(String author_url)
    {
        pageAuthorURL = author_url;
    }
    
    public String getAuthorURL()
    {
        return pageAuthorURL;
    }
    
    public void setLastUpdate(String last_update)
    {
        lastUpdate = last_update;
    }
    
    public String getLastPost()
    {
        return lastUpdate;
    }
    
    public void setLastUpdateURL(String last_update_url)
    {
        lastUpdateURL = last_update_url;
    }
    
    public String getLastPostURL()
    {
        return lastUpdateURL;
    }

	public String getAuthorID() 
	{
		return authorDiaryID;
	}

	public void setAuthorID(String _ID) 
	{
		this.authorDiaryID = _ID;
	}

    public String getPageHint()
    {
        return pageHint;
    }

    public void setPageHint(String _hint)
    {
        this.pageHint = _hint;
    }
}