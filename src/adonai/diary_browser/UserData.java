package adonai.diary_browser;

import java.util.ArrayList;

import org.htmlcleaner.TagNode;

import adonai.diary_browser.entities.Diary;
import adonai.diary_browser.entities.Post;
import android.util.Pair;

public class UserData implements DiaryList.onUserDataParseListener
{
    ArrayList<Pair<String, String>> communities;
    ArrayList<Diary> favorites;
    ArrayList<Post> currentDiaryPosts;
    ArrayList<Post> currentPostComments;
    ArrayList<Post> favoritePosts;
    ArrayList<Post> ownDiaryPosts;
    String ownDiaryURL = "";
    String ownProfileURL = "";
    String userName = "";
    String signature = "";
    String currentDiaryId = "";
    
    UserData()
    {
        favorites = new ArrayList<Diary>();
        currentDiaryPosts = new ArrayList<Post>();
        currentPostComments = new ArrayList<Post>();
        communities = new ArrayList<Pair<String, String>>();
        favoritePosts = new ArrayList<Post>();
        ownDiaryPosts = new ArrayList<Post>();
    }
    
    public void parseData(TagNode tag)
    {
        userName = Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "");
        TagNode sigNode = tag.findElementByAttValue("name", "signature", true, true);
        if (sigNode != null)
            signature = sigNode.getAttributeByName("value");

        TagNode[] nodes = tag.getElementsByName("a", true);
        for(TagNode node : nodes)
        {
            if(node.getText().toString().equals("Мой дневник"))
                ownDiaryURL = node.getAttributeByName("href");
            if(node.getText().toString().equals(userName) || node.getText().toString().equals("Мой профиль"))
                ownProfileURL = node.getAttributeByName("href");
        }
    }

    public boolean updateNeeded()
    {
        if(ownDiaryURL.equals(""))
            return true;
        
        if(ownProfileURL.equals(""))
            return true;
        
        if(userName.equals(""))
            return true;
        
        if(signature.equals(""))
            return true;
        
        return false;
    }

	public void updateIDs(TagNode tag) 
	{
		TagNode author = tag.findElementByAttValue("id", "authorName", true, true);
        if(author != null)
        {
            String Id = author.getAttributeByName("href");
            currentDiaryId = Id.substring(Id.lastIndexOf("?") + 1);
        }
        else
            currentDiaryId = "";
	}
}