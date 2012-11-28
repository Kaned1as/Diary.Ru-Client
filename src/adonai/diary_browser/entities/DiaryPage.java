package adonai.diary_browser.entities;

import org.jsoup.nodes.Document;

public class DiaryPage implements PageDescriber
{
    // текущий контекст
    public static final int DIARY_LIST = 0;
    public static final int POST_LIST = 1;
    public static final int COMMENT_LIST = 2;
    public static final int TAG_LIST = 5;
    public static final int AUTHOR_PAGE = 3;
    public static final int DISCUSSION_LIST = 4;
    public static final int PAGE_NOT_PROCESSED = -1;
    public static final int PAGE_NOT_RECOGNIZED = -2;
    
    // дневник, из которого просматривается пост. Если Избранное - то свой дневник.
    private String _diary_URL = ""; 
    // идентификатор этого же дневника
    private String _diary_ID = "";
    
    private String _post_ID = "";
    private String _post_URL = "";
    private Document _content;
    private int mType = PAGE_NOT_RECOGNIZED;
    
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

    public Document get_content()
    {
        return _content;
    }

    public void set_content(Document resultPage)
    {
        this._content = resultPage;
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

    public int getType()
    {
        return mType;
    }

    public void setType(int mType)
    {
        this.mType = mType;
    }
}
