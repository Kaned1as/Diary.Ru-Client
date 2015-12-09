package adonai.diary_browser.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import adonai.diary_browser.entities.AutocompleteItem;
import adonai.diary_browser.entities.CredentialsItem;

/**
 * Helper class for managing OrmLite database and DAOs
 *
 * @author Adonai
 */
public class PersistManager extends OrmLiteSqliteOpenHelper {
    private static final String TAG = PersistManager.class.getSimpleName();

    private static final String DATABASE_NAME ="diaryDB";

    private static final int DATABASE_VERSION = 5;

    //Dao fast access links
    private RuntimeExceptionDao<AutocompleteItem, Long> mAutocompleteDao;
    private RuntimeExceptionDao<CredentialsItem, Long> mCredentialsDao;

    public PersistManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, AutocompleteItem.class);
            TableUtils.createTable(connectionSource, CredentialsItem.class);
        } catch (SQLException e) {
            Log.e(TAG, "error upgrading DB " + DATABASE_NAME);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVer, int newVer) {
        switch (oldVer) {
            case 4:
                try {
                    // У нас уже есть таблица автокомплита (non-ormlite)
                    TableUtils.createTable(connectionSource, CredentialsItem.class);
                } catch (SQLException e) {
                    Log.e(TAG, "error upgrading DB " + DATABASE_NAME);
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    @NonNull
    public RuntimeExceptionDao<CredentialsItem, Long> getCredentialsDao() {
        if (mCredentialsDao == null) {
            mCredentialsDao = getRuntimeExceptionDao(CredentialsItem.class);
        }
        return mCredentialsDao;
    }

    @NonNull
    public RuntimeExceptionDao<AutocompleteItem, Long> getAutocompleteDao() {
        if (mAutocompleteDao == null) {
            mAutocompleteDao = getRuntimeExceptionDao(AutocompleteItem.class);
        }
        return mAutocompleteDao;
    }

    public void clearAllTables() {
        try {
            TableUtils.clearTable(DbProvider.getHelper().getConnectionSource(), AutocompleteItem.class);
            TableUtils.clearTable(DbProvider.getHelper().getConnectionSource(), CredentialsItem.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        super.close();
    }
}
