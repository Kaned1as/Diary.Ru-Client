package adonai.diary_browser;

import java.util.Deque;
import java.util.LinkedList;

public class BrowseHistory implements DiaryWebView.PositionTracker {
    
    private Deque<SavedPageInfo> savedPages = new LinkedList<>();
    private boolean backRequested;

    public void add(String url) {
        try {
            
            // обновляем страницу, а не загружаем новую. Запись в историю не нужна
            // позицию устанавливаем в 0, т.к. нам не нужно пролистывание при обновлении
            if (!isEmpty() && !backRequested && getUrl().equals(url)) {
                savedPages.peek().position = 0;
                return;
            }

            // убеждаемся, что это не результат нажатия кнопки "назад"
            if (!backRequested) {
                savedPages.push(new SavedPageInfo(url));
            }
        } finally {
            backRequested = false;
        }
    }
    
    public void moveBack() {
        backRequested = true;

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
        private Integer position = 0;

        public SavedPageInfo(String url) {
            this.url = url;
        }
    }
}
