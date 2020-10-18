package it.manzolo.bluetoothwatcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    private Context context;
    private Intent arg1;

    @Override
    public void onReceive(Context context, Intent arg1) {
        this.context = context;
        this.arg1 = arg1;
        Log.w("boot_broadcast_poc", "starting service...");

        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage("it.manzolo.bluetoothwatcher");
        context.startActivity(launchIntent);
    }

}