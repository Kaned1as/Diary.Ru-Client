package adonai.diary_browser.pages;

public abstract class WebPage {
    
    private String content = "";
    private String title = "";
    private final String url;

    public WebPage(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String resultPage) {
        this.content = resultPage;
    }

    public String getTitle() {
        return title.replace("@дневники — ", "");
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return "";
    }

    public final String getPageUrl() {
        return url;
    }
}
