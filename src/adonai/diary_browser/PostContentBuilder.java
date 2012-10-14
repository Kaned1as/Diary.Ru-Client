package adonai.diary_browser;

import java.util.ArrayList;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class PostContentBuilder extends SpannableStringBuilder
{
    private PostContentBuilder parent = null;
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

    public PostContentBuilder getParent()
    {
        return parent;
    }

    /* (non-Javadoc)
     * @see android.text.SpannableStringBuilder#insert(int, java.lang.CharSequence)
     */
    @Override
    public PostContentBuilder insert(int where, CharSequence tb)
    {
        if(tb instanceof PostContentBuilder)
            ((PostContentBuilder) tb).parent = this;
        
        return (PostContentBuilder)super.insert(where, tb);
    }
    
    public PostContentBuilder getRealContainer()
    {
        PostContentBuilder realContainer = this;
        while(realContainer.parent != null)
            realContainer = realContainer.parent;
        
        return realContainer;
    }
}
