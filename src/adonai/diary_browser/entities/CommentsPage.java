package adonai.diary_browser.entities;

public class CommentsPage extends PostsPage
{
    private String _post_ID = "";
    private String _post_URL = "";
    
    public CommentsPage()
    {
        set_type(COMMENT_LIST);
    }
    
    public String get_post_ID()
    {
        return _post_ID;
    }

    public void set_post_ID(String _ID)
    {
        this._post_ID = _ID;
    }

    public String get_post_URL()
    {
        return _post_URL;
    }

    public void set_post_URL(String _post_URL)
    {
        this._post_URL = _post_URL;
    }
    
    @Override
    public String get_page_URL()
    {
        return _post_URL;
    }
}
