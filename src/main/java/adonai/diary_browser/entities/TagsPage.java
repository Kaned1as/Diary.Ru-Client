package adonai.diary_browser.entities;

public class TagsPage extends DiaryPage
{
    public TagsPage(String diaryURL)
    {
        setDiaryURL(diaryURL);
    }

    @Override
    public String getPageURL()
    {
        return super.getPageURL() + "?tags";
    }
}