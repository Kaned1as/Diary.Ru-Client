package adonai.diary_browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.Post;
import adonai.diary_browser.entities.Umail;
import adonai.diary_browser.theming.HotLayoutInflater;

public class MessageSenderFragment extends Fragment implements OnClickListener, android.widget.CompoundButton.OnCheckedChangeListener, android.widget.RadioGroup.OnCheckedChangeListener
{

    private static final int HANDLE_DO_POST 		= 0;
    private static final int HANDLE_DO_COMMENT 		= 1;
    private static final int HANDLE_DO_UMAIL 		= 2;
    private static final int HANDLE_UMAIL_ACK 		= 3;
    private static final int HANDLE_UMAIL_REJ 		= 4;
    private static final int HANDLE_REQUEST_AVATARS = 5;
    private static final int HANDLE_SET_AVATAR      = 6;
    private static final int HANDLE_PROGRESS        = 8;
    private static final int HANDLE_GET_SMILIES     = 9;

    Button mPublish;
    Button mShowSmilies;

    EditText toText;
    EditText titleText;
    EditText contentText;
    EditText themesText;
    EditText musicText;
    EditText moodText;
    CheckBox mShowOptionals;
    CheckBox mShowPoll;
    CheckBox mSubscribe;
    CheckBox mShowCloseOptions;
    CheckBox mGetReceipt;
    CheckBox mRequote;
    CheckBox mCopyMessage;
    CheckBox mCustomAvatar;
    CheckBox mNoComments;
    RadioGroup mSecureOptions;
    TextView mTitle;
    TextView mCurrentPage;

    EditText mUserLogin;
    EditText mUserPassword;

    EditText mPollTitle;
    EditText mPollChoice1;
    EditText mPollChoice2;
    EditText mPollChoice3;
    EditText mPollChoice4;
    EditText mPollChoice5;
    EditText mPollChoice6;
    EditText mPollChoice7;
    EditText mPollChoice8;
    EditText mPollChoice9;
    EditText mPollChoice10;

    EditText mCloseAllowList;
    EditText mCloseDenyList;
    EditText mCloseText;

    RadioGroup mCloseOpts;

    Handler mHandler, mUiHandler;
    Looper mLooper;
    ProgressDialog pd;

    LinearLayout mAvatars, mOptionals, mPoll, mSmilies, mSmilieButtons, mPredefinedThemes, mMainLayout, mAsUserLayout;
    List<View> postElements = new ArrayList<>();
    List<View> commentElements = new ArrayList<>();
    List<View> umailElements = new ArrayList<>();

    List<NameValuePair> postParams;

    String mSignature;
    NetworkService mService;

    SparseArray<Object> avatarMap;
    Map<String, Object> smileMap;

    DiaryHttpClient mDHCL;
    String mSendURL = "";
    Comment mPost;

