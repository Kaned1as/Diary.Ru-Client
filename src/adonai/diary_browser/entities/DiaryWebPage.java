package adonai.diary_browser.entities;

import org.jsoup.nodes.Document;

public abstract class DiaryWebPage
{
    private Document content = null;
    
    public Document getContent()
    {
        return content;
    }

    public void setContent(Document resultPage)
    {
        this.content = resultPage;
    }

    public abstract String getPageURL();
}
