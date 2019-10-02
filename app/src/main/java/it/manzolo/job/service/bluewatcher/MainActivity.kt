package it.manzolo.job.service.bluewatcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.job.service.enums.BluetoothEvents
import it.manzolo.job.service.enums.WebserverEvents
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    private val mLocalBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            when (intent?.action) {
                BluetoothEvents.ERROR -> {
                    window.decorView.setBackgroundColor(Color.rgb(150, 0, 0))
                    context.run { textView.text = intent.getStringExtra("message") }
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.ERROR -> {
                    window.decorView.setBackgroundColor(Color.rgb(150, 0, 0))
                    context.run { textView.text = intent.getStringExtra("message") }
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                BluetoothEvents.DATA_RETRIEVED -> {
                    context.run { textView.text = intent.getStringExtra("message") }
                    window.decorView.setBackgroundColor(Color.rgb(1, 126, 0))
                }
                WebserverEvents.DATA_SENT -> {
                    context.run { textView.text = intent.getStringExtra("message") }
                    window.decorView.setBackgroundColor(Color.rgb(1, 126, 0))
                }
            }
        }
    }

    private fun getConnectionErrorLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(BluetoothEvents.ERROR)
        return iFilter
    }

    private fun getConnectionOkLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(BluetoothEvents.DATA_RETRIEVED)
        return iFilter
    }

    private fun getWebserverErrorDataSentLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.ERROR)
        return iFilter
    }

    private fun getWebserverDataSentLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.DATA_SENT)
        return iFilter
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getConnectionOkLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getConnectionErrorLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getWebserverDataSentLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getWebserverErrorDataSentLocalIntentFilter())

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
