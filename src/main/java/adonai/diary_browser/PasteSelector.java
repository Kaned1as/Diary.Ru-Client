package adonai.diary_browser;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class PasteSelector extends DialogFragment {

    SwitchCompat mShouldPaste;

    private View.OnClickListener selectorHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(acceptDialogClick(v, mShouldPaste.isChecked())) {
                dismiss();
            }
        }
    };

    public static PasteSelector newInstance() {
        return new PasteSelector();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View mainView = inflater.inflate(R.layout.special_paste_selector, null);

        LinearLayout layout = (LinearLayout) mainView.findViewById(R.id.selector_layout);
        mShouldPaste = (SwitchCompat) layout.findViewById(R.id.checkbox_paste_clip);

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof Button) {  // Кнопки вставки
                child.setOnClickListener(selectorHandler);
            }
        }

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.menu_special_paste)
                .customView(mainView, false)
                .build();
    }

    @SuppressWarnings("deprecation")
    public boolean acceptDialogClick(View view, boolean pasteClipboard) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        MessageSenderFragment msf = (MessageSenderFragment) fm.findFragmentById(R.id.message_pane);
        if(msf == null)
            return true;

        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        CharSequence paste = clipboard.getText();
        if (paste == null || !pasteClipboard)
            paste = "";

        switch (view.getId()) {
            case R.id.button_bold:
                msf.insertInCursorPosition("<b>", paste.toString(), "</b>");
                break;
            case R.id.button_italic:
                msf.insertInCursorPosition("<i>", paste.toString(), "</i>");
                break;
            case R.id.button_underlined:
                msf.insertInCursorPosition("<u>", paste.toString(), "</u>");
                break;
            case R.id.button_nick:
                msf.insertInCursorPosition("[L]", paste.toString(), "[/L]");
                break;
            case R.id.button_link:
                msf.insertInCursorPosition("<a href=\"" + paste.toString() + "\">", paste.toString(), "</a>");
                break;
            case R.id.button_more:
                msf.insertInCursorPosition("[MORE=" + getString(R.string.read_more) + "]", paste.toString(), "[/MORE]");
                break;
            case R.id.button_offtopic:
                msf.insertInCursorPosition("<span class='offtop'>", paste.toString(), "</span>");
                break;
            case R.id.button_stroked:
                msf.insertInCursorPosition("<s>", paste.toString(), "</s>");
                break;
            case R.id.button_image:
                if(!checkAndRequestPermissions())
                    return false;

                if (pasteClipboard) {
                    msf.insertInCursorPosition("<img src=\"", paste.toString(), "\" />");
                } else
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        msf.startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), Utils.ACTIVITY_ACTION_REQUEST_IMAGE);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(getActivity(), getString(R.string.no_file_manager_found), Toast.LENGTH_SHORT).show();
                    }
                break;
            case R.id.button_mp3:
                if(!checkAndRequestPermissions())
                    return false;

                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    msf.startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), Utils.ACTIVITY_ACTION_REQUEST_MUSIC);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), getString(R.string.no_file_manager_found), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.button_gif:
                if(!checkAndRequestPermissions())
                    return false;

                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/gif");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    msf.startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), Utils.ACTIVITY_ACTION_REQUEST_GIF);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), getString(R.string.no_file_manager_found), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return true;
    }

    private boolean checkAndRequestPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), WRITE_EXTERNAL_STORAGE);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{WRITE_EXTERNAL_STORAGE},
                    Utils.FROM_MESSAGE_SENDER);
            return false;
        }
        return true;
    }
}
