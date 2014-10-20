package adonai.diary_browser.entities;

public class UmailPage extends WebPage
{
    private String umailID = "";
    private String umailURL = "";
    private String senderName = "";
    private String messageTheme = "";
    private String messageText = "";

    public String getMessageTheme()
    {
        return messageTheme;
    }

    public void setMessageTheme(String messageTheme)
    {
        this.messageTheme = messageTheme;
    }

    @Override
    public String getPageURL()
    {
        return umailURL;
    }

    public String getUmailID()
    {
        return umailID;
    }

    public void setUmailID(String umailID)
    {
        this.umailID = umailID;
    }

    public String getUmailURL()
    {
        return umailURL;
    }

    public void setUmailURL(String umailURL)
    {
        this.umailURL = umailURL;
    }

    public String getSenderName()
    {
        return senderName;
    }

    public void setSenderName(String senderName)
    {
        this.senderName = senderName;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
}
