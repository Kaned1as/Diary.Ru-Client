package adonai.diary_browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.SelectArg;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adonai.diary_browser.database.DbProvider;
import adonai.diary_browser.entities.AutocompleteItem;
import adonai.diary_browser.pages.Comment;
import adonai.diary_browser.adapters.DraftListArrayAdapter;
import adonai.diary_browser.dto.ImgurImageResponse;
import adonai.diary_browser.dto.PleerEmbedAnswer;
import adonai.diary_browser.dto.PleerUploadAnswer;
import adonai.diary_browser.pages.Post;
import adonai.diary_browser.pages.Umail;
import adonai.diary_browser.misc.FileUtils;
import adonai.diary_browser.misc.SemicolonTokenizer;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageButton;

/**
 * Основной фрагмент посылки сообщения. Такой фрагмент присутствует в каждой {@link DiaryActivity}.
 * Отвечает за посылку и редактирование постов/комментариев/U-Mail с произвольными параметрами,
 * а также вставку смайлов и других объектов.
 * <p/>
 * <i>Замечание: содержит в себе асинхронный обработчик для посылки/приёма сообщений с сайта и отслеживания статуса</i>
 *
 * @see DiaryListActivity
 * @see UmailListActivity
 *
 * @author Адонай
 *
 * @// TODO: Добавить возможность отмены посылки поста/комментария
 */
public class MessageSenderFragment extends Fragment implements OnClickListener, OnCheckedChangeListener, android.widget.RadioGroup.OnCheckedChangeListener {

    // команды внутренним хэндлерам
    private static final int HANDLE_DO_POST         = 0;
    private static final int HANDLE_DO_COMMENT      = 1;
    private static final int HANDLE_DO_UMAIL        = 2;
    private static final int HANDLE_UMAIL_ACK       = 3;
    private static final int HANDLE_UMAIL_REJ       = 4;
    private static final int HANDLE_REQUEST_AVATARS = 5;
    private static final int HANDLE_SET_AVATAR      = 6;
    private static final int HANDLE_PROGRESS        = 8;
    private static final int HANDLE_GET_SMILIES     = 9;
    private static final int HANDLE_GET_DRAFTS      = 10;
    private static final int HANDLE_CONN_ERROR      = -1;

    static Pattern EMAIL_ANSWER_REGEX = Pattern.compile("Re\\[(\\d+)\\]: (.*)");

