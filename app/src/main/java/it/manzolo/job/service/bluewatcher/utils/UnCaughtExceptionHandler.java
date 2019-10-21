package it.manzolo.job.service.bluewatcher.utils;

import android.app.Activity;

import com.jakewharton.processphoenix.ProcessPhoenix;

public class UnCaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Activity activity;

    public UnCaughtExceptionHandler(Activity a) {
        activity = a;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        //do your life saving stuff here
        ProcessPhoenix.triggerRebirth(activity.getApplicationContext());
    }
}

