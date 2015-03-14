package adonai.diary_browser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.AlertDialogWrapper;


public class PasteSelector extends DialogFragment {
    CheckBox mShouldPaste;
    private View.OnClickListener selectorHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (getActivity() instanceof PasteAcceptor)
                ((PasteAcceptor) getActivity()).acceptDialogClick(v, mShouldPaste.isChecked());

            dismiss();
        }
    };

    public static PasteSelector newInstance() {
        return new PasteSelector();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View mainView = inflater.inflate(R.layout.special_paste_selector, null);

        LinearLayout layout = (LinearLayout) mainView.findViewById(R.id.selector_layout);
        mShouldPaste = (CheckBox) layout.findViewById(R.id.checkbox_paste_clip);

        for (int i = 0; i < layout.getChildCount(); i++)
            if (layout.getChildAt(i).getClass().equals(Button.class))  // Кнопки вставки, а не чекбокс, к примеру
                layout.getChildAt(i).setOnClickListener(selectorHandler);

        builder.setView(mainView);

        return builder.create();
    }

    public interface PasteAcceptor {
        void acceptDialogClick(View view, boolean pasteClipboard);
    }
}
