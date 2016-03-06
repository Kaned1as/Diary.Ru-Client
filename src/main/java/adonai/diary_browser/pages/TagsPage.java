package adonai.diary_browser.pages;

public class TagsPage extends DiaryPage {
    public TagsPage(String url) {
        super(url);
    }

    @Override
    public String getDiaryUrl() {
        return getPageUrl().substring(0, getPageUrl().lastIndexOf('/') + 1);
    }
}
