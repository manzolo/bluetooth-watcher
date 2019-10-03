package it.manzolo.job.service.bluewatcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File

class MainActivityFragment : Fragment() {

    companion object {
        val TAG: String = MainActivityFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fileupdate = File(context?.cacheDir, "app.ava")
        fileupdate.delete()

        Log.d(TAG, "startUpdateService")
        AppUpdate.scheduleUpdateService(activity as Context)
        activity.run { textView.text = "Service update started" }

        button2.isEnabled = false

        button.setOnClickListener {
            button.isEnabled = false
            startJobService()
        }
        button2.setOnClickListener {
            val activity = activity as MainActivity
            val file = File(activity.cacheDir, "app.apk")
            val photoURI = activity.applicationContext.let { it1 -> FileProvider.getUriForFile(it1, activity.applicationContext.packageName + ".provider", file) }

            val updateapp = UpdateApp()
            updateapp.setContext(activity)
            Log.i("manzolo", file.toString())
            var outputDir = photoURI.toString()
            updateapp.execute(button2.tag.toString(), outputDir)

        }
    }

    private fun startJobService() {
        Log.d(TAG, "startJobService")
        App.scheduleJobService(activity as Context)
        activity.run { textView.text = "Service started" }
    }

    fun installApk(uri: Uri) {

        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)

        }
        activity?.startActivity(intent)
    }

    /*fun install(context: Context, packageName: String, apkPath: String) {

        // PackageManager provides an instance of PackageInstaller
        val packageInstaller = context.packageManager.packageInstaller

        // Prepare params for installing one APK file with MODE_FULL_INSTALL
        // We could use MODE_INHERIT_EXISTING to install multiple split APKs
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(packageName)

        // Get a PackageInstaller.Session for performing the actual update
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        // Copy APK file bytes into OutputStream provided by install Session
        val out = session.openWrite(packageName, 0, -1)
        val fis = File(apkPath).inputStream()
        fis.copyTo(out)
        session.fsync(out)
        out.close()

        // The app gets killed after installation session commit
        session.commit(PendingIntent.getBroadcast(context, sessionId,
                Intent("android.intent.action.MAIN"), 0).intentSender)
    }*/
}
