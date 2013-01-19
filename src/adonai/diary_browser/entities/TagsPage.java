package adonai.diary_browser.entities;

public class TagsPage extends DiaryPage
{
    @Override
    public String getPageURL()
    {
        return super.getPageURL() + "?tags";
    }
}
