package adonai.diary_browser.entities;

import android.text.Spanned;

import java.util.ArrayList;

/**
 * Страница любого открываемого в дайри списка
 * @param <T> тип списка
 */
public class DiaryLinkList<T extends ListPage> extends ArrayList<T> {
    
    private Spanned pageLinks = null;
    private String pageURL = "";

    public DiaryLinkList() {
    }

    public DiaryLinkList(String pageURL) {
        this.pageURL = pageURL;
    }

    public Spanned getPageLinks() {
        return pageLinks;
    }

    public void setPageLinks(Spanned pageLinks) {
        this.pageLinks = pageLinks;
    }

    public String getURL() {
        return pageURL;
    }
}
