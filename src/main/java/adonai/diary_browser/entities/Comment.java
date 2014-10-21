package adonai.diary_browser.entities;

import java.io.Serializable;

public class Comment implements Serializable {
    // Содержимое поста
    public String content = "";
    // Идентификатор поста
    public String postID = "";
    // Идентификатор комментария
    public String commentID = "";
}
