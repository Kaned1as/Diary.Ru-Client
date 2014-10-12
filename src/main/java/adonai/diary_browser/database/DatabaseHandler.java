package adonai.diary_browser.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;

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

    public static final String AUTOCOMPLETIONS_TABLE_NAME = "autocomplete";
    public enum AutocompleteFields
    {
        _id,
        TYPE,
        TEXT,
        TITLE
    }

    /**
     * Null-value fields mean that no particular color can be chosen for particular widget
     */
    public static final String THEME_TABLE_NAME = "theme";
    public enum ThemeField
    {
        KEY,
        TITLE,
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
                ThemeField.KEY + " TEXT PRIMARY KEY, " +
                ThemeField.TITLE + " TEXT NOT NULL, " +
                ThemeField.BACKGROUND_COLOR + " INTEGER DEFAULT NULL, " +
                ThemeField.UP_COLOR + " INTEGER DEFAULT NULL, " +
                ThemeField.DOWN_COLOR + " INTEGER DEFAULT NULL, " +
                ThemeField.TEXT_COLOR + " INTEGER DEFAULT NULL, " +
                ThemeField.HINT_COLOR + " INTEGER DEFAULT NULL" +
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
                    ThemeField.KEY + " TEXT PRIMARY KEY, " +
                    ThemeField.TITLE + " TEXT NOT NULL, " +
                    ThemeField.BACKGROUND_COLOR + " INTEGER DEFAULT NULL, " +
                    ThemeField.UP_COLOR + " INTEGER DEFAULT NULL, " +
                    ThemeField.DOWN_COLOR + " INTEGER DEFAULT NULL, " +
                    ThemeField.TEXT_COLOR + " INTEGER DEFAULT NULL, " +
                    ThemeField.HINT_COLOR + " INTEGER DEFAULT NULL" +
                    ")");
        }
    }

    public Cursor getAutocompleteCursor(AutocompleteType type, String filter) {
        return getWritableDatabase().query(AUTOCOMPLETIONS_TABLE_NAME, new String[]{AutocompleteFields._id.toString(), AutocompleteFields.TEXT.toString(), AutocompleteFields.TITLE.toString()}, "TYPE = ? AND TEXT LIKE ?", new String[]{String.valueOf(type.ordinal()), "%" + filter + "%"}, null, null, null, null);
    }

    public HashMap<ThemeField, Object> getThemeRow(String type) {
        HashMap<ThemeField, Object> themeRow = new HashMap<>(5);
        Cursor cursor = getWritableDatabase().query(THEME_TABLE_NAME, null, "KEY = ?", new String[]{type}, null, null, null, null);
        if(cursor.moveToFirst()) {
            for(int i = 2; i < cursor.getColumnCount(); ++i) {
                if(!cursor.isNull(i)) {
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_INTEGER:
                            themeRow.put(ThemeField.valueOf(cursor.getColumnName(i)), cursor.getInt(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            themeRow.put(ThemeField.valueOf(cursor.getColumnName(i)), cursor.getString(i));
                    }
                }
            }
        }
        cursor.close();
        return themeRow;
    }

    public Cursor getThemesCursor() {
        return getWritableDatabase().query(THEME_TABLE_NAME, null, null, null, null, null, null, null);
    }

    public void modifyThemeRow(String type, ThemeField field, Object value) {
        ContentValues cv = new ContentValues(3);
        if(value instanceof Integer) {
            cv.put(field.toString(), (Integer) value);
        } else if (value instanceof String) {
            cv.put(field.toString(), (String) value);
        }
        getWritableDatabase().update(THEME_TABLE_NAME, cv, "KEY = ?", new String[]{type});
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
