package adonai.diary_browser.pages;

public class DiaryProfilePage extends WebPage {

    public DiaryProfilePage(String url) {
        super(url);
    }

    @Override
    public String getTitle() {
        return super.getTitle().replace("@дневники: ", "");
    }
}
