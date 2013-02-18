package adonai.diary_browser.entities;

import android.graphics.drawable.Drawable;

public class Post
{
    // Идентификатор поста
    public String ID = "";
    // Автор поста
    public String author = "";
    // ССылка на страничку автора
    public String author_URL = "";
    //  название комьюнити поста
    public String community = "";
    // ссылка на комьюнити поста
    public String community_URL = "";
    // ссылка на пост
    public String URL = "";
    // Содержимое поста
    public String content = "";
    // Дата размещения поста
    public String date;
    // Заголовок поста
    public String title = "";
    // Число комментариев к посту
    public String comment_count = "";
    // Аватарка автора ^_^
    public Drawable author_avatar = null;
    
	public String themes = "";
	public String music = "";
	public String mood = "";
}