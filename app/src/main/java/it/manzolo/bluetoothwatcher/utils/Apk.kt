package it.manzolo.bluetoothwatcher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

class Apk {

    fun installApk(context: Context, uri: Uri) {

        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)

        }
        context.startActivity(intent)
    }


}