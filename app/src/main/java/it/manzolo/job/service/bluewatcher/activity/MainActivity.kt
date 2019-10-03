package it.manzolo.job.service.bluewatcher.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.manzolo.job.service.bluewatcher.R
import it.manzolo.job.service.bluewatcher.utils.Apk
import it.manzolo.job.service.enums.BluetoothEvents
import it.manzolo.job.service.enums.WebserverEvents
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    private val mLocalBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            when (intent?.action) {
                BluetoothEvents.ERROR -> {
                    context.run { imageView.setImageResource(android.R.drawable.presence_busy) }
                    context.run { textView.text = intent.getStringExtra("message") }
                    context.run { editText.append(intent.getStringExtra("message") + "\n") }
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.ERROR -> {
                    context.run { imageView.setImageResource(android.R.drawable.presence_busy) }
                    context.run { textView.text = intent.getStringExtra("message") }
                    context.run { editText.append(intent.getStringExtra("message") + "\n") }
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                BluetoothEvents.DATA_RETRIEVED -> {
                    context.run { imageView.setImageResource(android.R.drawable.presence_online) }
                    context.run { textView.text = intent.getStringExtra("message") }
                    context.run { editText.append(intent.getStringExtra("message") + "\n") }
                }
                WebserverEvents.DATA_SENT -> {
                    context.run { imageView.setImageResource(android.R.drawable.presence_online) }
                    context.run { textView.text = intent.getStringExtra("message") }
                    //context.run { editText.append(intent.getStringExtra("message") + "\n") }
                }

                WebserverEvents.APP_UPDATE -> {
                    val file = File(applicationContext.cacheDir, "app.apk")
                    val apkfile = applicationContext.applicationContext.let { it1 -> FileProvider.getUriForFile(it1, applicationContext.applicationContext.packageName + ".provider", file) }
                    if (file.exists()) {
                        val apk = Apk()
                        apk.installApk(applicationContext, apkfile)
                        val fileupdate = File(applicationContext.cacheDir, "app.ava")
                        fileupdate.delete()
                    }
                }
                WebserverEvents.APP_AVAILABLE -> {
                    context.run { buttonUpdate.isEnabled = true }
                    context.run { buttonUpdate.tag = intent.getStringExtra("message") }
                    if (debug) {
                        Toast.makeText(context, "Update available at " + intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }

                }
                WebserverEvents.APP_CHECK_UPDATE -> {
                    if (debug) {
                        Toast.makeText(context, "Check for app update", Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.APP_NOAVAILABLEUPDATE -> {
                    Toast.makeText(context, "No available update", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class UpdateReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val file = File(context.cacheDir, "app.apk")

            if (file.exists()) {
                file.delete()
            }
            // Restart your app here
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    private fun getUpgradeLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.APP_UPDATE)
        return iFilter
    }

    private fun getCheckUpdateLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.APP_CHECK_UPDATE)
        return iFilter
    }

    private fun getNoUpdateLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.APP_NOAVAILABLEUPDATE)
        return iFilter
    }

    private fun getUpdateavailableLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.APP_AVAILABLE)
        return iFilter
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
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getUpgradeLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getUpdateavailableLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getCheckUpdateLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mLocalBroadcastReceiver, getNoUpdateLocalIntentFilter())
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