    BroadcastReceiver mPaneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Utils.CLOSING_PANE_BROADCAST:
                    // выключаем смайлики
                    setSmiliesState(false);
                    break;
                case Utils.OPENING_PANE_BROADCAST:
                    // включаем смайлики
                    setSmiliesState(true);
                    break;
            }
        }
    };

    Handler.Callback UiCallback = new Handler.Callback() {
        @SuppressWarnings("unchecked")
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case HANDLE_DO_POST:
                case HANDLE_DO_COMMENT: {
                    // Пост опубликован, возвращаемся
                    pd.dismiss();

                    closeMe(true);
                    break;
                }
                case HANDLE_GET_DRAFTS: {
                    pd.dismiss();

                    final List<Post> drafts = (List<Post>) message.obj;
                    if(drafts == null) {
                        Toast.makeText(getActivity(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                        break;
                    }

                    if(drafts.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.no_drafts), Toast.LENGTH_SHORT).show();
                        break;
                    }

                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.select_draft)
                            .items(drafts)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, int which, CharSequence charSequence) {
                                    final Post clicked = drafts.get(which);

                                    // выбираем действие - удалить или редактировать
                                    new MaterialDialog.Builder(getActivity())
                                            .title(R.string.select_action)
                                            .positiveText(R.string.edit)
                                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                                    // http://xxxx.diary.ru/?editpost&postid=#######&draft
                                                    String editUrl = clicked.url.substring(0, clicked.url.lastIndexOf('/') + 1) + "?editpost&postid=" + clicked.postID + "&draft";
                                                    ((DiaryActivity) getActivity()).handleBackground(Utils.HANDLE_EDIT_POST, editUrl);
                                                }
                                            })
                                            .negativeText(R.string.delete)
                                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                                    // диалог подтверждения
                                                    new MaterialDialog.Builder(getActivity())
                                                            .title(android.R.string.dialog_alert_title)
                                                            .content(R.string.really_delete)
                                                            .cancelable(false)
                                                            .positiveText(android.R.string.ok)
                                                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                                @Override
                                                                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                                                    ((DiaryActivity) getActivity()).handleBackground(Utils.HANDLE_DELETE_POST_DRAFT, clicked.postID);
                                                                }
                                                            })
                                                            .negativeText(android.R.string.no)
                                                            .show();
                                                }
                                            }).show();
                                }
                            }).show();
                    break;
                }
                case HANDLE_UMAIL_ACK: {
                    pd.dismiss();
                    new MaterialDialog.Builder(getActivity())
                            .title(android.R.string.ok)
                            .cancelable(false)
                            .content(R.string.message_send_ok)
                            .positiveText(android.R.string.ok)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                    closeMe(false);
                                }
                            }).show();
                    break;
                }
                case HANDLE_UMAIL_REJ: {
                    pd.dismiss();
                    new MaterialDialog.Builder(getActivity())
                            .title(android.R.string.no)
                            .cancelable(false)
                            .content(R.string.message_send_error)
                            .positiveText(android.R.string.no)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                    closeMe(false);
                                }
                            }).show();
                    break;
                }
                case HANDLE_REQUEST_AVATARS: {
                    for (int i = 0; i < avatarMap.size(); i++) {
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
                case Utils.HANDLE_UPLOAD_MUSIC:
                case Utils.HANDLE_UPLOAD_GIF: {
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

                    DisplayMetrics dm = new DisplayMetrics();
                    getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

                    mSmilies.removeAllViews();
                    for (Map.Entry<String, Object> smile : smileMap.entrySet()) {
                        GifDrawable gif = (GifDrawable) smile.getValue();
                        gif.start();

                        GifImageButton current = new GifImageButton(getActivity());
                        current.setTag(R.integer.smile_key, smile.getKey());

                        int maxImageHeight = (int) (gif.getIntrinsicHeight() * dm.density);
                        int maxImageWidth = (int) (gif.getIntrinsicWidth() * dm.density);

                        current.setScaleType(ImageView.ScaleType.MATRIX);
                        Matrix matrix = new Matrix();
                        matrix.setScale(dm.density, dm.density); // увеличим размеры смайликов
                        current.setImageMatrix(matrix);
                        current.setImageDrawable(gif);
                        current.setMinimumHeight(maxImageHeight + current.getPaddingTop() + current.getPaddingBottom());
                        current.setMinimumWidth(maxImageWidth + current.getPaddingLeft() + current.getPaddingRight());
                        current.setOnClickListener(MessageSenderFragment.this);
                        mSmilies.addView(current);
                    }

                    mSmilieButtons.removeAllViews();
                    for (Element link : smileLinks) {
                        if (link.text().isEmpty())
                            continue;

                        Button current = new Button(getActivity());
                        current.setTag(R.integer.smile_page, link.attr("href"));
                        current.setText(link.text());
                        current.setOnClickListener(MessageSenderFragment.this);
                        mSmilieButtons.addView(current);
                    }

                    pd.dismiss();
                    break;
                case HANDLE_CONN_ERROR:
                    Toast.makeText(getActivity(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                    break;
                default:
                    break;
            }

            return true;
        }
    };

    private Button mPublish, mSaveDraft, mLoadDraft;
    private Button mShowSmilies;
    private EditText toText;
    private EditText titleText;
    private EditText contentText;
    private EditText requoteText;
    private MultiAutoCompleteTextView themesText;
    private MultiAutoCompleteTextView musicText;
    private MultiAutoCompleteTextView moodText;
    private CheckBox mShowOptionals;
    private CheckBox mShowPoll;
    private CheckBox mSubscribe;
    private CheckBox mShowCloseOptions;
    private CheckBox mGetReceipt;
    private CheckBox mRequote;
    private CheckBox mCopyMessage;
    private CheckBox mCustomAvatar;
    private CheckBox mNoComments;
    private RadioGroup mSecureOptions;
    private TextView mTitle;
    private TextView mCurrentPage;
    private EditText mUserLogin;
    private EditText mUserPassword;
    private EditText mPollTitle;
    private EditText mPollChoice1;
    private EditText mPollChoice2;
    private EditText mPollChoice3;
    private EditText mPollChoice4;
    private EditText mPollChoice5;
    private EditText mPollChoice6;
    private EditText mPollChoice7;
    private EditText mPollChoice8;
    private EditText mPollChoice9;
    private EditText mPollChoice10;
    private EditText mCloseAllowList;
    private EditText mCloseDenyList;
    private EditText mCloseText;
    private RadioGroup mCloseOpts;
    private Handler mHandler, mUiHandler;
    private Looper mLooper;
    private MaterialDialog pd;
    private LinearLayout mAvatars, mOptionals, mPoll, mSmilies, mSmilieButtons, mPredefinedThemes, mMainLayout, mAsUserLayout;
    private List<View> postElements = new ArrayList<>();
    private List<View> commentElements = new ArrayList<>();
    private List<View> umailElements = new ArrayList<>();
    private List<Pair<String, String>> postParams;
    private String mSignature;
    private NetworkService mService;
    private SparseArray<Object> avatarMap;
    private Map<String, Object> smileMap;
    private DiaryHttpClient mHttpClient;

    private SharedPreferences mSharedPrefs;

    Handler.Callback HttpCallback = new Handler.Callback() {
        @SuppressWarnings("unchecked")
        public boolean handleMessage(Message message) {
            try {
                switch (message.what) {
                    case HANDLE_DO_POST:
                    case HANDLE_DO_COMMENT: {
                        mHttpClient.postPageToString(postParams);
                        mUiHandler.sendEmptyMessage(message.what);
                        return true;
                    }
                    case HANDLE_DO_UMAIL: {
                        String result = mHttpClient.postPageToString(postParams);
                        if (result.contains("Письмо отправлено"))
                            mUiHandler.sendEmptyMessage(HANDLE_UMAIL_ACK);
                        else
                            mUiHandler.sendEmptyMessage(HANDLE_UMAIL_REJ);
                        return true;
                    }
                    case HANDLE_GET_SMILIES: {
                        String url = message.obj != null ? (String) message.obj : "http://www.diary.ru/smile.php";
                        String urlValid = url.replaceAll("[:/]", "_"); // не содержащее недопустимых символов для ФС имя
                        String result;
                        if (mCache.hasData(getActivity(), urlValid))
                            result = new String(mCache.retrieveData(getActivity(), urlValid));
                        else {
                            result = mHttpClient.getPageAsString(url);
                            if(result == null) {
                                mUiHandler.sendEmptyMessage(HANDLE_CONN_ERROR);
                                return true;
                            }
                            mCache.cacheData(getActivity(), result.getBytes(), urlValid);
                        }
                        Document rootNode = Jsoup.parse(result);
                        Elements smilies = rootNode.select("tr img");
                        Elements smileLinks = rootNode.select("ul a");

                        smileMap = new HashMap<>();
                        for (Element smilie : smilies)
                            smileMap.put(smilie.attr("alt"), smilie.attr("src"));

                        // распараллеливаем получение смайликов
                        // в массиве теперь будет храниться строка вызова смайлика - задача загрузки аватара
                        ExecutorService executor = Executors.newFixedThreadPool(smileMap.size());
                        for (String id : smileMap.keySet()) {
                            final String smileUrl = (String) smileMap.get(id);
                            FutureTask<Drawable> future = new FutureTask<>(new Callable<Drawable>() {
                                @Override
                                public Drawable call() throws Exception {
                                    String name = smileUrl.substring(smileUrl.lastIndexOf('/') + 1);
                                    byte[] outputBytes;

                                    if (mCache.hasData(getActivity(), name))
                                        outputBytes = mCache.retrieveData(getActivity(), name);
                                    else {
                                        outputBytes = mHttpClient.getPageAsByteArray(smileUrl);
                                        // caching image
                                        mCache.cacheData(getActivity(), outputBytes, name);
                                    }

                                    return new GifDrawable(outputBytes);
                                }

                            });
                            smileMap.put(id, future);
                            executor.execute(future);
                        }

                        // по мере выполнения задач переписываем массив по кусочкам на результат задачи
                        // хак для того, чтобы всё работало в одном потоке
                        while (true) {
                            int remaining = 0;
                            for (String id : smileMap.keySet())
                                if (smileMap.get(id) instanceof FutureTask) {
                                    FutureTask<Drawable> future = (FutureTask<Drawable>) smileMap.get(id);
                                    if (future.isDone())
                                        smileMap.put(id, future.get());
                                    else
                                        remaining++;
                                }
                            if (remaining == 0)
                                break;
                        }

                        mUiHandler.sendMessage(mUiHandler.obtainMessage(HANDLE_GET_SMILIES, smileLinks));
                        return true;
                    }
                    case HANDLE_REQUEST_AVATARS: {
                        String url = "http://www.diary.ru/options/member/?avatar";
                        String dataPage = mHttpClient.getPageAsString(url);
                        if (dataPage == null)
                            return false;

                        // собираем пары ID аватара - url аватара
                        Elements avatardivs = Jsoup.parse(dataPage).select("div#avatarbit");
                        avatarMap = new SparseArray<>();
                        for (Element avatarbit : avatardivs) {
                            Integer avId = Integer.valueOf(avatarbit.select("input[name=use_avatar_id]").val());
                            String imageUrl = avatarbit.child(0).attr("style");
                            imageUrl = imageUrl.substring(imageUrl.lastIndexOf('(') + 1, imageUrl.lastIndexOf(')'));
                            avatarMap.put(avId, imageUrl);
                        }

                        // распараллеливаем получение аватарок
                        // в массиве теперь будет храниться ID аватара - задача загрузки аватара
                        ExecutorService executor = Executors.newFixedThreadPool(avatarMap.size());
                        for (int i = 0; i < avatarMap.size(); i++) {
                            final String imageUrl = (String) avatarMap.valueAt(i);
                            FutureTask<Drawable> future = new FutureTask<>(new Callable<Drawable>() {
                                @Override
                                public Drawable call() throws Exception {
                                    final byte[] imageBytes = mHttpClient.getPageAsByteArray(imageUrl);
                                    Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                    return new BitmapDrawable(getResources(), image);
                                }

                            });
                            avatarMap.setValueAt(i, future);
                            executor.execute(future);
                        }

                        // по мере выполнения задач переписываем массив по кусочкам на результат задачи
                        // хак для того, чтобы всё работало в одном потоке
                        while (true) {
                            int remaining = 0;
                            for (int i = 0; i < avatarMap.size(); i++)
                                if (avatarMap.valueAt(i) instanceof FutureTask) {
                                    FutureTask<Drawable> future = (FutureTask<Drawable>) avatarMap.valueAt(i);
                                    if (future.isDone())
                                        avatarMap.setValueAt(i, future.get());
                                    else
                                        remaining++;
                                }
                            if (remaining == 0)
                                break;
                        }

                        mUiHandler.sendEmptyMessage(HANDLE_REQUEST_AVATARS);
                        return true;
                    }
                    case HANDLE_GET_DRAFTS: {
                        String url = mHttpClient.getCurrentUrl() + "?draft";
                        String dataPage = mHttpClient.getPageAsString(url);
                        if (dataPage == null) {
                            // ошибка соединения
                            mUiHandler.sendMessage(mUiHandler.obtainMessage(message.what, null));
                            return false;
                        }

                        Elements posts = Jsoup.parse(dataPage).select("#postsArea > [id~=post\\d+]");
                        List<Post> drafts = new ArrayList<>(posts.size());
                        if(posts.isEmpty()) {
                            // нет черновиков?
                            mUiHandler.sendMessage(mUiHandler.obtainMessage(message.what, drafts));
                            return false;
                        }

                        for(Element post : posts) {
                            Post draft = new Post();
                            draft.date = post.select(".postTitle > span").attr("title");
                            draft.title = post.select(".postTitle h2").text();
                            String fullContent = post.select(".postContent .postInner .paragraph").text();
                            draft.content = fullContent.substring(0, fullContent.length() > 100 ? 100 : fullContent.length());
                            draft.postID = post.id().substring(4); // после post#####
                            draft.url = post.select(".postLinksBackg .urlLink a").attr("href");
                            drafts.add(draft);
                        }

                        mUiHandler.sendMessage(mUiHandler.obtainMessage(message.what, drafts));
                        return true;
                    }
                    case HANDLE_SET_AVATAR: {
                        String URL = "http://www.diary.ru/options/member/?avatar";
                        mHttpClient.postPageToString(URL, postParams);
                        Toast.makeText(getActivity(), R.string.avatar_selected, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    case Utils.HANDLE_UPLOAD_MUSIC: {
                        File file = new File((String) message.obj);
                        mHttpClient.getPage(URI.create("http://pleer.com/upload"));
                        long length = file.length();
                        final DiaryHttpClient.ProgressListener listener = new SendProgressListener(length);

                        MultipartBody mpEntity = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("module", "photolib")
                                .addFormDataPart("signature", mSignature)
                                .addFormDataPart("resulttype1", String.valueOf(message.arg1))
                                .addFormDataPart("file", file.getName(),
                                        new DiaryHttpClient.CountingFileRequestBody(file, listener))
                                .build();

                        String progressId = "";
                        for (int i = 0; i < 8; ++i) {
                            progressId += (int) Math.ceil(Math.random() * 100000);
                        }
                        String str = mHttpClient.postPageToString("http://pleer.com/upload/send?X-Progress-ID=" + progressId,
                                mpEntity);
                        if(str == null) {
                            Toast.makeText(getActivity(), getString(R.string.message_send_error), Toast.LENGTH_LONG).show();
                            pd.dismiss();
                            break;
                        }

                        Gson pleerGson = new GsonBuilder()
                                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                                .create();

                        // check if upload was successful
                        PleerUploadAnswer uploadAnswer = pleerGson.fromJson(str, PleerUploadAnswer.class);
                        if(!uploadAnswer.isCorrectFile()) {
                            Toast.makeText(getActivity(), getString(R.string.pp_wrong_file), Toast.LENGTH_LONG).show();
                            pd.dismiss();
                            break;
                        }
                        if(!uploadAnswer.isCorrectName()) {
                            Toast.makeText(getActivity(), getString(R.string.pp_wrong_name), Toast.LENGTH_LONG).show();
                            pd.dismiss();
                            break;
                        }

                        MultipartBody embedBody = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("id", uploadAnswer.getLink())
                                .build();
                        String embedStr = mHttpClient.postPageToString("http://pleer.com/site_api/embed/track",
                                embedBody);
                        if(embedStr == null) {
                            Toast.makeText(getActivity(), getString(R.string.message_send_error), Toast.LENGTH_LONG).show();
                            pd.dismiss();
                            break;
                        }
                        PleerEmbedAnswer embedAnswer = pleerGson.fromJson(embedStr, PleerEmbedAnswer.class);
                        if(!embedAnswer.isSuccess()) {
                            Toast.makeText(getActivity(), getString(R.string.embed_error), Toast.LENGTH_LONG).show();
                            pd.dismiss();
                            break;
                        }

                        InputStream is = getResources().getAssets().open("plaintext/prostopleer_embed.html");
                        String htmlToEmbed = String.format(Utils.getStringFromInputStream(is),
                                embedAnswer.getEmbedId(), message.arg1 == 2 ? "grey" : "black",
                                embedAnswer.getEmbedId(), message.arg1 == 2 ? "grey" : "black",
                                uploadAnswer.getLink(),
                                embedAnswer.getName());
                        mUiHandler.sendMessage(mUiHandler.obtainMessage(Utils.HANDLE_UPLOAD_MUSIC, htmlToEmbed));
                        pd.dismiss();
                        break;
                    }
                    case Utils.HANDLE_UPLOAD_GIF: {
                        File gifImage = new File((String) message.obj);
                        long length = gifImage.length();
                        final DiaryHttpClient.ProgressListener listener = new SendProgressListener(length);

                        Headers authHeaders = new Headers.Builder()
                                .add("Authorization", Utils.IMGUR_CLIENT_AUTH)
                                .build();

                        MultipartBody mpEntity = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("title", gifImage.getName())
                                .addFormDataPart("type", "file")
                                .addFormDataPart("image", gifImage.getName(),
                                        new DiaryHttpClient.CountingFileRequestBody(gifImage, listener))
                                .build();

                        String result = mHttpClient.postPageToString(Utils.IMGUR_API_ENDPOINT + "image",
                                mpEntity, authHeaders);

                        ImgurImageResponse response = new Gson().fromJson(result, ImgurImageResponse.class);
                        if(!response.success) {
                            Toast.makeText(getActivity(), getString(R.string.message_send_error), Toast.LENGTH_LONG).show();
                            pd.dismiss();
                            break;
                        }
                        int width = message.arg1 * 100; // 100 / 200 / 300
                        double rate = width / (float) response.data.width;
                        int height = (int) (response.data.height * rate);
                        String toPaste = String.format(Locale.getDefault(), "<img width='%d' height='%d' src='%s' />",
                                width,
                                height,
                                response.data.link);

                        mUiHandler.sendMessage(mUiHandler.obtainMessage(Utils.HANDLE_UPLOAD_GIF, toPaste));
                        pd.dismiss();
                        break;
                    }
                    case Utils.HANDLE_UPLOAD_FILE: {
                        try {
                            File file = new File((String) message.obj);
                            long length = file.length();
                            final DiaryHttpClient.ProgressListener listener = new SendProgressListener(length);

                            MultipartBody mpEntity = new MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("module", "photolib")
                                    .addFormDataPart("signature", mSignature)
                                    .addFormDataPart("resulttype1", String.valueOf(message.arg1))
                                    .addFormDataPart("attachment1",
                                            URLEncoder.encode(file.getName(), "windows-1251"),
                                            new DiaryHttpClient.CountingFileRequestBody(file, listener))
                                    .build();

                            String result = mHttpClient.postPageToString("http://www.diary.ru/diary.php?upload=1&js", mpEntity);
                            if (result != null) {
                                if (result.contains("допустимые:")) // ошибка отправки, слишком большая картинка
                                    Toast.makeText(getActivity(), getString(R.string.too_big_picture), Toast.LENGTH_LONG).show();
                                else {
                                    result = result.substring(result.indexOf("'") + 1, result.indexOf("';"));
                                    if (result.length() > 0)
                                        mUiHandler.sendMessage(mUiHandler.obtainMessage(Utils.HANDLE_UPLOAD_FILE, result));
                                    else
                                        Toast.makeText(getActivity(), getString(R.string.message_send_error), Toast.LENGTH_LONG).show();
                                }
                                //resEntity.consumeContent();
                            } else {
                                Toast.makeText(getActivity(), getString(R.string.message_send_error), Toast.LENGTH_LONG).show();
                            }
                            pd.dismiss();
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    default:
                        break;
                }
            } catch (Exception ignored) {
               mUiHandler.sendEmptyMessage(HANDLE_CONN_ERROR);
            }

            return false;
        }
    };
    Comment mPost;
    CacheManager mCache = CacheManager.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View sender = inflater.inflate(R.layout.fragment_message_sender, container, false);

        mSharedPrefs = getActivity().getSharedPreferences(Utils.mPrefsFile, Context.MODE_PRIVATE);
        mPost = new Post();
        postParams = new ArrayList<>();

        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, HttpCallback);
        mUiHandler = new Handler(UiCallback);

        IntentFilter paneActionsFilter = new IntentFilter();
        paneActionsFilter.addAction(Utils.OPENING_PANE_BROADCAST);
        paneActionsFilter.addAction(Utils.CLOSING_PANE_BROADCAST);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mPaneStateReceiver, paneActionsFilter);

        mMainLayout = (LinearLayout) sender.findViewById(R.id.message_main_layout);

        toText = (EditText) sender.findViewById(R.id.message_to);
        mGetReceipt = (CheckBox) sender.findViewById(R.id.message_getreceipt);
        mRequote = (CheckBox) sender.findViewById(R.id.message_requote);
        mRequote.setOnCheckedChangeListener(this);
        mCopyMessage = (CheckBox) sender.findViewById(R.id.message_copy);

        titleText = (EditText) sender.findViewById(R.id.message_title);
        contentText = (EditText) sender.findViewById(R.id.message_content);
        requoteText = (EditText) sender.findViewById(R.id.message_requote_content);
        themesText = (MultiAutoCompleteTextView) sender.findViewById(R.id.message_themes);
        themesText.setTokenizer(new SemicolonTokenizer());
        musicText = (MultiAutoCompleteTextView) sender.findViewById(R.id.message_music);
        musicText.setTokenizer(new SemicolonTokenizer());
        moodText = (MultiAutoCompleteTextView) sender.findViewById(R.id.message_mood);
        moodText.setTokenizer(new SemicolonTokenizer());
        mPublish = (Button) sender.findViewById(R.id.message_publish);
        mPublish.setOnClickListener(this);
        mSaveDraft = (Button) sender.findViewById(R.id.message_save_draft);
        mSaveDraft.setOnClickListener(this);
        mLoadDraft = (Button) sender.findViewById(R.id.message_load_draft);
        mLoadDraft.setOnClickListener(this);
        mTitle = (TextView) sender.findViewById(R.id.fragment_title);
        mCurrentPage = (TextView) sender.findViewById(R.id.fragment_page);

        final LinearLayout specials = (LinearLayout) sender.findViewById(R.id.message_specials);
        for (int i = 0; i < specials.getChildCount(); ++i)
            specials.getChildAt(i).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int cursorPos = contentText.getSelectionStart();
                    if (cursorPos == -1)
                        cursorPos = contentText.getText().length();

                    String toPaste = ((Button) view).getText().toString();
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

        postElements.add(mSaveDraft);
        postElements.add(mLoadDraft);
        postElements.add(titleText);
        postElements.add((View) titleText.getParent().getParent()); // контейнер (TextInputLayout)
        postElements.add(mShowOptionals);
        postElements.add(mShowCloseOptions);
        postElements.add(mShowPoll);
        postElements.add(mNoComments);

        umailElements.add(toText);
        umailElements.add((View) toText.getParent().getParent()); // контейнер (TextInputLayout)
        umailElements.add(titleText);
        umailElements.add((View) titleText.getParent().getParent()); // контейнер (TextInputLayout)
        umailElements.add(mGetReceipt);
        umailElements.add(mRequote);
        umailElements.add(mCopyMessage);
        umailElements.add(requoteText);

        return sender;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message_sender_a, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_special_paste:
                DialogFragment newFragment = PasteSelector.newInstance();
                newFragment.show(getActivity().getSupportFragmentManager(), "selector");
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        mLooper.quit();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mPaneStateReceiver);
        super.onDestroyView();
    }

    public String getContentText() {
        return contentText.getText().toString();
    }

    public void setContentText(String text) {
        contentText.setText(text);
    }

    public void setSelection(int pos) {
        contentText.setSelection(pos);
    }

    public <T extends Comment> void prepareFragment(String signature, T contents) {
        prepareFragment(signature, contents, true);
    }

    public <T extends Comment> void prepareFragment(String signature, T contents, boolean checkSame) {
        mService = NetworkService.getInstance(getActivity());
        assert (mService != null);
        mHttpClient = mService.mNetworkClient;

        final Comment oldpost = mPost;
        mPost = contents;

        // обязательно
        mSignature = signature;

        if (mPost.getClass() == Post.class) {
            // загружаем автодополнение тем, музыки и настроения
            loadCompletions(themesText, AutocompleteItem.AutocompleteType.THEME);
            loadCompletions(moodText, AutocompleteItem.AutocompleteType.MOOD);
            loadCompletions(musicText, AutocompleteItem.AutocompleteType.MUSIC);

            // Если это новый пост
            if (mPost.postID.isEmpty()) {
                mTitle.setText(R.string.new_post);
                mCurrentPage.setText(UserData.getInstance().getCurrentDiaryPage().getTitle());

                purgeContents();
                for (View v : postElements)
                    v.setVisibility(View.VISIBLE);

                if (oldpost.getClass() == Post.class && ((Post) oldpost).diaryID.equals(((Post) mPost).diaryID)) {
                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.confirmation)
                            .content(R.string.clear_contents)
                            .positiveText(android.R.string.yes)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                    prepareUi((Post) mPost);
                                }
                            })
                            .negativeText(R.string.no)
                            .show();
                } else {
                    prepareUi((Post) mPost);
                }
            } else { // если редактирование поста
                mTitle.setText(R.string.edit_post);
                mCurrentPage.setText(UserData.getInstance().getCurrentDiaryPage().getTitle());

                purgeContents();
                for (View v : postElements)
                    v.setVisibility(View.VISIBLE);

                prepareUi((Post) mPost);
            }
        } else if (mPost.getClass() == Comment.class) {
            // если это новый комментарий
            if (mPost.commentID.isEmpty()) {
                mTitle.setText(R.string.new_comment);
                mCurrentPage.setText(UserData.getInstance().getCurrentDiaryPage().getSubtitle());

                purgeContents();
                for (View v : commentElements)
                    v.setVisibility(View.VISIBLE);

                if (checkSame && oldpost.getClass() == Comment.class && oldpost.postID.equals(mPost.postID)) {
                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.confirmation)
                            .content(R.string.clear_contents)
                            .positiveText(android.R.string.yes)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                    prepareUi(mPost);
                                }
                            })
                            .negativeText(R.string.no)
                            .show();
                } else {
                    prepareUi(mPost);
                }
            } else { // редактирование комментария
                mTitle.setText(R.string.edit_comment);
                mCurrentPage.setText(UserData.getInstance().getCurrentDiaryPage().getSubtitle());

                purgeContents();
                for (View v : commentElements)
                    v.setVisibility(View.VISIBLE);

                prepareUi(mPost);
            }
        } else if (mPost.getClass() == Umail.class) { // Если почта
            mTitle.setText(R.string.new_umail);
            mCurrentPage.setVisibility(View.GONE);

            purgeContents();
            for (View v : umailElements)
                v.setVisibility(View.VISIBLE);

            prepareUi((Umail) mPost);
        }
    }

    private void prepareUi(Comment comment) {
        contentText.setText(comment.content);
        mSubscribe.setChecked(true);
        if (comment.commentID.isEmpty()) // новый коммент
            mSecureOptions.setVisibility(View.VISIBLE);
        else
            mSecureOptions.setVisibility(View.GONE);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void prepareUi(Umail mail) {
        toText.setText(mail.receiver);
        if(mail.receiver.isEmpty() && mail.messageTheme.isEmpty()) {        // новое сообщение
            titleText.setText(mail.messageTheme);
        } else if (mail.receiver.isEmpty()) {                               // пересылаемое сообщение
            titleText.setText("Fw: " + mail.messageTheme);
        } else {                                                            // сообщение-ответ
            Matcher replyFinder = EMAIL_ANSWER_REGEX.matcher(mail.messageTheme);
            if(replyFinder.find()) {                                        // это ответ с номером, инкрементим
                int counter = Integer.parseInt(replyFinder.group(1));
                String realTheme = replyFinder.group(2);
                titleText.setText(String.format("Re[%d]: %s", ++counter, realTheme));
            } else if(mail.messageTheme.startsWith("Re:")) {                // это ответ ещё без номера, вставляем
                titleText.setText(mail.messageTheme.replace("Re:", "Re[1]:"));
            } else {
                titleText.setText("Re: " + mail.messageTheme);              // это первый ответ
            }
        }

        mRequote.setChecked(true);
        requoteText.setText(mail.reMessage);
        mCopyMessage.setChecked(true);
    }

    private void prepareUi(Post post) {
        titleText.setText(post.title);
        contentText.setText(post.content);

        if (!"".equals(post.music + post.mood + post.themes) || !post.predefinedTags.isEmpty())
            mShowOptionals.setChecked(true);
        else
            mShowOptionals.setChecked(false);

        musicText.setText(post.music);
        moodText.setText(post.mood);
        themesText.setText(post.themes);

        if (!post.pollTitle.isEmpty())
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

        if (!post.closeAccessMode.isEmpty())
            mShowCloseOptions.setChecked(true);
        else
            mShowCloseOptions.setChecked(false);

        switch (post.closeAccessMode) {
            case "6":
                mCloseOpts.check(R.id.close_only_reg);
                break;
            case "1":
                mCloseOpts.check(R.id.close_only_fav);
                break;
            case "5":
                mCloseOpts.check(R.id.close_only_sub);
                break;
            case "4":
                mCloseOpts.check(R.id.close_only_white);
                break;
            case "3":
                mCloseOpts.check(R.id.close_for_list);
                break;
            case "2":
                mCloseOpts.check(R.id.close_only_list);
                break;
            case "7":
                mCloseOpts.check(R.id.close_for_all);
                break;
        }

        mCloseText.setText(post.closeText);
        mCloseAllowList.setText(post.closeAllowList);
        mCloseDenyList.setText(post.closeDenyList);

        mNoComments.setChecked(post.noComments);

        mPredefinedThemes.removeAllViews(); // always clear all checks since we recreate them now
        LinearLayout horizontal = null;
        int i = 0;
        for (Map.Entry<String, Boolean> theme : post.predefinedTags.entrySet()) {
            if (i % 4 == 0) // по 4 темы в горизонтальный ряд
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

    public void insertInCursorPosition(@NonNull CharSequence prefix, @NonNull CharSequence what, @NonNull CharSequence suffix) {
        int cursorPos = contentText.getSelectionStart();
        if (cursorPos == -1)
            cursorPos = contentText.getText().length();

        String beforeCursor = contentText.getText().toString().substring(0, cursorPos);
        String afterCursor = contentText.getText().toString().substring(cursorPos, contentText.getText().length());

        String beforeCursorWithPrefix = beforeCursor + prefix;
        String suffixWithAfterCursor = suffix + afterCursor;
        contentText.setText(beforeCursorWithPrefix + what + suffixWithAfterCursor);

        if(what.length() == 0) { // пустая строчка между тэгами, позиционируем в центр
            contentText.setSelection(contentText.getText().toString().indexOf(suffixWithAfterCursor, cursorPos));
        } else if(afterCursor.isEmpty()) { // вставляем в конец, позиционируем на конец строки
            contentText.setSelection(contentText.getText().length());
        } else { // вставляем в середину, позиционируем на строку после вставки
            contentText.setSelection(contentText.getText().toString().indexOf(afterCursor, cursorPos));
        }
    }

    public void onClick(View view) {

        // обработка кнопок аватаров
        if (view instanceof ImageButton && view.getTag(R.integer.avatar_id) != null && view.getParent() == mAvatars) {
            postParams.clear();
            postParams.add(Pair.create("use_avatar_id", view.getTag(R.integer.avatar_id).toString()));
            postParams.add(Pair.create("avatar_url", ""));
            postParams.add(Pair.create("signature", mSignature));
            mHandler.sendEmptyMessage(HANDLE_SET_AVATAR);
        }

        // обработка кнопок смайлов
        if (view instanceof GifImageButton && view.getTag(R.integer.smile_key) != null && view.getParent() == mSmilies) {
            insertInCursorPosition("", (CharSequence) view.getTag(R.integer.smile_key), "");
        }

        if (view instanceof Button && view.getTag(R.integer.smile_page) != null && view.getParent() == mSmilieButtons) {
            pd = new MaterialDialog.Builder(getActivity())
                    .title(R.string.loading)
                    .content(R.string.loading_data)
                    .progress(true, 0)
                    .build();
            pd.show();

            mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_SMILIES, view.getTag(R.integer.smile_page)));
        }

        // обычные кнопки
        switch (view.getId()) {
            case R.id.message_save_draft:
            case R.id.message_publish: {
                sendMessage(view);
                break;
            }
            case R.id.message_show_smilies:
                pd = new MaterialDialog.Builder(getActivity())
                        .title(R.string.loading)
                        .content(R.string.loading_data)
                        .progress(true, 0)
                        .build();
                pd.show();
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_SMILIES, null));
                break;
            case R.id.message_load_draft:
                pd = new MaterialDialog.Builder(getActivity())
                        .title(R.string.loading)
                        .content(R.string.loading_data)
                        .progress(true, 0)
                        .build();
                pd.show();
                mHandler.sendMessage(mHandler.obtainMessage(HANDLE_GET_DRAFTS, null));
                break;
        }
    }

    private void sendMessage(View view) {
        postParams.clear();

        // Добавляем параметры из настроек
        postParams.add(Pair.create("signature", mSignature));
        postParams.add(Pair.create("action", "dosend"));
        pd = new MaterialDialog.Builder(getActivity())
                .title(R.string.loading)
                .content(R.string.sending_data)
                .progress(true, 0)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mHttpClient.abort();
                    }
                })
                .build();
        pd.show();
        //ProgressDialog.show(getActivity(), getString(R.string.loading), getString(R.string.sending_data), true, false);

        // Если пост
        if (mPost.getClass() == Post.class) {
            // сохраняем темы, музыку, настроение для автокомплита
            saveCompletions(themesText, AutocompleteItem.AutocompleteType.THEME);
            saveCompletions(moodText, AutocompleteItem.AutocompleteType.MOOD);
            saveCompletions(musicText, AutocompleteItem.AutocompleteType.MUSIC);

            postParams.add(Pair.create("avatar", "1")); // Показываем аватарку
            postParams.add(Pair.create("module", "journal"));
            postParams.add(Pair.create("resulttype", "2"));
            postParams.add(Pair.create("save_type", "js2"));

            postParams.add(Pair.create("journal_id", ((Post) mPost).diaryID));
            postParams.add(Pair.create("referer", mHttpClient.getCurrentUrl()));
            postParams.add(Pair.create("attachment", ""));

            // draft or publish
            if(view.getId() == R.id.message_save_draft) {
                postParams.add(Pair.create("draft_save", "draft_save"));
            } else {
                postParams.add(Pair.create("rewrite", "rewrite"));
            }

            if (mPost.postID.isEmpty()) { // новый пост
                postParams.add(Pair.create("message", contentText.getText().toString() + mSharedPrefs.getString("post.signature", "")));
                postParams.add(Pair.create("act", "new_post_post"));
                postParams.add(Pair.create("post_id", ""));
                postParams.add(Pair.create("post_type", ""));
            } else { // редактируем пост
                postParams.add(Pair.create("message", contentText.getText().toString()));
                postParams.add(Pair.create("act", "edit_post_post"));
                postParams.add(Pair.create("post_id", mPost.postID));
                postParams.add(Pair.create("post_type", ((Post) mPost).postType));
            }

            postParams.add(Pair.create("title", titleText.getText().toString()));
            if (mShowOptionals.isChecked()) {
                postParams.add(Pair.create("themes", themesText.getText().toString() + (mPost.postID.isEmpty() ? mSharedPrefs.getString("post.tags", "") : "")));
                for (int i = 0; i < mPredefinedThemes.getChildCount(); ++i) {
                    LinearLayout horizontal = (LinearLayout) mPredefinedThemes.getChildAt(i);
                    for (int j = 0; j < horizontal.getChildCount(); ++j) {
                        CheckBox check = (CheckBox) horizontal.getChildAt(j);
                        if (check.isChecked())
                            postParams.add(Pair.create("fvtags[]", check.getText().toString()));
                    }
                }
                postParams.add(Pair.create("current_music", musicText.getText().toString()));
                postParams.add(Pair.create("current_mood", moodText.getText().toString()));
            } else {
                postParams.add(Pair.create("themes", ""));
                postParams.add(Pair.create("current_music", ""));
                postParams.add(Pair.create("current_mood", ""));
            }

            if (mShowPoll.isChecked()) {
                postParams.add(Pair.create("poll_title", mPollTitle.getText().toString()));
                postParams.add(Pair.create("poll_answer_1", mPollChoice1.getText().toString()));
                postParams.add(Pair.create("poll_answer_2", mPollChoice2.getText().toString()));
                postParams.add(Pair.create("poll_answer_3", mPollChoice3.getText().toString()));
                postParams.add(Pair.create("poll_answer_4", mPollChoice4.getText().toString()));
                postParams.add(Pair.create("poll_answer_5", mPollChoice5.getText().toString()));
                postParams.add(Pair.create("poll_answer_6", mPollChoice6.getText().toString()));
                postParams.add(Pair.create("poll_answer_7", mPollChoice7.getText().toString()));
                postParams.add(Pair.create("poll_answer_8", mPollChoice8.getText().toString()));
                postParams.add(Pair.create("poll_answer_9", mPollChoice9.getText().toString()));
                postParams.add(Pair.create("poll_answer_10", mPollChoice10.getText().toString()));
            } else {
                postParams.add(Pair.create("poll_title", ""));
                postParams.add(Pair.create("poll_answer_1", ""));
                postParams.add(Pair.create("poll_answer_2", ""));
                postParams.add(Pair.create("poll_answer_3", ""));
                postParams.add(Pair.create("poll_answer_4", ""));
                postParams.add(Pair.create("poll_answer_5", ""));
                postParams.add(Pair.create("poll_answer_6", ""));
                postParams.add(Pair.create("poll_answer_7", ""));
                postParams.add(Pair.create("poll_answer_8", ""));
                postParams.add(Pair.create("poll_answer_9", ""));
                postParams.add(Pair.create("poll_answer_10", ""));
            }

            if (mShowCloseOptions.isChecked()) {
                postParams.add(Pair.create("private_post", "1"));
                if (!mCloseText.getText().toString().isEmpty()) {
                    postParams.add(Pair.create("check_close_text", "1"));
                    postParams.add(Pair.create("close_text", mCloseText.getText().toString()));
                }

                switch (mCloseOpts.getCheckedRadioButtonId()) {
                    case R.id.close_only_reg:
                        postParams.add(Pair.create("close_access_mode", "6"));
                        break;
                    case R.id.close_only_fav:
                        postParams.add(Pair.create("close_access_mode", "1"));
                        break;
                    case R.id.close_only_sub:
                        postParams.add(Pair.create("close_access_mode", "5"));
                        break;
                    case R.id.close_only_white:
                        postParams.add(Pair.create("close_access_mode", "4"));
                        break;
                    case R.id.close_for_list:
                        postParams.add(Pair.create("close_access_mode", "2"));
                        postParams.add(Pair.create("access_list", mCloseDenyList.getText().toString()));
                        break;
                    case R.id.close_only_list:
                        postParams.add(Pair.create("close_access_mode", "3"));
                        postParams.add(Pair.create("access_list", mCloseAllowList.getText().toString()));
                        break;
                    case R.id.close_for_all:
                        postParams.add(Pair.create("close_access_mode", "7"));
                        break;
                }
            }
            if (mNoComments.isChecked())
                postParams.add(Pair.create("no_comments", "1"));

            mHandler.sendEmptyMessage(HANDLE_DO_POST);
        } else if (mPost.getClass() == Comment.class) {  // если коммент
            postParams.add(Pair.create("avatar", "1")); // Показываем аватарку
            postParams.add(Pair.create("module", "journal"));
            postParams.add(Pair.create("resulttype", "2"));
            if (mPost.commentID.isEmpty()) { // новый пост
                switch (mSecureOptions.getCheckedRadioButtonId()) {
                    case R.id.message_anonymous:
                        postParams.add(Pair.create("write_from", "1"));
                        break;
                    case R.id.message_as_user:
                        postParams.add(Pair.create("write_from", "2"));
                        postParams.add(Pair.create("write_from_name", mUserLogin.getText().toString()));
                        postParams.add(Pair.create("write_from_pass", mUserPassword.getText().toString()));
                        break;
                    default:
                        postParams.add(Pair.create("write_from", "0"));
                        break;
                }
                postParams.add(Pair.create("message", contentText.getText().toString() + mSharedPrefs.getString("post.signature", "")));
                postParams.add(Pair.create("act", "new_comment_post"));
                postParams.add(Pair.create("commentid", ""));
            } else { // редактируем пост
                postParams.add(Pair.create("message", contentText.getText().toString()));
                postParams.add(Pair.create("act", "edit_comment_post"));
                postParams.add(Pair.create("commentid", mPost.commentID));
            }

            postParams.add(Pair.create("post_id", mPost.postID));

            postParams.add(Pair.create("referer", ""));
            postParams.add(Pair.create("page", "last"));
            postParams.add(Pair.create("open_uri", ""));

            postParams.add(Pair.create("subscribe", mSubscribe.isChecked() ? "1/" : ""));
            postParams.add(Pair.create("attachment1", ""));

            mHandler.sendEmptyMessage(HANDLE_DO_COMMENT);
        } else if (mPost.getClass() == Umail.class) { // если почта
            postParams.add(Pair.create("message", contentText.getText().toString() + mSharedPrefs.getString("post.signature", "")));
            postParams.add(Pair.create("module", "umail"));
            postParams.add(Pair.create("act", "umail_send"));
            postParams.add(Pair.create("from_folder", ""));
            postParams.add(Pair.create("to_user", toText.getText().toString()));
            postParams.add(Pair.create("title", titleText.getText().toString()));
            postParams.add(Pair.create("save_copy", mCopyMessage.isChecked() ? "yes" : ""));
            postParams.add(Pair.create("need_receipt", mGetReceipt.isChecked() ? "yes" : ""));
            if (mRequote.isChecked()) {
                postParams.add(Pair.create("requote", "yes"));
                postParams.add(Pair.create("remessage", "\n\n" + requoteText.getText().toString()));
            }

            mHandler.sendEmptyMessage(HANDLE_DO_UMAIL);
        }
    }

    private void saveCompletions(EditText edit, AutocompleteItem.AutocompleteType type) {
        RuntimeExceptionDao<AutocompleteItem, Long> acDao = DbProvider.getHelper().getAutocompleteDao();
        String tokenString = edit.getText().toString();
        if(!tokenString.isEmpty()) {
            for(String token : tokenString.split(";")) {
                String testToken = token.trim();
                if(testToken.isEmpty()) {
                    continue;
                }

                SelectArg textEscaped = new SelectArg(testToken);
                List<AutocompleteItem> found = acDao.queryForEq("text", textEscaped);
                AutocompleteItem item = found.isEmpty() ? new AutocompleteItem() : found.get(0);
                item.setType(type);
                item.setText(testToken);
                acDao.createOrUpdate(item);
            }
        }
    }

    private void loadCompletions(MultiAutoCompleteTextView edit, AutocompleteItem.AutocompleteType type) {
        RuntimeExceptionDao<AutocompleteItem, Long> acDao = DbProvider.getHelper().getAutocompleteDao();
        List<AutocompleteItem> tokens = acDao.queryForEq("type", type);
        ArrayAdapter<AutocompleteItem> tokenAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, tokens);
        edit.setAdapter(tokenAdapter);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.message_requote:
                if (isChecked)
                    requoteText.setVisibility(View.VISIBLE);
                else
                    requoteText.setVisibility(View.GONE);
                break;
            case R.id.message_optional:
                if (isChecked)
                    mOptionals.setVisibility(View.VISIBLE);
                else
                    mOptionals.setVisibility(View.GONE);
                break;
            case R.id.message_poll:
                if (isChecked)
                    mPoll.setVisibility(View.VISIBLE);
                else
                    mPoll.setVisibility(View.GONE);
                break;
            case R.id.message_close:
                if (isChecked)
                    mCloseOpts.setVisibility(View.VISIBLE);
                else
                    mCloseOpts.setVisibility(View.GONE);
                break;
            case R.id.message_custom_avatar:
                if (isChecked) {
                    mAvatars.setVisibility(View.VISIBLE);
                    if (avatarMap == null) {
                        pd = new MaterialDialog.Builder(getActivity())
                                .title(R.string.loading)
                                .content(R.string.loading_data)
                                .progress(true, 0)
                                .build();
                        pd.show();
                        mHandler.sendEmptyMessage(HANDLE_REQUEST_AVATARS);
                    }
                } else
                    mAvatars.setVisibility(View.GONE);
            default:
                break;
        }
    }

    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group == mCloseOpts)
            switch (checkedId) {
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
        else if (group == mSecureOptions)
            switch (checkedId) {
                case R.id.message_as_user:
                    mAsUserLayout.setVisibility(View.VISIBLE);
                    break;
                default:
                    mAsUserLayout.setVisibility(View.GONE);
            }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Utils.ACTIVITY_ACTION_REQUEST_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    requestFileUpload(uri);
                }
                break;
            case Utils.ACTIVITY_ACTION_REQUEST_MUSIC:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    requestMusicUpload(uri);
                }
                break;
            case Utils.ACTIVITY_ACTION_REQUEST_GIF:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    requestGifUpload(uri);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void requestGifUpload(Uri uri) {
        File file = getFileFromUri(uri);
        try {
            if (file != null) {
                final Message msg = mHandler.obtainMessage(Utils.HANDLE_UPLOAD_GIF, file.getCanonicalPath());
                msg.arg1 = 3;
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.select_gif_width)
                        .negativeText(R.string.s100)
                        .neutralText(R.string.s200)
                        .positiveText(R.string.s300)
                        .onAny(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                switch (dialogAction) {
                                    case NEGATIVE:
                                        msg.arg1 = 1;
                                        break;
                                    case NEUTRAL:
                                        msg.arg1 = 2;
                                        break;
                                    case POSITIVE:
                                    default:
                                        msg.arg1 = 3;
                                        break;
                                }

                                pd = new MaterialDialog.Builder(getActivity())
                                        .title(R.string.loading)
                                        .content(R.string.sending_data)
                                        .progress(false, 100)
                                        .build();
                                pd.show();
                                mHandler.sendMessage(msg);
                            }
                        }).show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private File getFileFromUri(Uri uri) {
        File file = null;
        if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            file = FileUtils.getFile(getActivity(), uri);
        } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme()))
            file = new File(uri.getPath());
        return file;
    }

    private void requestMusicUpload(Uri uri) {
        File file = getFileFromUri(uri);
        try {
            if (file != null) {
                final Message msg = mHandler.obtainMessage(Utils.HANDLE_UPLOAD_MUSIC, file.getCanonicalPath());
                msg.arg1 = 2;
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.select_pleer_color)
                        .negativeText(R.string.black)
                        .positiveText(R.string.grey)
                        .onAny(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                switch (dialogAction) {
                                    case NEGATIVE:
                                        msg.arg1 = 1;
                                        break;
                                    case POSITIVE:
                                    default:
                                        msg.arg1 = 2;
                                        break;
                                }

                                pd = new MaterialDialog.Builder(getActivity())
                                        .title(R.string.loading)
                                        .content(R.string.sending_data)
                                        .progress(false, 100)
                                        .build();
                                pd.show();
                                mHandler.sendMessage(msg);
                            }
                        }).show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    public void requestFileUpload(Uri uri) {
        File file = getFileFromUri(uri);
        try {
            if (file != null) {
                final Message msg = mHandler.obtainMessage(Utils.HANDLE_UPLOAD_FILE, file.getCanonicalPath());
                msg.arg1 = 3;
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.howto_send_img)
                        .negativeText(R.string.pack_inoriginal)
                        .positiveText(R.string.pack_inmore)
                        .neutralText(R.string.pack_inlink)
                        .onAny(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                switch (dialogAction) {
                                    case NEGATIVE:
                                        msg.arg1 = 1;
                                        break;
                                    case NEUTRAL:
                                        msg.arg1 = 2;
                                        break;
                                    case POSITIVE:
                                    default:
                                        msg.arg1 = 3;
                                        break;
                                }

                                pd = new MaterialDialog.Builder(getActivity())
                                        .title(R.string.loading)
                                        .content(R.string.sending_data)
                                        .progress(false, 100)
                                        .build();
                                pd.show();
                                mHandler.sendMessage(msg);
                            }
                        }).show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    private void closeMe(boolean reload) {
        ((DiaryActivity) getActivity()).onMessagePaneRemove(reload);
    }

    private void setSmiliesState(boolean animate) {
        int childCount = mSmilies.getChildCount();
        for(int i = 0; i < childCount; ++i) {
            GifImageButton gib = (GifImageButton) mSmilies.getChildAt(i);
            GifDrawable gd = (GifDrawable) gib.getDrawable();
            if(animate) {
                gd.start();
            } else {
                gd.stop();
            }
        }
    }

    private void purgeContents() {
        for (int i = 0; i < mMainLayout.getChildCount(); ++i) {
            View curr = mMainLayout.getChildAt(i);
            if (curr.getContentDescription() == null || !curr.getContentDescription().toString().equals("persistent"))
                mMainLayout.getChildAt(i).setVisibility(View.GONE);
            if (curr instanceof CheckBox)
                ((CheckBox) curr).setChecked(false);
            if (curr instanceof RadioGroup)
                ((RadioGroup) curr).clearCheck();
        }

        // TODO: переделать обработку с использованием ViewGroup
        mSaveDraft.setVisibility(View.GONE);
        mLoadDraft.setVisibility(View.GONE);
    }

    private class SendProgressListener implements DiaryHttpClient.ProgressListener {
        private final long length;

        public SendProgressListener(long length) {
            this.length = length;
        }

        @Override
        public void transferred(long transferredBytes) {
            long percent = (transferredBytes * 100) / length;
            mUiHandler.sendMessage(mUiHandler.obtainMessage(HANDLE_PROGRESS, (int) percent));
        }
    }
}
