package adonai.diary_browser;

import adonai.diary_browser.tags.MoreTag;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

/**
 * 
 * @author Adonai
 * Класс - содержимое текстовых форм поста в списках постов/комментариев
 * Служит промежуточным звеном между парсингом HTML и выводом на экран
 * Используется при построении графического интерфейса и обработке тэгов
 * @see adonai.diary_browser.DiaryList#mPostListAdapter
 */
public class PostContentBuilder extends SpannableStringBuilder
{
    private PostContentBuilder parent = null;
    private MoreTag moresText = null;

    public PostContentBuilder(Spanned fromHtml)
    {
        super(fromHtml);
    }
    
    public PostContentBuilder(Spanned fromHtml, MoreTag moresText)
    {
        super(fromHtml);
        if(moresText != null)
            this.moresText = moresText;
    }
    
    public Spanned moresPop()
    {
        if(moresText != null && !moresText.isEmpty())
            return moresText.remove(0);
        
        return null;
    }
    
    public MoreTag getMore()
    {
        return moresText;
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