    CacheManager mCache = CacheManager.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View sender = HotLayoutInflater.wrap(inflater).inflate(R.layout.fragment_message_sender, container, false);
        mPost = new Post();
        postParams = new ArrayList<>();

        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, HttpCallback);
        mUiHandler = new Handler(UiCallback);

        mMainLayout = (LinearLayout) sender.findViewById(R.id.message_main_layout);

        toText = (EditText) sender.findViewById(R.id.message_to);
        mGetReceipt = (CheckBox) sender.findViewById(R.id.message_getreceipt);
        mRequote = (CheckBox) sender.findViewById(R.id.message_requote);
        mCopyMessage = (CheckBox) sender.findViewById(R.id.message_copy);

        titleText = (EditText) sender.findViewById(R.id.message_title);
        contentText = (EditText) sender.findViewById(R.id.message_content);
        themesText = (EditText) sender.findViewById(R.id.message_themes);
        musicText = (EditText) sender.findViewById(R.id.message_music);
        moodText = (EditText) sender.findViewById(R.id.message_mood);
        mPublish = (Button) sender.findViewById(R.id.message_publish);
        mPublish.setOnClickListener(this);
        mTitle = (TextView) sender.findViewById(R.id.fragment_title);
        mCurrentPage = (TextView) sender.findViewById(R.id.fragment_page);

        final LinearLayout specials = (LinearLayout) sender.findViewById(R.id.message_specials);
        for(int i = 0; i < specials.getChildCount(); ++i)
            specials.getChildAt(i).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int cursorPos = contentText.getSelectionStart();
                    if(cursorPos == -1)
                        cursorPos = contentText.getText().length();

                    String toPaste = ((Button)view).getText().toString();
                    contentText.setText(contentText.getText().toString().substring(0, cursorPos) + toPaste + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                    contentText.setSelection(contentText.getText().toString().indexOf(toPaste, cursorPos) + 1);
                }
            });

        mPollTitle = (EditText) sender.findViewById(R.id.message_poll_title);
        mPollChoice1 = (EditText) sender.findViewById(R.id.message_poll_1);
        mPollChoice2 = (EditText) sender.findViewById(R.id.message_poll_2);
        mPollChoice3 = (EditText) sender.findViewById(R.id.message_poll_3);
        mPollChoice4 = (EditText) sender.findViewById(R.id.message_poll_4);
        mPollChoice5 = (EditText) sender.findViewById(R.id.message_poll_5);
        mPollChoice6 = (EditText) sender.findViewById(R.id.message_poll_6);
        mPollChoice7 = (EditText) sender.findViewById(R.id.message_poll_7);
        mPollChoice8 = (EditText) sender.findViewById(R.id.message_poll_8);
        mPollChoice9 = (EditText) sender.findViewById(R.id.message_poll_9);
        mPollChoice10 = (EditText) sender.findViewById(R.id.message_poll_10);

        mCloseOpts = (RadioGroup) sender.findViewById(R.id.close_opts);
        mCloseOpts.setOnCheckedChangeListener(this);
        mCloseAllowList = (EditText) sender.findViewById(R.id.close_allowed_list);
        mCloseDenyList = (EditText) sender.findViewById(R.id.close_denied_list);
        mCloseText = (EditText) sender.findViewById(R.id.close_text);

        mNoComments = (CheckBox) sender.findViewById(R.id.message_no_comments);

        mCustomAvatar = (CheckBox) sender.findViewById(R.id.message_custom_avatar);
        mAvatars = (LinearLayout) sender.findViewById(R.id.message_avatars);
        mCustomAvatar.setOnCheckedChangeListener(this);

        mShowOptionals = (CheckBox) sender.findViewById(R.id.message_optional);
        mShowOptionals.setOnCheckedChangeListener(this);
        mOptionals = (LinearLayout) sender.findViewById(R.id.optionals_layout);
        mShowPoll = (CheckBox) sender.findViewById(R.id.message_poll);
        mShowPoll.setOnCheckedChangeListener(this);
        mPoll = (LinearLayout) sender.findViewById(R.id.poll_layout);
        mSubscribe = (CheckBox) sender.findViewById(R.id.message_subscribe);
        mShowCloseOptions = (CheckBox) sender.findViewById(R.id.message_close);
        mShowCloseOptions.setOnCheckedChangeListener(this);

        mSecureOptions = (RadioGroup) sender.findViewById(R.id.message_security);
        mSecureOptions.setOnCheckedChangeListener(this);
        mUserLogin = (EditText) sender.findViewById(R.id.as_user_login);
        mUserPassword = (EditText) sender.findViewById(R.id.as_user_password);
        mAsUserLayout = (LinearLayout) sender.findViewById(R.id.as_user_layout);

        mShowSmilies = (Button) sender.findViewById(R.id.message_show_smilies);
        mShowSmilies.setOnClickListener(this);
        mSmilies = (LinearLayout) sender.findViewById(R.id.message_smilies);
        mSmilieButtons = (LinearLayout) sender.findViewById(R.id.message_smilies_types);
        mPredefinedThemes = (LinearLayout) sender.findViewById(R.id.message_predef_themes);

        commentElements.add(mSubscribe);
        commentElements.add(mSecureOptions);

        postElements.add(sender.findViewById(R.id.message_title_hint));
        postElements.add(titleText);
        postElements.add(mShowOptionals);
        postElements.add(mShowCloseOptions);
        postElements.add(mShowPoll);
        postElements.add(mNoComments);

        umailElements.add(sender.findViewById(R.id.message_to_hint));
        umailElements.add(toText);
        umailElements.add(sender.findViewById(R.id.message_title_hint));
        umailElements.add(titleText);
        umailElements.add(mGetReceipt);
        umailElements.add(mRequote);
        umailElements.add(mCopyMessage);

        return sender;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.message_sender_a, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView()
    {
        mLooper.quit();
        super.onDestroyView();
    }

    Handler.Callback HttpCallback = new Handler.Callback()
    {
        @SuppressWarnings("unchecked")
        public boolean handleMessage(Message message)
        {
            try
            {
                switch (message.what)
                {
                    case HANDLE_DO_POST:
                    case HANDLE_DO_COMMENT:
                    {
                        mDHCL.postPageToString(mSendURL, new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
                        mUiHandler.sendEmptyMessage(message.what);
                        return true;
                    }
                    case HANDLE_DO_UMAIL:
                    {
                        String result = mDHCL.postPageToString(mSendURL, new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
                        if(result.contains("Письмо отправлено"))
                            mUiHandler.sendEmptyMessage(HANDLE_UMAIL_ACK);
                        else
                            mUiHandler.sendEmptyMessage(HANDLE_UMAIL_REJ);
                        return true;
                    }
                    case HANDLE_GET_SMILIES:
                    {
                        String URL = message.obj != null ? (String) message.obj : "http://www.diary.ru/smile.php";
                        String URLValid = URL.replaceAll("[:/]", "_"); // не содержащее недопустимых символов для ФС имя
                        String result;
                        if(mCache.hasData(getActivity(), URLValid))
                            result = new String(mCache.retrieveData(getActivity(), URLValid));
                        else
                        {
                            result = mDHCL.getPageAsString(URL);
                            mCache.cacheData(getActivity(), result.getBytes(), URLValid);
                        }
                        Document rootNode = Jsoup.parse(result);
                        Elements smilies = rootNode.select("tr img");
                        Elements smileLinks = rootNode.select("ul a");

                        smileMap = new HashMap<>();
                        for(Element smilie : smilies)
                            smileMap.put(smilie.attr("alt"), smilie.attr("src"));

                        // распараллеливаем получение смайликов
                        // в массиве теперь будет храниться строка вызова смайлика - задача загрузки аватара
                        ExecutorService executor = Executors.newFixedThreadPool(smileMap.size());
                        for(String id : smileMap.keySet())
                        {
                            final String url = (String) smileMap.get(id);
                            FutureTask<Drawable> future = new FutureTask<>(new Callable<Drawable>()
                            {
                                @Override
                                public Drawable call() throws Exception
                                {
                                    String name = url.substring(url.lastIndexOf('/') + 1);
                                    byte[] outputBytes;

                                    if(mCache.hasData(getActivity(), name))
                                        outputBytes = mCache.retrieveData(getActivity(), name);
                                    else
                                    {
                                        outputBytes = mDHCL.getPageAsByteArray(url);
                                        // caching image
                                        mCache.cacheData(getActivity(), outputBytes, name);
                                    }

                                    Bitmap smilie = BitmapFactory.decodeByteArray(outputBytes, 0, outputBytes.length);
                                    // scale smilie to larger size
                                    DisplayMetrics dm = new DisplayMetrics();
                                    getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
                                    smilie = Bitmap.createScaledBitmap(smilie, (int) (smilie.getWidth() * dm.density), (int) (smilie.getHeight() * dm.density), false);
                                    return new BitmapDrawable(getResources(), smilie);
                                }

                            });
                            smileMap.put(id, future);
                            executor.execute(future);
                        }

                        // по мере выполнения задач переписываем массив по кусочкам на результат задачи
                        while(true)
                        {
                            int remaining = 0;
                            for(String id : smileMap.keySet())
                                if(smileMap.get(id) instanceof FutureTask)
                                {
                                    FutureTask<Drawable> future = (FutureTask<Drawable>) smileMap.get(id);
                                    if(future.isDone())
                                        smileMap.put(id, future.get());
                                    else
                                        remaining++;
                                }
                            if(remaining == 0)
                                break;
                        }

                        mUiHandler.sendMessage(mUiHandler.obtainMessage(HANDLE_GET_SMILIES, smileLinks));
                        return true;
                    }
                    case HANDLE_REQUEST_AVATARS:
                    {
                        String URL = "http://www.diary.ru/options/member/?avatar";
                        String dataPage = mDHCL.getPageAsString(URL);
                        if(dataPage == null)
                            return false;

                        // собираем пары ID аватара - URL аватара
                        Elements avatardivs = Jsoup.parse(dataPage).select("div#avatarbit");
                        avatarMap = new SparseArray<>();
                        for(Element avatarbit : avatardivs)
                        {
                            Integer avId = Integer.valueOf(avatarbit.select("input[name=use_avatar_id]").val());
                            String url = avatarbit.child(0).attr("style");
                            url = url.substring(url.lastIndexOf('(') + 1, url.lastIndexOf(')'));
                            avatarMap.put(avId, url);
                        }

                        // распараллеливаем получение аватарок
                        // в массиве теперь будет храниться ID аватара - задача загрузки аватара
                        ExecutorService executor = Executors.newFixedThreadPool(avatarMap.size());
                        for(int i = 0; i < avatarMap.size(); i++)
                        {
                            final String url = (String) avatarMap.valueAt(i);
                            FutureTask<Drawable> future = new FutureTask<>(new Callable<Drawable>()
                            {
                                @Override
                                public Drawable call() throws Exception
                                {
                                    final byte[] imageBytes = mDHCL.getPageAsByteArray(url);
                                    Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                    return new BitmapDrawable(getResources(), image);
                                }

                            });
                            avatarMap.setValueAt(i, future);
                            executor.execute(future);
                        }

                        // по мере выполнения задач переписываем массив по кусочкам на результат задачи
                        while(true)
                        {
                            int remaining = 0;
                            for(int i = 0; i < avatarMap.size(); i++)
                                if(avatarMap.valueAt(i) instanceof FutureTask)
                                {
                                    FutureTask<Drawable> future = (FutureTask<Drawable>) avatarMap.valueAt(i);
                                    if(future.isDone())
                                        avatarMap.setValueAt(i, future.get());
                                    else
                                        remaining++;
                                }
                            if(remaining == 0)
                                break;
                        }

                        mUiHandler.sendEmptyMessage(HANDLE_REQUEST_AVATARS);
                        return true;
                    }
                    case HANDLE_SET_AVATAR:
                    {
                        String URL = "http://www.diary.ru/options/member/?avatar";
                        mDHCL.postPageToString(URL, new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
                        Toast.makeText(getActivity(), R.string.avatar_selected, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    case Utils.HANDLE_UPLOAD_FILE:
                    {
                        try
                        {
                            File file = new File((String) message.obj);
                            final long length = file.length();
                            MultipartEntity mpEntity = new MultipartEntity();
                            ContentBody cbFile = new FileBody(file, "image/*");
                            mpEntity.addPart("module", new StringBody("photolib"));
                            mpEntity.addPart("signature", new StringBody(mSignature));
                            mpEntity.addPart("resulttype1", new StringBody(String.valueOf(message.arg1)));
                            mpEntity.addPart("attachment1", cbFile);

                            final DiaryHttpClient.ProgressListener listener = new DiaryHttpClient.ProgressListener()
                            {
                                @Override
                                public void transferred(long transferredBytes)
                                {
                                    long percent = (transferredBytes * 100) / length;
                                    mUiHandler.sendMessage(mUiHandler.obtainMessage(HANDLE_PROGRESS, (int)percent));
                                }
                            };

                            String result = mDHCL.postPageToString(mSendURL.substring(0, mSendURL.lastIndexOf('/') + 1) + "diary.php?upload=1&js", mpEntity, listener);
                            if (result != null)
                            {
                                if(result.contains("допустимые:")) // ошибка отправки, слишком большая картинка
                                    Toast.makeText(getActivity(), getString(R.string.too_big_picture), Toast.LENGTH_LONG).show();
                                else {
                                    result = result.substring(result.indexOf("'") + 1, result.indexOf("';"));
                                    if (result.length() > 0)
                                        mUiHandler.sendMessage(mUiHandler.obtainMessage(Utils.HANDLE_UPLOAD_FILE, result));
                                    else
                                        Toast.makeText(getActivity(), getString(R.string.message_send_error), Toast.LENGTH_LONG).show();
                                }
                                pd.dismiss();
                                //resEntity.consumeContent();
                            }
                        } catch (Exception e)
                        {
                            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    default:
                        break;
                }
            }
            catch (Exception ignored)
            {
                // all URLs are valid
            }

            return false;
        }
    };

    Handler.Callback UiCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            switch (message.what)
            {
                case HANDLE_DO_POST:
                case HANDLE_DO_COMMENT:
                {
                    // Пост опубликован, возвращаемся
                    pd.dismiss();

                    closeMe(true);
                    break;
                }
                case HANDLE_UMAIL_ACK:
                {
                    pd.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(android.R.string.ok).setCancelable(false).setMessage(R.string.message_send_ok);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            closeMe(false);
                        }
                    });
                    builder.create().show();
                    break;
                }
                case HANDLE_UMAIL_REJ:
                {
                    pd.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(android.R.string.no).setCancelable(false).setMessage(R.string.message_send_error);
                    builder.setPositiveButton(android.R.string.no, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            closeMe(false);
                        }
                    });
                    builder.create().show();
                    break;
                }
                case HANDLE_REQUEST_AVATARS:
                {
                    for(int i = 0; i < avatarMap.size(); i++)
                    {
                        ImageButton current = new ImageButton(getActivity());
                        current.setImageDrawable((Drawable) avatarMap.valueAt(i));
                        current.setTag(R.integer.avatar_id, avatarMap.keyAt(i));
                        current.setOnClickListener(MessageSenderFragment.this);
                        mAvatars.addView(current);
                    }
                    pd.dismiss();
                    break;
                }
                case Utils.HANDLE_UPLOAD_FILE:
                {
                    int cursorPos = contentText.getSelectionStart();
                    contentText.setText(contentText.getText().toString().substring(0, cursorPos) + message.obj + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                    contentText.setSelection(contentText.getText().toString().indexOf("/>", cursorPos));
                    break;
                }
                case HANDLE_PROGRESS:
                    pd.setProgress((int) message.obj);
                    break;
                case HANDLE_GET_SMILIES:
                    Elements smileLinks = (Elements) message.obj;

                    mSmilies.removeAllViews();
                    for(Map.Entry<String, Object> smile : smileMap.entrySet())
                    {
                        ImageButton current = new ImageButton(getActivity());
                        current.setTag(R.integer.smile_page, smile.getKey());

                        current.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        current.setAdjustViewBounds(true);
                        current.setImageDrawable((Drawable) smile.getValue());
                        current.setOnClickListener(MessageSenderFragment.this);
                        mSmilies.addView(current);
                    }

                    mSmilieButtons.removeAllViews();
                    for(Element link : smileLinks)
                    {
                        if(link.text().equals(""))
                            continue;

                        Button current = new Button(getActivity());
                        current.setTag(R.integer.smile_key, link.attr("href"));
                        current.setText(link.text());
                        current.setOnClickListener(MessageSenderFragment.this);
                        mSmilieButtons.addView(current);
                    }

                    pd.dismiss();
                    break;
                default:
                    break;
            }

            return false;
        }
    };

    public <T extends Comment> void prepareFragment(String signature, String sendURL, T contents)
    {
        mService = NetworkService.getInstance(getActivity());
        assert(mService != null);
        mDHCL = mService.mDHCL;

        final Comment oldpost = mPost;
        mPost = contents;

        // обязательно
        mSignature = signature;
        mSendURL = sendURL;

        if(mPost.getClass() == Post.class)
        {
            // Если это новый пост
            if (mPost.postID.equals(""))
            {
                mTitle.setText(R.string.new_post);
                mCurrentPage.setText(mService.mUser.currentDiaryPage.getTitle());

                purgeContents();
                for(View v : postElements)
                    v.setVisibility(View.VISIBLE);

                if(oldpost.getClass() == Post.class && ((Post)oldpost).diaryID.equals(((Post)mPost).diaryID))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.confirmation).setMessage(R.string.clear_contents);
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            prepareUi((Post) mPost);
                        }
                    });
                    builder.setNegativeButton(R.string.no, null);
                    builder.create().show();
                }
                else
                    prepareUi((Post) mPost);
            }
            else // если редактирование поста
            {
                mTitle.setText(R.string.edit_post);
                mCurrentPage.setText(mService.mUser.currentDiaryPage.getTitle());

                purgeContents();
                for(View v : postElements)
                    v.setVisibility(View.VISIBLE);

                prepareUi((Post) mPost);
            }
        }
        else if (mPost.getClass() == Comment.class)
        {
            // если это новый комментарий
            if(mPost.commentID.equals(""))
            {
                mTitle.setText(R.string.new_comment);
                mCurrentPage.setText(mService.mUser.currentDiaryPage.getTitle());

                purgeContents();
                for(View v : commentElements)
                    v.setVisibility(View.VISIBLE);

                if(oldpost.getClass() == Comment.class && oldpost.postID.equals(mPost.postID))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.confirmation).setMessage(R.string.clear_contents);
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            prepareUi(mPost);
                        }
                    });
                    builder.setNegativeButton(R.string.no, null);
                    builder.create().show();
                }
                else
                    prepareUi(mPost);
            }
            else // редактирование комментария
            {
                mTitle.setText(R.string.edit_comment);
                mCurrentPage.setText(mService.mUser.currentDiaryPage.getTitle());

                purgeContents();
                for(View v : commentElements)
                    v.setVisibility(View.VISIBLE);

                prepareUi(mPost);
            }
        }
        else if(mPost.getClass() == Umail.class) // Если почта
        {
            mTitle.setText(R.string.new_umail);
            mCurrentPage.setVisibility(View.GONE);

            purgeContents();
            for(View v : umailElements)
                v.setVisibility(View.VISIBLE);

            prepareUi((Umail) mPost);
        }
    }

    private void prepareUi(Comment comment)
    {
        contentText.setText(comment.content);
        mSubscribe.setChecked(true);
        if(comment.commentID.equals("")) // новый коммент
            mSecureOptions.setVisibility(View.VISIBLE);
        else
            mSecureOptions.setVisibility(View.GONE);
    }

    private void prepareUi(Umail mail)
    {
        toText.setText(mail.receiver);
        titleText.setText(mail.messageTheme);
        mRequote.setChecked(true);
        mCopyMessage.setChecked(true);
    }

    private void prepareUi(Post post)
    {
        titleText.setText(post.title);
        contentText.setText(post.content);

        if(!"".equals(post.music + post.mood + post.themes) || !post.predefinedTags.isEmpty())
            mShowOptionals.setChecked(true);
        else
            mShowOptionals.setChecked(false);

        musicText.setText(post.music);
        moodText.setText(post.mood);
        themesText.setText(post.themes);

        if(!post.pollTitle.equals(""))
            mShowPoll.setChecked(true);
        else
            mShowPoll.setChecked(false);

        mPollTitle.setText(post.pollTitle);
        mPollChoice1.setText(post.pollAnswer1);
        mPollChoice2.setText(post.pollAnswer2);
        mPollChoice3.setText(post.pollAnswer3);
        mPollChoice4.setText(post.pollAnswer4);
        mPollChoice5.setText(post.pollAnswer5);
        mPollChoice6.setText(post.pollAnswer6);
        mPollChoice7.setText(post.pollAnswer7);
        mPollChoice8.setText(post.pollAnswer8);
        mPollChoice9.setText(post.pollAnswer9);
        mPollChoice10.setText(post.pollAnswer10);

        if(!post.closeAccessMode.equals(""))
            mShowCloseOptions.setChecked(true);
        else
            mShowCloseOptions.setChecked(false);

        switch (post.closeAccessMode)
        {
            case "6": mCloseOpts.check(R.id.close_only_reg); break;
            case "1": mCloseOpts.check(R.id.close_only_fav); break;
            case "5": mCloseOpts.check(R.id.close_only_sub); break;
            case "4": mCloseOpts.check(R.id.close_only_white); break;
            case "3": mCloseOpts.check(R.id.close_for_list); break;
            case "2": mCloseOpts.check(R.id.close_only_list); break;
            case "7": mCloseOpts.check(R.id.close_for_all); break;
        }

        mCloseText.setText(post.closeText);
        mCloseAllowList.setText(post.closeAllowList);
        mCloseDenyList.setText(post.closeDenyList);

        mNoComments.setChecked(post.noComments);

        mPredefinedThemes.removeAllViews(); // always clear all checks since we recreate them now
        LinearLayout horizontal = null;
        int i = 0;
        for(Map.Entry<String, Boolean> theme : post.predefinedTags.entrySet())
        {
            if(i % 4 == 0) // по 4 темы в горизонтальный ряд
            {
                horizontal = new LinearLayout(getActivity());
                horizontal.setOrientation(LinearLayout.HORIZONTAL);
                horizontal.setVerticalGravity(Gravity.CENTER);
                mPredefinedThemes.addView(horizontal);
            }

            CheckBox current = new CheckBox(getActivity());
            current.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            current.setEms(12);
            current.setMaxLines(2);
            current.setEllipsize(TextUtils.TruncateAt.END);

            current.setText(theme.getKey());
            current.setChecked(theme.getValue());
            horizontal.addView(current);
            ++i;
        }
    }

    public void onClick(View view) 
    {
        if(view instanceof ImageButton && view.getTag(R.integer.avatar_id) != null && view.getParent() == mAvatars)
        {
            postParams.clear();
            postParams.add(new BasicNameValuePair("use_avatar_id", view.getTag().toString()));
            postParams.add(new BasicNameValuePair("avatar_url", ""));
            postParams.add(new BasicNameValuePair("signature", mSignature));
            mHandler.sendEmptyMessage(HANDLE_SET_AVATAR);
        }

        if(view instanceof ImageButton && view.getTag(R.integer.smile_key) != null && view.getParent() == mSmilies)
            contentText.append((CharSequence) view.getTag(R.integer.smile_key));

        if(view instanceof Button && view.getTag(R.integer.smile_page) != null && view.getParent() == mSmilieButtons)
        {
            pd = ProgressDialog.show(getActivity(), getString(R.string.loading), getString(R.string.loading_data), true, true);
            mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_SMILIES, view.getTag(R.integer.smile_page)));
        }
        switch(view.getId())
        {
            case R.id.message_publish:
            {
                // TODO: Сохранение в черновики
                postParams.clear();

                // Добавляем параметры из настроек
                postParams.add(new BasicNameValuePair("signature", mSignature));
                postParams.add(new BasicNameValuePair("action", "dosend"));
                pd = ProgressDialog.show(getActivity(), getString(R.string.loading), getString(R.string.sending_data), true, false);

                // Если пост
                if(mPost.getClass() == Post.class)
                {
                    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("resulttype", "2"));

                    if(mPost.postID.equals("")) // новый пост
                    {
                        postParams.add(new BasicNameValuePair("message", contentText.getText().toString() + mService.mPreferences.getString("post.signature", "")));
                        postParams.add(new BasicNameValuePair("act", "new_post_post"));
                        postParams.add(new BasicNameValuePair("post_id", ""));
                    }
                    else // редактируем пост
                    {
                        postParams.add(new BasicNameValuePair("message", contentText.getText().toString()));
                        postParams.add(new BasicNameValuePair("act", "edit_post_post"));
                        postParams.add(new BasicNameValuePair("post_id", mPost.postID));

                    }
                    postParams.add(new BasicNameValuePair("journal_id", ((Post) mPost).diaryID));
                    postParams.add(new BasicNameValuePair("referer", mDHCL.currentURL));
                    postParams.add(new BasicNameValuePair("post_type", ""));

                    postParams.add(new BasicNameValuePair("title", titleText.getText().toString()));
                    if(mShowOptionals.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("themes", themesText.getText().toString() + (mPost.postID.equals("") ? mService.mPreferences.getString("post.tags", "") : "")));
                        for(int i = 0; i < mPredefinedThemes.getChildCount(); ++i)
                        {
                            LinearLayout horizontal = (LinearLayout) mPredefinedThemes.getChildAt(i);
                            for(int j = 0; j < horizontal.getChildCount(); ++j)
                            {
                                CheckBox check = (CheckBox) horizontal.getChildAt(j);
                                if(check.isChecked())
                                    postParams.add(new BasicNameValuePair("fvtags[]", check.getText().toString()));
                            }
                        }
                        postParams.add(new BasicNameValuePair("current_music", musicText.getText().toString()));
                        postParams.add(new BasicNameValuePair("current_mood", moodText.getText().toString()));
                    }
                    else
                    {
                        postParams.add(new BasicNameValuePair("themes", ""));
                        postParams.add(new BasicNameValuePair("current_music", ""));
                        postParams.add(new BasicNameValuePair("current_mood", ""));
                    }

                    postParams.add(new BasicNameValuePair("attachment", ""));

                    if(mShowPoll.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("poll_title", mPollTitle.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_1", mPollChoice1.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_2", mPollChoice2.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_3", mPollChoice3.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_4", mPollChoice4.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_5", mPollChoice5.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_6", mPollChoice6.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_7", mPollChoice7.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_8", mPollChoice8.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_9", mPollChoice9.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_10", mPollChoice10.getText().toString()));
                    }
                    else
                    {
                        postParams.add(new BasicNameValuePair("poll_title", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_1", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_2", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_3", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_4", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_5", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_6", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_7", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_8", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_9", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_10", ""));
                    }

                    if(mShowCloseOptions.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("private_post", "1"));
                        if(!mCloseText.getText().toString().equals(""))
                        {
                            postParams.add(new BasicNameValuePair("check_close_text", "1"));
                            postParams.add(new BasicNameValuePair("close_text", mCloseText.getText().toString()));
                        }

                        switch(mCloseOpts.getCheckedRadioButtonId())
                        {
                            case R.id.close_only_reg:
                                postParams.add(new BasicNameValuePair("close_access_mode", "6"));
                            break;
                            case R.id.close_only_fav:
                                postParams.add(new BasicNameValuePair("close_access_mode", "1"));
                            break;
                            case R.id.close_only_sub:
                                postParams.add(new BasicNameValuePair("close_access_mode", "5"));
                            break;
                            case R.id.close_only_white:
                                postParams.add(new BasicNameValuePair("close_access_mode", "4"));
                            break;
                            case R.id.close_for_list:
                                postParams.add(new BasicNameValuePair("close_access_mode", "2"));
                                postParams.add(new BasicNameValuePair("access_list", mCloseDenyList.getText().toString()));
                            break;
                            case R.id.close_only_list:
                                postParams.add(new BasicNameValuePair("close_access_mode", "3"));
                                postParams.add(new BasicNameValuePair("access_list", mCloseAllowList.getText().toString()));
                            break;
                            case R.id.close_for_all:
                                postParams.add(new BasicNameValuePair("close_access_mode", "7"));
                            break;
                        }
                    }
                    if(mNoComments.isChecked())
                        postParams.add(new BasicNameValuePair("no_comments", "1"));

                    postParams.add(new BasicNameValuePair("rewrite", "rewrite"));
                    postParams.add(new BasicNameValuePair("save_type", "js2"));

                    mHandler.sendEmptyMessage(HANDLE_DO_POST);
                }
                else if(mPost.getClass() == Comment.class)  // если коммент
                {
                    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("resulttype", "2"));
                    if(mPost.commentID.equals("")) // новый пост
                    {
                        switch (mSecureOptions.getCheckedRadioButtonId())
                        {
                           case R.id.message_anonymous:
                                postParams.add(new BasicNameValuePair("write_from", "1"));
                                break;
                            case R.id.message_as_user:
                                postParams.add(new BasicNameValuePair("write_from", "2"));
                                postParams.add(new BasicNameValuePair("write_from_name", mUserLogin.getText().toString()));
                                postParams.add(new BasicNameValuePair("write_from_pass", mUserPassword.getText().toString()));
                                break;
                            default:
                                postParams.add(new BasicNameValuePair("write_from", "0"));
                                break;
                        }
                        postParams.add(new BasicNameValuePair("message", contentText.getText().toString() + mService.mPreferences.getString("post.signature", "")));
                        postParams.add(new BasicNameValuePair("act", "new_comment_post"));
                        postParams.add(new BasicNameValuePair("commentid", ""));
                    }
                    else // редактируем пост
                    {
                        postParams.add(new BasicNameValuePair("message", contentText.getText().toString()));
                        postParams.add(new BasicNameValuePair("act", "edit_comment_post"));
                        postParams.add(new BasicNameValuePair("commentid", mPost.commentID));
                    }

                    postParams.add(new BasicNameValuePair("post_id", mPost.postID));

                    postParams.add(new BasicNameValuePair("referer", ""));
                    postParams.add(new BasicNameValuePair("page", "last"));
                    postParams.add(new BasicNameValuePair("open_uri", ""));

                    //postParams.add(new BasicNameValuePair("write_from_name", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
                    //postParams.add(new BasicNameValuePair("write_from_pass", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));

                    postParams.add(new BasicNameValuePair("subscribe", mSubscribe.isChecked() ? "1/" : ""));
                    postParams.add(new BasicNameValuePair("attachment1", ""));

                    mHandler.sendEmptyMessage(HANDLE_DO_COMMENT);
                }
                else if(mPost.getClass() == Umail.class)  // если почта
                {
                    postParams.add(new BasicNameValuePair("message", contentText.getText().toString() + mService.mPreferences.getString("post.signature", "")));
                    postParams.add(new BasicNameValuePair("module", "umail"));
                    postParams.add(new BasicNameValuePair("act", "umail_send"));
                    postParams.add(new BasicNameValuePair("from_folder", ""));
                    postParams.add(new BasicNameValuePair("to_user", toText.getText().toString()));
                    postParams.add(new BasicNameValuePair("title", titleText.getText().toString()));
                    postParams.add(new BasicNameValuePair("save_copy", mCopyMessage.isChecked() ? "yes" : ""));
                    postParams.add(new BasicNameValuePair("need_receipt", mGetReceipt.isChecked() ? "yes" : ""));
                    postParams.add(new BasicNameValuePair("requote", mRequote.isChecked() ? "yes" : ""));

                    mHandler.sendEmptyMessage(HANDLE_DO_UMAIL);
                }
            break;
            }
            case R.id.message_show_smilies:
                pd = ProgressDialog.show(getActivity(), getString(R.string.loading), getString(R.string.loading_data), true, true);
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_SMILIES, null));
            break;
        }
    }

    public int getColorFromPicture(ImageButton view)
    {
        Drawable old = view.getDrawable();
        Bitmap example = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        Canvas tCanvas = new Canvas(example);
        old.draw(tCanvas);
        return example.getPixel(0, 0);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch(buttonView.getId())
        {
            case R.id.message_optional:
                if(isChecked)
                    mOptionals.setVisibility(View.VISIBLE);
                else
                    mOptionals.setVisibility(View.GONE);
                break;
            case R.id.message_poll:
                if(isChecked)
                    mPoll.setVisibility(View.VISIBLE);
                else
                    mPoll.setVisibility(View.GONE);
                break;
            case R.id.message_close:
                if(isChecked)
                    mCloseOpts.setVisibility(View.VISIBLE);
                else
                    mCloseOpts.setVisibility(View.GONE);
                break;
            case R.id.message_custom_avatar:
                if(isChecked)
                {
                    mAvatars.setVisibility(View.VISIBLE);
                    if(avatarMap == null)
                    {
                        pd = ProgressDialog.show(getActivity(), getString(R.string.loading), getString(R.string.loading_data), true, true);
                        mHandler.sendEmptyMessage(HANDLE_REQUEST_AVATARS);
                    }
                }
                else
                    mAvatars.setVisibility(View.GONE);
            default:
                break;
        }
    }

    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        if(group == mCloseOpts)
            switch(checkedId)
            {
                case R.id.close_for_list:
                    mCloseDenyList.setVisibility(View.VISIBLE);
                    mCloseAllowList.setVisibility(View.GONE);
                break;
                case R.id.close_only_list:
                    mCloseAllowList.setVisibility(View.VISIBLE);
                    mCloseDenyList.setVisibility(View.GONE);
                break;
                default:
                    mCloseDenyList.setVisibility(View.GONE);
                    mCloseAllowList.setVisibility(View.GONE);
                break;
            }
        else if(group == mSecureOptions)
            switch (checkedId)
            {
                case R.id.message_as_user:
                    mAsUserLayout.setVisibility(View.VISIBLE);
                    break;
                default:
                    mAsUserLayout.setVisibility(View.GONE);
            }
    }

    @SuppressWarnings("deprecation")
    public void acceptDialogClick(View view, boolean pasteClipboard)
    {
        int cursorPos = contentText.getSelectionStart();
        if(cursorPos == -1)
            cursorPos = contentText.getText().length();

        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        CharSequence paste = clipboard.getText();
        if(paste == null || !pasteClipboard)
            paste = "";

        switch (view.getId())
        {
            case R.id.button_bold:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<b>" + paste.toString() + "</b>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("</b>", cursorPos));
                break;
            }
            case R.id.button_italic:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<i>" + paste.toString() + "</i>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("</i>", cursorPos));
                break;
            }
            case R.id.button_underlined:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<u>" + paste.toString() + "</u>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("</u>", cursorPos));
                break;
            }
            case R.id.button_nick:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "[L]" + paste.toString() + "[/L]" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("[/L]", cursorPos));
                break;
            }
            case R.id.button_link:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<a href=\"" + paste.toString() + "\">" + paste.toString() + "</a>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("/a>", cursorPos));
                break;
            }
            case R.id.button_more:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "[MORE=" + getString(R.string.read_more) + "]" + paste.toString() + "[/MORE]" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("[/MORE]", cursorPos));
                break;
            }
            case R.id.button_offtopic:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<span class='offtop'>" + paste.toString() + "</span>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("</span>", cursorPos));
                break;
            }
            case R.id.button_image:
            {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                if(pasteClipboard)
                {
                    contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<img src=\"" + paste.toString() + "\" />" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                    contentText.setSelection(contentText.getText().toString().indexOf("/>", cursorPos));
                }
                else
                    try
                    {
                        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), 0);
                    } catch (android.content.ActivityNotFoundException ex)
                    {
                        Toast.makeText(getActivity(), getString(R.string.no_file_manager_found), Toast.LENGTH_SHORT).show();
                    }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case 0:
                if (resultCode == Activity.RESULT_OK)
                {
                    Uri uri = data.getData();
                    File file = null;
                    if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()))
                    {
                        String[] projection = { MediaStore.Images.Media.DATA };
                        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
                        int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                        if (cursor.moveToFirst() && cursor.getString(0) != null) // ensure we have this file on our device...
                            file = new File(cursor.getString(column_index));
                        else
                            Toast.makeText(getActivity(), R.string.no_file_on_device, Toast.LENGTH_LONG).show();

                    }
                    else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme()))
                        file = new File(uri.getPath());

                    try
                    {
                        if (file != null)
                        {
                            final Message msg = mHandler.obtainMessage(Utils.HANDLE_UPLOAD_FILE, file.getCanonicalPath());
                            msg.arg1 = 3;
                            AlertDialog.Builder origOrMoreOrLink = new AlertDialog.Builder(getActivity());
                            DialogInterface.OnClickListener selector = new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    switch(which)
                                    {
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            msg.arg1 = 1;
                                            break;
                                        case DialogInterface.BUTTON_NEUTRAL:
                                            msg.arg1 = 2;
                                            break;
                                        case DialogInterface.BUTTON_POSITIVE:
                                        default:
                                            msg.arg1 = 3;
                                            break;
                                    }

                                    pd = new ProgressDialog(getActivity());
                                    pd.setIndeterminate(false);
                                    pd.setTitle(R.string.loading);
                                    pd.setMessage(getString(R.string.sending_data));
                                    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    pd.show();
                                    mHandler.sendMessage(msg);
                                }
                            };
                            origOrMoreOrLink.setTitle(R.string.howto_send_img);
                            origOrMoreOrLink.setNegativeButton(R.string.pack_inoriginal, selector);
                            origOrMoreOrLink.setPositiveButton(R.string.pack_inmore, selector);
                            origOrMoreOrLink.setNeutralButton(R.string.pack_inlink, selector);
                            origOrMoreOrLink.create().show();
                        }
                        else
                            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                    } catch (IOException e)
                    {
                        Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void closeMe(boolean reload)
    {
        ((DiaryActivity)getActivity()).onMessagePaneRemove(reload);
    }

    private void purgeContents()
    {
        for(int i = 0; i < mMainLayout.getChildCount(); ++i)
        {
            View curr = mMainLayout.getChildAt(i);
            if(curr.getTag() == null || !curr.getTag().toString().equals("persistent"))
                mMainLayout.getChildAt(i).setVisibility(View.GONE);
            if(curr instanceof CheckBox)
                ((CheckBox) curr).setChecked(false);
            if(curr instanceof RadioGroup)
                ((RadioGroup) curr).clearCheck();
        }
    }
}
