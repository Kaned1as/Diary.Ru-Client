package adonai.diary_browser;

import adonai.diary_browser.entities.DiaryListArrayAdapter;
import adonai.diary_browser.entities.ListPage;
import adonai.diary_browser.entities.UmailPage;
import adonai.diary_browser.preferences.PreferencesScreen;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TabWidget;
import android.widget.TextView;

public class UmailListActivity extends DiaryActivity implements OnClickListener
{
    private final String inFolderAddress = "http://www.diary.ru/u-mail/folder/?f_id=1";
    private final String outFolderAddress = "http://www.diary.ru/u-mail/folder/?f_id=2";

    static final int TAB_INCOMING                                   =   0;
    static final int TAB_OUTCOMING                                  =   1;

    private static final int PART_WEB = 0;
    private static final int PART_LIST = 1;

    private int mCurrentComponent = 1;

    ListView mFolderBrowser;
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

        setContentView(R.layout.activity_umail);

        mPageBrowser = (DiaryWebView) findViewById(R.id.page_browser);
        mPageBrowser.setDefaultSettings();
        registerForContextMenu(mPageBrowser);
        mPullToRefreshAttacher.addRefreshableView(mPageBrowser, mPageBrowser.refresher);

        mFolderBrowser = (ListView) findViewById(R.id.ufolder_browser);
        mFolderBrowser.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                ListPage uMail = (ListPage) mFolderBrowser.getAdapter().getItem(position);
                handleBackground(Utils.HANDLE_OPEN_MAIL, uMail.getURL());
            }
        });
        mPullToRefreshAttacher.addRefreshableView(mFolderBrowser, new PullToRefreshAttacher.OnRefreshListener()
        {
            @Override
            public void onRefreshStarted(View view)
            {
                handleBackground(Utils.HANDLE_OPEN_FOLDER, inFolderAddress);
            }
        });

        mTabs = (TabWidget) findViewById(R.id.folder_selector);

        mIncoming = (TextView) findViewById(R.id.incoming);
        mOutcoming = (TextView) findViewById(R.id.outcoming);
        mIncoming.setOnClickListener(this);
        mOutcoming.setOnClickListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) 
    {
        super.onNewIntent(intent);
        pageToLoad = intent.getStringExtra("url");
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
            Intent returnIntent = new Intent(getApplicationContext(), DiaryListActivity.class);
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

                if(pageToLoad != null)
                    handleBackground(Utils.HANDLE_OPEN_FOLDER, pageToLoad);
                else if(mFolderAdapter == null) // стартуем в первый раз
                    handleBackground(Utils.HANDLE_OPEN_FOLDER, inFolderAddress);
                return true;
            case Utils.HANDLE_OPEN_FOLDER:
                setCurrentVisibleComponent(PART_LIST);
                mFolderAdapter = new DiaryListArrayAdapter(UmailListActivity.this, android.R.layout.simple_list_item_1, mUser.currentUmails);
                mFolderBrowser.removeFooterView(mFolderBrowser.findViewWithTag("footer"));
                if(mUser.currentUmails.getPageLinks() != null)
                {
                    LinearLayout LL = new LinearLayout(mFolderBrowser.getContext());
                    LL.setTag("footer");
                    Spanned pageLinks = mUser.currentUmails.getPageLinks();
                    URLSpan[] URLs = pageLinks.getSpans(0, pageLinks.length(), URLSpan.class);
                    for(URLSpan url : URLs)
                    {
                        Button click = new Button(LL.getContext());
                        click.setMaxLines(1);
                        click.setText(pageLinks.subSequence(pageLinks.getSpanStart(url), pageLinks.getSpanEnd(url)));
                        click.setTag(url.getURL());
                        click.setOnClickListener(UmailListActivity.this);
                        LL.addView(click);

                        LayoutParams LP = (LayoutParams) click.getLayoutParams();
                        LP.width = LayoutParams.MATCH_PARENT;
                        LP.weight = 1.0f;
                    }
                    mFolderBrowser.addFooterView(LL);
                }
                mFolderBrowser.setAdapter(mFolderAdapter);
                break;
            case Utils.HANDLE_OPEN_MAIL:
                setCurrentVisibleComponent(PART_WEB);
                mPageBrowser.loadDataWithBaseURL(mUser.currentUmailPage.getPageURL(), mUser.currentUmailPage.getContent().html(), null, "utf-8", mUser.currentUmailPage.getPageURL());
                setTitle(mUser.currentUmailPage.getContent().title());
                mPullToRefreshAttacher.setRefreshComplete();
                break;
            case Utils.HANDLE_PROGRESS:
                if(pd != null)
                    pd.setMessage(getString(R.string.parsing_data));
                return true;
        }

        super.handleMessage(message);
        return true;
    }

    @Override
    protected void onFragmentRemove(boolean reload)
    {

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
        Intent postIntent = new Intent(getApplicationContext(), MessageSenderFragment.class);

        postIntent.putExtra("TypeId", "umailTo");
        postIntent.putExtra("umailTo", receiver);

        postIntent.putExtra("signature", mUser.signature);
        postIntent.putExtra("sendURL", "http://www.diary.ru/diary.php");

        startActivity(postIntent);
    }

    private void setCurrentVisibleComponent(int needed)
    {   
        mFolderBrowser.setVisibility(needed == PART_LIST ? View.VISIBLE : View.GONE);
        mPageBrowser.setVisibility(needed == PART_WEB ? View.VISIBLE : View.GONE);
        mCurrentComponent = needed;
    }
}
