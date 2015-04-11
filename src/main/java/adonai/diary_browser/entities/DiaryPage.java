package adonai.diary_browser.entities;

import java.util.HashMap;
import java.util.Map;

public class DiaryPage extends WebPage {
    public Map<String, String> userLinks = new HashMap<>();
    // дневник, из которого просматривается пост. Если Избранное - то свой дневник.
    private String diaryUrl = "";
    // идентификатор этого же дневника
    private String diaryId = "";

    public DiaryPage() {

    }

    public DiaryPage(String diaryUrl) {
        this.diaryUrl = diaryUrl;
    }

    public String getDiaryUrl() {
        return diaryUrl;
    }

    public void setDiaryUrl(String diaryUrl) {
        this.diaryUrl = diaryUrl;
    }

    public String getDiaryId() {
        return diaryId;
    }

    public void setDiaryId(String diaryId) {
        this.diaryId = diaryId;
    }

    public String getPageURL() {
        return diaryUrl;
    }
}
