package adonai.diary_browser;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.SelectArg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import adonai.diary_browser.database.DbProvider;
import adonai.diary_browser.entities.AutocompleteItem;
import adonai.diary_browser.pages.Comment;
import adonai.diary_browser.pages.CommentsPage;
import adonai.diary_browser.adapters.DiaryListArrayAdapter;
import adonai.diary_browser.pages.DiaryPage;
import adonai.diary_browser.adapters.DiscListArrayAdapter;
import adonai.diary_browser.pages.DiscPage;
import adonai.diary_browser.pages.DiscussionList;
import adonai.diary_browser.pages.ListPage;
import adonai.diary_browser.pages.Post;
import adonai.diary_browser.pages.WebPage;
import adonai.diary_browser.misc.ArrowDrawable;
import adonai.diary_browser.preferences.PreferencePage;

/**
 * Основная активность дайри-клиента. Здесь происходит всё взаимодействие с интерактивной веб-частью сайта.
 * <br/>
 * Возможности:
 * <ul>
 *     <li>Просмотр списка избранных дневников/списков ПЧ (напр. {@link #TAB_FAV_LIST})</li>
 *     <li>Просмотр ленты избранного ({@link #TAB_FAV_POSTS})</li>
 *     <li>Просмотр дискуссий ({@link #TAB_DISCUSSIONS})</li>
 *     <li>Просмотр дневников (напр. {@link #TAB_MY_DIARY})</li>
 *     <li>Отправка ({@link MessageSenderFragment#prepareUi(Post)}) и удаление ({@link DiaryWebView.DiaryWebClient#shouldOverrideUrlLoading(WebView, String)}) постов/комментариев</li>
 * </ul>
 *
 * @author Адонай
 */
public class DiaryListActivity extends DiaryActivity implements OnClickListener, OnChildClickListener, OnGroupClickListener, OnItemLongClickListener, View.OnLongClickListener {

    // вкладки приложения
    public static final int TAB_FAV_LIST        = 0;
    public static final int TAB_FAV_POSTS       = 1;
    public static final int TAB_MY_DIARY        = 2;
    public static final int TAB_DISCUSSIONS     = 3;

    public static final int PART_LIST                  = 0;
    public static final int PART_WEB                   = 1;
    public static final int PART_DISC_LIST             = 2;

    private BrowseHistory browserHistory;
    private int mCurrentTab = 0;

    // Адаптеры типов
    private DiaryListArrayAdapter mFavouritesAdapter;
    private DiscListArrayAdapter mDiscussionsAdapter;
    private TextView mLogin;
    private Button mDiscussNum;
    private Button mCommentsNum;
    private TextView mUmailNum;
    private ListView mDiaryBrowser;
    private ExpandableListView mDiscussionBrowser;
    private ImageButton mExitButton;
    private ImageButton mQuotesButton;
    private ImageButton mUmailButton;
    private LinearLayout mTabs;
    private Handler mUiHandler;

