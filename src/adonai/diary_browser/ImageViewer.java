package adonai.diary_browser;

import java.io.File;

import contrib.gesture.GestureImageView;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;

public class ImageViewer extends Activity implements OnClickListener 
{
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        String addr = getIntent().getStringExtra("image_file");
        if(addr == null)
        {
            finish();
            return;
        }
        
        File file = new File(addr); // Already checked at intent sending
        BitmapDrawable sendDrawable = (BitmapDrawable) BitmapDrawable.createFromPath(file.getAbsolutePath());
        sendDrawable.setBounds(0, 0, sendDrawable.getIntrinsicWidth(), sendDrawable.getIntrinsicHeight());
        
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        GestureImageView view = new GestureImageView(this);
        view.setOnClickListener(this);
        view.setImageDrawable(sendDrawable);
        view.setLayoutParams(params);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        getWindow().setBackgroundDrawableResource(android.R.color.background_dark);
        setContentView(view);
    }
	
    @Override
	protected void onStart() 
    {
		super.onStart();
	}

	public void onClick(View v) 
	{
		
	}
}
