package adonai.diary_browser;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import adonai.diary_browser.R;
import yuku.ambilwarna.AmbilWarnaDialog;

import adonai.diary_browser.entities.Post;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;

public class MessageSender extends Activity implements OnClickListener, OnCheckedChangeListener, android.widget.RadioGroup.OnCheckedChangeListener
{

	private static final int HANDLE_DO_POST = 0;
	private static final int HANDLE_DO_COMMENT = 1;
	private static final int HANDLE_DO_UMAIL = 2;
	private static final int HANDLE_UMAIL_ACK = 3;
	private static final int HANDLE_UMAIL_REJ = 4;
	
	ImageButton mLeftGradient;
	ImageButton mRightGradient;
	Button mSetGradient;
	
	EditText toText;
	EditText titleText;
	EditText contentText;
	EditText themesText;
	EditText musicText;
	EditText moodText;
	Button mPublish;
	CheckBox mShowOptionals;
	CheckBox mShowPoll;
	CheckBox mSubscribe;
	CheckBox mShowAndClose;
	CheckBox mGetReceipt;
	CheckBox mCopyMessage;
	
	EditText mPollTitle;
	EditText mPollChoice1;
	EditText mPollChoice2;
	EditText mPollChoice3;
	EditText mPollChoice4;
	EditText mPollChoice5;
	EditText mPollChoice6;
	EditText mPollChoice7;
	EditText mPollChoice8;
	EditText mPollChoice9;
	EditText mPollChoice10;
	
	EditText mCloseAllowList;
	EditText mCloseDenyList;
	EditText mCloseText;
	
	RadioGroup mCloseOpts;
	
	Handler mHandler, mUiHandler;
	Looper mLooper;
	ProgressDialog pd = null;
	
	List<View> postElements = new ArrayList<View>();
	List<View> commentElements = new ArrayList<View>();
	List<View> umailElements = new ArrayList<View>();
	
	List<View> optionals = new ArrayList<View>();
	List<View> pollScheme = new ArrayList<View>();
	List<NameValuePair> postParams;
	
	String mSignature = null;
	String mId = null;
	String mTypeId = null;
	
