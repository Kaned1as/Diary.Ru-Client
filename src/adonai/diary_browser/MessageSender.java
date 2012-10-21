package adonai.diary_browser;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import adonai.diary_browser.entities.Post;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class MessageSender extends Activity implements OnClickListener, OnCheckedChangeListener
{
	TextView titleText;
	TextView contentText;
	TextView themesText;
	TextView musicText;
	TextView moodText;
	Button mPublish;
	CheckBox mShowOptionals;
	CheckBox mShowPoll;
	
	List<View> optionals = new ArrayList<View>();
	List<View> pollScheme = new ArrayList<View>();
	
	String mSignature = null;
	String mDiaryId = null;
	
	DiaryHttpClient mDHCL;
	Post mPost;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mDHCL = Globals.mDHCL;
        mPost = new Post();
        
        setContentView(R.layout.message_sender_a);
        
    	titleText = (TextView) findViewById(R.id.message_title);
    	contentText = (TextView) findViewById(R.id.message_content);
    	themesText = (TextView) findViewById(R.id.message_themes);
    	musicText = (TextView) findViewById(R.id.message_music);
    	moodText = (TextView) findViewById(R.id.message_mood);
    	mPublish = (Button)findViewById(R.id.message_publish);
    	mPublish.setOnClickListener(this);
    	
    	mShowOptionals = (CheckBox) findViewById(R.id.message_optional);
    	mShowOptionals.setOnCheckedChangeListener(this);
    	mShowPoll = (CheckBox) findViewById(R.id.message_poll);
    	
    	optionals.add(findViewById(R.id.message_themes_hint));
    	optionals.add(themesText);
    	optionals.add(findViewById(R.id.message_music_hint));
    	optionals.add(musicText);
    	optionals.add(findViewById(R.id.message_mood_hint));
    	optionals.add(moodText);
    }

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
        
        mSignature = intent.getStringExtra("signature");
        mDiaryId = intent.getStringExtra("DiaryId");
        
        super.onStart();
    }

	public void onClick(View view) 
	{
		switch(view.getId())
		{
			case R.id.message_publish:
				
				// TODO: Сохранение в черновики
				// Задел на будущее - для сохранения в черновики
				mPost.set_title(titleText.getText().toString());
				mPost.set_text(Html.fromHtml(contentText.getText().toString()));
				mPost.set_themes(themesText.getText().toString());
				mPost.set_music(musicText.getText().toString());
				mPost.set_mood(moodText.getText().toString());

				try 
				{
					List<NameValuePair> postParams = new ArrayList<NameValuePair>();
					postParams.add(new BasicNameValuePair("act", "new_post_post"));
					postParams.add(new BasicNameValuePair("module", "journal"));
					postParams.add(new BasicNameValuePair("post_id", ""));
					postParams.add(new BasicNameValuePair("journal_id", mDiaryId));
					postParams.add(new BasicNameValuePair("referer", mDHCL.lastURL));
					postParams.add(new BasicNameValuePair("action", "dosend"));
					postParams.add(new BasicNameValuePair("post_type", ""));
					
					postParams.add(new BasicNameValuePair("title", mPost.get_title()));
					postParams.add(new BasicNameValuePair("message", mPost.get_text().toString()));
					postParams.add(new BasicNameValuePair("themes", mPost.get_themes()));
					postParams.add(new BasicNameValuePair("current_music", mPost.get_music()));
					postParams.add(new BasicNameValuePair("current_mood", mPost.get_mood()));
					
					postParams.add(new BasicNameValuePair("attachment", ""));
					postParams.add(new BasicNameValuePair("close_text", ""));
					
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
					
					postParams.add(new BasicNameValuePair("rewrite", "rewrite"));
					postParams.add(new BasicNameValuePair("save_type", "js2"));
					postParams.add(new BasicNameValuePair("signature", mSignature));
					
					mDHCL.postPage(mDHCL.lastURL + "diary.php", new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
					
					// Пост опубликован, возвращаемся
					Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
					returnIntent.putExtra("reloadContent", true);
					returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(returnIntent);
					finish();
					
				} 
				catch (UnsupportedEncodingException e) 
				{
					e.printStackTrace();
				}
				
			break;
		}
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
	{

		
	}
    
}
