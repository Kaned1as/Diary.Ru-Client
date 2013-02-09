package adonai.diary_browser;

import java.util.Map;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

import adonai.diary_browser.entities.DiaryListArrayAdapter;
import adonai.diary_browser.entities.Openable;
import adonai.diary_browser.entities.UmailPage;
import adonai.diary_browser.preferences.PreferencesScreen;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class UmailList extends DiaryActivity implements OnClickListener
{
    private final String inFolderAddress = "http://www.diary.ru/u-mail/folder/?f_id=1";
    private final String outFolderAddress = "http://www.diary.ru/u-mail/folder/?f_id=2";
    
	static final int TAB_INCOMING                                   =   0;
	static final int TAB_OUTCOMING                                  =   1;
	
    private static final int PART_WEB = 0;
    private static final int PART_LIST = 1;
    
    private int mCurrentComponent = 1;
	
    Map<String, String> namesUrls;
    
    DiaryWebView mMessageBrowser;
    PullToRefreshListView mFolderBrowser;
    DiaryListArrayAdapter mFolderAdapter;
    TabWidget mTabs;
    TextView mIncoming;
    TextView mOutcoming;
    
    Handler mUiHandler;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler(this);
        
        setContentView(R.layout.umail_list_a);
        mMessageBrowser = (DiaryWebView) findViewById(R.id.umessage_browser);
        mMessageBrowser.setDefaultSettings();
        mFolderBrowser = (PullToRefreshListView) findViewById(R.id.ufolder_browser);
        mTabs = (TabWidget) findViewById(R.id.folder_selector);
        
        mIncoming = (TextView) findViewById(R.id.incoming);
        mOutcoming = (TextView) findViewById(R.id.outcoming);
        mIncoming.setOnClickListener(this);
        mOutcoming.setOnClickListener(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }
    
    @Override
    protected void onNewIntent(Intent intent) 
    {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }
    
    @Override
	protected void onDestroy()
	{
		mService.removeListener(this);
		super.onDestroy();
	}

    @Override
    public void onBackPressed()
    {
        if(mCurrentComponent == PART_WEB)
            setCurrentVisibleComponent(PART_LIST);
        else
        {
            Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
            returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(returnIntent);
            finish();
        }
    }
    
    @Override
    public boolean handleMessage(Message message)
    {
        switch (message.what)
        {
        	case Utils.HANDLE_START:
                mService.addListener(this);
                
                if(getIntent() != null && getIntent().getStringExtra("url") != null)
                {
                    handleBackground(Utils.HANDLE_OPEN_FOLDER, getIntent().getStringExtra("url"));
                }
                else // стартуем в первый раз
                    handleBackground(Utils.HANDLE_OPEN_FOLDER, inFolderAddress);
        		return true;
        	case Utils.HANDLE_OPEN_FOLDER:
        	    setCurrentVisibleComponent(PART_LIST);
        		mFolderAdapter = new DiaryListArrayAdapter(UmailList.this, android.R.layout.simple_list_item_1, mUser.currentUmails);
        		mFolderBrowser.getRefreshableView().removeFooterView(mFolderBrowser.getRefreshableView().findViewWithTag("footer"));
                if(mUser.currentUmails.getPageLinks() != null)
                {
                    LinearLayout LL = new LinearLayout(mFolderBrowser.getRefreshableView().getContext());
                    LL.setTag("footer");
                    Spanned pageLinks = mUser.currentUmails.getPageLinks();
                    URLSpan[] URLs = pageLinks.getSpans(0, pageLinks.length(), URLSpan.class);
                    for(URLSpan url : URLs)
                    {
                        Button click = new Button(LL.getContext());
                        click.setMaxLines(1);
                        click.setText(pageLinks.subSequence(pageLinks.getSpanStart(url), pageLinks.getSpanEnd(url)));
                        click.setTag(url.getURL());
                        click.setOnClickListener(UmailList.this);
                        LL.addView(click);
                        
                        LayoutParams LP = (LayoutParams) click.getLayoutParams();
                        LP.width = LayoutParams.MATCH_PARENT;
                        LP.weight = 1.0f;
                    }
                    mFolderBrowser.getRefreshableView().addFooterView(LL);
                }
        		mFolderBrowser.setAdapter(mFolderAdapter);
        		pd.dismiss();
        		return true;
        	case Utils.HANDLE_OPEN_MAIL:
        	    setCurrentVisibleComponent(PART_WEB);
        	    mMessageBrowser.getRefreshableView().loadDataWithBaseURL(mUser.currentUmailPage.getPageURL(), mUser.currentUmailPage.getContent().html(), null, "utf-8", mUser.currentUmailPage.getPageURL());
                setTitle(mUser.currentUmailPage.getContent().title());
                mMessageBrowser.onRefreshComplete();
                pd.dismiss();
                return true;
            case Utils.HANDLE_PROGRESS:
                if(pd != null && pd.isShowing())
                    pd.setMessage(getString(R.string.parsing_data));
                return true;
            default:
            	super.handleMessage(message);
                /*pd.dismiss();
                if((message.what & Utils.DIARY_HANDLERS_MASK) != 0 && message.obj instanceof Pair) // Если это команда для другой активности
                {
                    if(((Pair<?, ?>)message.obj).first instanceof String) // Если это запрос на страничку
                    {
                        Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
                        returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        returnIntent.putExtra("url", ((Pair<?, ?>)message.obj).first.toString());
                        startActivity(returnIntent);
                        finish();
                    }
                }*/
                return false;
        }
    }
    
	public void onClick(View view)
	{
		switch (view.getId())
		{
		case R.id.incoming:
			mTabs.setCurrentTab(TAB_INCOMING);
			handleBackground(Utils.HANDLE_OPEN_FOLDER, inFolderAddress);
			break;
		case R.id.outcoming:
			mTabs.setCurrentTab(TAB_OUTCOMING);
			handleBackground(Utils.HANDLE_OPEN_FOLDER, outFolderAddress);
			break;
		case R.id.title:
		    int pos = mFolderBrowser.getRefreshableView().getPositionForView((View) view.getParent());
            Openable uMail = (Openable) mFolderBrowser.getRefreshableView().getAdapter().getItem(pos);
            
		    handleBackground(Utils.HANDLE_OPEN_MAIL, uMail.getURL());
		    break;
		}
		
		if(view instanceof Button) // нижние панельки
        {
		    handleBackground(Utils.HANDLE_OPEN_FOLDER, view.getTag());
        }
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.umail_list_a, menu);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
        // Только если это письмо из папки входящих
        if(mCurrentComponent == PART_WEB && mUser.currentUmails.getURL().equals(inFolderAddress)) // Если мы в папке "входящие"
        {
            menu.findItem(R.id.menu_reply_umail).setVisible(true);
        }
        else
        {
            menu.findItem(R.id.menu_reply_umail).setVisible(false);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_new_umail:
                newUmail(null);
                return true;
            case R.id.menu_reply_umail:
                newUmail(((UmailPage)mUser.currentUmailPage).getSender_Name());
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencesScreen.class));
                return true;
            case R.id.menu_about:
                ContextThemeWrapper ctw = new ContextThemeWrapper(this, android.R.style.Theme_Black);
                AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
                builder.setTitle(R.string.about);
                View aboutContent = View.inflate(ctw, R.layout.about_d, null);
                TextView author = (TextView) aboutContent.findViewById(R.id.author_info);
                author.setText(Html.fromHtml(getString(R.string.author_description)));
                author.setMovementMethod(LinkMovementMethod.getInstance());
                TextView app = (TextView) aboutContent.findViewById(R.id.app_info);
                app.setText(Html.fromHtml(getString(R.string.application_description)));
                app.setMovementMethod(LinkMovementMethod.getInstance());
                builder.setView(aboutContent);
                builder.create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	private void newUmail(String receiver)
    {
	    Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
        
        postIntent.putExtra("TypeId", "umailTo");
        postIntent.putExtra("umailTo", receiver);
        
        postIntent.putExtra("signature", mUser.signature);
        postIntent.putExtra("sendURL", "http://www.diary.ru/diary.php");
        
        startActivity(postIntent);
    }

    private void setCurrentVisibleComponent(int needed)
    {   
	    mFolderBrowser.setVisibility(needed == PART_LIST ? View.VISIBLE : View.GONE);
	    mMessageBrowser.setVisibility(needed == PART_WEB ? View.VISIBLE : View.GONE);
	    mCurrentComponent = needed;
    }
}
