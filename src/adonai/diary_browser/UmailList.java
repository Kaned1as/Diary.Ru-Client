package adonai.diary_browser;

import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;

public class UmailList extends DiaryList
{
    
    Map<String, String> namesUrls;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diary_list_item);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        
        mTabHost.addTab(mTabHost.newTabSpec("tab_favourites").setIndicator(getString(R.string.favourites)).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_posts").setIndicator(getString(R.string.posts)).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_owndiary").setIndicator(getString(R.string.my_diary)).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_owndiary").setIndicator("").setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_discussions").setIndicator(getString(R.string.discussions)).setContent(android.R.id.tabcontent));
        mTabHost.addTab(mTabHost.newTabSpec("tab_discussions_newest").setIndicator("").setContent(android.R.id.tabcontent));
        mTabHost.getCurrentView().setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
        returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(returnIntent);
        finish();
    }
    
}
