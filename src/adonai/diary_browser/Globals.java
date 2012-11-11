package adonai.diary_browser;

import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;

public class Globals 
{
	public static BitmapDrawable tempDrawable = null;
	public static SharedPreferences mSharedPrefs = null;
	public static UserData mUser = new UserData();
	public static DiaryHttpClient mDHCL = new DiaryHttpClient();
	public static String currentURL = "";
	public static DiaryList mMain = null;
}
