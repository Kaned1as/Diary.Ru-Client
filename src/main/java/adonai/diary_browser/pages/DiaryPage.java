package adonai.diary_browser.pages;

import java.util.HashMap;
import java.util.Map;

public class DiaryPage extends WebPage {
    public Map<String, String> userLinks = new HashMap<>();
    // дневник, из которого просматривается пост. Если Избранное - то свой дневник.
    // идентификатор этого же дневника
    private String diaryId = "";

    public DiaryPage(String url) {
        super(url);
    }

    public String getDiaryUrl() {
        return getPageUrl();
    }

    public String getDiaryId() {
        return diaryId;
    }

    public void setDiaryId(String diaryId) {
        this.diaryId = diaryId;
    }

}
