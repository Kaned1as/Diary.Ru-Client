package adonai.diary_browser;

import adonai.diary_browser.entities.DiaryListPage;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiscListPage;
import adonai.diary_browser.entities.WebPage;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class UserData
{
    public interface OnDataChangeListener
    {
        public void handleDataChange();
    }

    OnDataChangeListener mListener = null;
    public void setOnDataChangeListener(OnDataChangeListener listener)
    {
        mListener = listener;
    }
    boolean isAuthorised;

    // Динамические списки постов
    DiaryListPage currentDiaries;
    DiaryListPage currentUmails;
    DiscListPage discussions;
    WebPage currentDiaryPage;
    WebPage currentUmailPage;

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
        discussions = new DiscListPage();
    }

    // обновляем контент
    public void parseData(Element tag)
    {
        // цифровая подпись
        Element sigNode = tag.getElementsByAttributeValue("name", "signature").first();
        if (sigNode != null)
            signature = sigNode.attr("value");

        // данные о ссылках на свои страницы
        Elements nodes = tag.select("div#inf_contant, div#myDiaryLinks").select("a[href]");
        boolean hasNewDiscussions = false, hasNewDiaryComments = false, hasNewUmails = false;
        for(Element node : nodes)
        {
            // ссылка на свой дневник
            if(node.text().equals("Мой дневник"))
                ownDiaryURL = node.attr("href");

            // идентификатор своего профиля
            if(node.attr("href").startsWith("/member/"))
            {
                if(!node.text().equals("Мой профиль"))
                    userName = node.text();

                String Id = node.attr("href");
                ownProfileID = Id.substring(Id.lastIndexOf("?") + 1);
            }

            // дискасс
            if(node.id().equals("menuNewDescussions"))
            {
                hasNewDiscussions = true;
                newDiscussNum = Integer.valueOf(node.text());
                newDiscussLink = node.attr("href");
            }

            // дневник
            // как минимум 3 родителя точно будет - div, body и html, значит, можно не проверять.
            if(node.parent().parent().id().equals("new_comments_count") || node.parent().parent().parent().id().equals("myDiaryLink"))
            {
                hasNewDiaryComments = true;
                newDiaryCommentsNum = Integer.valueOf(node.text());
                newDiaryLink = node.attr("href");
            }

            // U-мылки
            if(node.attr("href").contains("/u-mail/folder/"))
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

        if(mListener != null)
            mListener.handleDataChange();
    }
}