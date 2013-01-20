package adonai.diary_browser;

import java.util.ArrayList;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryWebPage;
import adonai.diary_browser.entities.DiscList;
import adonai.diary_browser.entities.DiaryPage;

public class UserData implements DiaryList.onUserDataParseListener
{
	// Динамические списки постов
    DiaryListPage currentDiaries;
    DiaryListPage currentUmails;
    ArrayList<DiscList> discussions;
    DiaryWebPage currentDiaryPage;
    DiaryWebPage currentUmailPage;
    
    // Личные данные
    String ownDiaryURL = "";
    String ownProfileID = "";
    String userName = "";
    String signature = "";
    
    // число новых постов в дискуссиях
    Integer newDiscussNum = 0;
    String newDiscussLink = "";
    
    Integer newUmailNum = 0;
    String newUmailLink = "";
    
    // число новых постов в дневнике
    Integer newDiaryCommentsNum = 0;
    String newDiaryLink = "";
    
    UserData()
    {
        currentDiaries = new DiaryListPage();
        currentUmails = new DiaryListPage();
        currentDiaryPage = new DiaryPage();
        currentUmailPage = new DiaryPage();
        discussions = new ArrayList<DiscList>();
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
        Elements nodes = tag.select("div#inf_contant").select("a[href]");
        boolean hasNewDiscussions = false, hasNewDiaryComments = false, hasNewUmails = false;
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
            
            // U-мылки
            if(node.attr("href").startsWith("/u-mail/folder/"))
            {
                hasNewUmails = true;
                newUmailNum = Integer.valueOf(node.text());
                newUmailLink = node.attr("href");
            }
        }
        
        if(!hasNewDiscussions)
        	newDiscussNum = 0;
        if(!hasNewDiaryComments)
        	newDiaryCommentsNum = 0;
        if(!hasNewUmails)
            newUmailNum = 0;
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
}