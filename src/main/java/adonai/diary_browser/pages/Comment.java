package adonai.diary_browser.pages;

import java.io.Serializable;

public class Comment implements Serializable {
    // Содержимое поста
    public String content = "";
    // Идентификатор поста
    public String postID = "";
    // Идентификатор комментария
    public String commentID = "";

    @Override
    public String toString() {
        return content;
    }
}
