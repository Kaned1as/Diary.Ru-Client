package adonai.diary_browser;

import java.util.ArrayList;

import android.util.Pair;

public class UserData {
	ArrayList<Pair<String, String>> communities;
	ArrayList<Diary> favorites;
	UserData() {
		favorites = new ArrayList<Diary>();
		communities = new ArrayList<Pair<String, String>>();
	}
	
	class Diary {
		String _title = ""; String _title_url = ""; 
		String _author = ""; String _author_url = "";
		String _last_post = ""; String _last_post_url = "";
		
		Diary(String title, String title_url, String author, String author_url, String last_post, String last_post_url) {
			_title = title; _title_url = title_url;
			_author = author; _author_url = author_url;
			_last_post = last_post; _last_post_url = last_post_url;
		}
	}
}
