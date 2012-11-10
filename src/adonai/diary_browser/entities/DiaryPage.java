package adonai.diary_browser.entities;

import org.jsoup.select.Elements;

public class DiaryPage
{
    // дневник, из которого просматривается пост. Если Избранное - то свой дневник.
    private String _diary_URL = ""; 
    // идентификатор этого же дневника
    private String _diary_Id = "";
    
    private String _ID;
    private String _post_URL;
    private Elements _content;
    
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
        return _diary_Id;
    }
    
    public void set_diary_Id(String _diary_Id)
    {
        this._diary_Id = _diary_Id;
    }

    public Elements get_content()
    {
        return _content;
    }

    public void set_content(Elements _content)
    {
        this._content = _content;
    }

    public String get_ID()
    {
        return _ID;
    }

    public void set_ID(String _ID)
    {
        this._ID = _ID;
    }

    public String get_post_URL()
    {
        return _post_URL;
    }

    public void set_post_URL(String _post_URL)
    {
        this._post_URL = _post_URL;
    }
}
