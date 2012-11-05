package adonai.diary_browser;

import java.util.ArrayList;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import adonai.diary_browser.entities.Diary;
import adonai.diary_browser.entities.DiscussionList;
import adonai.diary_browser.entities.Post;

public class UserData implements DiaryList.onUserDataParseListener
{
	// Динамические списки постов
    ArrayList<Diary> currentDiaries;
    ArrayList<DiscussionList> discussions;
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
        currentDiaries = new ArrayList<Diary>();
        currentDiaryPosts = new ArrayList<Post>();
        currentPostComments = new ArrayList<Post>();
        favoritePosts = new ArrayList<Post>();
        ownDiaryPosts = new ArrayList<Post>();
        discussions = new ArrayList<DiscussionList>();
    }
    
    // обновляем контент
    public void parseData(Element tag)
    {
    	// имя пользователя
        userName = Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "");
        
        // цифровая подпись
        Element sigNode = tag.getElementsByAttributeValue("name", "signature").first();
        if (sigNode != null)
            signature = sigNode.attr("value");

        // данные о ссылках на свои страницы
        Elements nodes = tag.getElementsByTag("a");
        boolean hasNewDiscussions = false, hasNewDiaryComments = false;
        for(Element node : nodes)
        {
        	// ссылка на свой дневник
            if(node.text().equals("Мой дневник"))
                ownDiaryURL = node.attr("href");
            
            // идентификатор своего профиля
            if(node.text().equals(userName) || node.text().equals("Мой профиль"))
            {
            	String Id = node.attr("href");
                ownProfileID = Id.substring(Id.lastIndexOf("?") + 1);
            }
            
            // дискасс
            if(node.attr("id").equals("menuNewDescussions"))
            {
            	hasNewDiscussions = true;
            	newDiscussNum = Integer.valueOf(node.text());
            	newDiscussLink = node.attr("href");
            }
            
            // дневник
            if(node.parent() != null && node.parent().parent() != null && node.parent().parent().attr("id").equals("new_comments_count"))
            {
            	hasNewDiaryComments = true;
            	newDiaryCommentsNum = Integer.valueOf(node.text());
                newDiaryLink = node.attr("href");
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

	public void updateCurrentDiary(Element tag)
	{
		currentDiaryURL = Globals.currentURL;
		
        if(tag != null)
        {
            String Id = tag.getElementsByTag("a").last().attr("href");
            currentDiaryId = Id.substring(Id.lastIndexOf("?") + 1);
        }
        else
            currentDiaryId = "";
	}

	public void updateCurrent(Post post) 
	{
		currentPostId = post.get_ID();
		currentPostURL = post.get_URL();
		currentDiaryURL = post.get_diary_URL();
		currentDiaryId = post.get_diary_Id();
	}
}