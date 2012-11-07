package adonai.diary_browser.entities;

import java.util.Vector;

public class DiaryPage extends Vector<Post>
{
    private static final long serialVersionUID = 4399107957675394115L;
	
    // дневник, из которого просматривается пост. Если Избранное - то свой дневник.
    private String _diary_URL = ""; 
    // идентификатор этого же дневника
    private String _diary_Id = "";
    
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
    
    public String get_ID()
    {
        if(!isEmpty())
            return get(0).get_ID();
        
        return "";
    }
    
    public String get_URL()
    {
        if(!isEmpty())
            return get(0).get_URL();
        
        return "";
    }
}
