package adonai.diary_browser.pages;

public class Umail extends Comment {
    public String receiver = "";
    public String messageTheme = "";
    public String reMessage = "";

    @Override
    public String toString() {
        if (messageTheme.isEmpty())
            return super.toString();

        return messageTheme + ": " + content;
    }
}
