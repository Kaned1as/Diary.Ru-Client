package adonai.diary_browser;

import java.util.ArrayList;

import adonai.diary_browser.entities.Diary;
import adonai.diary_browser.entities.Post;
import android.util.Pair;

public class UserData
{
    ArrayList<Pair<String, String>> communities;
    ArrayList<Diary> favorites;
    ArrayList<Post> currentDiaryPosts;
    ArrayList<Post> currentPostComments;
    ArrayList<Post> favoritePosts;
    String ownDiaryURL = "";
    String ownProfileURL = "";
    
    UserData()
    {
        favorites = new ArrayList<Diary>();
        currentDiaryPosts = new ArrayList<Post>();
        currentPostComments = new ArrayList<Post>();
        communities = new ArrayList<Pair<String, String>>();
        favoritePosts = new ArrayList<Post>();
    }
}
