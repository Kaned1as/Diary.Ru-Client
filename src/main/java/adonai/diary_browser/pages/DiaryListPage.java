package adonai.diary_browser.pages;

import java.util.List;

public abstract class DiaryListPage<T> extends WebPage {

    private List<T> discussions;
    
    public DiaryListPage(String url) {
        super(url);
    }

    public List<T> getDiscussions() {
        return discussions;
    }

    public void setDiscussions(List<T> discussions) {
        this.discussions = discussions;
    }
}
