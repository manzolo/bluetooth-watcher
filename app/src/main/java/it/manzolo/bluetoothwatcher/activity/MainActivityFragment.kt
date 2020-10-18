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
            startBluetoothService()
            startWebserviceSendService()
            startLocationService()
            startUpdateService()
            startRebootService()
        }
    }

    private fun startBluetoothService() {
        Log.d(TAG, "startBluetoothService")
        App.scheduleBluetoothService(activity as Context)
    }

    private fun startUpdateService() {
        Log.d(TAG, "startUpdateService")
        App.scheduleUpdateService(activity as Context)
    }

    private fun startWebserviceSendService() {
        Log.d(TAG, "startWebserviceSendService")
        App.scheduleWebserviceSendService(activity as Context)
    }

    private fun startLocationService() {
        Log.d(TAG, "startLocationService")
        App.scheduleLocationService(activity as Context)
    }

    private fun startRebootService() {
        Log.d(TAG, "startRebootService")
        App.scheduleRestartAppService(activity as Context)
    }
}
