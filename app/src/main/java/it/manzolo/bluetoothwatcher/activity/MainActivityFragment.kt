package it.manzolo.bluetoothwatcher.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import it.manzolo.bluetoothwatcher.App
import it.manzolo.bluetoothwatcher.R
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
        if (savedInstanceState == null) {
            // only create fragment if activity is started for the first time
            val fileUpdate = File(context?.cacheDir, "app.ava")
            fileUpdate.delete()

            //buttonUpdate.isEnabled = false
            startBluetoothService()
            startWebserviceSendService()
            startLocationService()
            startUpdateService()
            startRebootService()

        }

    }

    private fun startBluetoothService() {
        Log.d(TAG, "startBluetoothService")
        App.scheduleWatcherService(activity as Context)

        //activity.run { editText.append("$now Service started\n") }
    }

    private fun startUpdateService() {
        Log.d(TAG, "startUpdateService")
        App.scheduleUpdateService(activity as Context)
        //val now = DateUtils.now()
        //activity.run { editText.append("$now Service update started\n") }
    }

    private fun startWebserviceSendService() {
        Log.d(TAG, "startWebserviceSendService")
        App.scheduleWebsendService(activity as Context)

        //val now = DateUtils.now()
        //activity.run { editText.append("$now Service websender started\n") }
    }

    private fun startLocationService() {
        Log.d(TAG, "startLocationService")
        App.scheduleLocationService(activity as Context)
        //val now = DateUtils.now()
        //activity.run { editText.append("$now Service location started\n") }
    }

    private fun startRebootService() {
        Log.d(TAG, "startRebootService")
        App.scheduleRebootService(activity as Context)

        //val now = DateUtils.now()
        //activity.run { editText.append("$now Service reboot started\n") }
    }


}
