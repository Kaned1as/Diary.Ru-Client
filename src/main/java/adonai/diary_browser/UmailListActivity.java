package adonai.diary_browser;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TabWidget;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import adonai.diary_browser.entities.ListPage;
import adonai.diary_browser.entities.Umail;
import adonai.diary_browser.entities.UmailListArrayAdapter;
import adonai.diary_browser.entities.UmailListPage;
import adonai.diary_browser.entities.UmailPage;
import adonai.diary_browser.entities.WebPage;
import adonai.diary_browser.preferences.PreferencePage;

/**
 * Активность, отвечающая за работу с механизмом U-mail.
 * <br/>
 * Возможности:
 * <ul>
 *     <li>Чтение писем</li>
 *     <li>Посылка писем</li>
 *     <li>Ответ на письма с цитированием</li>
 *     <li>Пересылка писем</li>
 *     <li>Автоотслеживание счётчика Re[*] в переписке (см {@link MessageSenderFragment#prepareUi(Umail)})</li>
 * </ul>
 * 
 * @author Адонай
 * @see MessageSenderFragment#umailElements
 */
public class UmailListActivity extends DiaryActivity implements OnClickListener {
    
    static final String IN_FOLDER_ADDRESS   = "http://www.diary.ru/u-mail/folder/?f_id=1";
    static final String OUT_FOLDER_ADDRESS  = "http://www.diary.ru/u-mail/folder/?f_id=2";

    static final int TAB_INCOMING           = 0;
    static final int TAB_OUTCOMING          = 1;

    static final int PART_WEB               = 0;
    static final int PART_LIST              = 1;

    ListView mFolderBrowser;
    UmailListArrayAdapter mFolderAdapter;
    TabWidget mTabs;
    TextView mIncoming; // TODO: реализовать работу с другими папками U-mail ( добавленные вручную и т.д.)
    TextView mOutcoming;

    Handler mUiHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler(this);
        setContentView(R.layout.activity_umail);

        mainPane = (UmailListFragment) getSupportFragmentManager().findFragmentById(R.id.main_pane);
        messagePane = (MessageSenderFragment) getSupportFragmentManager().findFragmentById(R.id.message_pane);

