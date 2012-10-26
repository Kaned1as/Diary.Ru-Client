package adonai.diary_browser;

import java.util.ArrayList;

import org.htmlcleaner.TagNode;

import adonai.diary_browser.entities.Diary;
import adonai.diary_browser.entities.Post;

public class UserData implements DiaryList.onUserDataParseListener
{
	// Динамические списки постов
    ArrayList<Diary> favorites;
    ArrayList<Post> currentDiaryPosts;
    ArrayList<Post> currentPostComments;
    ArrayList<Post> favoritePosts;
    ArrayList<Post> ownDiaryPosts;
    
    // Личные данные
    String ownDiaryURL = "";
    String ownProfileID = "";
    String userName = "";
    String signature = "";
    
    /** 
     * Динамически обновляемый на загрузке странички контент
     * 
     */
    
    // Текущий дневник
    String currentDiaryId = "";
    String currentDiaryURL = "";
    
    // Текущий пост
    String currentPostId = "";
    String currentPostURL = "";
    
    // число новых постов в дискуссиях
    Integer newDiscussNum = 0;
    String newDiscussLink = "";
    
    // число новых постов в дневнике
    Integer newDiaryCommentsNum = 0;
    String newDiaryLink = "";
    
    UserData()
    {
        favorites = new ArrayList<Diary>();
        currentDiaryPosts = new ArrayList<Post>();
        currentPostComments = new ArrayList<Post>();
        favoritePosts = new ArrayList<Post>();
        ownDiaryPosts = new ArrayList<Post>();
    }
    
    // обновляем контент
    public void parseData(TagNode tag)
    {
    	// имя пользователя
        userName = Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "");
        
        // цифровая подпись
        TagNode sigNode = tag.findElementByAttValue("name", "signature", true, true);
        if (sigNode != null)
            signature = sigNode.getAttributeByName("value");

        // данные о ссылках на свои страницы
        TagNode[] nodes = tag.getElementsByName("a", true);
        boolean hasNewDiscussions = false, hasNewDiaryComments = false;
        for(TagNode node : nodes)
        {
        	// ссылка на свой дневник
            if(node.getText().toString().equals("Мой дневник"))
                ownDiaryURL = node.getAttributeByName("href");
            
            // идентификатор своего профиля
            if(node.getText().toString().equals(userName) || node.getText().toString().equals("Мой профиль"))
            {
            	String Id = node.getAttributeByName("href");
                ownProfileID = Id.substring(Id.lastIndexOf("?") + 1);
            }
            
            // дискасс
            if(node.hasAttribute("id") && node.getAttributeByName("id").equals("menuNewDescussions"))
            {
            	hasNewDiscussions = true;
            	newDiscussNum = Integer.valueOf(node.getText().toString());
            	newDiscussLink = node.getAttributeByName("href");
            }
            
            // дневник
            if(node.hasAttribute("id") && node.getAttributeByName("id").equals("new_comments_count"))
            {
            	hasNewDiaryComments = true;
            	newDiaryCommentsNum = Integer.valueOf(node.getText().toString());
                newDiaryLink = node.getAttributeByName("href");
            }
        }
        
        if(!hasNewDiscussions)
        	newDiscussNum = 0;
        if(!hasNewDiaryComments)
        	newDiaryCommentsNum = 0;
    }

    public boolean updateNeeded()
    {
        if(ownDiaryURL.equals(""))
            return true;
        
        if(ownProfileID.equals(""))
            return true;
        
        if(userName.equals(""))
            return true;
        
        if(signature.equals(""))
            return true;
        
        return false;
    }

	public void updateCurrentDiary(TagNode tag)
	{
		currentDiaryURL = Globals.mDHCL.lastURL;
		
		TagNode author = tag.findElementByAttValue("id", "authorName", true, true);
        if(author != null)
        {
            String Id = author.getAttributeByName("href");
            currentDiaryId = Id.substring(Id.lastIndexOf("?") + 1);
        }
        else
            currentDiaryId = "";
	}

	public void updateCurrentPost(Post post) 
	{
		currentPostId = post.get_ID();
		currentPostURL = post.get_URL();
	}
}