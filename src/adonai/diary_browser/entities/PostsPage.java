package adonai.diary_browser.entities;

public class PostsPage extends DiaryWebPage
{
    // дневник, из которого просматривается пост. Если Избранное - то свой дневник.
    private String _diary_URL = ""; 
    // идентификатор этого же дневника
    private String _diary_ID = "";
    
    public String get_diary_URL()
    {
        return _diary_URL;
    }
    
    public void set_diary_URL(String _diary_URL)
    {
        this._diary_URL = _diary_URL;
    }
    
    public String get_diary_Id()
    {
        return _diary_ID;
    }
    
    public void set_diary_Id(String _diary_Id)
    {
        this._diary_ID = _diary_Id;
    }

    public String get_page_URL()
    {
    	return _diary_URL;
    }
}
