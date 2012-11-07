package adonai.diary_browser;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import adonai.diary_browser.entities.Post;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioGroup;

public class MessageSender extends Activity implements OnClickListener, OnCheckedChangeListener, android.widget.RadioGroup.OnCheckedChangeListener
{

	private static final int HANDLE_DO_POST = 0;
	private static final int HANDLE_DO_COMMENT = 1;
	
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
	
	RadioGroup mCloseOpts;
	
	Handler mHandler, mUiHandler;
	Looper mLooper;
	ProgressDialog pd = null;
	
	List<View> optionals = new ArrayList<View>();
	List<View> pollScheme = new ArrayList<View>();
	List<View> closeScheme = new ArrayList<View>();
	List<NameValuePair> postParams;
	
	String mSignature = null;
	String mDiaryId = null;
	String mPostId = null;
	
	DiaryHttpClient mDHCL;
	UserData mUser;
	Post mPost;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mDHCL = Globals.mDHCL;
        mUser = Globals.mUser;
        mPost = new Post();
		postParams = new ArrayList<NameValuePair>();
        
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, HttpCallback);
        mUiHandler = new Handler(UiCallback);
        
        setContentView(R.layout.message_sender_a);
        
    	titleText = (EditText) findViewById(R.id.message_title);
    	contentText = (EditText) findViewById(R.id.message_content);
    	themesText = (EditText) findViewById(R.id.message_themes);
    	musicText = (EditText) findViewById(R.id.message_music);
    	moodText = (EditText) findViewById(R.id.message_mood);
    	mPublish = (Button) findViewById(R.id.message_publish);
    	mPublish.setOnClickListener(this);
    	
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
        	switch (message.what)
        	{
        		case HANDLE_DO_POST:
					try 
					{
						mDHCL.postPage(mUser.currentDiaryPosts.get_diary_URL() + "diary.php", new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
						mUiHandler.sendEmptyMessage(HANDLE_DO_POST);
					} 
					catch (UnsupportedEncodingException e) 
					{
						e.printStackTrace();
					}
        			return true;
        		case HANDLE_DO_COMMENT:
					try 
					{
						mDHCL.postPage(mUser.currentPostComments.get_diary_URL() + "diary.php", new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
						mUiHandler.sendEmptyMessage(HANDLE_DO_COMMENT);
					} 
					catch (UnsupportedEncodingException e) 
					{
						e.printStackTrace();
					}
        			return true;
        		default:
        			break;
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
        			// Пост опубликован, возвращаемся
        			pd.dismiss();
        			
    				Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
    				returnIntent.putExtra("reloadContent", true);
    				returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    				startActivity(returnIntent);
					finish();
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
        	finish();
        
        // обязательно
        mSignature = intent.getStringExtra("signature");
        
        // одно из двух
        mDiaryId = intent.getStringExtra("DiaryId");
        mPostId = intent.getStringExtra("PostId");
        
        // если это комментарий
        if (mDiaryId == null)
        {
        	mShowPoll.setVisibility(View.GONE);
        	mShowOptionals.setVisibility(View.GONE);
        	mShowAndClose.setVisibility(View.GONE);
        	titleText.setVisibility(View.GONE);
        	
        	mSubscribe.setVisibility(View.VISIBLE);
        }
        
        super.onStart();
    }

	public void onClick(View view) 
	{
		switch(view.getId())
		{
			case R.id.message_publish:
			{
				// TODO: Сохранение в черновики
				// Задел на будущее - для сохранения в черновики
				mPost.set_title(titleText.getText().toString());
				mPost.set_text(new Spannable.Factory().newSpannable(contentText.getText().toString()));
				mPost.set_themes(themesText.getText().toString());
				mPost.set_music(musicText.getText().toString());
				mPost.set_mood(moodText.getText().toString());
				
				postParams.add(new BasicNameValuePair("module", "journal"));
				postParams.add(new BasicNameValuePair("action", "dosend"));
				postParams.add(new BasicNameValuePair("resulttype", "2"));
				// Добавляем параметры из настроек
				postParams.add(new BasicNameValuePair("message", mPost.get_text().toString() + Globals.mSharedPrefs.getString("post.signature", "")));
				postParams.add(new BasicNameValuePair("signature", mSignature));
				pd = ProgressDialog.show(MessageSender.this, getString(R.string.loading), getString(R.string.sending_data), true, true);
				
				// Если пост
				if(mDiaryId != null)
				{
					postParams.add(new BasicNameValuePair("act", "new_post_post"));
					postParams.add(new BasicNameValuePair("post_id", ""));
					postParams.add(new BasicNameValuePair("journal_id", mDiaryId));
					postParams.add(new BasicNameValuePair("referer", Globals.currentURL));
					postParams.add(new BasicNameValuePair("post_type", ""));
					
					postParams.add(new BasicNameValuePair("title", mPost.get_title()));
					if(mShowOptionals.isChecked())
					{
						postParams.add(new BasicNameValuePair("themes", mPost.get_themes() + Globals.mSharedPrefs.getString("post.tags", "")));
						postParams.add(new BasicNameValuePair("current_music", mPost.get_music()));
						postParams.add(new BasicNameValuePair("current_mood", mPost.get_mood()));
					}
					else
					{
						postParams.add(new BasicNameValuePair("themes", ""));
						postParams.add(new BasicNameValuePair("current_music", ""));
						postParams.add(new BasicNameValuePair("current_mood", ""));
					}

					postParams.add(new BasicNameValuePair("attachment", ""));
					postParams.add(new BasicNameValuePair("close_text", ""));
					
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
						
					}
					
					postParams.add(new BasicNameValuePair("rewrite", "rewrite"));
					postParams.add(new BasicNameValuePair("save_type", "js2"));
					
					mHandler.sendEmptyMessage(HANDLE_DO_POST);
				}
				else // если коммент
				{
					postParams.add(new BasicNameValuePair("act", "new_comment_post"));
					postParams.add(new BasicNameValuePair("post_id", mPostId));
					postParams.add(new BasicNameValuePair("commentid", ""));
					postParams.add(new BasicNameValuePair("referer", ""));
					postParams.add(new BasicNameValuePair("page", "last"));
					postParams.add(new BasicNameValuePair("open_uri", ""));
					
					postParams.add(new BasicNameValuePair("write_from", "0"));
					postParams.add(new BasicNameValuePair("write_from_name", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
					postParams.add(new BasicNameValuePair("write_from_pass", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));
					
					postParams.add(new BasicNameValuePair("subscribe", mSubscribe.isChecked() ? "1/" : ""));
					postParams.add(new BasicNameValuePair("attachment1", ""));
					
					mHandler.sendEmptyMessage(HANDLE_DO_COMMENT);
				}
			break;
			}
		}
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
			break;
			case R.id.close_only_list:
				mCloseAllowList.setVisibility(View.VISIBLE);
			break;
			default:
				mCloseDenyList.setVisibility(View.GONE);
				mCloseAllowList.setVisibility(View.GONE);
			break;
		}
	}
}
