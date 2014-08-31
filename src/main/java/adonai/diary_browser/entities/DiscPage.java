package adonai.diary_browser.entities;


import java.util.ArrayList;

public class DiscPage extends ListPage
{
    public static class Discussion extends Post
    {

    }

    private ArrayList<Discussion> discussions;

    public DiscPage()
    {
        setDiscussions(new ArrayList<Discussion>());
    }

    public ArrayList<Discussion> getDiscussions()
    {
        return discussions;
    }

    public void setDiscussions(ArrayList<Discussion> discussions)
    {
        this.discussions = discussions;
    }
}