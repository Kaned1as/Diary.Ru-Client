package adonai.diary_browser.pages;

public class CommentsPage extends DiaryPage {
    private String postId = "";
    private String postUrl = "";
    public CommentsPage(String diaryURL) {
        setDiaryUrl(diaryURL);
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String _ID) {
        this.postId = _ID;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String _post_URL) {
        this.postUrl = _post_URL;
    }

    @Override
    public String getPageURL() {
        return postUrl;
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
