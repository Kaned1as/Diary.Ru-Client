package adonai.diary_browser;

import java.util.Deque;
import java.util.LinkedList;

public class BrowseHistory implements DiaryWebView.PositionTracker {
    
    private Deque<SavedPageInfo> savedPages = new LinkedList<>();
    private boolean freeze;

    public void add(String url) {
        // обновляем страницу, а не загружаем новую. Запись в историю не нужна
        if ((savedPages.isEmpty() || !getUrl().equals(url)) && !freeze)
            savedPages.push(new SavedPageInfo(url, 0));

        freeze = false;
    }
    
    public void moveBack() {
        freeze = true;

        savedPages.pop();
    }

    public boolean hasPrevious() {
        return savedPages.size() > 1;
    }

    public boolean isEmpty() {
        return savedPages.isEmpty();
    }

    public String getUrl() {
        return savedPages.peek().url;
    }

    @Override
    public void savePosition(String url, int position) {
        for(SavedPageInfo pInfo : savedPages) {
            if(pInfo.url.equals(url)) {
                pInfo.position = position;
                return;
            }
        }
    }

    @Override
    public int restorePosition(String url) {
        for(SavedPageInfo pInfo : savedPages) {
            if(pInfo.url.equals(url)) {
                return pInfo.position;
            }
        }
        return 0;
    }

    private class SavedPageInfo {
        
        private String url;
        private Integer position;

        public SavedPageInfo(String url, Integer position) {
            this.url = url;
            this.position = position;
        }
    }
}
