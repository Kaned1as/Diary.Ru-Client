package adonai.diary_browser.dto;

/**
 * Wrapper around ProstoPleer answer for http://pleer.com/site_api/embed/track POST
 * @author Adonai
 */
public class PleerEmbedAnswer {
    /**
     * Example:
     * <pre>
     *     {
     *        "success": true,
     *        "embed_id": "B808oaB8kmhp4B4ke",
     *        "link": "http://pleer.com/tracks/13448170JXS2",
     *        "name": "Vexare The Clock Maker"
     *     }
     * </pre>
     */
    private boolean success;
    private String embedId;
    private String link;
    private String name;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getEmbedId() {
        return embedId;
    }

    public void setEmbedId(String embedId) {
        this.embedId = embedId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