        initializeUI(mainPane.getView());
    }

    public void initializeUI(View main) {
        mPageBrowser = (DiaryWebView) main.findViewById(R.id.page_browser);
        mPageBrowser.setDefaultSettings();
        registerForContextMenu(mPageBrowser);
        swipeList = (SwipeRefreshLayout) main.findViewById(R.id.refresher_layout_list);
        swipeList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handleBackground(Utils.HANDLE_OPEN_FOLDER, getUser().getCurrentUmails().getURL());
                swipeList.setRefreshing(false);
            }
        });

        swipeBrowser = (SwipeRefreshLayout) main.findViewById(R.id.refresher_layout_browser);
        swipeBrowser.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handleBackground(Utils.HANDLE_OPEN_MAIL, getUser().getCurrentUmailPage().getPageURL());
                swipeBrowser.setRefreshing(false);
            }
        });

        mFolderBrowser = (ListView) main.findViewById(R.id.ufolder_browser);
        mFolderBrowser.setMultiChoiceModeListener(new UmailDeleteChoiceListener());
        mFolderBrowser.setOnItemClickListener(new UmailItemClickListener());
        //mFolderBrowser.setOnItemLongClickListener(new UmailItemLongClickListener());

        mTabs = (TabWidget) main.findViewById(R.id.folder_selector);
        for (int i = 0; i < mTabs.getChildCount(); i++) {
            Button current = (Button) mTabs.getChildAt(i);

            Drawable drawable = current.getBackground().mutate();
            drawable.setAlpha(50);
            current.setBackgroundDrawable(drawable);

            current.setOnClickListener(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        pageToLoad = intent.getStringExtra("url");
    }

    @Override
    protected void onDestroy() {
        mService.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (slider.isOpen())
            slider.closePane();
        else if (mainPane.mCurrentComponent == PART_WEB)
            setCurrentVisibleComponent(PART_LIST);
        else {
            Intent returnIntent = new Intent(getApplicationContext(), DiaryListActivity.class);
            returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(returnIntent);
            finish();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case Utils.HANDLE_START:
                mService.addListener(this);

                if (pageToLoad != null)
                    handleBackground(Utils.HANDLE_OPEN_FOLDER, pageToLoad);
                else if (mFolderAdapter == null) // стартуем в первый раз
                    handleBackground(Utils.HANDLE_OPEN_FOLDER, IN_FOLDER_ADDRESS);
                return true;
            case Utils.HANDLE_DELETE_UMAILS:
                // перед пересозданием индикатора загрузки его нужно уничтожить
                if(pd.isShowing()) {
                    pd.dismiss();
                }
                handleBackground(Utils.HANDLE_OPEN_FOLDER, getUser().getCurrentUmails().getURL());
                return true;
            case Utils.HANDLE_OPEN_FOLDER:
                setCurrentVisibleComponent(PART_LIST);
                mFolderAdapter = new UmailListArrayAdapter(UmailListActivity.this, android.R.layout.simple_list_item_1, getUser().getCurrentUmails());
                mFolderBrowser.removeFooterView(mFolderBrowser.findViewWithTag("footer"));
                if (getUser().getCurrentUmails().getPageLinks() != null) {
                    LinearLayout LL = new LinearLayout(mFolderBrowser.getContext());
                    LL.setTag("footer");
                    Spanned pageLinks = getUser().getCurrentUmails().getPageLinks();
                    URLSpan[] URLs = pageLinks.getSpans(0, pageLinks.length(), URLSpan.class);
                    for (URLSpan url : URLs) {
                        Button click = new Button(LL.getContext());
                        click.setMaxLines(1);
                        click.setText(pageLinks.subSequence(pageLinks.getSpanStart(url), pageLinks.getSpanEnd(url)).toString());
                        click.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        click.setTag(R.integer.button_url, url.getURL());
                        click.setOnClickListener(UmailListActivity.this);
                        LL.addView(click);

                        LayoutParams LP = (LayoutParams) click.getLayoutParams();
                        LP.width = LayoutParams.MATCH_PARENT;
                        LP.weight = 1.0f;
                    }
                    mFolderBrowser.addFooterView(LL);
                }
                mFolderBrowser.setAdapter(mFolderAdapter);

                supportInvalidateOptionsMenu();
                break;
            case Utils.HANDLE_OPEN_MAIL:
                setCurrentVisibleComponent(PART_WEB);
                mPageBrowser.loadDataWithBaseURL(getUser().getCurrentUmailPage().getPageURL(), getUser().getCurrentUmailPage().getContent(), null, "utf-8", getUser().getCurrentUmailPage().getPageURL());
                
                // смена заголовка
                WebPage page = getUser().getCurrentUmailPage();
                getSupportActionBar().setTitle(page.getTitle());
                getSupportActionBar().setSubtitle(page.getSubtitle());

                supportInvalidateOptionsMenu();
                break;
            case Utils.HANDLE_PRELOAD_UMAIL:
                newUmail((Umail) message.obj);
                break;
            case Utils.HANDLE_PROGRESS:
                if (pd != null)
                    pd.setContent(getString(R.string.parsing_data));
                return true;
        }

        return super.handleMessage(message);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.incoming:
                mTabs.setCurrentTab(TAB_INCOMING);
                handleBackground(Utils.HANDLE_OPEN_FOLDER, IN_FOLDER_ADDRESS);
                return;
            case R.id.outgoing:
                mTabs.setCurrentTab(TAB_OUTCOMING);
                handleBackground(Utils.HANDLE_OPEN_FOLDER, OUT_FOLDER_ADDRESS);
                return;
        }

        if (view instanceof Button) // нижние панельки
        {
            handleBackground(Utils.HANDLE_OPEN_FOLDER, view.getTag(R.integer.button_url));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_umail:
                newUmail((UmailPage) null);
                return true;
            case R.id.menu_reply_umail:
                if (mService.mPreloadUmails)
                    handleBackground(Utils.HANDLE_PRELOAD_UMAIL, Utils.UMAIL_REPLY);
                else
                    newUmail(getUser().getCurrentUmailPage());
                return true;
            case R.id.menu_forward_umail:
                if (mService.mPreloadUmails)
                    handleBackground(Utils.HANDLE_PRELOAD_UMAIL, Utils.UMAIL_FORWARD);
                else
                    newUmail(getUser().getCurrentUmailPage());
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencePage.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void newUmail(UmailPage receiver) {
        Umail mail = new Umail();
        if (receiver != null) {
            mail.receiver = receiver.getSenderName();
            mail.messageTheme = receiver.getMessageTheme();
        }
        messagePane.prepareFragment(getUser().getSignature(), mail);
        slider.openPane();
    }

    private void newUmail(Umail composed) {
        messagePane.prepareFragment(getUser().getSignature(), composed);
        slider.openPane();
    }

    private void setCurrentVisibleComponent(int needed) {
        swipeList.setVisibility(needed == PART_LIST ? View.VISIBLE : View.GONE);
        swipeBrowser.setVisibility(needed == PART_WEB ? View.VISIBLE : View.GONE);
        mainPane.mCurrentComponent = needed;
    }

    private class UmailDeleteChoiceListener implements AbsListView.MultiChoiceModeListener {
        
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mode.setSubtitle(getString(R.string.selected) + mFolderBrowser.getCheckedItemCount());
            if (checked)
                mFolderAdapter.addSelection(id);
            else
                mFolderAdapter.removeSelection(id);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(R.string.select_umails);
            menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.delete_umails).setIcon(android.R.drawable.ic_menu_delete);
            mFolderBrowser.setLongClickable(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            handleBackground(Utils.HANDLE_DELETE_UMAILS, new Pair<>(mFolderBrowser.getCheckedItemIds(), getUser().getCurrentUmails().getURL().equals(IN_FOLDER_ADDRESS) ? 1 : 2));
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mFolderBrowser.clearChoices();
            mFolderAdapter.clearSelections();
            mFolderBrowser.invalidateViews();
            mFolderBrowser.setLongClickable(true);
        }
    }

    private class UmailItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ListPage uMail = (ListPage) mFolderBrowser.getAdapter().getItem(position);
            handleBackground(Utils.HANDLE_OPEN_MAIL, uMail.getURL());
        }
    }
}
