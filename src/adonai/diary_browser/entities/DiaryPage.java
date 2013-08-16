package adonai.diary_browser.entities;

public class DiaryPage extends WebPage
{
    // дневник, из которого просматривается пост. Если Избранное - то свой дневник.
    private String diaryURL = "";
    // идентификатор этого же дневника
    private String diaryID = "";

    public DiaryPage()
    {
        
    }

    public DiaryPage(String diaryUrl)
    {
        this.diaryURL = diaryUrl;
    }

    public String getDiaryURL()
    {
        return diaryURL;
    }

    public void setDiaryURL(String diaryURL)
    {
        this.diaryURL = diaryURL;
    }

    public String getDiaryID()
    {
        return diaryID;
    }
    
    public void setDiaryID(String diaryId)
    {
        this.diaryID = diaryId;
    }

    public String getPageURL()
    {
    	return diaryURL;
    }
}
