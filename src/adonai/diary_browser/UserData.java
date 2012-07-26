package adonai.diary_browser;

import java.util.HashMap;

public class UserData {
	HashMap<String, String> favorites;
	HashMap<String, String> communities;
	
	UserData() {
		favorites = new HashMap<String, String>();
		communities = new HashMap<String, String>();
	}
}