    private SwipeRefreshLayout swipeDiscussions;
    private ArrowDrawable mActionBarToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_main);

        mainPane = (DiaryListFragment) getSupportFragmentManager().findFragmentById(R.id.main_pane);
        messagePane = (MessageSenderFragment) getSupportFragmentManager().findFragmentById(R.id.message_pane);

        // Оповещаем остальных, что мы создались
        // Если был простой приложения
        browserHistory = new BrowseHistory();

        mUiHandler = new Handler(this);
        CookieSyncManager.createInstance(this);

        initializeUI(mainPane.getView());
    }

    public void initializeUI(View main) {
        // Настраиваем верхнюю панель
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mActionBarToggle = new ArrowDrawable(getSupportActionBar().getThemedContext());
        getSupportActionBar().setHomeAsUpIndicator(mActionBarToggle);

        swipeList = (SwipeRefreshLayout) main.findViewById(R.id.refresher_layout_list);
        swipeList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getCurrentDiaries().getURL(), true));
                swipeList.setRefreshing(false);
            }
        });

        swipeBrowser = (SwipeRefreshLayout) main.findViewById(R.id.refresher_layout_browser);
        swipeBrowser.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(mHttpClient.getCurrentUrl(), true));
                swipeBrowser.setRefreshing(false);
            }
        });
        swipeDiscussions = (SwipeRefreshLayout) main.findViewById(R.id.refresher_layout_discussions);
        swipeDiscussions.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getDiscussionsUrl(), true));
                swipeDiscussions.setRefreshing(false);
            }
        });

        mPageBrowser = (DiaryWebView) main.findViewById(R.id.page_browser);
        mPageBrowser.setDefaultSettings();
        mPageBrowser.setOnClickListener(this);
        mPageBrowser.setPositionTracker(browserHistory);
        registerForContextMenu(mPageBrowser);

        mLogin = (TextView) main.findViewById(R.id.login_name);

        mExitButton = (ImageButton) main.findViewById(R.id.exit_button);
        mExitButton.setOnClickListener(this);
        mQuotesButton = (ImageButton) main.findViewById(R.id.quotes_button);
        mQuotesButton.setOnClickListener(this);
        mUmailButton = (ImageButton) main.findViewById(R.id.umail_button);
        mUmailButton.setOnClickListener(this);

        mDiaryBrowser = (ListView) main.findViewById(R.id.diary_browser);
        mDiaryBrowser.setOnItemLongClickListener(this);
        mDiaryBrowser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListPage diary = (ListPage) mDiaryBrowser.getAdapter().getItem(position);
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(diary.getURL(), false));
            }
        });
        mDiscussionBrowser = (ExpandableListView) main.findViewById(R.id.discussion_browser);

        mTabs = (LinearLayout) main.findViewById(R.id.tabs);
        for (int i = 0; i < mTabs.getChildCount(); i++) {
            Button current = (Button) mTabs.getChildAt(i);

            Drawable drawable = current.getBackground().mutate();
            drawable.setAlpha(50);
            current.setBackgroundDrawable(drawable);

            current.setOnClickListener(this);
            current.setOnLongClickListener(this);
        }

        mCommentsNum = (Button) main.findViewById(R.id.diary_button);
        mDiscussNum = (Button) main.findViewById(R.id.discussions_button);

        mUmailNum = (TextView) main.findViewById(R.id.umail_counter);
        mUmailNum.setOnClickListener(this);

        mDiscussionBrowser.setOnChildClickListener(this);
        mDiscussionBrowser.setOnGroupClickListener(this);
        mDiscussionBrowser.setOnItemLongClickListener(this);

        setCurrentVisibleComponent(PART_LIST);
    }

    @Override
    protected void onDestroy() {
        mService.removeListener(this);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // swipeDiscussions не обрабатывается в DiaryActivity
        TypedValue color = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, color, true);
        swipeDiscussions.setColorSchemeColors(color.data);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (RuntimeException ex) {
            Log.e("Activity error", "Cannot restore activity state", ex);
        }
    }

    // старые телефоны тоже должны работать
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == DiaryListFragment.ITEM_PAGE_LINKS) {
            String url = ((DiaryPage) getUser().getCurrentDiaryPage()).userLinks.get(item.getTitle().toString());
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(url, false));
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                onSearchRequested();
                return true;
            case R.id.menu_new_post:
                if (mService.mPreloadThemes)
                    handleBackground(Utils.HANDLE_PRELOAD_THEMES, null);
                else
                    newPost("");
                return true;
            case R.id.menu_show_online_list:
                handleBackground(Utils.HANDLE_QUERY_ONLINE, null);
                return true;
            case R.id.menu_new_comment:
                newComment("");
                return true;
            case R.id.menu_purchase:
                purchaseGift();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencePage.class));
                return true;
            case R.id.menu_share:
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_TITLE, getUser().getCurrentDiaryPage().getTitle());
                sendIntent.putExtra(Intent.EXTRA_TEXT, getUser().getCurrentDiaryPage().getPageUrl());
                startActivity(Intent.createChooser(sendIntent, getString(R.string.menu_share)));
                return true;
            case R.id.copy_to_clipboard:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                Toast.makeText(getApplicationContext(), getString(R.string.copied) + " " + getUser().getCurrentDiaryPage().getPageUrl(), Toast.LENGTH_SHORT).show();
                clipboard.setPrimaryClip(ClipData.newPlainText(getUser().getCurrentDiaryPage().getTitle(), getUser().getCurrentDiaryPage().getPageUrl()));
                return true;
            case R.id.menu_subscr_list:
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getSubscribersUrl(), false));
                return true;
            case R.id.menu_refresh:
                reloadContent();
                return true;
            case R.id.menu_close_app:
                stopService(new Intent(this, NetworkService.class));
                finish();
                System.exit(0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent incoming = getIntent();

        // обработка открытия странички дайри в самих дайри
        if (incoming.hasExtra("url")) {
            pageToLoad = incoming.getStringExtra("url");
            incoming.removeExtra("url");
        } else if (incoming.getData() != null) {
            pageToLoad = getIntent().getDataString();
            incoming.setData(null);
        }

        // обработка передачи изображения/текста в дайри
        String action = incoming.getAction();
        String type = incoming.getType();
        if (TextUtils.equals(action, Intent.ACTION_SEND) && type != null) {
            if (TextUtils.equals(type, "text/plain")) {
                textToWrite = incoming.getStringExtra(Intent.EXTRA_TEXT);
            } else if (type.startsWith("image/")) {
                imageToUpload = incoming.getParcelableExtra(Intent.EXTRA_STREAM); // Handle single image being sent
            }
            incoming.setType(null);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case Utils.HANDLE_START:
                mService.addListener(this);
                handleAuthorize();
                return true;
            case Utils.HANDLE_PROGRESS:
                if (pd != null)
                    pd.setContent(getString(R.string.parsing_data));
                return true;
            case Utils.HANDLE_PROGRESS_2:
                if (pd != null)
                    pd.setContent(getString(R.string.sorting_data));
                return true;
            case Utils.HACKING_CLOUDFLARE:
                if (pd != null)
                    pd.setContent(getString(R.string.hacking_cloudflare));
                return true;
            case Utils.HANDLE_UPDATE_HEADERS:
                // обрабатываем обновление контента
                mLogin.setText(getUser().getUserName());
                if (getUser().getNewDiaryCommentsNum() != 0)
                    mCommentsNum.setText(getString(R.string.my_diary) + " - " + getUser().getNewDiaryCommentsNum().toString());
                else
                    mCommentsNum.setText(getString(R.string.my_diary));

                if (getUser().getNewDiscussNum() != 0)
                    mDiscussNum.setText(getString(R.string.discussions) + " - " + getUser().getNewDiscussNum());
                else
                    mDiscussNum.setText(getString(R.string.discussions));

                if (getUser().getNewUmailNum() != 0) {
                    mUmailNum.setText(getUser().getNewUmailNum().toString());
                    mUmailNum.setVisibility(View.VISIBLE);
                } else {
                    mUmailNum.setText("");
                    mUmailNum.setVisibility(View.GONE);
                }
                return true;
            case Utils.HANDLE_AUTHORIZE: // успешно авторизовались
                if(textToWrite != null || imageToUpload != null) { // если действие - "поделиться"
                    setCurrentTab(TAB_MY_DIARY, false);
                } else if (pageToLoad != null) { // если действие - "открыть с помощью"
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(pageToLoad, false));
                    pageToLoad = null;
                } else if (browserHistory.isEmpty()) { // запускаем в первый раз
                    int defaultTab = Integer.parseInt(mSharedPrefs.getString(NetworkService.SHARED_PROP_DEFAULT_TAB, "0"));
                    setCurrentTab(defaultTab, false);
                }
                return true;
            case Utils.HANDLE_GET_LIST_PAGE_DATA:
                setCurrentVisibleComponent(PART_LIST);
                mDiaryBrowser.setAdapter(null);
                mDiaryBrowser.removeFooterView(mDiaryBrowser.findViewWithTag("footer"));
                mFavouritesAdapter = new DiaryListArrayAdapter(DiaryListActivity.this, android.R.layout.simple_list_item_1, getUser().getCurrentDiaries());
                mDiaryBrowser.setAdapter(mFavouritesAdapter);
                if (getUser().getCurrentDiaries().getPageLinks() != null) {
                    LinearLayout LL = new LinearLayout(mDiaryBrowser.getContext());
                    LL.setTag("footer");
                    Spanned pageLinks = getUser().getCurrentDiaries().getPageLinks();
                    URLSpan[] URLs = pageLinks.getSpans(0, pageLinks.length(), URLSpan.class);
                    for (URLSpan url : URLs) {
                        Button click = new Button(LL.getContext());
                        click.setMaxLines(1);
                        click.setText(pageLinks.subSequence(pageLinks.getSpanStart(url), pageLinks.getSpanEnd(url)).toString());
                        click.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        click.setTag(R.integer.button_url, url.getURL());
                        click.setOnClickListener(DiaryListActivity.this);
                        LL.addView(click);

                        LayoutParams LP = (LayoutParams) click.getLayoutParams();
                        LP.width = 0;
                        LP.height = LayoutParams.MATCH_PARENT;
                        LP.weight = 1.0f;
                    }
                    mDiaryBrowser.addFooterView(LL);
                }
                browserHistory.add(getUser().getCurrentDiaries().getURL());
                handleTabChange(mHttpClient.getCurrentUrl());

                // На Андроиде > 2.3.3 нужно обновлять меню для верного отображения нужных для страниц кнопок
                supportInvalidateOptionsMenu(); // PART_LIST
                break;
            case Utils.HANDLE_GET_WEB_PAGE_DATA: // самое важное
                setCurrentVisibleComponent(PART_WEB);
                if (message.obj == null) { // это страница
                    String url = getUser().getCurrentDiaryPage().getPageUrl();
                    mPageBrowser.loadDataWithBaseURL(url,  getUser().getCurrentDiaryPage().getContent(),  null, "utf-8", url);

                    browserHistory.add(url);
                    handleTabChange(mHttpClient.getCurrentUrl());

                    // меняем заголовок приложения и подзаголовок, если есть
                    WebPage page = getUser().getCurrentDiaryPage();
                    getSupportActionBar().setTitle(page.getTitle());
                    getSupportActionBar().setSubtitle(page.getSubtitle());

                    if (getUser().getCurrentDiaryPage().getClass() == DiaryPage.class) {
                        persistAutocompleteInfo();
                    }
                } else { // это картинка
                    String src = (String) message.obj;
                    mPageBrowser.loadUrl(src);
                    browserHistory.add(src);
                }

                supportInvalidateOptionsMenu(); // PART_WEB
                break;
            case Utils.HANDLE_GET_DISCUSSIONS_DATA:
                setCurrentVisibleComponent(PART_DISC_LIST);
                mDiscussionsAdapter = new DiscListArrayAdapter(this, (DiscussionList) getUser().getCurrentDiaryPage());
                mDiscussionBrowser.setAdapter(mDiscussionsAdapter);

                browserHistory.add(getUser().getCurrentDiaryPage().getPageUrl());
                handleTabChange(mHttpClient.getCurrentUrl());

                swipeDiscussions.setRefreshing(false);
                supportInvalidateOptionsMenu(); // PART_DISC_LIST
                break;
            case Utils.HANDLE_AUTHORIZATION_ERROR:
                Toast.makeText(getApplicationContext(), getString(R.string.not_authorized), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
                finish();
                break;
            case Utils.HANDLE_GET_DISCUSSION_LIST_DATA:
                int pos = (Integer) message.obj;
                mDiscussionBrowser.expandGroup(pos);
                break;
            case Utils.HANDLE_NAME_CLICK: {
                String href = message.getData().getString("url");
                if (href != null && href.contains("#form")) {
                    String name = message.getData().getString("title");
                    String toPaste = "[L]" + name + "[/L], ";
                    handleMessagePaneAddText(toPaste);
                }
                return true;
            }
            case Utils.HANDLE_IMAGE_CLICK: {
                final String src = message.getData().getString("url");
                if (src == null) // нет картинки!
                    return false;

                List<String> itemsBuilder = new ArrayList<>();
                itemsBuilder.add(getString(R.string.image_save));
                itemsBuilder.add(getString(R.string.image_copy_url));
                itemsBuilder.add(getString(R.string.image_open_here));
                itemsBuilder.add(getString(R.string.image_open));

                new MaterialDialog.Builder(mPageBrowser.getContext())
                        .title(R.string.image_action)
                        .items(itemsBuilder)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                switch (i) {
                                    case DiaryWebView.IMAGE_SAVE: {
                                        Toast.makeText(DiaryListActivity.this, getString(R.string.loading), Toast.LENGTH_SHORT).show();
                                        mService.handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(src, true));
                                    }
                                    break;
                                    case DiaryWebView.IMAGE_COPY_URL: {
                                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        Toast.makeText(DiaryListActivity.this, getString(R.string.copied) + " " + src, Toast.LENGTH_SHORT).show();
                                        clipboard.setText(src);
                                    }
                                    break;
                                    case DiaryWebView.IMAGE_OPEN_HERE:  {
                                        Toast.makeText(DiaryListActivity.this, getString(R.string.loading), Toast.LENGTH_SHORT).show();
                                        mService.handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(src, false));
                                    }
                                    break;
                                    case DiaryWebView.IMAGE_OPEN_EXTERNAL:  {
                                        final Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(src));
                                        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)));
                                    }
                                    break;
                                }
                            }
                        }).show();
                break;
            }
            case Utils.HANDLE_EDIT_POST:
                Post sendPost = (Post) message.obj;
                editPost(sendPost);
                break;
            case Utils.HANDLE_EDIT_COMMENT:
                Comment sendComment = (Comment) message.obj;
                editComment(sendComment);
                break;
            case Utils.HANDLE_REPOST:
                Post repost = (Post) message.obj;
                newPost(repost);
                break;
            case Utils.HANDLE_PRELOAD_THEMES:
                Post newPost = (Post) message.obj;
                newPost(newPost);
                break;
            case Utils.HANDLE_QUERY_ONLINE:
                HashMap<Integer, Spanned> onliners = (HashMap<Integer, Spanned>) message.obj;
                if(onliners.isEmpty()) {
                    Toast.makeText(DiaryListActivity.this, getString(R.string.nobody_here), Toast.LENGTH_SHORT).show();
                    break;
                }
                View whosOnline = LayoutInflater.from(this).inflate(R.layout.whos_online_d, null);
                TextView favs = (TextView) whosOnline.findViewById(R.id.favs_online);
                favs.setMovementMethod(LinkMovementMethod.getInstance());
                if(onliners.containsKey(R.string.favourites_online)) {
                    favs.setText(onliners.get(R.string.favourites_online));
                }
                TextView subs = (TextView) whosOnline.findViewById(R.id.subs_online);
                favs.setMovementMethod(LinkMovementMethod.getInstance());
                if(onliners.containsKey(R.string.subscribers_online)) {
                    subs.setText(onliners.get(R.string.subscribers_online));
                }
                new MaterialDialog.Builder(this)
                        .title(R.string.whos_online)
                        .customView(whosOnline, false)
                        .show();
                break;
            case Utils.HANDLE_REQUEST_DIARY:
                Boolean result = (Boolean) message.obj;
                if(result) {
                    new MaterialDialog.Builder(this)
                        .title(R.string.diary_created)
                        .content(R.string.diary_created_congratulation)
                        .positiveText(android.R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                setCurrentTab(TAB_MY_DIARY, true);
                            }
                        }).show();
                }
                break;
        }

        super.handleMessage(message);
        return true;
    }

    // К сожалению, в OrmLite отсутствует ON CONFLICT REPLACE
    private void persistAutocompleteInfo() {
        RuntimeExceptionDao<AutocompleteItem, Long> dao = DbProvider.getHelper().getAutocompleteDao();
        String url = getUser().getCurrentDiaryPage().getPageUrl();
        String title = getUser().getCurrentDiaryPage().getTitle();

        SelectArg urlEscaped = new SelectArg(url);
        List<AutocompleteItem> found = dao.queryForEq("text", urlEscaped);
        AutocompleteItem ai = found.isEmpty() ? new AutocompleteItem() : found.get(0);
        ai.setType(AutocompleteItem.AutocompleteType.URL);
        ai.setText(url);
        ai.setTitle(title);
        DbProvider.getHelper().getAutocompleteDao().createOrUpdate(ai);
    }

    private void handleAuthorize() {
        if(getUser().isAuthorized()) { // уже авторизовывались, высылаем положительный ответ
            mUiHandler.sendEmptyMessage(Utils.HANDLE_AUTHORIZE);
        } else { // надо авторизоваться через сервис, посылаем запрос
            handleBackground(Utils.HANDLE_AUTHORIZE, null);
        }
    }

    public void handleMessagePaneAddText(String toPaste) {
        if(slider.isDouble()) { // слайдер уже открыт, просто добавляем текст
            String toSet = messagePane.getContentText() + toPaste;
            slider.openPane();
            messagePane.setContentText(toSet);
        } else { // слайдер закрыт, нужно начать пост/коммент
            if(getUser().getCurrentDiaryPage().getClass() == DiaryPage.class) {
                newPost(toPaste);
            } else if (getUser().getCurrentDiaryPage().getClass() == CommentsPage.class) {
                newComment(toPaste);
            }
        }
        messagePane.setSelection(messagePane.getContentText().length());
    }

    @Override
    protected void onMessagePaneRemove(boolean reload) {
        super.onMessagePaneRemove(reload);
        if (reload)
            reloadContent();
    }

    private void handleTabChange(String url) {
        // Обработка случая, когда URL страницы совпадает с URL одного из табов
        mTabs.getChildAt(mCurrentTab).getBackground().setAlpha(50);
        if (url.equals(getUser().getFavoritesUrl())) {
            getSupportActionBar().setTitle(R.string.title_activity_diary_list);
            getSupportActionBar().setSubtitle("");
            mTabs.getChildAt(mCurrentTab).setSelected(false);
            mCurrentTab = TAB_FAV_LIST;
            mTabs.getChildAt(mCurrentTab).setSelected(true);
        } else if (url.equals(getUser().getOwnFavoritesPageUrl())) {
            mTabs.getChildAt(mCurrentTab).setSelected(false);
            mCurrentTab = TAB_FAV_POSTS;
            mTabs.getChildAt(mCurrentTab).setSelected(true);
        } else if (url.equals(getUser().getOwnDiaryUrl()) || getUser().getNewDiaryLink().startsWith(url)) {
            mTabs.getChildAt(mCurrentTab).setSelected(false);
            mCurrentTab = TAB_MY_DIARY;
            mTabs.getChildAt(mCurrentTab).setSelected(true);

            // обработка текста, который был прислан интентом
            if(textToWrite != null) {
                String fullText = "<span class='quote_text'><blockquote>" + textToWrite + "</blockquote></span>";
                handleMessagePaneAddText(fullText);
                textToWrite = null;
            } else if(imageToUpload != null) { // обработка изображения, которое было прислано интентом
                handleMessagePaneAddText("");
                messagePane.requestFileUpload(imageToUpload);
                imageToUpload = null;
            }
        } else if (url.equals(getUser().getDiscussionsUrl()) || getUser().getNewDiscussLink().startsWith(url)) {
            getSupportActionBar().setTitle(R.string.discussions);
            getSupportActionBar().setSubtitle("");
            mTabs.getChildAt(mCurrentTab).setSelected(false);
            mCurrentTab = TAB_DISCUSSIONS;
            mTabs.getChildAt(mCurrentTab).setSelected(true);
        }
        mTabs.getChildAt(mCurrentTab).getBackground().setAlpha(150);
    }

    public void onClick(View view) {
        if (view == mExitButton) {
            new MaterialDialog.Builder(mPageBrowser.getContext())
                    .title(R.string.really_exit)
                    .positiveText(android.R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            CookieManager cookieManager = CookieManager.getInstance();
                            cookieManager.removeSessionCookie();
                            CookieSyncManager.getInstance().sync();
                            mService.newSession();

                            startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
                            finish();
                        }
                    })
                    .negativeText(android.R.string.no)
                    .show();
        } else if (view == mQuotesButton) {
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getOwnDiaryUrl() + "?quote", false));
        } else if (view == mUmailButton) {
            Intent postIntent = new Intent(getApplicationContext(), UmailListActivity.class);
            startActivity(postIntent);
        } else if (view == mUmailNum) {
            Intent postIntent = new Intent(getApplicationContext(), UmailListActivity.class);
            postIntent.putExtra("url", getUser().getNewUmailLink());
            startActivity(postIntent);
        } else if (view.getParent() == mTabs) {
            setCurrentTab(mTabs.indexOfChild(view), false);
        } else if (view.getTag(R.integer.button_url) != null) {
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>((String) view.getTag(R.integer.button_url), false));
        }
    }

    @Override
    public boolean onLongClick(View view) {
        // по долгому клику принудительно входим в дневник/дискуссии без читки новых сообщений
        if (view.getParent() == mTabs)
            setCurrentTab(mTabs.indexOfChild(view), true);

        return true;
    }

    // Загружаем дискуссии
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        String link = ((DiscPage.Discussion) parent.getExpandableListAdapter().getChild(groupPosition, childPosition)).url;
        handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(link, false));
        return true;
    }

    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        if (parent.isGroupExpanded(groupPosition)) {
            parent.collapseGroup(groupPosition);
            return true;
        }

        if (((DiscPage) parent.getExpandableListAdapter().getGroup(groupPosition)).getDiscussions().isEmpty()) {
            ArrayList<Object> params = new ArrayList<>();
            params.add(groupPosition);
            params.add(mDiscussionBrowser.getExpandableListAdapter().getGroup(groupPosition));
            params.add(false);
            handleBackground(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, params);
        } else
            parent.expandGroup(groupPosition);
        return true;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDiscussionBrowser) {
            ExpandableListView elv = (ExpandableListView) parent;
            if (ExpandableListView.getPackedPositionType(id) != ExpandableListView.PACKED_POSITION_TYPE_GROUP)
                return false;

            int groupPosition = ExpandableListView.getPackedPositionGroup(id);
            if (elv.isGroupExpanded(groupPosition)) {
                elv.collapseGroup(groupPosition);
                return true;
            }
            ArrayList<Object> params = new ArrayList<>();
            params.add(groupPosition);
            params.add(mDiscussionBrowser.getExpandableListAdapter().getGroup(groupPosition));
            params.add(true);
            handleBackground(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, params);
            return true;
        }

        if (parent == mDiaryBrowser) {
            ListPage diary = (ListPage) mDiaryBrowser.getAdapter().getItem(position);
            new MaterialDialog.Builder(this)
                    .content(diary.getPageHint())
                    .show();
        }

        return true;
    }

    /*            is.close();
     * (non-Javadoc) Sets the contents to current tab and hides everything other. In addition, refreshes content on
     * page, if needed.
     */
    private void setCurrentTab(int index, boolean force) {
        switch (index) {
            case TAB_FAV_LIST:
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getFavoritesUrl(), false));
                break;
            case TAB_FAV_POSTS:
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getOwnFavoritesPageUrl(), false));
                break;
            case TAB_MY_DIARY:
                // нет дневника, нужно завести!
                if(getUser().getOwnDiaryUrl().isEmpty()) {
                    showDiaryCreateRequest();
                    break;
                }

                if (getUser().getNewDiaryCommentsNum() != 0 && !force)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getNewDiaryLink(), true));
                else
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getOwnDiaryUrl(), false));
                break;
            case TAB_DISCUSSIONS:
                if (getUser().getNewDiscussNum() != 0 && !force)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getNewDiscussLink(), true));
                else
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getDiscussionsUrl(), false));
                break;
            default:
                Utils.showDevelSorry(this);
                break;
        }
    }

    private void showDiaryCreateRequest() {
        new MaterialDialog.Builder(this)
            .title(R.string.no_diary)
            .content(R.string.diary_create_question)
            .positiveText(android.R.string.ok)
            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
            .input(R.string.diary_title, R.string.empty, false, new MaterialDialog.InputCallback() {
                @Override
                public void onInput(@NonNull MaterialDialog materialDialog, CharSequence diaryName) {
                    handleBackground(Utils.HANDLE_REQUEST_DIARY, diaryName.toString());
                }
            }).show();
    }

    private void setCurrentVisibleComponent(int needed) {
        swipeList.setVisibility(needed == PART_LIST ? View.VISIBLE : View.GONE);
        swipeBrowser.setVisibility(needed == PART_WEB ? View.VISIBLE : View.GONE);
        //mAuthorBrowser.setVisibility(needed == AUTHOR_PAGE ? View.VISIBLE : View.GONE);
        swipeDiscussions.setVisibility(needed == PART_DISC_LIST ? View.VISIBLE : View.GONE);

        mainPane.mCurrentComponent = needed;
    }

    private void reloadContent() {
        if (mainPane.mCurrentComponent == PART_WEB)
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(mHttpClient.getCurrentUrl(), true));
        else if (mainPane.mCurrentComponent == PART_LIST)
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getFavoritesUrl(), true));
    }

    @Override
    public void onBackPressed() {
        if (slider.isOpen())
            slider.closePane();
        else if (browserHistory.hasPrevious()) {
            browserHistory.moveBack();
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(browserHistory.getUrl(), false));
        } else
            finish();
    }

    @Override
    public boolean onSearchRequested() {
        View main = mainPane.getView();
        if(main == null) { // ещё не создано
            return false;
        }

        final RelativeLayout upperDeck = (RelativeLayout) mainPane.getView().findViewById(R.id.upper_deck);
        final LinearLayout.LayoutParams lp = (LayoutParams) upperDeck.getLayoutParams();
        final int height = upperDeck.getHeight();
        int visibility = upperDeck.getVisibility();
        if (visibility == View.GONE) { // делаем видимым
            ValueAnimator animator = ValueAnimator.ofInt(-height, 0);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    lp.topMargin = (Integer) valueAnimator.getAnimatedValue();
                    upperDeck.requestLayout();
                }
            });
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    upperDeck.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setDuration(300).start();

            Animator toBread = ObjectAnimator.ofFloat(mActionBarToggle, "position", 1, 0);
            toBread.setInterpolator(new DecelerateInterpolator());
            toBread.setDuration(300).start();
        } else { // скрываем
            ValueAnimator animator = ValueAnimator.ofInt(0, -height);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator)
                {
                    lp.topMargin = (Integer) valueAnimator.getAnimatedValue();
                    upperDeck.requestLayout();
                }
            });
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    upperDeck.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setDuration(300).start();

            Animator fromBread = ObjectAnimator.ofFloat(mActionBarToggle, "position", 0, 1);
            fromBread.setInterpolator(new DecelerateInterpolator());
            fromBread.setDuration(300).start();
        }

        return super.onSearchRequested();
    }

    public void newPost(String text) {
        if (!(getUser().getCurrentDiaryPage() instanceof DiaryPage))
            return;

        if (((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryId().isEmpty())
            return;

        Post post = new Post();
        post.diaryID = ((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryId();
        post.content = text;

        messagePane.prepareFragment(getUser().getSignature(), post, text.isEmpty());
        slider.openPane();
    }

    public void newPost(Post post) {
        if (!(getUser().getCurrentDiaryPage() instanceof DiaryPage))
            return;

        if (((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryId().isEmpty())
            return;

        messagePane.prepareFragment(getUser().getSignature(), post);
        slider.openPane();
    }

    public void newComment(String text) {
        if (!(getUser().getCurrentDiaryPage() instanceof CommentsPage))
            return;

        if (((CommentsPage) getUser().getCurrentDiaryPage()).getPostId().isEmpty())
            return;

        Comment comment = new Comment();
        comment.postID = ((CommentsPage) getUser().getCurrentDiaryPage()).getPostId();
        comment.content = text;

        messagePane.prepareFragment(getUser().getSignature(), comment, text.isEmpty());
        slider.openPane();
    }

    public void editPost(Post post) {
        messagePane.prepareFragment(getUser().getSignature(), post, false);
        slider.openPane();
    }

    public void editComment(Comment comment) {
        messagePane.prepareFragment(getUser().getSignature(), comment, false);
        slider.openPane();
    }
}