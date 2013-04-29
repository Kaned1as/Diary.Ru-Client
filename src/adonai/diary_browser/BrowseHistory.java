package adonai.diary_browser;

import java.util.ArrayList;
import java.util.List;

public class BrowseHistory
{
    private List<String> urls = new ArrayList<String>();
    private boolean freeze;

    public void add(String url)
    {
        if(!urls.isEmpty() && urls.get(urls.size() - 1).equals(url)) // обновляем страницу, а не загружаем новую. Запись в историю не нужна.
            return;

        if(!freeze)
        {
            urls.add(url);
        }

        freeze = false;
    }

    public void moveBack()
    {
        freeze = true;

        urls.remove(urls.size() - 1);
    }

    public boolean hasPrevious()
    {
        return urls.size() > 1;
    }

    public String getUrl()
    {
        return urls.get(urls.size() - 1);
    }
}
