package adonai.diary_browser.pages;

/**
 * Элемент списка дневников
 */
public class ListPage {
    private String pageTitle = "", pageUrl = "";
    private String pageAuthor = "";
    private String lastUpdate = "", lastUpdateUrl = "";
    private String authorDiaryID = "";
    private String pageHint = "";

    public ListPage() {
    }

    public String getTitle() {
        return pageTitle;
    }

    public void setTitle(String title) {
        pageTitle = title;
    }

    public String getURL() {
        return pageUrl;
    }

    public void setURL(String url) {
        pageUrl = url;
    }

    public String getAuthor() {
        return pageAuthor;
    }

    public void setAuthor(String author) {
        pageAuthor = author;
    }

    public void setLastUpdate(String last_update) {
        lastUpdate = last_update;
    }

    public String getLastPost() {
        return lastUpdate;
    }

    public void setLastUpdateUrl(String last_update_url) {
        lastUpdateUrl = last_update_url;
    }

    public String getLastPostURL() {
        return lastUpdateUrl;
    }

    public String getAuthorID() {
        return authorDiaryID;
    }

    public void setAuthorID(String _ID) {
        this.authorDiaryID = _ID;
    }

    public String getPageHint() {
        return pageHint;
    }

    public void setPageHint(String _hint) {
        this.pageHint = _hint;
    }
}