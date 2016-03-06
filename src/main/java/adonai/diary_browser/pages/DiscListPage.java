package adonai.diary_browser.pages;

import java.util.ArrayList;

public class DiscListPage extends ArrayList<DiscPage> {
    private String pageURL = null;

    public DiscListPage() {

    }

    public String getURL() {
        return pageURL;
    }

    public void setURL(String _URL) {
        this.pageURL = _URL;
    }
}
