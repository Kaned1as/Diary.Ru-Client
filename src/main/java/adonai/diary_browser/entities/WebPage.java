package adonai.diary_browser.entities;

public abstract class WebPage {
    private String content;
    private String title;

    public String getContent() {
        return content;
    }

    public void setContent(String resultPage) {
        this.content = resultPage;
    }

    public String getTitle() {
        return title.replaceAll("@дневники — ", "");
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public abstract String getPageURL();
}
