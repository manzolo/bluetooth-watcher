package it.manzolo.job.service.bluewatcher.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import it.manzolo.job.service.bluewatcher.App
import it.manzolo.job.service.bluewatcher.R
import it.manzolo.job.service.bluewatcher.updater.UpdateApp
import it.manzolo.job.service.bluewatcher.utils.GithubUpdater
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

        //buttonUpdate.isEnabled = false
        startUpdateService()
        startJobService()
        startWebsendService()

        buttonUpdate.setOnClickListener {
            val activity = activity as MainActivity
            val file = File(activity.cacheDir, "app.apk")
            val photoURI = activity.applicationContext.let { it1 -> FileProvider.getUriForFile(it1, activity.applicationContext.packageName + ".provider", file) }

            buttonUpdate.isEnabled = false

            val updateapp = UpdateApp()
            updateapp.setContext(activity)
            //Log.i("manzolo", file.toString())
            var outputDir = photoURI.toString()
            if (buttonUpdate.tag == null) {
                val githubup = GithubUpdater()
                githubup.checkUpdate(activity.applicationContext)
            } else {
                updateapp.execute(buttonUpdate.tag.toString(), outputDir)
            }
            buttonUpdate.isEnabled = true


        }
    }

    private fun startJobService() {
        Log.d(TAG, "startJobService")
        App.scheduleWatcherService(activity as Context)
        activity.run { textView.text = "Service started" }
    }

    private fun startUpdateService() {
        Log.d(TAG, "startUpdateService")
        App.scheduleUpdateService(activity as Context)
        activity.run { textView.text = "Service update started" }
    }

    private fun startWebsendService() {
        Log.d(TAG, "startWebsendService")
        App.scheduleWebsendService(activity as Context)
        activity.run { textView.text = "Service websend started" }
    }
}
