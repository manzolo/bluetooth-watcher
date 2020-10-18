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
import it.manzolo.bluetoothwatcher.log.Bluelog
import it.manzolo.bluetoothwatcher.log.MyRecyclerViewAdapter
import it.manzolo.bluetoothwatcher.network.GithubUpdater
import it.manzolo.bluetoothwatcher.service.BluetoothService
import it.manzolo.bluetoothwatcher.service.RestartAppService
import it.manzolo.bluetoothwatcher.service.WebserviceSendService
import it.manzolo.bluetoothwatcher.updater.Apk
import it.manzolo.bluetoothwatcher.updater.UpdateApp
import it.manzolo.bluetoothwatcher.utils.Date
import it.manzolo.bluetoothwatcher.utils.Session
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    private val mLogs: ArrayList<Bluelog> = ArrayList()
    private var mRecyclerView: RecyclerView? = null
    val myRecyclerViewAdapter = MyRecyclerViewAdapter(mLogs)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            super.onCreate(savedInstanceState)
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

            //LocalBroadcast
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getConnectionOkLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getConnectionErrorLocalIntentFilter())

            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserviceDataSentLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserviceErrorDataSentLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserviceInfoDataSentLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserviceDebugLocalIntentFilter())

            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getUpgradeLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getUpdateavailableLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getCheckUpdateLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getNoUpdateLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getUpdateErrorLocalIntentFilter())

            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getDatabaseErrorIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getLogMessagesIntentFilter())

            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getLocationChangedIntentFilter())

            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getMainServiceIntentFilter())

            Thread.setDefaultUncaughtExceptionHandler(UnCaughtExceptionHandler(this))

            //EventViewer
            //Reference of RecyclerView
            mRecyclerView = findViewById(R.id.myRecyclerView)
            //Linear Layout Manager
            val linearLayoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
            //Set Layout Manager to RecyclerView
            mRecyclerView!!.layoutManager = linearLayoutManager
            //Set adapter to RecyclerView
            mRecyclerView!!.adapter = myRecyclerViewAdapter

            mLogs.add(0, Bluelog(Date.now(), "System ready", Bluelog.logEvents.INFO))

            //Service enabled by default
            PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putBoolean("enabled", true).apply()

        }

    }

    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debugApp", false)
            val now = Date.now()
            val dbLog = DatabaseLog(applicationContext)
            dbLog.open()

            when (intent?.action) {
                MainEvents.BROADCAST -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), intent.getStringExtra("type")))
                    dbLog.createRow(now, intent.getStringExtra("message"), intent.getStringExtra("type"))
                }
                Bluelog.logEvents.BROADCAST -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), intent.getStringExtra("type")))
                    dbLog.createRow(now, intent.getStringExtra("message"), intent.getStringExtra("type"))
                }
                BluetoothEvents.ERROR -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR))
                    dbLog.createRow(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR)
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserviceEvents.ERROR -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR))
                    dbLog.createRow(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR)
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserviceEvents.INFO -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO))
                    dbLog.createRow(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO)
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                BluetoothEvents.DATA_RETRIEVED -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO))
                    dbLog.createRow(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO)

                    val device = intent.getStringExtra("device")
                    val data = intent.getStringExtra("data")
                    val volt = intent.getStringExtra("volt")
                    val temp = intent.getStringExtra("tempC")

                    try {
                        val bp = getDeviceBatteryPercentage(applicationContext)
                        val session = Session(context)

                        val db = DatabaseVoltwatcher(applicationContext)
                        db.open()
                        db.createRow(device, volt, temp, data, session.longitude, session.latitude, bp.toString())
                        db.close()
                    } catch (e: Exception) {
                        //Log.e(TAG, e.message)
                        val dbIntent = Intent(DatabaseEvents.ERROR)
                        // You can also include some extra data.
                        dbIntent.putExtra("message", e.message)
                        dbLog.createRow(now, e.message, Bluelog.logEvents.ERROR)
                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(dbIntent)
                    }

                }
                WebserviceEvents.DATA_SENT -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO))
                    dbLog.createRow(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO)
                    // You can also include some extra data.
                    if (debug) {
                        Toast.makeText(context, "Data sent " + intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserviceEvents.DEBUG -> {
                    Log.d(TAG, intent.getStringExtra("message"))
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                        dbLog.createRow(now, intent.getStringExtra("message"), Bluelog.logEvents.DEBUG)
                    }
                }

                WebserviceEvents.APP_UPDATE -> {

                    //val filepath = intent.getStringExtra("file")
                    val session = Session(applicationContext)
                    val file = File(applicationContext.cacheDir, "app.apk")
                    val apkFile = applicationContext.applicationContext.let { it1 -> FileProvider.getUriForFile(it1, applicationContext.applicationContext.packageName + ".provider", file) }
                    if (file.exists()) {
                        if (debug) {
                            Toast.makeText(context, "Install update", Toast.LENGTH_LONG).show()
                        }
                        val apk = Apk()
                        apk.installApk(applicationContext, apkFile)
                        dbLog.createRow(now, "App Updated", Bluelog.logEvents.INFO)
                        //install(applicationContext,applicationContext.packageName,file)
                        //val fileUpdate = File(applicationContext.cacheDir, "app.ava")
                        //fileUpdate.delete()
                        session.isAvailableUpdate = false
                        session.updateApkUrl = ""
                    } else {
                        dbLog.createRow(now, "Update file not found", Bluelog.logEvents.WARNING)
                        if (debug) {
                            Toast.makeText(context, "Update file not found", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                WebserviceEvents.APP_AVAILABLE -> {
                    val session = Session(context)
                    val updateUrl = intent.getStringExtra("message")
                    session.updateApkUrl = updateUrl
                    mLogs.add(0, Bluelog(now, "Update available at " + updateUrl, Bluelog.logEvents.WARNING))
                    dbLog.createRow(now, "Update available at " + updateUrl, Bluelog.logEvents.WARNING)
                    if (debug) {
                        Toast.makeText(context, "Update available at " + updateUrl, Toast.LENGTH_LONG).show()
                    }
                }
                WebserviceEvents.APP_CHECK_UPDATE -> {
                    mLogs.add(0, Bluelog(now, "Check for app update", Bluelog.logEvents.INFO))
                    dbLog.createRow(now, "Check for app update", Bluelog.logEvents.INFO)
                    if (debug) {
                        Toast.makeText(context, "Check for app update", Toast.LENGTH_LONG).show()
                    }
                }
                WebserviceEvents.APP_UPDATE_ERROR -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), intent.getStringExtra("type")))
                    dbLog.createRow(now, intent.getStringExtra("message"), intent.getStringExtra("type"))
                }
                WebserviceEvents.APP_NO_AVAILABLE_UPDATE -> {
                    mLogs.add(0, Bluelog(now, "No available update", Bluelog.logEvents.INFO))
                    dbLog.createRow(now, "No available update", Bluelog.logEvents.INFO)
                    Toast.makeText(context, "No available update", Toast.LENGTH_SHORT).show()
                }
                LocationEvents.LOCATION_CHANGED -> {
                    val locationLog = "Obtain longitude:" + intent.getStringExtra("longitude") + " latitude:" + intent.getStringExtra("latitude")
                    mLogs.add(0, Bluelog(now, locationLog, Bluelog.logEvents.INFO))
                    dbLog.createRow(now, locationLog, Bluelog.logEvents.INFO)
                    if (debug) {
                        Toast.makeText(context, locationLog, Toast.LENGTH_LONG).show()
                    }
                }
                DatabaseEvents.ERROR -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR))
                    //dbLog.createRow(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR)
                    Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                }
            }
            dbLog.close()
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
            R.id.action_updateapp -> {
                val session = Session(applicationContext)
                val file = File(applicationContext.cacheDir, "app.apk")
                val photoURI = applicationContext.let { it1 -> FileProvider.getUriForFile(it1, applicationContext.packageName + ".provider", file) }

                val updateApp = UpdateApp(applicationContext)
                //Log.i("manzolo", file.toString())
                val outputDir = photoURI.toString()
                //Log.e(TAG, session.updateApkUrl)
                //Log.e(TAG, outputDir)
                if (session.updateApkUrl?.isEmpty()!!) {
                    val githubUpdater = GithubUpdater()
                    githubUpdater.checkUpdate(applicationContext)
                } else {
                    updateApp.execute(session.updateApkUrl, outputDir)
                }
                return true
            }
            R.id.action_clear_log -> {
                mLogs.clear()
                val db = DatabaseLog(applicationContext)
                db.open()
                db.clean()
                db.close()
                myRecyclerViewAdapter.notifyDataSetChanged()
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

    private fun getLogMessagesIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(Bluelog.logEvents.BROADCAST)
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

    private fun getUpdateavailableLocalIntentFilter(): IntentFilter {
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

    private fun getWebserviceDebugLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserviceEvents.DEBUG)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


}
