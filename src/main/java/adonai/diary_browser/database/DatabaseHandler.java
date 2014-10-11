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
    public static final int DB_VERSION = 2;

    public static enum AutocompleteType
    {
        URL,
        TITLE,
        THEME,
        MUSIC,
        MOOD
    }

    private static final String AUTOCOMPLETIONS_TABLE_NAME = "autocomplete";
    private static enum AutocompleteFields
    {
        _id,
        TYPE,
        TEXT,
        TITLE
    }

    private static final String THEME_TABLE_NAME = "theme";
    private static enum ThemeFields
    {
        KEY,
        BACKGROUND_COLOR,
        UP_COLOR,
        DOWN_COLOR,
        TEXT_COLOR,
        HINT_COLOR,
    }

    public DatabaseHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE " + AUTOCOMPLETIONS_TABLE_NAME + " (" +
                AutocompleteFields._id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                AutocompleteFields.TYPE + " INTEGER NOT NULL, " +
                AutocompleteFields.TEXT + " TEXT NOT NULL, " +
                AutocompleteFields.TITLE + " TEXT DEFAULT NULL" +
                ")");
        db.execSQL("CREATE UNIQUE INDEX " + "AUTOCOMPLETE_NAME_IDX ON " + AUTOCOMPLETIONS_TABLE_NAME + " (" + AutocompleteFields.TEXT + ")");

        db.execSQL("CREATE TABLE " + THEME_TABLE_NAME + " (" +
                ThemeFields.KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ThemeFields.BACKGROUND_COLOR + " INTEGER DEFAULT NULL, " +
                ThemeFields.UP_COLOR + " INTEGER DEFAULT NULL, " +
                ThemeFields.DOWN_COLOR + " INTEGER DEFAULT NULL, " +
                ThemeFields.TEXT_COLOR + " INTEGER DEFAULT NULL, " +
                ThemeFields.HINT_COLOR + " INTEGER DEFAULT NULL" +
                ")");
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
        if(oldVersion == 1 && newVersion == 2) {
            db.execSQL("CREATE TABLE " + THEME_TABLE_NAME + " (" +
                    ThemeFields.KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ThemeFields.BACKGROUND_COLOR + " INTEGER DEFAULT NULL, " +
                    ThemeFields.UP_COLOR + " INTEGER DEFAULT NULL, " +
                    ThemeFields.DOWN_COLOR + " INTEGER DEFAULT NULL, " +
                    ThemeFields.TEXT_COLOR + " INTEGER DEFAULT NULL, " +
                    ThemeFields.HINT_COLOR + " INTEGER DEFAULT NULL" +
                    ")");
        }
    }

    public Cursor getAutocompleteCursor(AutocompleteType type, String filter) {
        return getWritableDatabase().query(AUTOCOMPLETIONS_TABLE_NAME, new String[]{AutocompleteFields._id.toString(), AutocompleteFields.TEXT.toString(), AutocompleteFields.TITLE.toString()}, "TYPE = ? AND TEXT LIKE ?", new String[]{String.valueOf(type.ordinal()), "%" + filter + "%"}, null, null, null, null);
    }

    public Cursor getThemeRow(String type) {
        Cursor cursor = getWritableDatabase().query(THEME_TABLE_NAME, null, "KEY = ?", new String[]{type}, null, null, null, null);
        if(cursor.moveToFirst()) {
            return cursor;
        }
        return null;
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
        getWritableDatabase().insertWithOnConflict(AUTOCOMPLETIONS_TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
