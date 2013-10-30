package adonai.diary_browser;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TabWidget;
import android.widget.TextView;

import adonai.diary_browser.entities.DiaryListArrayAdapter;
import adonai.diary_browser.entities.ListPage;
import adonai.diary_browser.entities.UmailPage;
import adonai.diary_browser.preferences.PreferencesScreen;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public class UmailListActivity extends DiaryActivity implements OnClickListener
{
    static final String inFolderAddress = "http://www.diary.ru/u-mail/folder/?f_id=1";
    static final String outFolderAddress = "http://www.diary.ru/u-mail/folder/?f_id=2";

    static final int TAB_INCOMING                                   =   0;
    static final int TAB_OUTCOMING                                  =   1;

    static final int PART_WEB = 0;
    static final int PART_LIST = 1;

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

        mainPane = (UmailListFragment) getSupportFragmentManager().findFragmentById(R.id.main_pane);
        messagePane = (MessageSenderFragment) getSupportFragmentManager().findFragmentById(R.id.message_pane);

        initializeUI(mainPane.getView());
    }

    public void initializeUI(View main)
    {
        mPageBrowser = (DiaryWebView) main.findViewById(R.id.page_browser);
        mPageBrowser.setDefaultSettings();
        registerForContextMenu(mPageBrowser);
        mPullToRefreshAttacher.addRefreshableView(mPageBrowser, mPageBrowser.refresher);

        mFolderBrowser = (ListView) main.findViewById(R.id.ufolder_browser);

        // Механизм удаления U-Mail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mFolderBrowser.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener()
            {
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
                {
                    mode.setSubtitle(getString(R.string.selected) + mFolderBrowser.getCheckedItemCount());
                    if (checked)
                        mFolderAdapter.addSelection(id);
                    else
                        mFolderAdapter.removeSelection(id);
                    mFolderBrowser.invalidateViews();
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu)
                {
                    mode.setTitle(R.string.select_items);
                    menu.add(0, 1, 100, R.string.delete_umails).setIcon(android.R.drawable.ic_menu_delete);
                    mFolderBrowser.setLongClickable(false);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu)
                {
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item)
                {
                    handleBackground(Utils.HANDLE_DELETE_UMAILS, new Pair<long[], Integer>(mFolderBrowser.getCheckedItemIds(), mUser.currentUmails.getURL().equals(inFolderAddress) ? 1 : 2));
                    mode.finish();
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode)
                {
                    mFolderBrowser.clearChoices();
                    mFolderAdapter.clearSelections();
                    mFolderBrowser.invalidateViews();
                    mFolderBrowser.setLongClickable(true);
                }
            });

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
                handleBackground(Utils.HANDLE_OPEN_FOLDER, mUser.currentUmails.getURL());
            }
        });

        mTabs = (TabWidget) main.findViewById(R.id.folder_selector);

        mIncoming = (TextView) main.findViewById(R.id.incoming);
        mOutcoming = (TextView) main.findViewById(R.id.outcoming);
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
        if(slider.isOpen())
            slider.closePane();
        else if(mainPane.mCurrentComponent == PART_WEB)
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
            case Utils.HANDLE_DELETE_UMAILS:
                handleBackground(Utils.HANDLE_OPEN_FOLDER, mUser.currentUmails.getURL());
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
                mPullToRefreshAttacher.setRefreshComplete();

                supportInvalidateOptionsMenu();
                break;
            case Utils.HANDLE_OPEN_MAIL:
                setCurrentVisibleComponent(PART_WEB);
                mPageBrowser.loadDataWithBaseURL(mUser.currentUmailPage.getPageURL(), mUser.currentUmailPage.getContent().html(), null, "utf-8", mUser.currentUmailPage.getPageURL());
                setTitle(mUser.currentUmailPage.getContent().title());
                mPullToRefreshAttacher.setRefreshComplete();

                supportInvalidateOptionsMenu();
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
        slider.closePane();
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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_new_umail:
                newUmail(null);
                return true;
            case R.id.menu_reply_umail:
                newUmail((UmailPage)mUser.currentUmailPage);
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

    private void newUmail(UmailPage receiver)
    {
        if(receiver != null)
            messagePane.prepareFragment(mUser.signature, "http://www.diary.ru/diary.php", "umailTo", receiver.getSenderName(), receiver.getMessageTheme());
        else
            messagePane.prepareFragment(mUser.signature, "http://www.diary.ru/diary.php", "umailTo", "", "");
        slider.openPane();
    }

    private void setCurrentVisibleComponent(int needed)
    {   
        mFolderBrowser.setVisibility(needed == PART_LIST ? View.VISIBLE : View.GONE);
        mPageBrowser.setVisibility(needed == PART_WEB ? View.VISIBLE : View.GONE);
        mainPane.mCurrentComponent = needed;
    }
}
