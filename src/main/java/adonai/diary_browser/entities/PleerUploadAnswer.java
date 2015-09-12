package adonai.diary_browser.entities;

/**
 * Wrapper around ProstoPleer answer for pleer.com/upload POST
 * @author Adonai
 */
public class PleerUploadAnswer {
    /**
     * Example:
     * <pre>
     * {
     *      "correct_file" : true,
     *      "correct_name" : true,
     *      "singer" : "TeddyLoid",
     *      "song" : "ME!ME!ME! feat. daoko",
     *      "file_name" : "TeddyLoid-ME_ME_ME_feat_daoko.mp3",
     *      "link" : "12919249kaC0"
     * }
     * </pre>
     */
    
    private boolean correctFile;
    private boolean correctName;
    private String singer;
    private String song;
    private String fileName;
    private String link;

    public boolean isCorrectFile() {
        return correctFile;
    }

    public void setCorrectFile(boolean correctFile) {
        this.correctFile = correctFile;
    }

    public boolean isCorrectName() {
        return correctName;
    }

    public void setCorrectName(boolean correctName) {
        this.correctName = correctName;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String signer) {
        this.singer = signer;
    }

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
