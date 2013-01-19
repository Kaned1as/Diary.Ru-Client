package adonai.diary_browser.entities;

public class UmailPage extends DiaryWebPage
{
    private String umail_ID = "";
    private String umail_URL = "";
    private String sender_Name = "";
    
    
    @Override
    public String getPageURL()
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

    public String getSender_Name()
    {
        return sender_Name;
    }

    public void setSender_Name(String sender_Name)
    {
        this.sender_Name = sender_Name;
    }
}
