package adonai.diary_browser;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;
import android.util.Pair;

public class UserData {
	ArrayList<Pair<String, String>> communities;
	ArrayList<Diary> favorites;
	UserData() {
		favorites = new ArrayList<Diary>();
		communities = new ArrayList<Pair<String, String>>();
	}
	
	class Diary {
		private String _title = "", _url = ""; 
		private String _author = "", _author_url = "";
		private String _last_post = "", _last_post_url = "";
		private ArrayList<Post> _posts = new ArrayList<Post>();
		
		Diary(String title, String url, String author, String author_url, String last_post, String last_post_url) {
			_title = title; _url = url;
			_author = author; _author_url = author_url;
			_last_post = last_post; _last_post_url = last_post_url;
		}
		
		void setPosts(ArrayList<Post> posts) {
			_posts = posts;
		}
		
		ArrayList<Post> getPosts() {
			return _posts;
		}
		
		void setTitle(String title) {
			_title = title;
		}
		
		String getTitle() {
			return _title;
		}
		
		void setDiaryUrl(String url) {
			_url = url;
		}
		
		String getDiaryUrl() {
			return _url;
		}
		
		void setAuthor(String author) {
			_author = author;
		}
		
		String getAuthor() {
			return _author;
		}
		
		void setAuthorUrl(String author_url) {
			_author_url = author_url;
		}
		
		String getAuthorUrl() {
			return _author_url;
		}
		
		void setLastPost(String last_post) {
			_last_post = last_post;
		}
		
		String getLastPost() {
			return _last_post;
		}
		
		void setLastPostUrl(String last_post_url) {
			_last_post_url = last_post_url;
		}
		
		String getLastPostUrl() {
			return _last_post_url;
		}
	}
		
	class Post {
		String _author = ""; String _URL = "";
		String _text = ""; String _date;
		Drawable _author_avatar;
		Post() {
			
		}
	}
}
