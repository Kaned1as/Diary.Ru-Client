package adonai.diary_browser;

import contrib.gesture.GestureImageView;
import android.R.color;
import android.os.Bundle;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

public class ImageViewer extends Activity 
{
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        GestureImageView view = new GestureImageView(this);
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
		if (Globals.tempDrawable == null || !(Globals.tempDrawable instanceof BitmapDrawable))
			finish();
	}
}
