package adonai.diary_browser.entities;

public class Openable
{
    private String pageTitle = "", pageUrl = "";
    private String pageAuthor = "", pageAuthorURL = "";
    private String lastPost = "", lastPostURL = "";
    private String authorDiaryID = "";
    private String pageHint = "";
    
    public Openable(String title, String url, String author, String author_url, String last_post, String last_post_url)
    {
        pageTitle = title;
        pageUrl = url;
        pageAuthor = author;
        pageAuthorURL = author_url;
        lastPost = last_post;
        lastPostURL = last_post_url;
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
    
    public void setLastPost(String last_post)
    {
        lastPost = last_post;
    }
    
    public String getLastPost()
    {
        return lastPost;
    }
    
    public void setLastPostURL(String last_post_url)
    {
        lastPostURL = last_post_url;
    }
    
    public String getLastPostURL()
    {
        return lastPostURL;
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