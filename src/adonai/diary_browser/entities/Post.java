package adonai.diary_browser.entities;

import java.lang.reflect.Field;

public class Post
{
    // Идентификатор поста
    public String ID = "";
    // Идентификатор дневника
    public String diaryID = "";
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
    public String date = "";
    // Заголовок поста
    public String title = "";
    // Число комментариев к посту
    public String comment_count = "";
    
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
	
	public String serialize()
	{
	    String result = "";
        try
        {
    	    for(Field current : getClass().getFields())
    	        if(current.getType() == String.class && !current.get(this).equals(""))
                        result += current.getName() + "=postFieldContent=" + current.get(this) + ";postField;";
        }
        catch (Exception e) {} // никогда не произойдет - это POD
        
        return result;
	}
	
	public static Post deserialize(String postData)
	{
	    Post post = new Post();
	    String[] fields = postData.split(";postField;");
        try
        {
    	    for(String field : fields)
    	    {
    	        String[] namevalue = field.split("=postFieldContent=");
    	        if(namevalue.length == 2)
                    post.getClass().getField(namevalue[0]).set(post, namevalue[1]);
    	    }
        }
        catch (Exception e) {} // никогда не произойдет
	    
	    return post;
	}
}