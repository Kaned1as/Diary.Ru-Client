package adonai.diary_browser.entities;

import java.util.Vector;

import android.text.Spanned;


public class DiaryListPage extends Vector<Openable>
{
    private static final long serialVersionUID = 1L;
    private Spanned pageLinks = null;
    private String _URL = null;
    
    public Spanned getPageLinks()
    {
        return pageLinks;
    }
    public void setPageLinks(Spanned pageLinks)
    {
        this.pageLinks = pageLinks;
    }
    public String get_URL()
    {
        return _URL;
    }
    public void set_URL(String _URL)
    {
        this._URL = _URL;
    }
}
