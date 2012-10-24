package adonai.diary_browser;

public class Utils 
{
	public static int checkDiaryUrl(String response)
	{
		if(response.contains("commentsArea"))
			return DiaryList.COMMENT_LIST;
		
		if(response.contains("postsArea"))
			return DiaryList.POST_LIST;
		
		if(response.contains("table r"))
			return DiaryList.DIARY_LIST;
		
		return -1;
	}
}
