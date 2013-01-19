package adonai.diary_browser.entities;

public class DiaryPage extends DiaryWebPage
{
    // дневник, из которого просматривается пост. Если Избранное - то свой дневник.
    private String diaryURL = ""; 
    // идентификатор этого же дневника
    private String diaryID = "";
    
    public String getDiaryURL()
    {
        return diaryURL;
    }
    
    public void setDiaryURL(String _diary_URL)
    {
        this.diaryURL = _diary_URL;
    }
    
    public String getDiaryID()
    {
        return diaryID;
    }
    
    public void setDiaryID(String _diary_Id)
    {
        this.diaryID = _diary_Id;
    }

    public String getPageURL()
    {
    	return diaryURL;
    }
}
