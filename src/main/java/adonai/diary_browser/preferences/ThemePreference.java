package adonai.diary_browser.preferences;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.util.Map;

import adonai.diary_browser.NetworkService;
import adonai.diary_browser.R;
import adonai.diary_browser.theming.HotLayoutInflater;

/**
 * Created by adonai on 09.10.14.
 */
public class ThemePreference extends DialogPreference {
    private Map<String, String> mCssMappings;

    @TargetApi(Build.VERSION_CODES.L)
    public ThemePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ThemePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ThemePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.L)
    public ThemePreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateDialogView() {
        final LinearLayout verticalContainer = new LinearLayout(getContext()); // global container
        verticalContainer.setOrientation(LinearLayout.VERTICAL);

        mCssMappings = NetworkService.getCssColors(getContext());
        for(final Map.Entry<String, String> pair : mCssMappings.entrySet()) {
            LinearLayout item = (LinearLayout) HotLayoutInflater.from(getContext()).inflate(R.layout.color_list_item, verticalContainer, false);
            TextView label = (TextView) item.findViewById(R.id.text_label);
            label.setText(pair.getKey());
            final View colorView = item.findViewById(R.id.color_view);
            final int originalColor = Color.parseColor(pair.getValue());
            colorView.setBackgroundColor(originalColor);
            colorView.setOnClickListener(new CssOnClickListener(originalColor, colorView, pair));
            verticalContainer.addView(item);
        }
        return verticalContainer;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE) {
            NetworkService.replaceCssColors(getContext(), mCssMappings);
        } else
            super.onClick(dialog, which);
    }

    private class CssOnClickListener implements View.OnClickListener {
        private final int originalColor;
        private final View colorView;
        private final Map.Entry<String, String> pair;

        public CssOnClickListener(int originalColor, View colorView, Map.Entry<String, String> pair) {
            this.originalColor = originalColor;
            this.colorView = colorView;
            this.pair = pair;
        }

        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LinearLayout colorPickerView = (LinearLayout) HotLayoutInflater.from(getContext()).inflate(R.layout.color_picker, null, false);
            final ColorPicker cp = (ColorPicker) colorPickerView.findViewById(R.id.picker);
            final SaturationBar sBar = (SaturationBar) colorPickerView.findViewById(R.id.saturation_bar);
            final ValueBar vBar = (ValueBar) colorPickerView.findViewById(R.id.value_bar);
            //final OpacityBar opBar = (OpacityBar) colorPickerView.findViewById(R.id.opacitybar);
            cp.setColor(originalColor);
            //opBar.setColor(originalColor);
            sBar.setColor(originalColor);
            vBar.setColor(originalColor);
            //cp.addOpacityBar(opBar);
            cp.addSaturationBar(sBar);
            cp.addValueBar(vBar);
            builder.setView(colorPickerView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    colorView.setBackgroundColor(cp.getColor());
                    pair.setValue(String.format("#%06X", 0xFFFFFF & cp.getColor()));
                }
            }).create().show();
        }
    }
}
