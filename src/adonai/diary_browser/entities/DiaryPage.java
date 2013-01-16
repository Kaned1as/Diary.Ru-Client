package adonai.diary_browser.entities;

import org.jsoup.nodes.Document;

public abstract class DiaryPage
{
    // текущий контекст
    public static final int PAGE_LIST = 0;
    public static final int POST_LIST = 1;
    public static final int COMMENT_LIST = 2;
    public static final int AUTHOR_PAGE = 3;
    public static final int DISCUSSION_LIST = 4;
    public static final int TAG_LIST = 5;
    public static final int PAGE_NOT_PROCESSED = -1;
    public static final int PAGE_NOT_RECOGNIZED = -2;
    
    private Document _content;
    private int mType = PAGE_NOT_RECOGNIZED;
    
    public Document get_content()
    {
        return _content;
    }

    public void set_content(Document resultPage)
    {
        this._content = resultPage;
    }

    protected void set_type(int type)
    {
        this.mType = type;
    }
    
    public int get_type()
    {
        return mType;
    }
    
    public abstract String get_page_URL();
}
