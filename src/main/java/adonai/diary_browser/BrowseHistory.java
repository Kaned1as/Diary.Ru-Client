package adonai.diary_browser;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class BrowseHistory {
    private List<Pair<String, Integer>> urls = new ArrayList<>();
    private boolean freeze;

    public void add(String url) {
        if ((urls.isEmpty() || !urls.get(urls.size() - 1).first.equals(url)) && !freeze) // обновляем страницу, а не загружаем новую. Запись в историю не нужна.
            urls.add(new Pair<>(url, 0));

        freeze = false;
    }

    public void moveBack() {
        freeze = true;

        urls.remove(urls.size() - 1);
    }

    public boolean hasPrevious() {
        return urls.size() > 1;
    }

    public boolean isEmpty() {
        return urls.isEmpty();
    }

    public String getUrl() {
        return urls.get(urls.size() - 1).first;
    }

    public Integer getPosition() {
        return urls.get(urls.size() - 1).second;
    }

    public void setPosition(Integer scroll) {
        urls.set(urls.size() - 1, new Pair<>(urls.get(urls.size() - 1).first, scroll));
    }
}
