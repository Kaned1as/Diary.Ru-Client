package adonai.diary_browser.entities;

import org.jsoup.nodes.Document;

public abstract class DiaryWebPage
{
    private Document _content;
    
    public Document get_content()
    {
        return _content;
    }

    public void set_content(Document resultPage)
    {
        this._content = resultPage;
    }

    public abstract String get_page_URL();
}
