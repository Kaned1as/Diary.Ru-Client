package adonai.diary_browser.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Класс базы данных, нужен преимущественно для предоставления
 * сведений автодополнения и оффлайн-работы
 *
 * @author Adonai
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
        TEXT,
        TITLE
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
                AutocompleteFields.TEXT + " TEXT NOT NULL, " +
                AutocompleteFields.TITLE + " TEXT DEFAULT NULL" +
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

    public Cursor getAutocompleteCursor(AutocompleteType type, String filter) {
        return getWritableDatabase().query(AUTOCOMPETIONS_TABLE_NAME, new String[]{AutocompleteFields.TEXT.toString(), AutocompleteFields.TITLE.toString()}, "TYPE = ? AND TEXT LIKE ?", new String[]{String.valueOf(type.ordinal()), "%" + filter + "%"}, null, null, null, null);
    }

    public void addAutocompleteText(AutocompleteType type, String query) {
        addAutocompleteText(type, query, null);
    }

    public void addAutocompleteText(AutocompleteType type, String query, String caption) {
        final ContentValues cv = new ContentValues(3);
        cv.put(AutocompleteFields.TYPE.toString(), type.ordinal());
        cv.put(AutocompleteFields.TEXT.toString(), query);
        if(caption != null)
            cv.put(AutocompleteFields.TITLE.toString(), caption);
        getWritableDatabase().insertWithOnConflict(AUTOCOMPETIONS_TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
