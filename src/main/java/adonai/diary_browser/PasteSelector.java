package adonai.diary_browser;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;


public class PasteSelector extends DialogFragment {
    CheckBox mShouldPaste;
    private View.OnClickListener selectorHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            acceptDialogClick(v, mShouldPaste.isChecked());

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
            if (layout.getChildAt(i) instanceof Button)  // Кнопки вставки, а не чекбокс, к примеру
                layout.getChildAt(i).setOnClickListener(selectorHandler);

        builder.setTitle(R.string.menu_special_paste);
        builder.setView(mainView);

        return builder.create();
    }

    @SuppressWarnings("deprecation")
    public void acceptDialogClick(View view, boolean pasteClipboard) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        MessageSenderFragment msf = (MessageSenderFragment) fm.findFragmentById(R.id.message_pane);
        if(msf == null)
            return;

        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        CharSequence paste = clipboard.getText();
        if (paste == null || !pasteClipboard)
            paste = "";

        switch (view.getId()) {
            case R.id.button_bold:
                msf.insertInCursorPosition("<b>" + paste.toString() + "</b>");
                break;
            case R.id.button_italic:
                msf.insertInCursorPosition("<i>" + paste.toString() + "</i>");
                break;
            case R.id.button_underlined:
                msf.insertInCursorPosition("<u>" + paste.toString() + "</u>");
                break;
            case R.id.button_nick:
                msf.insertInCursorPosition("[L]" + paste.toString() + "[/L]");
                break;
            case R.id.button_link:
                msf.insertInCursorPosition("<a href=\"" + paste.toString() + "\">" + paste.toString() + "</a>");
                break;
            case R.id.button_more:
                msf.insertInCursorPosition("[MORE=" + getString(R.string.read_more) + "]" + paste.toString() + "[/MORE]");
                break;
            case R.id.button_offtopic:
                msf.insertInCursorPosition("<span class='offtop'>" + paste.toString() + "</span>");
                break;
            case R.id.button_stroked:
                msf.insertInCursorPosition("<s>" + paste.toString() + "</s>");
                break;
            case R.id.button_image:
                if (pasteClipboard) {
                    msf.insertInCursorPosition("<img src=\"" + paste.toString() + "\" />");
                } else
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), Utils.ACTIVITY_ACTION_REQUEST_IMAGE);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(getActivity(), getString(R.string.no_file_manager_found), Toast.LENGTH_SHORT).show();
                    }
                break;
        }
    }
}
