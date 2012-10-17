package adonai.diary_browser;

import contrib.gesture.GestureImageView;
import android.R.color;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;

public class ImageViewer extends Activity 
{
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        GestureImageView view = new GestureImageView(this);
        view.setBackgroundColor(color.widget_edittext_dark);
        view.setImageDrawable(Globals.tempDrawable);
        view.setLayoutParams(params);

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
