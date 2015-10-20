package adonai.diary_browser;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import adonai.diary_browser.entities.DiaryLinkList;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiscListPage;
import adonai.diary_browser.entities.ListPage;
import adonai.diary_browser.entities.UmailListPage;
import adonai.diary_browser.entities.UmailPage;
import adonai.diary_browser.entities.WebPage;

/**
 * Синглетон данных пользователя в текущем сеансе.
 * Все записи и чтения отсюда, хоть и разнесены по разным потокам, но не выполняются одновременно.
 * 
 * Обновляются эти данные в потоке NetworkService'a, запрашиваются - в UI-потоках различных активностей
 */
public class UserData {

    private static UserData user = new UserData();

    private boolean isAuthorized;
    
    // Динамические списки постов
    private DiaryLinkList<ListPage> currentDiaries = new DiaryLinkList<>();
    private DiaryLinkList<UmailListPage> currentUmails = new DiaryLinkList<>();
    private DiscListPage discussions = new DiscListPage();
    private WebPage currentDiaryPage = new DiaryPage();
    private UmailPage currentUmailPage = new UmailPage();
    
    // Личные данные
    private String ownProfileId = "";
    private String userName = "";
    private String signature = "";
    
    // Ссылки
    private String ownDiaryUrl = "";
    private String ownFavoritesPageUrl = "http://www.diary.ru/?favorite";
    
    // число новых постов в дискуссиях
    private int newDiscussNum;
    private String newDiscussLink = "";

    // число новых U-Mail
    private int newUmailNum;
    private String newUmailLink = "";
    
    // число новых постов в дневнике
    private int newDiaryCommentsNum;
    private String newDiaryLink = "";


    public static UserData getInstance() {
        return user;
    }

    public static void clear() {
        user = new UserData();
    }

    private UserData()
    {
    }

    // обновляем контент
    public void updateData(Element tag) {
        // цифровая подпись
        Element sigNode = tag.getElementsByAttributeValue("name", "signature").first();
        if (sigNode != null)
            setSignature(sigNode.attr("value"));

        // данные о ссылках на свои страницы
        Elements nodes = tag.select("div#inf_contant, div#myDiaryLinks").select("a[href]");
        boolean hasNewDiscussions = false, hasNewDiaryComments = false, hasNewUmails = false;
        for (Element node : nodes) {
            // ссылка на свой дневник
            if (node.text().equals("Мой дневник"))
                setOwnDiaryUrl(node.attr("href"));

            // идентификатор своего профиля
            if (node.attr("href").startsWith("/member/")) {
                if (!node.text().equals("Мой профиль"))
                    setUserName(node.text());

                String Id = node.attr("href");
                setOwnProfileId(Id.substring(Id.lastIndexOf("?") + 1));
            }

            // дискасс
            if (node.id().equals("menuNewDescussions")) {
                hasNewDiscussions = true;
                setNewDiscussNum(Integer.valueOf(node.text()));
                setNewDiscussLink(node.attr("href"));
            }

            // дневник
            // как минимум 3 родителя точно будет - div, body и html, значит, можно не проверять.
            if (node.parent().parent().id().equals("new_comments_count") || node.parent().parent().parent().id().equals("myDiaryLink")) {
                hasNewDiaryComments = true;
                setNewDiaryCommentsNum(Integer.valueOf(node.text()));
                setNewDiaryLink(node.attr("href"));
            }

            // U-мылки
            if (node.attr("href").contains("/u-mail/folder/")) {
                hasNewUmails = true;
                setNewUmailNum(Integer.valueOf(node.text()));
                setNewUmailLink(node.attr("href"));
            }
        }

        if (!hasNewDiscussions)
            setNewDiscussNum(0);
        if (!hasNewDiaryComments)
            setNewDiaryCommentsNum(0);
        if (!hasNewUmails)
            setNewUmailNum(0);
    }

    String getDiscussionsUrl() {
        return Utils.DISCUSSIONS_PAGE;
    }

