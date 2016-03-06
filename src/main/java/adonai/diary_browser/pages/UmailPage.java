package adonai.diary_browser.pages;

public class UmailPage extends WebPage {
    private String senderName = "";
    private String messageTheme = "";
    private String messageText = "";

    public UmailPage(String url) {
        super(url);
    }

    public String getMessageTheme() {
        return messageTheme;
    }

    public void setMessageTheme(String messageTheme) {
        this.messageTheme = messageTheme;
    }

    public String getUmailId() {
        return getPageUrl().substring(getPageUrl().lastIndexOf('=') + 1);
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    @Override
    public String getTitle() {
        return super.getTitle().replace("@дневники: ", "");
    }

    @Override
    public String getSubtitle() {
        return senderName;
    }
}
