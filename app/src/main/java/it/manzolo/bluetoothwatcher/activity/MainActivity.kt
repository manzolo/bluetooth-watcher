package it.manzolo.bluetoothwatcher.activity

import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
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
import it.manzolo.bluetoothwatcher.service.LocationService
import it.manzolo.bluetoothwatcher.service.RestartAppService
import it.manzolo.bluetoothwatcher.service.WebserviceSendService
import it.manzolo.bluetoothwatcher.service.WebserviceSendService.Companion.webServiceParameter
import it.manzolo.bluetoothwatcher.updater.Apk
import it.manzolo.bluetoothwatcher.updater.AppReceiveSettings
import it.manzolo.bluetoothwatcher.utils.Date
import it.manzolo.bluetoothwatcher.utils.Session
import it.manzolo.bluetoothwatcher.webservice.WebServiceParameters
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    private val logList: ArrayList<BluetoothWatcherLog> = ArrayList()
    private var recyclerView: RecyclerView? = null
    val logViewAdapter = MyRecyclerViewAdapter(logList)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            super.onCreate(savedInstanceState)

            registerLocalBroadcast()

            Thread.setDefaultUncaughtExceptionHandler(UnCaughtExceptionHandler(this))

            setContentView(R.layout.activity_main)
            setSupportActionBar(findViewById(R.id.toolbar))

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
            recyclerView = findViewById(R.id.myRecyclerView)
            //Linear Layout Manager
            val linearLayoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
            //Set Layout Manager to RecyclerView
            recyclerView!!.layoutManager = linearLayoutManager
            //Set adapter to RecyclerView
            recyclerView!!.adapter = logViewAdapter

            logList.add(0, BluetoothWatcherLog(Date.now(), "System ready", MainEvents.INFO))

            //Service enabled by default
            PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putBoolean("enabled", true).apply()

        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
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
                        applicationContext.sendBroadcast(dbIntent)
                    }
                }
                WebserviceEvents.DATA_SENT -> {
                    captureLog("Data sent " + intent.getStringExtra("message"), MainEvents.INFO)
                }
                WebserviceEvents.APP_UPDATE -> {
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
                    captureLog("Update available at $updateUrl", MainEvents.WARNING)
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
                    return
                }
            }
            logViewAdapter.notifyItemInserted(0)

        }
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (menuItem.itemId) {
            R.id.action_settings -> {
                val intentSettings = Intent(this, SettingsActivity::class.java)
                this.startActivity(intentSettings)
                return true
            }
            R.id.action_trigger_app_update_settings_service -> {
                val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                if (pref.getString("webserviceUrl", "").toString().isEmpty()) {
                    val intent = Intent(WebserviceEvents.ERROR)
                    // You can also include some extra data.
                    intent.putExtra("message", "No webservice url in settings")
                    applicationContext.sendBroadcast(intent)
                    return false
                }
                webServiceParameter = WebServiceParameters(
                    pref.getString("webserviceUrl", "").toString(),
                    pref.getString("webserviceUsername", "username").toString(),
                    pref.getString("webservicePassword", "password").toString()
                )
                val autoSettingsUpdate =
                    PreferenceManager.getDefaultSharedPreferences(applicationContext)
                        .getBoolean("autoSettingsUpdate", true)
                if (autoSettingsUpdate) {
                    val appSettings =
                        AppReceiveSettings(this.applicationContext, webServiceParameter)
                    appSettings.execute()
                }
                return true
            }
            R.id.action_trigger_bluetooth_service -> {
                val serviceIntent = Intent(this, BluetoothService::class.java)
                this.startService(serviceIntent)
                return true
            }
            R.id.action_trigger_location_service -> {
                val serviceIntent = Intent(this, LocationService::class.java)
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

                val outputDir = photoURI.toString()
                if (!session.updateApkUrl?.isEmpty()!!) {
                    this.updateApp(session.updateApkUrl!!, outputDir)
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
                val size: Int = logList.size
                logList.clear()
                val db = DatabaseLog(applicationContext)
                db.open()
                db.clear()
                db.close()
                logViewAdapter.notifyItemRangeRemoved(0, size)
                Toast.makeText(applicationContext, "Done", Toast.LENGTH_SHORT).show()
                return true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

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
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getConnectionOkLocalIntentFilter()
        )
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getConnectionErrorLocalIntentFilter()
        )

        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getWebserviceDataSentLocalIntentFilter()
        )
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getWebserviceErrorDataSentLocalIntentFilter()
        )
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getWebserviceInfoDataSentLocalIntentFilter()
        )
        applicationContext.registerReceiver(localBroadcastReceiver, getDebugLocalIntentFilter())

        applicationContext.registerReceiver(localBroadcastReceiver, getUpgradeLocalIntentFilter())
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getUpdateAvailableLocalIntentFilter()
        )
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getCheckUpdateLocalIntentFilter()
        )
        applicationContext.registerReceiver(localBroadcastReceiver, getNoUpdateLocalIntentFilter())
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getUpdateErrorLocalIntentFilter()
        )

        applicationContext.registerReceiver(localBroadcastReceiver, getDatabaseErrorIntentFilter())

        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getLocationChangedIntentFilter()
        )

        applicationContext.registerReceiver(localBroadcastReceiver, getMainServiceIntentFilter())
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getMainServiceInfoIntentFilter()
        )
        applicationContext.registerReceiver(
            localBroadcastReceiver,
            getMainServiceErrorIntentFilter()
        )

    }

    private fun captureLog(message: String, type: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val debug = preferences.getBoolean("debugApp", false)
        val dbLog = DatabaseLog(applicationContext)
        dbLog.open()
        dbLog.createRow(Date.now(), message, type)
        logList.add(0, BluetoothWatcherLog(Date.now(), message, type))
        dbLog.close()
        if (debug) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateApp(urlUpdate: String, filepath: String) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        //val handler = Handler(Looper.getMainLooper())
        executor.execute {
            try {
                val url = URL(urlUpdate)
                Log.d("url", url.toString())
                Log.d("filepath", filepath)
                val c = url.openConnection() as HttpURLConnection
                c.requestMethod = "GET"
                c.connect()
                val file = File(applicationContext.cacheDir, "app.apk")
                if (file.exists()) {
                    if (!file.delete()) {
                        val intent = Intent(WebserviceEvents.APP_UPDATE_ERROR)
                        // You can also include some extra data.
                        intent.putExtra("message", "Unable to delete " + file.absolutePath)
                        intent.putExtra("type", MainEvents.ERROR)
                        applicationContext.sendBroadcast(intent)
                        return@execute
                    }
                }
                val fos = FileOutputStream(file)
                val `is` = c.inputStream
                val buffer = ByteArray(1024)
                var len1: Int
                while (`is`.read(buffer).also { len1 = it } != -1) {
                    fos.write(buffer, 0, len1)
                }
                fos.close()
                `is`.close()
                Log.i("ManzoloUpdate", "Download complete")
                val intent = Intent(WebserviceEvents.APP_UPDATE)
                // You can also include some extra data.
                intent.putExtra("message", "Download complete")
                intent.putExtra("file", file.absolutePath)
                applicationContext.sendBroadcast(intent)
            } catch (e: Exception) {
                //e.printStackTrace();
                Log.e("UpdateAPP", "Update error! " + e.message)
                val intent = Intent(WebserviceEvents.APP_UPDATE_ERROR)
                // You can also include some extra data.
                intent.putExtra("message", e.message)
                intent.putExtra("type", MainEvents.ERROR)
                applicationContext.sendBroadcast(intent)
            }
            return@execute
        }
    }
}
