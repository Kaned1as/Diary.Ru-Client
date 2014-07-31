package adonai.diary_browser.entities;

/**
 * Created by adonai on 27.05.14.
 */
public class SearchPage extends WebPage {

    private String searchURL;

    public SearchPage(String searchURL) {
        this.searchURL = searchURL;
    }

    @Override
    public String getPageURL() {
        return searchURL;
    }
}
