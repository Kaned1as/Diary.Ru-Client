package adonai.diary_browser.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by adonai on 26.05.14.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "diaryDB";
    public static final int DB_VERSION = 1;

    public static enum AutocompleteType
    {
        URL,
        TITLE,
        THEME,
        MUSIC,
        MOOD
    }

    public static final String AUTOCOMPETIONS_TABLE_NAME = "autocomplete";
    public static enum AutocompleteFields
    {
        _id,
        TYPE,
        TEXT
    }

    public DatabaseHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE " + AUTOCOMPETIONS_TABLE_NAME + " (" +
                AutocompleteFields._id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                AutocompleteFields.TYPE + " INTEGER NOT NULL, " +
                AutocompleteFields.TEXT + " TEXT NOT NULL" +
                ")");
        db.execSQL("CREATE UNIQUE INDEX " + "AUTOCOMPLETE_NAME_IDX ON " + AUTOCOMPETIONS_TABLE_NAME + " (" + AutocompleteFields.TYPE + ")");
    }

    @Override
    public void onOpen(SQLiteDatabase db) { // called AFTER upgrade!
        super.onOpen(db);
        if (!db.isReadOnly())
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys = ON");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
