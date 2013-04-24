package adonai.diary_browser;

import adonai.diary_browser.entities.*;
import adonai.diary_browser.preferences.PreferencesScreen;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.*;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.LinearLayout.LayoutParams;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DiaryList extends DiaryActivity implements OnClickListener, OnChildClickListener, OnGroupClickListener, OnRefreshListener<ListView>, OnItemLongClickListener, UserData.OnDataChangeListener
{
    // Команды хэндлеру вида
    static final int HANDLE_IMAGE_CLICK 							=   0x100;
    static final int HANDLE_UPDATE_HEADERS 							= 	0x200;
    
    
    // дополнительные команды хэндлерам
    
    // вкладки приложения
    public static final int TAB_FAVOURITES = 0;
    public static final int TAB_FAV_POSTS = 1;
    public static final int TAB_MY_DIARY = 2;
    public static final int TAB_DISCUSSIONS = 3;

    int mCurrentTab = 0;
    int mCurrentComponent = 0;
    
    private static final int PART_LIST = 0;
    private static final int PART_WEB = 1;
    private static final int PART_DISC_LIST = 2;
    
    // Адаптеры типов
    DiaryListArrayAdapter mFavouritesAdapter;
    DiscListArrayAdapter mDiscussionsAdapter;
    
    // Видимые объекты
    TextView mLogin;
    Button mDiscussNum;
    Button mCommentsNum;
    TextView mUmailNum;
    PullToRefreshListView mDiaryBrowser;
    DiaryWebView mPageBrowser;
    ExpandableListView mDiscussionBrowser;
    
    ImageButton mExitButton;
    ImageButton mQuotesButton;
    ImageButton mUmailButton;
    ImageButton mScrollButton;
    LinearLayout mTabs;
    AlertDialog ad;
    Paint mPaint;
    
    // Сервисные объекты
    DisplayMetrics gMetrics;

    BrowseHistory browserHistory;
    Handler mUiHandler;

    // Часть кода относится к кнопке быстрой промотки
    private Runnable fadeAnimation = new Runnable()
    {
        @Override
        public void run() {
            Animation animation = AnimationUtils.loadAnimation(mScrollButton.getContext(), android.R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener()
            {
                @Override
                public void onAnimationStart(Animation animation)
                {
                }

                @Override
                public void onAnimationEnd(Animation animation)
                {
                    mScrollButton.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation)
                {
                }
            });
            mScrollButton.startAnimation(animation);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Оповещаем остальных, что мы создались
        // Если был простой приложения
        browserHistory = new BrowseHistory();

        mUiHandler = new Handler(this);
        CookieSyncManager.createInstance(this);

        setContentView(R.layout.activity_diary_list_a);
        initializeUI();
    }
    

    @Override
    public void handleDataChange()
    {
        mUiHandler.sendEmptyMessage(HANDLE_UPDATE_HEADERS);
    }
    
    public void initializeUI()
    {
        // for text drawing
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setTextSize(12f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);

        gMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(gMetrics);
        
        mLogin = (TextView) findViewById(R.id.login_name);
        
        mExitButton = (ImageButton) findViewById(R.id.exit_button);
        mExitButton.setOnClickListener(this);
        mQuotesButton = (ImageButton) findViewById(R.id.quotes_button);
        mQuotesButton.setOnClickListener(this);
        mUmailButton = (ImageButton) findViewById(R.id.umail_button);
        mUmailButton.setOnClickListener(this);
        mScrollButton = (ImageButton) findViewById(R.id.updown_button);
        mScrollButton.setOnClickListener(this);
        
        mDiaryBrowser = (PullToRefreshListView) findViewById(R.id.diary_browser);
        mPageBrowser = (DiaryWebView) findViewById(R.id.page_browser);
        mPageBrowser.setDefaultSettings();
        registerForContextMenu(mPageBrowser);
        mDiscussionBrowser = (ExpandableListView) findViewById(R.id.discussion_browser);
        mTabs = (LinearLayout) findViewById(R.id.tabs);

        for(int i = 0; i < mTabs.getChildCount(); i++)
        {
            Button current = (Button) mTabs.getChildAt(i);
            current.setOnClickListener(this);
        }

        mCommentsNum = (Button) findViewById(R.id.diary_button);
        mDiscussNum = (Button) findViewById(R.id.discussions_button);
        
        mUmailNum = (TextView) findViewById(R.id.umail_counter);
        mUmailNum.setOnClickListener(this);
        
        mDiaryBrowser.setOnRefreshListener(this);
        
        mDiscussionBrowser.setOnChildClickListener(this);
        mDiscussionBrowser.setOnGroupClickListener(this);
        mDiscussionBrowser.setOnItemLongClickListener(this);

        setCurrentVisibleComponent(0);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onDestroy()
    {
    	mService.removeListener(this);
        mUser.setOnDataChangeListener(null);

        super.onDestroy();

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.diary_list_a, menu);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
	public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	// Только если это дневник
        if(mCurrentComponent == PART_WEB && mUser.currentDiaryPage.getClass().equals(DiaryPage.class))
        {
        	menu.findItem(R.id.menu_tags).setVisible(true);
        	menu.findItem(R.id.menu_new_post).setVisible(true);
        }
        else
        {
        	menu.findItem(R.id.menu_tags).setVisible(false);
        	menu.findItem(R.id.menu_new_post).setVisible(false);
        }
        
        // Только если это пост
        if(mCurrentComponent == PART_WEB && mUser.currentDiaryPage.getClass().equals(CommentsPage.class))
        	menu.findItem(R.id.menu_new_comment).setVisible(true);
        else
        	menu.findItem(R.id.menu_new_comment).setVisible(false);
        
        
        // Для всех веб-страничек
        if(mCurrentComponent == PART_WEB)
            menu.setGroupVisible(R.id.group_web, true);
        else
            menu.setGroupVisible(R.id.group_web, false);
        
        // Для всех списков
        if(mCurrentComponent == PART_LIST)
        {
        	menu.findItem(R.id.menu_share).setVisible(false);
        	menu.findItem(R.id.menu_subscr_list).setVisible(true);
        }
        else
        {
        	menu.findItem(R.id.menu_share).setVisible(true);
        	menu.findItem(R.id.menu_subscr_list).setVisible(false);
        }
        
        
        
		return super.onPrepareOptionsMenu(menu);
	}

    // старые телефоны тоже должны работать
	@SuppressWarnings("deprecation")
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_new_post:
                newPostPost();
                return true;
            case R.id.menu_new_comment:
            	newCommentPost();
            	return true;
            case R.id.menu_settings:
            	startActivity(new Intent(this, PreferencesScreen.class));
            	return true;
            case R.id.menu_share:
            	android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    			Toast.makeText(getApplicationContext(), getString(R.string.copied) + " " + mUser.currentDiaryPage.getPageURL(), Toast.LENGTH_SHORT).show();
    			clipboard.setText(mUser.currentDiaryPage.getPageURL());
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
            case R.id.menu_tags:
                // Берем lastIndex из-за того, что список постов может быть не только в дневниках (к примеру, ?favorite)
                assert(mUser.currentDiaryPage instanceof DiaryPage); // следим чтобы текущая страничка обязательно была в пределах иерархии
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(((DiaryPage)mUser.currentDiaryPage).getDiaryURL().substring(0, ((DiaryPage)mUser.currentDiaryPage).getDiaryURL().lastIndexOf('/') + 1) + "?tags", false));
            	return true;
            case R.id.menu_subscr_list:
                handleBackground(Utils.HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>("http://www.diary.ru/list/?act=show&fgroup_id=-1", false));
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        if(v.getId() == R.id.page_browser)
        {
        	Message msg = Message.obtain(mUiHandler, HANDLE_IMAGE_CLICK);
        	mPageBrowser.getRefreshableView().requestImageRef(msg);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) 
    {
        super.onNewIntent(intent);

        if(intent.getStringExtra("url") != null)
        {
            if(intent.getStringExtra("url").equals("")) // default case
                reloadContent();
            else
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(intent.getStringExtra("url"), true));
        }
        if(intent.getData() != null)
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(intent.getDataString(), false));
    }
    
    @Override
    public boolean handleMessage(Message message)
    {
        switch (message.what)
        {
        	case Utils.HANDLE_START:
                mService.addListener(this);
                mUser.setOnDataChangeListener(this);
        		
        	    pd = ProgressDialog.show(DiaryList.this, getString(R.string.loading), getString(R.string.please_wait), true, true);
                handleBackground(Utils.HANDLE_SET_HTTP_COOKIE, null);
                return true;
            case Utils.HANDLE_PROGRESS:
                if(pd != null)
                    pd.setMessage(getString(R.string.parsing_data));
                return true;
            case Utils.HANDLE_PROGRESS_2:
                if(pd != null)
                    pd.setMessage(getString(R.string.sorting_data));
                return true;
            case HANDLE_UPDATE_HEADERS:
            	// обрабатываем обновление контента
            	mLogin.setText(mUser.userName);
                if(mUser.newDiaryCommentsNum != 0)
                {
                    mCommentsNum.setText(mCommentsNum.getText() + " - " + mUser.newDiaryCommentsNum.toString());
                }
                else
                    mCommentsNum.setText(getString(R.string.my_diary));

                if(mUser.newDiscussNum != 0)
                {
                    mDiscussNum.setText(mDiscussNum.getText() + " - " + mUser.newDiscussNum);
                }
                else
                    mDiscussNum.setText(getString(R.string.discussions));

                if(mUser.newUmailNum != 0)
                {
                    mUmailNum.setText(mUser.newUmailNum.toString());
                    mUmailNum.setVisibility(View.VISIBLE);
                }
                else
                {
                    mUmailNum.setText("");
                    mUmailNum.setVisibility(View.GONE);
                }
                return true;
            case Utils.HANDLE_SET_HTTP_COOKIE:
                pd.setMessage(getString(R.string.getting_user_info));
                if(getIntent().getData() == null)
                    setCurrentTab(TAB_FAVOURITES);
                else
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(getIntent().getDataString(), false));
                return true;
            case Utils.HANDLE_GET_DIARIES_DATA:
                setCurrentVisibleComponent(PART_LIST);
                mDiaryBrowser.setAdapter(null);
                mDiaryBrowser.getRefreshableView().removeFooterView(mDiaryBrowser.getRefreshableView().findViewWithTag("footer"));
                mFavouritesAdapter = new DiaryListArrayAdapter(DiaryList.this, android.R.layout.simple_list_item_1, mUser.currentDiaries);
                if(mUser.currentDiaries.getPageLinks() != null)
                {
                    LinearLayout LL = new LinearLayout(mDiaryBrowser.getRefreshableView().getContext());
                    LL.setTag("footer");
                    Spanned pageLinks = mUser.currentDiaries.getPageLinks();
                    URLSpan[] URLs = pageLinks.getSpans(0, pageLinks.length(), URLSpan.class);
                    for(URLSpan url : URLs)
                    {
                        Button click = new Button(LL.getContext());
                        click.setMaxLines(1);
                        click.setText(pageLinks.subSequence(pageLinks.getSpanStart(url), pageLinks.getSpanEnd(url)));
                        click.setTag(url.getURL());
                        click.setOnClickListener(DiaryList.this);
                        LL.addView(click);
                        
                        LayoutParams LP = (LayoutParams) click.getLayoutParams();
                        LP.width = 0;
                        LP.height = LayoutParams.MATCH_PARENT;
                        LP.weight = 1.0f;
                    }
                    mDiaryBrowser.getRefreshableView().addFooterView(LL);
                }

                mDiaryBrowser.setAdapter(mFavouritesAdapter);

                browserHistory.add(mUser.currentDiaries.getURL(), getString(R.string.favourites));

                mDiaryBrowser.onRefreshComplete();
                
                // На Андроиде > 2.3.3 нужно обновлять меню для верного отображения нужных для страниц кнопок
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) 
                    invalidateOptionsMenu();

                break;
            case Utils.HANDLE_GET_DIARY_PAGE_DATA: // the most important part!
                setCurrentVisibleComponent(PART_WEB);
                mPageBrowser.getRefreshableView().loadDataWithBaseURL(mUser.currentDiaryPage.getPageURL(), mUser.currentDiaryPage.getContent().html(), null, "utf-8", mUser.currentDiaryPage.getPageURL());

                browserHistory.add(mUser.currentDiaryPage.getPageURL(), mUser.currentDiaryPage.getContent().title());

                setTitle(mUser.currentDiaryPage.getContent().title());
                mPageBrowser.onRefreshComplete();
                
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) 
                    invalidateOptionsMenu(); // PART_WEB
                break;
            case Utils.HANDLE_GET_DISCUSSIONS_DATA:
                mDiscussionsAdapter = new DiscListArrayAdapter(this, mUser.discussions);
                mDiscussionBrowser.setAdapter(mDiscussionsAdapter);
            	setCurrentVisibleComponent(PART_DISC_LIST);
            	
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) 
                    invalidateOptionsMenu(); // PART_DISC_LIST
            	break;
            case Utils.HANDLE_AUTHORIZATION_ERROR:
                mPageBrowser.onRefreshComplete();
                mDiaryBrowser.onRefreshComplete();
                Toast.makeText(getApplicationContext(), "Not authorized, retry!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
                finish();
                break;
            case Utils.HANDLE_GET_DISCUSSION_LIST_DATA:
            	int pos = (Integer) message.obj;
            	mDiscussionBrowser.expandGroup(pos);
            	break;
            case HANDLE_IMAGE_CLICK:
            {
            	final String src = message.getData().getString("url");
            	if(src == null) // нет картинки!
            		return false;
            	
                ArrayList<String> itemsBuilder = new ArrayList<String>();
	        	itemsBuilder.add(getString(R.string.image_save));
	        	itemsBuilder.add(getString(R.string.image_copy_url));
	        	itemsBuilder.add(getString(R.string.image_open));

	        	final String[] items = itemsBuilder.toArray(new String[itemsBuilder.size()]);
	        	AlertDialog.Builder builder = new AlertDialog.Builder(mPageBrowser.getContext());
	        	builder.setTitle(R.string.image_action);
	        	builder.setItems(items, new DialogInterface.OnClickListener() 
	        	{
	        	    @SuppressWarnings("deprecation")
					public void onClick(DialogInterface dialog, int item) 
	        	    {
	        	    	switch(item)
	        	    	{
	        	    		case DiaryWebView.IMAGE_SAVE: // save
	        	    		{	        
	        	    		    // На Андроиде > 2.3.3 используется иной метод сохранения кэша. Просто так картинку не получить, увы.
	        	    		    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) 
	        	    	        {
	        	    		        Toast.makeText(DiaryList.this, getString(R.string.loading), Toast.LENGTH_SHORT).show();
	        	    		        mService.handleRequest(Utils.HANDLE_GET_IMAGE, new Pair<String, Boolean>(src, false));
	        	    	        }
	        	    		    else
	        	    		    {
    	                            String hashCode = String.format("%08x", src.hashCode());
    	                            File file = new File(new File(getCacheDir(), "webviewCache"), hashCode);
    	                            if(file.exists())
    	                            {
    	                                String realName = URLUtil.guessFileName(src, null, MimeTypeMap.getFileExtensionFromUrl(src));
    	                                CacheManager.saveDataToSD(getApplicationContext(), realName, file);
    	                            }
	        	    		    }
	        	    		}
	        	    		break;
	        	    		case DiaryWebView.IMAGE_COPY_URL: // copy
	        	    		{
	                            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
	                            Toast.makeText(DiaryList.this, getString(R.string.copied) + " " + src, Toast.LENGTH_SHORT).show();
	                            clipboard.setText(src);
	        	    		}
	        	    		break;
	        	    		case DiaryWebView.IMAGE_OPEN: // open Link
	        	    		{
	        	    		 // На Андроиде > 2.3.3 используется иной метод сохранения кэша.
	        	    		    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) 
                                {
                                    Toast.makeText(DiaryList.this, getString(R.string.loading), Toast.LENGTH_SHORT).show();
                                    mService.handleRequest(Utils.HANDLE_GET_IMAGE, new Pair<String, Boolean>(src, true));
                                }
	        	    		    else
	        	    		    {
    	                        	String hashCode = String.format("%08x", src.hashCode());
    	                            File file = new File(new File(getCacheDir(), "webviewCache"), hashCode);
    	                            if(file.exists())
    	                            	try
                                        {
    	                            	    Intent intent = new Intent(getApplicationContext(), ImageViewer.class);
                                            intent.putExtra("image_file", file.getCanonicalPath());
                                            startActivity(intent);
                                        }
                                        catch (IOException e)
                                        {
                                            Toast.makeText(DiaryList.this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
                                        }
	        	    		    }
	        	    		}
	        	    		break;
	        	    	}
	        	    }
	        	});
	        	AlertDialog alert = builder.create();
	        	alert.show();
	        }
            break;
            case Utils.HANDLE_EDIT_POST:
                Post sendPost = (Post)message.obj;
                editPost(sendPost);
                break;
            case Utils.HANDLE_EDIT_COMMENT:
                Comment sendComment = (Comment)message.obj;
                editComment(sendComment);
                break;
        }

        super.handleMessage(message);
        return true;
    }

    // Часть кода относится к кнопке быстрой промотки
    @Override
    void handleScroll(int direction)
    {
        mScrollButton.setVisibility(View.VISIBLE);
        mScrollButton.removeCallbacks(fadeAnimation);
        mScrollButton.clearAnimation();
        mScrollButton.postDelayed(fadeAnimation, 2000);
        switch (direction)
        {
            case Utils.VIEW_SCROLL_DOWN:
                mScrollButton.setImageResource(R.drawable.overscroll_button_down);
                break;
            case Utils.VIEW_SCROLL_UP:
                mScrollButton.setImageResource(R.drawable.overscroll_button_up);
                break;
        }

    }

    public void onClick(View view)
    {	
        if (view == mExitButton)
        {
        	AlertDialog.Builder builder = new AlertDialog.Builder(mPageBrowser.getContext());
        	builder.setTitle(R.string.really_exit);
        	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() 
        	{
				public void onClick(DialogInterface dialog, int item) 
        	    {
					Editor lysosome = mService.mPreferences.edit();
		            lysosome.remove(Utils.KEY_USERNAME);
		            lysosome.remove(Utils.KEY_PASSWORD);
		            lysosome.commit();
		            
		            CookieManager cookieManager = CookieManager.getInstance();
		            cookieManager.removeSessionCookie();
		            CookieSyncManager.getInstance().sync();
		            mService.mUser = new UserData();
		            
		            //TODO: просмотр без логина тоже еще не введен
		            startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
		            finish();
        	    }
        	});
        	builder.setNegativeButton(android.R.string.no, null);
        	builder.create().show();
        }
        else if (view == mQuotesButton)
        {
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.ownDiaryURL + "?quote", false));
        }
        else if (view == mUmailButton)
        {
            Intent postIntent = new Intent(getApplicationContext(), UmailList.class);
            startActivity(postIntent);
        }
        else if (view == mScrollButton)
        {
            // Офигительная штука, документации по которой нет.
            // Устанавливает начальную скорость скролла даже если в данный момент уже происходит скроллинг
            if(mPageBrowser.scrolling == Utils.VIEW_SCROLL_DOWN)
                mPageBrowser.getRefreshableView().flingScroll(0, 100000);
            else
                mPageBrowser.getRefreshableView().flingScroll(0, -100000);
        }
        else if (view == mUmailNum)
        {
            Intent postIntent = new Intent(getApplicationContext(), UmailList.class);
            postIntent.putExtra("url", mUser.newUmailLink);
            startActivity(postIntent);
        }
        else if (view.getParent() == mTabs)   // Если это кнопка табов
        {
            setCurrentTab(mTabs.indexOfChild(view));
        }
        else if (view.getTag() != null)  // нижние кнопки списков
        {
            handleBackground(Utils.HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>((String)view.getTag(), false));
        }
        else
            switch (view.getId())
            {
            	// Загружаем посты дневника
                case R.id.title:
                {
                    int pos = mDiaryBrowser.getRefreshableView().getPositionForView((View) view.getParent());
                    Openable diary = (Openable) mDiaryBrowser.getRefreshableView().getAdapter().getItem(pos);
                    
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(diary.getURL(), false));
                }
                break;
                default:
                    Utils.showDevelSorry(this);
                break;
            }
    }
    

    // Загружаем дискуссии
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) 
	{
		String link = ((DiscList.Discussion) parent.getExpandableListAdapter().getChild(groupPosition, childPosition)).URL;
	    handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(link, false));
		return true;
	}
	
	public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) 
	{
		if(parent.isGroupExpanded(groupPosition))
		{
			parent.collapseGroup(groupPosition);
			return true;
		}
		
		if(((DiscList)parent.getExpandableListAdapter().getGroup(groupPosition)).getDiscussions().isEmpty())
		{
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(groupPosition);
			params.add(mDiscussionBrowser.getExpandableListAdapter().getGroup(groupPosition));
			params.add(false);
		    handleBackground(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, params);
		}
		else
			parent.expandGroup(groupPosition);
    	return true;
	}
	
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        if(parent instanceof ExpandableListView)
        {
            ExpandableListView elv = (ExpandableListView) parent;
            if(ExpandableListView.getPackedPositionType(id) != ExpandableListView.PACKED_POSITION_TYPE_GROUP)
                return false;
            
            int groupPosition = ExpandableListView.getPackedPositionGroup(id);
            if(elv.isGroupExpanded(groupPosition))
            {
                elv.collapseGroup(groupPosition);
                return true;
            }
            handleBackground(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, new Pair<Integer, Boolean>(groupPosition, true));
            return true;
        }
        return false;
    }
    
    /*            is.close();
     * (non-Javadoc) Sets the contents to current tab and hides everything other. In addition, refreshes content on
     * page, if needed.
     */
    private void setCurrentTab(int index)
    {
        mTabs.getChildAt(mCurrentTab).setSelected(false);

        switch (index)
        {
            case TAB_FAVOURITES:
                handleBackground(Utils.HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>("http://www.diary.ru/list/?act=show&fgroup_id=0", false));
            break;
            case TAB_FAV_POSTS:
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.ownDiaryURL + "?favorite", false));
            break;
            case TAB_MY_DIARY:
                if(mUser.newDiaryCommentsNum != 0)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.newDiaryLink, true));
                else
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.ownDiaryURL, false));
            break;
            case TAB_DISCUSSIONS:
                if(mUser.newDiscussNum != 0)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.newDiscussLink, true));
                else
                    handleBackground(Utils.HANDLE_GET_DISCUSSIONS_DATA, null);
            break;
            default:
            	Utils.showDevelSorry(this);
            break;
        }
        
        mCurrentTab = index;
        mTabs.getChildAt(index).setSelected(true);
    }
    
    private void setCurrentVisibleComponent(int needed)
    {   
        mDiaryBrowser.setVisibility(needed == PART_LIST ? View.VISIBLE : View.GONE);
        mPageBrowser.setVisibility(needed == PART_WEB ? View.VISIBLE : View.GONE);
        //mAuthorBrowser.setVisibility(needed == AUTHOR_PAGE ? View.VISIBLE : View.GONE);
        mDiscussionBrowser.setVisibility(needed == PART_DISC_LIST ? View.VISIBLE : View.GONE);
        
        mCurrentComponent = needed;
    }
    
    private void reloadContent()
    {
    	if(mCurrentComponent == PART_WEB)
    		handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(mUser.currentDiaryPage.getPageURL(), true));
    	else if (mCurrentComponent == PART_LIST)
    		handleBackground(Utils.HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>("http://www.diary.ru/list/?act=show&fgroup_id=0", true));
    }

    @Override
    public void onBackPressed()
    {
        if(browserHistory.getCurrentIndex() > 1)
        {
            browserHistory.moveBack();
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(browserHistory.getCurrentUrl(), false));
        }
        else
        {
        	ContextThemeWrapper ctw = new ContextThemeWrapper(this, android.R.style.Theme_Black);
        	final ScrollView dialogView = new ScrollView(ctw);
        	LinearLayout LL = new LinearLayout(ctw);
        	LL.setOrientation(LinearLayout.VERTICAL);

            ArrayList<String> urls = browserHistory.getUrlsCopy();
        	for(Integer index = urls.size() - 1; index >= 0; index--)
        	{

    			TextView tmpTxt = new TextView(ctw);
    			tmpTxt.setText(browserHistory.getName(urls.get(index)));
    			tmpTxt.setTag(urls.get(index));
    			tmpTxt.setMaxLines(1);
    			tmpTxt.setPadding(5, 5, 5, 5);
    			tmpTxt.setTextAppearance(ctw, android.R.style.TextAppearance_Large);
    			tmpTxt.setOnClickListener(new OnClickListener()
				{
					
					public void onClick(View v)
					{
						String url = (String) v.getTag();
						handleBackground(Utils.HANDLE_PICK_URL, new Pair<String, Boolean>(url, false));
	        	    	if(ad.isShowing())
	        	    		ad.dismiss();
					}
				});
    			TextView tmpDescTxt = new TextView(ctw);
    			tmpDescTxt.setText(urls.get(index));
    			LL.addView(tmpTxt);
    			LL.addView(tmpDescTxt);
        	}
        	dialogView.addView(LL);
        	AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
        	builder.setTitle(R.string.image_action);
        	builder.setView(dialogView);
        	ad = builder.create();
        	ad.show();
        }
    }

    @Override
    public boolean onSearchRequested()
    {
        int visibility = mTabs.getVisibility();
        if(visibility == View.GONE)
            mTabs.setVisibility(View.VISIBLE);
        else
            mTabs.setVisibility(View.GONE);
        
        return super.onSearchRequested();
    }
    
    public void newPostPost()
    {
        assert(mUser.currentDiaryPage instanceof DiaryPage);
        
        if(((DiaryPage)mUser.currentDiaryPage).getDiaryID().equals(""))
            return;
        
        Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
        
        postIntent.putExtra("TypeId", "DiaryId");
        postIntent.putExtra("DiaryId", ((DiaryPage)mUser.currentDiaryPage).getDiaryID());
        
        postIntent.putExtra("signature", mUser.signature);
        postIntent.putExtra("sendURL", ((DiaryPage)mUser.currentDiaryPage).getDiaryURL() + "diary.php");
        
        startActivity(postIntent);
    }
    
    public void newCommentPost()
    {
        assert(mUser.currentDiaryPage instanceof CommentsPage);
        
    	if(((CommentsPage)mUser.currentDiaryPage).getPostID().equals(""))
            return;
    	
    	Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
    	
    	postIntent.putExtra("TypeId", "PostId");
        postIntent.putExtra("PostId", ((CommentsPage)mUser.currentDiaryPage).getPostID());
        
        postIntent.putExtra("signature", mUser.signature);
        postIntent.putExtra("sendURL", ((CommentsPage)mUser.currentDiaryPage).getDiaryURL() + "diary.php");
        
        startActivity(postIntent);
    }
    
    public void editPost(Post post)
    {
        Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);
        
        postIntent.putExtra("TypeId", "PostEditId");
        postIntent.putExtra("PostEditId", post.postID);
        
        postIntent.putExtra("signature", mUser.signature);
        postIntent.putExtra("sendURL", ((DiaryPage)mUser.currentDiaryPage).getDiaryURL() + "diary.php");
        postIntent.putExtra("postContents", post.serialize());
        
        startActivity(postIntent);
    }

    public void editComment(Comment comment)
    {
        Intent postIntent = new Intent(getApplicationContext(), MessageSender.class);

        postIntent.putExtra("TypeId", "CommentEditId");
        postIntent.putExtra("CommentEditId", comment.commentID);

        postIntent.putExtra("signature", mUser.signature);
        postIntent.putExtra("sendURL", ((DiaryPage)mUser.currentDiaryPage).getDiaryURL() + "diary.php");
        postIntent.putExtra("commentContents", comment.serialize());

        startActivity(postIntent);
    }
    
	public void onRefresh(PullToRefreshBase<ListView> refreshView)
    {
        switch (refreshView.getId())
        {
            case R.id.diary_browser:
                handleBackground(Utils.HANDLE_GET_DIARIES_DATA, new Pair<String, Boolean>(mUser.currentDiaries.getURL(), true));
            break;
        }
    }
}
