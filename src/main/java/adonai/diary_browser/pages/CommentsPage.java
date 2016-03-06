package adonai.diary_browser.pages;

/**
 * Комменты имеют собственную страничку, которая всегда является дочерней 
 */
public class CommentsPage extends DiaryPage {
    
    private String postId = "";
    
    public CommentsPage(String url) {
        super(url);
    }

    @Override
    public String getDiaryUrl() {
        return getPageUrl().substring(0, getPageUrl().lastIndexOf('/') + 1);
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String _ID) {
        this.postId = _ID;
    }

    @Override
    public String getSubtitle() {
        String title = super.getTitle();
        if(title.contains(" — ")) { // скорее всего, заголовок + имя дневника
            String[] tokens = title.split(" — ");
            if(tokens.length >= 2) {
                return tokens[0];
            }
        }
        return super.getSubtitle();
    }

    @Override
    public String getTitle() {
        String rawTitle = super.getTitle();
        if(rawTitle.contains(" — ")) { // скорее всего, заголовок + имя дневника
            String[] tokens = rawTitle.split(" — ");
            return tokens[1];
        }
        return rawTitle;
    }
}
