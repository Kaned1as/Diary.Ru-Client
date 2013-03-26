package adonai.diary_browser;

import java.util.ArrayList;

public class BrowseHistory
{
    private ArrayList<String> urls = new ArrayList<String>();
    private ArrayList<String> titles = new ArrayList<String>();
    private Integer mCurrentHistoryItem = 0;
    private boolean freezed;

    public void add(String url, String name)
    {
        if(urls.get(urls.size() - 1).equals(url)) // обновляем страницу, а не загружаем новую. Запись в историю не нужна.
            return;

        if(!freezed)
        {
            mCurrentHistoryItem = urls.size();

            urls.add(url);
            titles.add(name);
        }

        freezed = false;
    }

    public ArrayList<String> getUrlsCopy()
    {
        return (ArrayList<String>) urls.clone();
    }

    public void moveBack()
    {
        freezed = true;

        if(mCurrentHistoryItem > 0)
            mCurrentHistoryItem--;
    }

    public Integer getCurrentIndex()
    {
        return mCurrentHistoryItem;
    }

    public String getCurrentUrl()
    {
        if(urls.isEmpty())
            return null;

        return urls.get(mCurrentHistoryItem);
    }

    public String getName(String url)
    {
        if(!urls.contains(url))
            return null;

        return titles.get(urls.indexOf(url));
    }
}
