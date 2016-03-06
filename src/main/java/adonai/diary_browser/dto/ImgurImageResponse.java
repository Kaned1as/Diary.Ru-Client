package adonai.diary_browser.dto;

/**
 * Wrapper around Imgur answer for https://api.imgur.com/3/image POST
 * @author Adonai
 */
public class ImgurImageResponse {
    public boolean success;
    public int status;
    public UploadedImage data;

    public static class UploadedImage {
        public String id;
        public String title;
        public String description;
        public String type;
        public boolean animated;
        public int width;
        public int height;
        public int size;
        public int views;
        public int bandwidth;
        public String vote;
        public boolean favorite;
        public String account_url;
        public String deletehash;
        public String name;
        public String link;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("UploadedImage{");
            sb.append("id='").append(id).append('\'');
            sb.append(", title='").append(title).append('\'');
            sb.append(", description='").append(description).append('\'');
            sb.append(", type='").append(type).append('\'');
            sb.append(", animated=").append(animated);
            sb.append(", width=").append(width);
            sb.append(", height=").append(height);
            sb.append(", size=").append(size);
            sb.append(", views=").append(views);
            sb.append(", bandwidth=").append(bandwidth);
            sb.append(", vote='").append(vote).append('\'');
            sb.append(", favorite=").append(favorite);
            sb.append(", account_url='").append(account_url).append('\'');
            sb.append(", deletehash='").append(deletehash).append('\'');
            sb.append(", name='").append(name).append('\'');
            sb.append(", link='").append(link).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ImgurImageResponse{");
        sb.append("success=").append(success);
        sb.append(", status=").append(status);
        sb.append(", data=").append(data);
        sb.append('}');
        return sb.toString();
    }
}
