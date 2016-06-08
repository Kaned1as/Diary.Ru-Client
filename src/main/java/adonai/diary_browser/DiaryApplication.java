package adonai.diary_browser;

import android.app.Application;

import adonai.diary_browser.database.DbProvider;

/**
 * Для инициализации ресурсов, общих для всего приложения
 */
public class DiaryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DbProvider.setHelper(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        DbProvider.releaseHelper();
    }
}
