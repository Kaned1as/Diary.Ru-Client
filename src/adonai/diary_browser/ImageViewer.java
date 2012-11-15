package adonai.diary_browser;

import contrib.gesture.GestureImageView;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.app.Activity;

public class ImageViewer extends Activity implements OnClickListener 
{
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        GestureImageView view = new GestureImageView(this);
        view.setOnClickListener(this);
        view.setImageDrawable(Globals.tempDrawable);
        view.setLayoutParams(params);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        getWindow().setBackgroundDrawableResource(android.R.color.background_dark);
        setContentView(view);
    }
	
    @Override
	protected void onStart() 
    {
		super.onStart();
		if (Globals.tempDrawable == null)
			finish();
	}

	public void onClick(View v) 
	{
		
	}
}
