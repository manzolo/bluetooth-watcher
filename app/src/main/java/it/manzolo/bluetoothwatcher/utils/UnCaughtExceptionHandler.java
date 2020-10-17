package it.manzolo.bluetoothwatcher.utils;

import android.app.Activity;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.jetbrains.annotations.NotNull;

public class UnCaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Activity activity;

    public UnCaughtExceptionHandler(Activity a) {
        activity = a;
    }

    @Override
    public void uncaughtException(@NotNull Thread thread, @NotNull Throwable ex) {
        //do your life saving stuff here
        ProcessPhoenix.triggerRebirth(activity.getApplicationContext());
    }
}

