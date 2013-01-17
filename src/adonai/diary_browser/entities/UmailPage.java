package adonai.diary_browser.entities;

public class UmailPage extends DiaryWebPage
{
    private String umail_ID = "";
    private String umail_URL = "";

    @Override
    public String get_page_URL()
    {
        return umail_URL;
    }

    public String getUmail_ID()
    {
        return umail_ID;
    }

    public void setUmail_ID(String umail_ID)
    {
        this.umail_ID = umail_ID;
    }

    public String getUmail_URL()
    {
        return umail_URL;
    }

    public void setUmail_URL(String umail_URL)
    {
        this.umail_URL = umail_URL;
    }
}
