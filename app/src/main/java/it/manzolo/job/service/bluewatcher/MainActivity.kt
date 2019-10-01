package it.manzolo.job.service.bluewatcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.job.service.enums.BluetoothEvents
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    private val mLocalBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothEvents.ERROR_CONNECTION -> {
                    Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getConnectionErrorLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(BluetoothEvents.ERROR_CONNECTION)
        return iFilter
    }

    private fun getWebserverDataSentLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(BluetoothEvents.WEBSERVER_SEND_DATA)
        return iFilter
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getConnectionErrorLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getWebserverDataSentLocalIntentFilter())

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent1 = Intent(this, SettingsActivity::class.java)
                this.startActivity(intent1)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
