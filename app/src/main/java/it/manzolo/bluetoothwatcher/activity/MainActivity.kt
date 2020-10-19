package it.manzolo.bluetoothwatcher.activity

import android.content.*
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.manzolo.bluetoothwatcher.R
import it.manzolo.bluetoothwatcher.database.DatabaseHelper
import it.manzolo.bluetoothwatcher.database.DatabaseLog
import it.manzolo.bluetoothwatcher.database.DatabaseVoltwatcher
import it.manzolo.bluetoothwatcher.device.getDeviceBatteryPercentage
import it.manzolo.bluetoothwatcher.enums.*
import it.manzolo.bluetoothwatcher.error.UnCaughtExceptionHandler
import it.manzolo.bluetoothwatcher.log.BluetoothWatcherLog
import it.manzolo.bluetoothwatcher.log.MyRecyclerViewAdapter
import it.manzolo.bluetoothwatcher.network.GithubUpdater
import it.manzolo.bluetoothwatcher.service.BluetoothService
import it.manzolo.bluetoothwatcher.service.RestartAppService
import it.manzolo.bluetoothwatcher.service.WebserviceSendService
import it.manzolo.bluetoothwatcher.updater.Apk
import it.manzolo.bluetoothwatcher.updater.AppReceiveSettings
import it.manzolo.bluetoothwatcher.updater.UpdateApp
import it.manzolo.bluetoothwatcher.utils.Date
import it.manzolo.bluetoothwatcher.utils.Session
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    private val mLogs: ArrayList<BluetoothWatcherLog> = ArrayList()
    private var mRecyclerView: RecyclerView? = null
    val myRecyclerViewAdapter = MyRecyclerViewAdapter(mLogs)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            super.onCreate(savedInstanceState)

            registerLocalBroadcast()

            Thread.setDefaultUncaughtExceptionHandler(UnCaughtExceptionHandler(this))

            setContentView(R.layout.activity_main)
            setSupportActionBar(toolbar)

            //Ask user for permission
            val permissions = arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, 0)



            //EventViewer
            //Reference of RecyclerView
            mRecyclerView = findViewById(R.id.myRecyclerView)
            //Linear Layout Manager
            val linearLayoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
            //Set Layout Manager to RecyclerView
            mRecyclerView!!.layoutManager = linearLayoutManager
            //Set adapter to RecyclerView
            mRecyclerView!!.adapter = myRecyclerViewAdapter

            mLogs.add(0, BluetoothWatcherLog(Date.now(), "System ready", MainEvents.INFO))

            //Service enabled by default
            PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putBoolean("enabled", true).apply()

        }

    }

    private fun captureLog(message: String, type: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val debug = preferences.getBoolean("debugApp", false)
        val dbLog = DatabaseLog(applicationContext)
        dbLog.open()
        dbLog.createRow(Date.now(), message, type)
        mLogs.add(0, BluetoothWatcherLog(Date.now(), message, type))
        dbLog.close()
        if (debug) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MainEvents.BROADCAST -> {
                    captureLog(intent.getStringExtra("message")!!, intent.getStringExtra("type")!!)
                }
                MainEvents.INFO -> {
                    captureLog(intent.getStringExtra("message")!!, MainEvents.INFO)
                }
                MainEvents.ERROR -> {
                    captureLog(intent.getStringExtra("message")!!, MainEvents.ERROR)
                }
                BluetoothEvents.ERROR -> {
                    captureLog(intent.getStringExtra("message")!!, MainEvents.ERROR)
                }
                WebserviceEvents.ERROR -> {
                    captureLog(intent.getStringExtra("message")!!, MainEvents.ERROR)
                }
                WebserviceEvents.INFO -> {
                    captureLog(intent.getStringExtra("message")!!, MainEvents.INFO)
                }
                BluetoothEvents.DATA_RETRIEVED -> {
                    captureLog(intent.getStringExtra("message")!!, MainEvents.INFO)

                    val device = intent.getStringExtra("device")!!
                    val data = intent.getStringExtra("data")!!
                    val volt = intent.getStringExtra("volt")!!
                    val temp = intent.getStringExtra("tempC")!!

                    try {
                        val session = Session(applicationContext)
                        val bp = getDeviceBatteryPercentage(applicationContext)

                        val db = DatabaseVoltwatcher(applicationContext)
                        db.open()
                        db.createRow(device, volt, temp, data, session.longitude!!, session.latitude!!, bp.toString())
                        db.close()
                    } catch (e: Exception) {
                        //Log.e(TAG, e.message)
                        val dbIntent = Intent(DatabaseEvents.ERROR)
                        // You can also include some extra data.
                        dbIntent.putExtra("message", e.message)
                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(dbIntent)
                    }

                }
                WebserviceEvents.DATA_SENT -> {
                    captureLog("Data sent " + intent.getStringExtra("message"), MainEvents.INFO)
                }
                WebserviceEvents.APP_UPDATE -> {

                    //val filepath = intent.getStringExtra("file")
                    val session = Session(applicationContext)
                    val file = File(applicationContext.cacheDir, "app.apk")
                    val apkFile = applicationContext.applicationContext.let { it1 -> FileProvider.getUriForFile(it1, applicationContext.applicationContext.packageName + ".provider", file) }
                    if (file.exists()) {
                        captureLog("Install update", MainEvents.WARNING)
                        val apk = Apk()
                        apk.installApk(applicationContext, apkFile)
                        captureLog("Start app update", MainEvents.INFO)
                        session.isAvailableUpdate = false
                        session.updateApkUrl = ""
                    } else {
                        captureLog("Update file not found", MainEvents.WARNING)
                    }
                }
                WebserviceEvents.APP_AVAILABLE -> {
                    val session = Session(context)
                    val updateUrl = intent.getStringExtra("message")!!
                    session.updateApkUrl = updateUrl
                    captureLog("Update available at " + updateUrl, MainEvents.WARNING)
                }
                WebserviceEvents.APP_CHECK_UPDATE -> {
                    captureLog("Check for app update", MainEvents.INFO)
                }
                WebserviceEvents.APP_UPDATE_ERROR -> {
                    captureLog(intent.getStringExtra("message")!!, intent.getStringExtra("type")!!)
                }
                WebserviceEvents.APP_NO_AVAILABLE_UPDATE -> {
                    captureLog("No available update", MainEvents.INFO)
                }
                LocationEvents.LOCATION_CHANGED -> {
                    captureLog("Obtain longitude:" + intent.getStringExtra("longitude")!! + " latitude:" + intent.getStringExtra("latitude")!!, MainEvents.INFO)
                }
                DatabaseEvents.ERROR -> {
                    captureLog(intent.getStringExtra("message")!!, MainEvents.ERROR)
                }
                MainEvents.DEBUG -> {
                    //saveLog(intent.getStringExtra("message"), MainEvents.DEBUG)
                    if (PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("debugApp", false)) {
                        Toast.makeText(applicationContext, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
            }
            myRecyclerViewAdapter.notifyDataSetChanged()

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intentSettings = Intent(this, SettingsActivity::class.java)
                this.startActivity(intentSettings)
                return true
            }
            R.id.action_trigger_app_update_settings_service -> {
                val webserviceUrl = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("webserviceUrl", "").toString()
                if (webserviceUrl.isEmpty()) {
                    val intent = Intent(WebserviceEvents.ERROR)
                    // You can also include some extra data.
                    intent.putExtra("message", "No webservice url in settings")
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                    return false
                }
                val webserviceUsername = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("webserviceUsername", "username").toString()
                val webservicePassword = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("webservicePassword", "password").toString()
                val autoSettingsUpdate = PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean("autoSettingsUpdate", true)
                if (autoSettingsUpdate) {
                    val appSettings = AppReceiveSettings(this.applicationContext, webserviceUrl, webserviceUsername, webservicePassword)
                    appSettings.receive()
                }
                return true
            }
            R.id.action_trigger_bluetooth_service -> {
                val serviceIntent = Intent(this, BluetoothService::class.java)
                this.startService(serviceIntent)
                return true
            }
            R.id.action_trigger_webservice_send_service -> {
                val serviceIntent = Intent(this, WebserviceSendService::class.java)
                this.startService(serviceIntent)
                return true
            }
            R.id.action_dbbackup -> {
                showBackupDialog()
                return true
            }
            R.id.action_dbrestore -> {
                showRestoreDialog()
                return true
            }
            R.id.action_trigger_restart_service -> {
                val serviceIntent = Intent(this, RestartAppService::class.java)
                this.startService(serviceIntent)
                return true
            }
            R.id.action_updateApp -> {
                val session = Session(applicationContext)
                val file = File(applicationContext.cacheDir, "app.apk")
                val photoURI = applicationContext.let { it1 -> FileProvider.getUriForFile(it1, applicationContext.packageName + ".provider", file) }

                val updateApp = UpdateApp(applicationContext)
                val outputDir = photoURI.toString()
                if (!session.updateApkUrl?.isEmpty()!!) {
                    updateApp.execute(session.updateApkUrl, outputDir)
                } else {
                    Toast.makeText(applicationContext, "No available updates", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            R.id.action_checkForUpdates -> {
                val githubUpdater = GithubUpdater()
                githubUpdater.checkUpdate(applicationContext)
                return true
            }
            R.id.action_clear_log -> {
                mLogs.clear()
                val db = DatabaseLog(applicationContext)
                db.open()
                db.clear()
                db.close()
                myRecyclerViewAdapter.notifyDataSetChanged()
                Toast.makeText(applicationContext, "Done", Toast.LENGTH_SHORT).show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Method to show an alert dialog with yes, no and cancel button
    private fun showBackupDialog() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog


        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this)

        // Set a title for alert dialog
        builder.setTitle("Are you sure?")

        // On click listener for dialog buttons
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val db = DatabaseHelper(applicationContext)
                    db.backup()
                }
            }
        }

        // Set the alert dialog positive/yes button
        builder.setPositiveButton("YES", dialogClickListener)

        // Set the alert dialog negative/no button
        builder.setNegativeButton("NO", dialogClickListener)

        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    // Method to show an alert dialog with yes, no and cancel button
    private fun showRestoreDialog() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog


        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this)

        // Set a title for alert dialog
        builder.setTitle("Are you sure?")

        // On click listener for dialog buttons
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val db = DatabaseHelper(applicationContext)
                    db.restore()
                }
            }
        }

        // Set the alert dialog positive/yes button
        builder.setPositiveButton("YES", dialogClickListener)

        // Set the alert dialog negative/no button
        builder.setNegativeButton("NO", dialogClickListener)

        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    private fun getUpgradeLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.APP_UPDATE)
        return iFilter
    }

    private fun getUpdateErrorLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.APP_UPDATE_ERROR)
        return iFilter
    }

    private fun getCheckUpdateLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.APP_CHECK_UPDATE)
        return iFilter
    }

    private fun getNoUpdateLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.APP_NO_AVAILABLE_UPDATE)
        return iFilter
    }

    private fun getUpdateAvailableLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.APP_AVAILABLE)
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

    private fun getWebserviceErrorDataSentLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.ERROR)
        return iFilter
    }

    private fun getWebserviceInfoDataSentLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.INFO)
        return iFilter
    }

    private fun getWebserviceDataSentLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.DATA_SENT)
        return iFilter
    }

    private fun getDebugLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(MainEvents.DEBUG)
        return iFilter
    }

    private fun getDatabaseErrorIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(DatabaseEvents.ERROR)
        return iFilter
    }

    private fun getLocationChangedIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(LocationEvents.LOCATION_CHANGED)
        return iFilter
    }

    private fun getMainServiceIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(MainEvents.BROADCAST)
        return iFilter
    }

    private fun getMainServiceInfoIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(MainEvents.INFO)
        return iFilter
    }

    private fun getMainServiceErrorIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(MainEvents.ERROR)
        return iFilter
    }

    private fun registerLocalBroadcast() {
        //LocalBroadcast
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getConnectionOkLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getConnectionErrorLocalIntentFilter())

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserviceDataSentLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserviceErrorDataSentLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserviceInfoDataSentLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getDebugLocalIntentFilter())

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getUpgradeLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getUpdateAvailableLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getCheckUpdateLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getNoUpdateLocalIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getUpdateErrorLocalIntentFilter())

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getDatabaseErrorIntentFilter())

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getLocationChangedIntentFilter())

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getMainServiceIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getMainServiceInfoIntentFilter())
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getMainServiceErrorIntentFilter())

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


}
