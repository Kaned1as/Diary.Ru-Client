package adonai.diary_browser.entities;

import java.util.Vector;

import android.text.Spanned;


public class DiaryListPage extends Vector<Diary>
{
    private static final long serialVersionUID = 1L;
    private Spanned pageLinks = null;
    
    public Spanned getPageLinks()
    {
        return pageLinks;
    }
    public void setPageLinks(Spanned pageLinks)
    {
        this.pageLinks = pageLinks;
    }
}
