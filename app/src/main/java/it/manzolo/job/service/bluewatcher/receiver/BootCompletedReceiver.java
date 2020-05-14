package it.manzolo.job.service.bluewatcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {
        // TODO Auto-generated method stub
        Log.w("boot_broadcast_poc", "starting service...");

        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage("it.manzolo.job.service.bluewatcher");
        context.startActivity(launchIntent);

        //context.startService(new Intent(context, MainService.class));
        //context.startService(new Intent(context, UpdateService.class));
        //context.startService(new Intent(context, WebsendService.class));
        //context.startService(new Intent(context, LocationService.class));
    }

}