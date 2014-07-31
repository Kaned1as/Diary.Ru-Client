package adonai.diary_browser.entities;

public class CommentsPage extends DiaryPage
{
    public CommentsPage(String diaryURL)
    {
        setDiaryURL(diaryURL);
    }

    private String postID = "";
    private String postURL = "";

    public String getPostID()
    {
        return postID;
    }

    public void setPostID(String _ID) 
    {
        this.postID = _ID;
    }

    public String getPostURL()
    {
        return postURL;
    }

    public void setPostURL(String _post_URL)
    {
        this.postURL = _post_URL;
    }
    
    @Override
    public String getPageURL()
    {
        return postURL;
    }
}
