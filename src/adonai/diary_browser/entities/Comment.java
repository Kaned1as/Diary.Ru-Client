package adonai.diary_browser.entities;

import java.lang.reflect.Field;

public class Comment
{
    // Содержимое поста
    public String content = "";
    // Идентификатор поста
    public String postID = "";
    // Идентификатор комментария
    public String commentID = "";

    public String serialize()
    {
        String result = "";
        try
        {
            for(Field current : getClass().getFields())
                if(current.getType() == String.class && !current.get(this).equals(""))
                    result += current.getName() + "=postFieldContent=" + current.get(this) + ";postField;";
        }
        catch (Exception ignored) {} // никогда не произойдет - это POD

        return result;
    }

    public void deserialize(String data)
    {
        String[] fields = data.split(";postField;");
        try
        {
            for(String field : fields)
            {
                String[] namevalue = field.split("=postFieldContent=");
                if(namevalue.length == 2)
                    getClass().getField(namevalue[0]).set(this, namevalue[1]);
            }
        }
        catch (Exception ignored) {} // никогда не произойдет
    }
}
