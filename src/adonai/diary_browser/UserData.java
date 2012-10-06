package adonai.diary_browser;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.util.Pair;

public class UserData
{
    ArrayList<Pair<String, String>> communities;
    ArrayList<Diary> favorites;
    ArrayList<Post> currentDiary;
    
    UserData()
    {
        favorites = new ArrayList<Diary>();
        currentDiary = new ArrayList<Post>();
        communities = new ArrayList<Pair<String, String>>();
    }
    
    public class Diary
    {
        private String _title = "", _url = "";
        private String _author = "", _author_url = "";
        private String _last_post = "", _last_post_url = "";
        private ArrayList<Post> _posts = new ArrayList<Post>();
        
        Diary(String title, String url, String author, String author_url, String last_post, String last_post_url)
        {
            _title = title;
            _url = url;
            _author = author;
            _author_url = author_url;
            _last_post = last_post;
            _last_post_url = last_post_url;
        }
        
        void setPosts(ArrayList<Post> posts)
        {
            _posts = posts;
        }
        
        ArrayList<Post> getPosts()
        {
            return _posts;
        }
        
        void setTitle(String title)
        {
            _title = title;
        }
        
        String getTitle()
        {
            return _title;
        }
        
        void setDiaryUrl(String url)
        {
            _url = url;
        }
        
        String getDiaryUrl()
        {
            return _url;
        }
        
        void setAuthor(String author)
        {
            _author = author;
        }
        
        String getAuthor()
        {
            return _author;
        }
        
        void setAuthorUrl(String author_url)
        {
            _author_url = author_url;
        }
        
        String getAuthorUrl()
        {
            return _author_url;
        }
        
        void setLastPost(String last_post)
        {
            _last_post = last_post;
        }
        
        String getLastPost()
        {
            return _last_post;
        }
        
        void setLastPostUrl(String last_post_url)
        {
            _last_post_url = last_post_url;
        }
        
        String getLastPostUrl()
        {
            return _last_post_url;
        }
    }
    
    public class Post
    {
        public Post()
        {
            
        }
        
        public Post(String _title, String _author, String _author_URL, String _URL, Spanned _text, String _date, Drawable _author_avatar)
        {
            this._title = _title;
            this._author = _author;
            this._author_URL = _author_URL;
            this._URL = _URL;
            this._text = _text;
            this._date = _date;
            this._author_avatar = _author_avatar;
        }
        
        private String _author = "";
        private String _author_URL = "";
        private String _URL = "";
        private Spanned _text = null;
        private String _date;
        private String _title = "";
        private Drawable _author_avatar = null;
        
        /**
         * @return the _author
         */
        public String get_author()
        {
            return _author;
        }
        
        /**
         * @param _author
         *            the _author to set
         */
        public void set_author(String _author)
        {
            this._author = _author;
        }
        
        /**
         * @return the _URL
         */
        public String get_URL()
        {
            return _URL;
        }
        
        /**
         * @param _URL
         *            the _URL to set
         */
        public void set_URL(String _URL)
        {
            this._URL = _URL;
        }
        
        /**
         * @return the _text
         */
        public Spanned get_text()
        {
            return _text;
        }
        
        /**
         * @param spanned
         *            the _text to set
         */
        public void set_text(Spanned spanned)
        {
            this._text = spanned;
        }
        
        /**
         * @return the _date
         */
        public String get_date()
        {
            return _date;
        }
        
        /**
         * @param _date
         *            the _date to set
         */
        public void set_date(String _date)
        {
            this._date = _date;
        }
        
        /**
         * @return the _author_avatar
         */
        public Drawable get_author_avatar()
        {
            return _author_avatar;
        }
        
        /**
         * @param _author_avatar
         *            the _author_avatar to set
         */
        public void set_author_avatar(Drawable _author_avatar)
        {
            this._author_avatar = _author_avatar;
        }
        
        public String get_title()
        {
            return _title;
        }
        
        public void set_title(String _title)
        {
            this._title = _title;
        }

        public String get_author_URL()
        {
            return _author_URL;
        }

        public void set_author_URL(String _author_URL)
        {
            this._author_URL = _author_URL;
        }
    }
}
