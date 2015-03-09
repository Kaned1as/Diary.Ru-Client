package adonai.diary_browser.preferences;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.util.Map;

import adonai.diary_browser.NetworkService;
import adonai.diary_browser.R;

/**
 * Created by adonai on 09.10.14.
 */
public class ThemePreference extends DialogPreference {
    private Map<String, String> mCssMappings;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ThemePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ThemePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ThemePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ThemePreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateDialogView() {
        final ScrollView sv = new ScrollView(getContext());
        final LinearLayout verticalContainer = new LinearLayout(getContext()); // global container
        verticalContainer.setOrientation(LinearLayout.VERTICAL);
        verticalContainer.setBackgroundColor(Color.parseColor("#10000000"));
        sv.addView(verticalContainer);

        TextView cssColors = new TextView(getContext());
        cssColors.setTextSize(16);
        cssColors.setGravity(Gravity.CENTER_HORIZONTAL);
        cssColors.setTypeface(null, Typeface.BOLD);
        cssColors.setText(R.string.web_themes);
        verticalContainer.addView(cssColors);

        mCssMappings = NetworkService.getCssColors(getContext());
        for (final Map.Entry<String, String> pair : mCssMappings.entrySet()) {
            LinearLayout item = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.color_list_item, verticalContainer, false);
            TextView label = (TextView) item.findViewById(R.id.text_label);
            label.setText(pair.getKey());
            final View colorView = item.findViewById(R.id.color_view);
            final int originalColor = Color.parseColor(pair.getValue());
            colorView.setBackgroundColor(originalColor);
            colorView.setOnClickListener(new CssOnClickListener(originalColor, colorView, pair));
            verticalContainer.addView(item);
        }

        return sv;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        //builder.setNeutralButton(R.string.reset, this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                NetworkService.replaceCssColors(getContext(), mCssMappings);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                super.onClick(dialog, which);
                break;
        }
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
            LinearLayout colorPickerView = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.color_picker, null, false);
            final ColorPicker cp = (ColorPicker) colorPickerView.findViewById(R.id.picker);
            final SaturationBar sBar = (SaturationBar) colorPickerView.findViewById(R.id.saturation_bar);
            final ValueBar vBar = (ValueBar) colorPickerView.findViewById(R.id.value_bar);
            final OpacityBar opBar = (OpacityBar) colorPickerView.findViewById(R.id.opacitybar);
            final EditText colorChooserEdit = (EditText) colorPickerView.findViewById(R.id.certain_color_selector);
            cp.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
                @Override
                public void onColorChanged(int i) {
                    colorChooserEdit.setText(String.format("%06x", 0xFFFFFF & cp.getColor()));
                }
            });
            colorChooserEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    String text = s.toString();
                    if (s.length() == 6) {
                        try {
                            int color = Color.parseColor("#" + text);
                            if (cp.getColor() != color) {
                                cp.setColor(color);
                            }
                        } catch (IllegalArgumentException ignored) {

                        }
                    }
                }
            });
            opBar.setVisibility(View.GONE);
            cp.addSaturationBar(sBar);
            cp.addValueBar(vBar);
            cp.setColor(originalColor);

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

