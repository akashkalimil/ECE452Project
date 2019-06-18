package com.teamred.candid;

import android.app.Application;

import io.reactivex.plugins.RxJavaPlugins;

public class CandidApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RxJavaPlugins.setErrorHandler(Throwable::printStackTrace);
    }
}
