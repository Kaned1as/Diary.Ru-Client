package adonai.diary_browser.entities;

public class UmailListPage extends ListPage
{
    private boolean isRead;

    public boolean isRead()
    {
        return isRead;
    }

    public void setRead(boolean isRead)
    {
        this.isRead = isRead;
    }

    public long getId()
    {
        return Long.valueOf(getURL().substring(getURL().lastIndexOf('=') + 1));
    }
}