	DiaryHttpClient mDHCL;
	String mSendURL;
	Post mPost;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mPost = new Post();
		postParams = new ArrayList<NameValuePair>();
        
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, HttpCallback);
        mUiHandler = new Handler(UiCallback);
        
        setContentView(R.layout.message_sender_a);
        
        toText = (EditText) findViewById(R.id.message_to);
        mGetReceipt = (CheckBox) findViewById(R.id.message_getreceipt);
        mCopyMessage = (CheckBox) findViewById(R.id.message_copy);
        
    	titleText = (EditText) findViewById(R.id.message_title);
    	contentText = (EditText) findViewById(R.id.message_content);
    	themesText = (EditText) findViewById(R.id.message_themes);
    	musicText = (EditText) findViewById(R.id.message_music);
    	moodText = (EditText) findViewById(R.id.message_mood);
    	mPublish = (Button) findViewById(R.id.message_publish);
    	mPublish.setOnClickListener(this);
    	
    	mLeftGradient = (ImageButton) findViewById(R.id.left_gradient);
    	mLeftGradient.setOnClickListener(this);
    	mRightGradient = (ImageButton) findViewById(R.id.right_gradient);
    	mRightGradient.setOnClickListener(this);
    	mSetGradient = (Button) findViewById(R.id.set_gradient);
    	mSetGradient.setOnClickListener(this);
    	
    	mPollTitle = (EditText) findViewById(R.id.message_poll_title);
    	mPollChoice1 = (EditText) findViewById(R.id.message_poll_1);
    	mPollChoice2 = (EditText) findViewById(R.id.message_poll_2);
    	mPollChoice3 = (EditText) findViewById(R.id.message_poll_3);
    	mPollChoice4 = (EditText) findViewById(R.id.message_poll_4);
    	mPollChoice5 = (EditText) findViewById(R.id.message_poll_5);
    	mPollChoice6 = (EditText) findViewById(R.id.message_poll_6);
    	mPollChoice7 = (EditText) findViewById(R.id.message_poll_7);
    	mPollChoice8 = (EditText) findViewById(R.id.message_poll_8);
    	mPollChoice9 = (EditText) findViewById(R.id.message_poll_9);
    	mPollChoice10 = (EditText) findViewById(R.id.message_poll_10);
    	
    	mCloseOpts = (RadioGroup) findViewById(R.id.close_opts);
    	mCloseOpts.setOnCheckedChangeListener(this);
    	mCloseAllowList = (EditText) findViewById(R.id.close_allowed_list);
    	mCloseDenyList = (EditText) findViewById(R.id.close_denied_list);
    	mCloseText = (EditText) findViewById(R.id.close_text);
    	
    	mShowOptionals = (CheckBox) findViewById(R.id.message_optional);
    	mShowOptionals.setOnCheckedChangeListener(this);
    	mShowPoll = (CheckBox) findViewById(R.id.message_poll);
    	mShowPoll.setOnCheckedChangeListener(this);
    	mSubscribe = (CheckBox) findViewById(R.id.message_subscribe);
    	mShowAndClose = (CheckBox) findViewById(R.id.message_close);
    	mShowAndClose.setOnCheckedChangeListener(this);
    	
    	optionals.add(findViewById(R.id.message_themes_hint));
    	optionals.add(themesText);
    	optionals.add(findViewById(R.id.message_music_hint));
    	optionals.add(musicText);
    	optionals.add(findViewById(R.id.message_mood_hint));
    	optionals.add(moodText);
    	
    	pollScheme.add(mPollTitle);
    	pollScheme.add(mPollChoice1);
    	pollScheme.add(mPollChoice2);
    	pollScheme.add(mPollChoice3);
    	pollScheme.add(mPollChoice4);
    	pollScheme.add(mPollChoice5);
    	pollScheme.add(mPollChoice6);
    	pollScheme.add(mPollChoice7);
    	pollScheme.add(mPollChoice8);
    	pollScheme.add(mPollChoice9);
    	pollScheme.add(mPollChoice10);
    	
        commentElements.add(findViewById(R.id.message_content_hint));
        commentElements.add(findViewById(R.id.message_specials));
    	commentElements.add(contentText);
    	commentElements.add(mSubscribe);

    	postElements.add(findViewById(R.id.message_title_hint));
    	postElements.add(titleText);
    	postElements.add(findViewById(R.id.message_content_hint));
    	postElements.add(findViewById(R.id.message_specials));
    	postElements.add(contentText);
    	postElements.add(mShowOptionals);
    	postElements.add(mShowAndClose);
    	postElements.add(mShowPoll);
    	
    	umailElements.add(findViewById(R.id.message_to_hint));
    	umailElements.add(toText);
        umailElements.add(findViewById(R.id.message_title_hint));
    	umailElements.add(titleText);
    	umailElements.add(findViewById(R.id.message_content_hint));
    	umailElements.add(findViewById(R.id.message_specials));
    	umailElements.add(contentText);
    	umailElements.add(mGetReceipt);
    	umailElements.add(mCopyMessage);
    }
    
	@Override
	protected void onDestroy() 
	{
		mLooper.quit();
		super.onDestroy();
	}
    
    Handler.Callback HttpCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            try
            {
            	switch (message.what)
            	{
            		case HANDLE_DO_POST:
            		case HANDLE_DO_COMMENT:
						mDHCL.postPage(mSendURL, new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
						mUiHandler.sendEmptyMessage(message.what);
            			return true;
                    case HANDLE_DO_UMAIL:
                        HttpResponse page = mDHCL.postPage(mSendURL, new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
                        String result = EntityUtils.toString(page.getEntity());
                        if(result.contains("Письмо отправлено"))
                            mUiHandler.sendEmptyMessage(HANDLE_UMAIL_ACK);
                        else
                            mUiHandler.sendEmptyMessage(HANDLE_UMAIL_REJ);
                        return true;
            		default:
            			break;
            	}
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        	
        	return false;
        }
    };
    
    Handler.Callback UiCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
        	switch (message.what)
        	{
        		case HANDLE_DO_POST:
        		case HANDLE_DO_COMMENT:
        		{
        			// Пост опубликован, возвращаемся
        			pd.dismiss();
        			
    				Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
    				returnIntent.putExtra("url", "");
    				returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    				startActivity(returnIntent);
					finish();
					break;
        		}
        		case HANDLE_UMAIL_ACK:
        		{
                    pd.dismiss();
        		    AlertDialog.Builder builder = new AlertDialog.Builder(MessageSender.this);
                    builder.setTitle(android.R.string.ok).setCancelable(false).setMessage(R.string.message_send_ok);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent returnIntent = new Intent(getApplicationContext(), UmailList.class);
                            returnIntent.putExtra("sendCompleted", true);
                            returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(returnIntent);
                            finish();
                        }
                    });
                    builder.create().show();
        		    break;
        		}
        		case HANDLE_UMAIL_REJ:
                {
                    pd.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(MessageSender.this);
                    builder.setTitle(android.R.string.no).setCancelable(false).setMessage(R.string.message_send_error);
                    builder.setPositiveButton(android.R.string.no, new DialogInterface.OnClickListener()
                    {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent returnIntent = new Intent(getApplicationContext(), UmailList.class);
                            returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(returnIntent);
                            finish();
                        }
                    });
                    builder.create().show();
                    break;
                }
        		default:
        			break;
        	}
        	
        	return false;
        }
    };

    /* (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart()
    {
        Intent intent = getIntent();
        
        // запущено без запроса
        if(intent == null)
        {
        	finish();
        	return;
        }
        
        if(NetworkService.getInstance(this) == null)
        {
            finish();
            return;
        }
        
        // обязательно
        mDHCL = NetworkService.getInstance(this).mDHCL;
        mSignature = intent.getStringExtra("signature");
        mSendURL = intent.getStringExtra("sendURL");
        
        // одно из двух
        mTypeId = intent.getStringExtra("TypeId");
        mId = intent.getStringExtra(mTypeId);
        
        // Если это пост
        if(mTypeId.equals("DiaryId"))
        {
            setTitle(R.string.new_post);
            
            for(View v : umailElements)
                v.setVisibility(View.GONE);
            
            for(View v : commentElements)
                v.setVisibility(View.GONE);
            
            for(View v : postElements)
                v.setVisibility(View.VISIBLE);
        }
        else if (mTypeId.equals("PostId")) // если это комментарий
        {
            setTitle(R.string.new_comment);
            
            for(View v : umailElements)
                v.setVisibility(View.GONE);
            
            for(View v : postElements)
                v.setVisibility(View.GONE);
            
            
            for(View v : commentElements)
                v.setVisibility(View.VISIBLE);
        }
        else if(mTypeId.equals("umailTo")) // Если почта
        {
            setTitle(R.string.new_umail);
            
            for(View v : commentElements)
                v.setVisibility(View.GONE);
            
            for(View v : postElements)
                v.setVisibility(View.GONE);
            
            for(View v : umailElements)
                v.setVisibility(View.VISIBLE);
            
            if(mId != null)
                toText.setText(mId);
        }
        else if(mTypeId.equals("PostEditId")) // Редактирование поста (самое зло)
        {
            setTitle(R.string.edit_post);
            
            for(View v : umailElements)
                v.setVisibility(View.GONE);
            
            for(View v : commentElements)
                v.setVisibility(View.GONE);
            
            for(View v : postElements)
                v.setVisibility(View.VISIBLE);
            
            mPost = Post.deserialize(intent.getStringExtra("postContents"));
            prepareUi(mPost);
        }
        
        super.onStart();
    }

	private void prepareUi(Post post)
    {
        titleText.setText(post.title);
        contentText.setText(post.content);
        
        if(!"".equals(post.music + post.mood + post.themes))
        {
            mShowOptionals.setChecked(true);
            musicText.setText(post.music);
            moodText.setText(post.mood);
            themesText.setText(post.themes);
        }
        
        if(!post.pollTitle.equals(""))
        {
        	mShowPoll.setChecked(true);
        	mPollTitle.setText(post.pollTitle);
        	mPollChoice1.setText(post.pollAnswer1);
        	mPollChoice2.setText(post.pollAnswer2);
        	mPollChoice3.setText(post.pollAnswer3);
        	mPollChoice4.setText(post.pollAnswer4);
        	mPollChoice5.setText(post.pollAnswer5);
        	mPollChoice6.setText(post.pollAnswer6);
        	mPollChoice7.setText(post.pollAnswer7);
        	mPollChoice8.setText(post.pollAnswer8);
        	mPollChoice9.setText(post.pollAnswer9);
        	mPollChoice10.setText(post.pollAnswer10);
        }
        
        if(!post.closeAccessMode.equals(""))
        {
            mShowAndClose.setChecked(true);
            if(post.closeAccessMode.equals("6"))
                mCloseOpts.check(R.id.close_only_reg);
            else if(post.closeAccessMode.equals("1"))
                mCloseOpts.check(R.id.close_only_fav);
            else if(post.closeAccessMode.equals("5"))
                mCloseOpts.check(R.id.close_only_sub);
            else if(post.closeAccessMode.equals("4"))
                mCloseOpts.check(R.id.close_only_white);
            else if(post.closeAccessMode.equals("3"))
                mCloseOpts.check(R.id.close_for_list);
            else if(post.closeAccessMode.equals("2"))
                mCloseOpts.check(R.id.close_only_list);
            else if(post.closeAccessMode.equals("7"))
                mCloseOpts.check(R.id.close_for_all);
            
            mCloseText.setText(post.closeText);
            mCloseAllowList.setText(post.closeAllowList);
            mCloseDenyList.setText(post.closeDenyList);
        }
    }

    public void onClick(View view) 
	{
		switch(view.getId())
		{
			case R.id.message_publish:
			{
				// TODO: Сохранение в черновики
				// Задел на будущее - для сохранения в черновики
				mPost.title = titleText.getText().toString();
				//mPost.set_text(new Spannable.Factory().newSpannable(contentText.getText().toString()));
				mPost.content = Jsoup.parse(contentText.getText().toString()).html();
				mPost.themes = themesText.getText().toString();
				mPost.music = musicText.getText().toString();
				mPost.mood = moodText.getText().toString();

				// Добавляем параметры из настроек
				postParams.add(new BasicNameValuePair("message", contentText.getText().toString() + NetworkService.getInstance(this).mPreferences.getString("post.signature", "")));
				postParams.add(new BasicNameValuePair("signature", mSignature));
                postParams.add(new BasicNameValuePair("action", "dosend"));
				pd = ProgressDialog.show(MessageSender.this, getString(R.string.loading), getString(R.string.sending_data), true, true);
				
				// Если пост
				if(mTypeId.equals("DiaryId"))
				{
				    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
	                postParams.add(new BasicNameValuePair("module", "journal"));
	                postParams.add(new BasicNameValuePair("resulttype", "2"));
	                
					postParams.add(new BasicNameValuePair("act", "new_post_post"));
					postParams.add(new BasicNameValuePair("post_id", ""));
					postParams.add(new BasicNameValuePair("journal_id", mId));
					postParams.add(new BasicNameValuePair("referer", mDHCL.currentURL));
					postParams.add(new BasicNameValuePair("post_type", ""));
					
					postParams.add(new BasicNameValuePair("title", mPost.title));
					if(mShowOptionals.isChecked())
					{
						postParams.add(new BasicNameValuePair("themes", mPost.themes + NetworkService.getInstance(this).mPreferences.getString("post.tags", "")));
						postParams.add(new BasicNameValuePair("current_music", mPost.music));
						postParams.add(new BasicNameValuePair("current_mood", mPost.mood));
					}
					else
					{
						postParams.add(new BasicNameValuePair("themes", ""));
						postParams.add(new BasicNameValuePair("current_music", ""));
						postParams.add(new BasicNameValuePair("current_mood", ""));
					}

					postParams.add(new BasicNameValuePair("attachment", ""));
					
					if(mShowPoll.isChecked())
					{
						postParams.add(new BasicNameValuePair("poll_title", mPollTitle.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_1", mPollChoice1.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_2", mPollChoice2.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_3", mPollChoice3.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_4", mPollChoice4.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_5", mPollChoice5.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_6", mPollChoice6.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_7", mPollChoice7.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_8", mPollChoice8.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_9", mPollChoice9.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_10", mPollChoice10.getText().toString()));
					}
					else
					{
						postParams.add(new BasicNameValuePair("poll_title", ""));
						postParams.add(new BasicNameValuePair("poll_answer_1", ""));
						postParams.add(new BasicNameValuePair("poll_answer_2", ""));
						postParams.add(new BasicNameValuePair("poll_answer_3", ""));
						postParams.add(new BasicNameValuePair("poll_answer_4", ""));
						postParams.add(new BasicNameValuePair("poll_answer_5", ""));
						postParams.add(new BasicNameValuePair("poll_answer_6", ""));
						postParams.add(new BasicNameValuePair("poll_answer_7", ""));
						postParams.add(new BasicNameValuePair("poll_answer_8", ""));
						postParams.add(new BasicNameValuePair("poll_answer_9", ""));
						postParams.add(new BasicNameValuePair("poll_answer_10", ""));
					}
					
					if(mShowAndClose.isChecked())
					{
						postParams.add(new BasicNameValuePair("private_post", "1"));
						if(!mCloseText.getText().equals(""))
						{
						    postParams.add(new BasicNameValuePair("check_close_text", "1"));
						    postParams.add(new BasicNameValuePair("close_text", mCloseText.getText().toString()));
						}
						
						switch(mCloseOpts.getCheckedRadioButtonId())
						{
							case R.id.close_only_reg:
								postParams.add(new BasicNameValuePair("close_access_mode", "6"));
							break;
							case R.id.close_only_fav:
								postParams.add(new BasicNameValuePair("close_access_mode", "1"));
							break;
							case R.id.close_only_sub:
								postParams.add(new BasicNameValuePair("close_access_mode", "5"));
							break;
							case R.id.close_only_white:
								postParams.add(new BasicNameValuePair("close_access_mode", "4"));
							break;
							case R.id.close_for_list:
								postParams.add(new BasicNameValuePair("close_access_mode", "2"));
								postParams.add(new BasicNameValuePair("access_list", mCloseDenyList.getText().toString()));
							break;
							case R.id.close_only_list:
								postParams.add(new BasicNameValuePair("close_access_mode", "3"));
								postParams.add(new BasicNameValuePair("access_list", mCloseAllowList.getText().toString()));
							break;
							case R.id.close_for_all:
								postParams.add(new BasicNameValuePair("close_access_mode", "7"));
							break;
						}
					}
					
					postParams.add(new BasicNameValuePair("rewrite", "rewrite"));
					postParams.add(new BasicNameValuePair("save_type", "js2"));
					
					mHandler.sendEmptyMessage(HANDLE_DO_POST);
				}
				else if(mTypeId.equals("PostEditId")) // Если редактируем пост
				{
				    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("resulttype", "2"));
                    
                    postParams.add(new BasicNameValuePair("act", "edit_post_post"));
                    postParams.add(new BasicNameValuePair("post_id", mPost.ID));
                    postParams.add(new BasicNameValuePair("journal_id", mPost.diaryID));
                    postParams.add(new BasicNameValuePair("referer", mDHCL.currentURL));
                    postParams.add(new BasicNameValuePair("post_type", ""));
                    
                    postParams.add(new BasicNameValuePair("title", mPost.title));
                    if(mShowOptionals.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("themes", mPost.themes + NetworkService.getInstance(this).mPreferences.getString("post.tags", "")));
                        postParams.add(new BasicNameValuePair("current_music", mPost.music));
                        postParams.add(new BasicNameValuePair("current_mood", mPost.mood));
                    }
                    else
                    {
                        postParams.add(new BasicNameValuePair("themes", ""));
                        postParams.add(new BasicNameValuePair("current_music", ""));
                        postParams.add(new BasicNameValuePair("current_mood", ""));
                    }

                    postParams.add(new BasicNameValuePair("attachment", ""));
                    
                    if(mShowPoll.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("poll_title", mPollTitle.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_1", mPollChoice1.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_2", mPollChoice2.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_3", mPollChoice3.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_4", mPollChoice4.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_5", mPollChoice5.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_6", mPollChoice6.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_7", mPollChoice7.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_8", mPollChoice8.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_9", mPollChoice9.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_10", mPollChoice10.getText().toString()));
                    }
                    else
                    {
                        postParams.add(new BasicNameValuePair("poll_title", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_1", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_2", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_3", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_4", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_5", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_6", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_7", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_8", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_9", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_10", ""));
                    }
                    
                    if(mShowAndClose.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("private_post", "1"));
                        if(!mCloseText.getText().equals(""))
                        {
                            postParams.add(new BasicNameValuePair("check_close_text", "1"));
                            postParams.add(new BasicNameValuePair("close_text", mCloseText.getText().toString()));
                        }
                        
                        switch(mCloseOpts.getCheckedRadioButtonId())
                        {
                            case R.id.close_only_reg:
                                postParams.add(new BasicNameValuePair("close_access_mode", "6"));
                            break;
                            case R.id.close_only_fav:
                                postParams.add(new BasicNameValuePair("close_access_mode", "1"));
                            break;
                            case R.id.close_only_sub:
                                postParams.add(new BasicNameValuePair("close_access_mode", "5"));
                            break;
                            case R.id.close_only_white:
                                postParams.add(new BasicNameValuePair("close_access_mode", "4"));
                            break;
                            case R.id.close_for_list:
                                postParams.add(new BasicNameValuePair("close_access_mode", "2"));
                                postParams.add(new BasicNameValuePair("access_list", mCloseDenyList.getText().toString()));
                            break;
                            case R.id.close_only_list:
                                postParams.add(new BasicNameValuePair("close_access_mode", "3"));
                                postParams.add(new BasicNameValuePair("access_list", mCloseAllowList.getText().toString()));
                            break;
                            case R.id.close_for_all:
                                postParams.add(new BasicNameValuePair("close_access_mode", "7"));
                            break;
                        }
                    }
                    
                    postParams.add(new BasicNameValuePair("rewrite", "rewrite"));
                    postParams.add(new BasicNameValuePair("save_type", "js2"));
                    
                    mHandler.sendEmptyMessage(HANDLE_DO_POST);
				}
				else if(mTypeId.equals("PostId"))  // если коммент
				{
				    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
	                postParams.add(new BasicNameValuePair("module", "journal"));
	                postParams.add(new BasicNameValuePair("resulttype", "2"));
				    
					postParams.add(new BasicNameValuePair("act", "new_comment_post"));
					postParams.add(new BasicNameValuePair("post_id", mId));
					postParams.add(new BasicNameValuePair("commentid", ""));
					postParams.add(new BasicNameValuePair("referer", ""));
					postParams.add(new BasicNameValuePair("page", "last"));
					postParams.add(new BasicNameValuePair("open_uri", ""));
					
					postParams.add(new BasicNameValuePair("write_from", "0"));
					//postParams.add(new BasicNameValuePair("write_from_name", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
					//postParams.add(new BasicNameValuePair("write_from_pass", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));
					
					postParams.add(new BasicNameValuePair("subscribe", mSubscribe.isChecked() ? "1/" : ""));
					postParams.add(new BasicNameValuePair("attachment1", ""));
					
					mHandler.sendEmptyMessage(HANDLE_DO_COMMENT);
				}
				else if(mTypeId.equals("umailTo"))  // если почта
				{
				    postParams.add(new BasicNameValuePair("module", "umail"));
				    postParams.add(new BasicNameValuePair("act", "umail_send"));
				    postParams.add(new BasicNameValuePair("from_folder", ""));
				    postParams.add(new BasicNameValuePair("to_user", toText.getText().toString()));
				    postParams.add(new BasicNameValuePair("title", mPost.title));
				    postParams.add(new BasicNameValuePair("save_copy", mCopyMessage.isChecked() ? "yes" : ""));
				    postParams.add(new BasicNameValuePair("need_receipt", mGetReceipt.isChecked() ? "yes" : ""));
				    
				    mHandler.sendEmptyMessage(HANDLE_DO_UMAIL);
				}
			break;
			}
			case R.id.left_gradient:
			case R.id.right_gradient:
			{
			    int oldColor = getColorFromPicture((ImageButton) view);
			    final ImageButton imgbutton = (ImageButton) view;
			    AmbilWarnaDialog dialog = new AmbilWarnaDialog(MessageSender.this, oldColor, new AmbilWarnaDialog.OnAmbilWarnaListener() 
			    {
			        public void onOk(AmbilWarnaDialog dialog, int color) 
			        {
			            ColorDrawable newColor = new ColorDrawable(color);
			            ((ImageButton) imgbutton).setImageDrawable(newColor);
			        }
			                
			        public void onCancel(AmbilWarnaDialog dialog) 
			        {
			        }
			    });
			    dialog.show();
			}
			break;
			case R.id.set_gradient:
			{
			    int startColor = getColorFromPicture(mLeftGradient);
			    int endColor = getColorFromPicture(mRightGradient);
			    
			    CharSequence text = contentText.getText();
			    String newText = "";
			    int length = text.length();
			    for(int i = 0; i < text.length(); i++)
			    {
			        char current = text.charAt(i);
			        if (current == ' ' || current == '\n')
			        {
			            newText += current;
			            continue;
			        }
			        int newRed = ((Color.red(startColor) - Color.red(startColor) * i / length) + (Color.red(endColor) - Color.red(endColor) * (length - i) / text.length()));
			        int newGreen = ((Color.green(startColor) - Color.green(startColor) * i / length) + (Color.green(endColor) - Color.green(endColor) * (length - i) / text.length()));
			        int newBlue = ((Color.blue(startColor) - Color.blue(startColor) * i / length) + (Color.blue(endColor) - Color.blue(endColor) * (length - i) / text.length()));
			        String red = String.format("%02X", newRed > 0xFF ? 0xFF : newRed);
			        String green = String.format("%02X", newGreen > 0xFF ? 0xFF : newGreen);
			        String blue = String.format("%02X", newBlue > 0xFF ? 0xFF : newBlue);
			        
			        String addiction = "<span style=\"color: #" + red + green + blue + "\">" + current + "</span>";
			        newText += addiction;
			    }
			    contentText.setText(newText);
			}
		    break;
		}
	}
	
	public int getColorFromPicture(ImageButton view)
	{
	    Drawable old = ((ImageButton) view).getDrawable();
        Bitmap example = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        Canvas tCanvas = new Canvas(example);
        old.draw(tCanvas);
        return example.getPixel(0, 0);
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
	{
		switch(buttonView.getId())
		{
			case R.id.message_optional:
				if(isChecked)
					for(View view : optionals)
						view.setVisibility(View.VISIBLE);
				else
					for(View view : optionals)
						view.setVisibility(View.GONE);
			break;
			case R.id.message_poll:
				if(isChecked)
					for(View view : pollScheme)
						view.setVisibility(View.VISIBLE);
				else
					for(View view : pollScheme)
						view.setVisibility(View.GONE);
			break;
			case R.id.message_close:
				if(isChecked)
					mCloseOpts.setVisibility(View.VISIBLE);
				else
					mCloseOpts.setVisibility(View.GONE);
			default:
			break;
		}
	}
	
	public void onCheckedChanged(RadioGroup group, int checkedId) 
	{
		switch(checkedId)
		{
			case R.id.close_for_list:
				mCloseDenyList.setVisibility(View.VISIBLE);
				mCloseAllowList.setVisibility(View.GONE);
			break;
			case R.id.close_only_list:
				mCloseAllowList.setVisibility(View.VISIBLE);
				mCloseDenyList.setVisibility(View.GONE);
			break;
			default:
				mCloseDenyList.setVisibility(View.GONE);
				mCloseAllowList.setVisibility(View.GONE);
			break;
		}
	}
}