    String getFavoritesUrl() {
        return Utils.FAVORITES_PAGE;
    }

    String getSubscribersUrl() {
        return Utils.SUBSCRIBERS_PAGE;
    }

    boolean isAuthorized() {
        return isAuthorized;
    }

    void setAuthorized(boolean isAuthorised) {
        this.isAuthorized = isAuthorised;
    }

    DiaryLinkList<ListPage> getCurrentDiaries() {
        return currentDiaries;
    }

    void setCurrentDiaries(DiaryLinkList<ListPage> currentDiaries) {
        this.currentDiaries = currentDiaries;
    }

    DiaryLinkList<UmailListPage> getCurrentUmails() {
        return currentUmails;
    }

    void setCurrentUmails(DiaryLinkList<UmailListPage> currentUmails) {
        this.currentUmails = currentUmails;
    }

    DiscListPage getDiscussions() {
        return discussions;
    }

    void setDiscussions(DiscListPage discussions) {
        this.discussions = discussions;
    }

    WebPage getCurrentDiaryPage() {
        return currentDiaryPage;
    }

    void setCurrentDiaryPage(WebPage currentDiaryPage) {
        this.currentDiaryPage = currentDiaryPage;
    }

    UmailPage getCurrentUmailPage() {
        return currentUmailPage;
    }

    void setCurrentUmailPage(UmailPage currentUmailPage) {
        this.currentUmailPage = currentUmailPage;
    }

    String getOwnDiaryUrl() {
        return ownDiaryUrl;
    }

    public String getOwnFavoritesPageUrl() {
        return ownFavoritesPageUrl;
    }

    /**
     * Также устанавливает ссылку на избранное
     * @param ownDiaryUrl ссылка на дневник владельца
     */
    void setOwnDiaryUrl(String ownDiaryUrl) {
        this.ownDiaryUrl = ownDiaryUrl;
        this.ownFavoritesPageUrl = ownDiaryUrl + "?favorite";
    }

    String getOwnProfileId() {
        return ownProfileId;
    }

    void setOwnProfileId(String ownProfileId) {
        this.ownProfileId = ownProfileId;
    }

    String getUserName() {
        return userName;
    }

    void setUserName(String userName) {
        this.userName = userName;
    }

    String getSignature() {
        return signature;
    }

    void setSignature(String signature) {
        this.signature = signature;
    }

    Integer getNewDiscussNum() {
        return newDiscussNum;
    }

    void setNewDiscussNum(Integer newDiscussNum) {
        this.newDiscussNum = newDiscussNum;
    }

    String getNewDiscussLink() {
        return newDiscussLink;
    }

    void setNewDiscussLink(String newDiscussLink) {
        this.newDiscussLink = newDiscussLink;
    }

    Integer getNewUmailNum() {
        return newUmailNum;
    }

    void setNewUmailNum(Integer newUmailNum) {
        this.newUmailNum = newUmailNum;
    }

    String getNewUmailLink() {
        return newUmailLink;
    }

    void setNewUmailLink(String newUmailLink) {
        this.newUmailLink = newUmailLink;
    }

    Integer getNewDiaryCommentsNum() {
        return newDiaryCommentsNum;
    }

    void setNewDiaryCommentsNum(Integer newDiaryCommentsNum) {
        this.newDiaryCommentsNum = newDiaryCommentsNum;
    }

    String getNewDiaryLink() {
        return newDiaryLink;
    }

    void setNewDiaryLink(String newDiaryLink) {
        this.newDiaryLink = newDiaryLink;
    }

    public String getMostRecentNotification() {
        if (newDiscussNum > 0) {
            return newDiscussLink;
        } else if (newDiaryCommentsNum > 0) {
            return newDiaryLink;
        } /* else if (newUmailNum > 0)
            return newUmailLink; */

        return null;
    }
    
    public boolean hasNotifications() {
        return  getNewDiaryCommentsNum() + getNewDiscussNum() + getNewUmailNum() > 0;
    }
}