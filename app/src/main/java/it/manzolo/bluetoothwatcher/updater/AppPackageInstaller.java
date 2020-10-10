package it.manzolo.bluetoothwatcher.updater;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class AppPackageInstaller {
    public static final String TAG = "CosuSetup";
    public static final boolean DEBUG = false;

    public static final int MSG_DOWNLOAD_COMPLETE = 1;
    public static final int MSG_DOWNLOAD_TIMEOUT = 2;
    public static final int MSG_INSTALL_COMPLETE = 3;
    public static final String ACTION_INSTALL_COMPLETE = "com.afwsamples.testdpc.INSTALL_COMPLETE";
    private static final int DOWNLOAD_TIMEOUT_MILLIS = 120_000;

    public static boolean installPackage(Context context, String filepathApk) {
        try {
            PackageInstaller pi = context.getPackageManager().getPackageInstaller();
            int sessionId = pi.createSession(new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));
            PackageInstaller.Session session = pi.openSession(sessionId);
            long sizeBytes = 0;
            final File file = new File(filepathApk);
            if (file.isFile()) {
                sizeBytes = file.length();
            }
            InputStream in = new FileInputStream(filepathApk);
            OutputStream out = session.openWrite("app_store_session", 0, sizeBytes);
            int total = 0;
            byte[] buffer = new byte[65536];
            int len;
            while ((len = in.read(buffer)) != -1) {
                total += len;
                out.write(buffer, 0, len);
            }
            session.fsync(out);
            in.close();
            out.close();
            Log.i(TAG, "InstallApkViaPackageInstaller - Success: streamed apk " + total + " bytes");
            PendingIntent broadCastTest = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    new Intent(ACTION_INSTALL_COMPLETE),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(broadCastTest.getIntentSender());
            session.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /*public static void uninstallPackage(Context context, String packageName) {
        Intent intent = new Intent(context, context.getClass());
        PendingIntent sender = PendingIntent.getActivity(context, 0, intent, 0);
        PackageInstaller mPackageInstaller = context.getPackageManager().getPackageInstaller();
        mPackageInstaller.uninstall(packageName, sender.getIntentSender());
    }*/

}