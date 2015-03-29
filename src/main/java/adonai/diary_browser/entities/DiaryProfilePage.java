package adonai.diary_browser.entities;

public class DiaryProfilePage extends WebPage {
    private String profileURL;

    public DiaryProfilePage(String profileURL) {
        this.profileURL = profileURL;
    }

    @Override
    public String getPageURL() {
        return profileURL;
    }

    @Override
    public String getTitle() {
        return super.getTitle().replace("@дневники: ", "");
    }
}
