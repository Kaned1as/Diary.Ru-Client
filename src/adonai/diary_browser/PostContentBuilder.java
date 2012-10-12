package adonai.diary_browser;

import java.util.ArrayList;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class PostContentBuilder extends SpannableStringBuilder
{
    private ArrayList<Spanned> moresText = null;

    public PostContentBuilder(Spanned fromHtml)
    {
        super(fromHtml);
    }
    
    public PostContentBuilder(Spanned fromHtml, ArrayList<Spanned> moresText)
    {
        super(fromHtml);
        if(moresText != null)
            this.moresText = moresText;
    }
    
    public Spanned moresPop()
    {
        if(!moresText.isEmpty())
            return moresText.remove(0);
        return null;
    }
}
