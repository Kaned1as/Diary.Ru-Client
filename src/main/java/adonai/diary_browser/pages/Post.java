package adonai.diary_browser.pages;

import java.util.LinkedHashMap;
import java.util.Map;

public class Post extends Comment {
    // Идентификатор дневника
    public String diaryID = "";
    // ссылка на пост
    public String url = "";
    // Дата размещения поста
    public String date = "";
    // Заголовок поста
    public String title = "";
    // Черновик?
    public String postType = "";

    public String themes = "";
    public String music = "";
    public String mood = "";

    public String pollTitle = "";
    public String pollAnswer1 = "";
    public String pollAnswer2 = "";
    public String pollAnswer3 = "";
    public String pollAnswer4 = "";
    public String pollAnswer5 = "";
    public String pollAnswer6 = "";
    public String pollAnswer7 = "";
    public String pollAnswer8 = "";
    public String pollAnswer9 = "";
    public String pollAnswer10 = "";

    public String closeText = "";
    public String closeAccessMode = "";
    public String closeAllowList = "";
    public String closeDenyList = "";

    public Boolean noComments = false;

    public Map<String, Boolean> predefinedTags = new LinkedHashMap<>();

    @Override
    public String toString() {
        if (title.isEmpty())
            return super.toString();

        return title + ": " + content;
    }
}