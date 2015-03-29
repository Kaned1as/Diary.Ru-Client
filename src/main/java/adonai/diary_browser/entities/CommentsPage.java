package adonai.diary_browser.entities;

public class CommentsPage extends DiaryPage {
    private String postID = "";
    private String postURL = "";
    public CommentsPage(String diaryURL) {
        setDiaryURL(diaryURL);
    }

    public String getPostID() {
        return postID;
    }

    public void setPostID(String _ID) {
        this.postID = _ID;
    }

    public String getPostURL() {
        return postURL;
    }

    public void setPostURL(String _post_URL) {
        this.postURL = _post_URL;
    }

    @Override
    public String getPageURL() {
        return postURL;
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
