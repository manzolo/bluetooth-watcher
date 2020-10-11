package it.manzolo.bluetoothwatcher.activity

import android.content.*
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.manzolo.bluetoothwatcher.BuildConfig
import it.manzolo.bluetoothwatcher.R
import it.manzolo.bluetoothwatcher.enums.BluetoothEvents
import it.manzolo.bluetoothwatcher.enums.DatabaseEvents
import it.manzolo.bluetoothwatcher.enums.WebserverEvents
import it.manzolo.bluetoothwatcher.service.BluetoothService
import it.manzolo.bluetoothwatcher.service.RebootService
import it.manzolo.bluetoothwatcher.service.WebserviceSendService
import it.manzolo.bluetoothwatcher.updater.UpdateApp
import it.manzolo.bluetoothwatcher.utils.*
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
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getConnectionOkLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getConnectionErrorLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserverDataSentLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserverErrorDataSentLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getWebserverInfoDataSentLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getUpgradeLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getUpdateavailableLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getCheckUpdateLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getNoUpdateLocalIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getDatabaseErrorIntentFilter())
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(localBroadcastReceiver, getLogMessagesIntentFilter())

            Thread.setDefaultUncaughtExceptionHandler(UnCaughtExceptionHandler(this))

            //Reference of RecyclerView
            mRecyclerView = findViewById(R.id.myRecyclerView)
            //Linear Layout Manager
            val linearLayoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
            //Set Layout Manager to RecyclerView
            mRecyclerView!!.layoutManager = linearLayoutManager
            //Set adapter to RecyclerView
            mRecyclerView!!.adapter = myRecyclerViewAdapter

            mLogs.add(0, Bluelog(DateUtils.now(), "Service started", Bluelog.logEvents.INFO))
            if (BuildConfig.DEBUG) {
                // do something for a debug build
                val dbVoltwatcherAdapter = DbVoltwatcherAdapter(applicationContext)
                dbVoltwatcherAdapter.open()
                //dbVoltwatcherAdapter.createRow("44:44:09:04:01:CC", "12.99", "30", DateUtils.now(), "1.1", "2.2", "0")
                dbVoltwatcherAdapter.createRow("44:44:09:04:01:CC", "24.00", "30", "2000-01-01 00:00:00", "1.1", "2.2", "0")
                dbVoltwatcherAdapter.close()
                mLogs.add(0, Bluelog(DateUtils.now(), "Debug data set", Bluelog.logEvents.WARNING))
                //mLogs.add(0, Bluelog(DateUtils.now(), "Debug data set long message string set long message string set long message string set long message string set long message string set long message string ", Bluelog.logEvents.WARNING))
            }
        }

    }

    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val now = DateUtils.now()

            when (intent?.action) {
                Bluelog.logEvents.BROADCAST -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), intent.getStringExtra("type")))
                }
                BluetoothEvents.ERROR -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR))
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.ERROR -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR))
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.INFO -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO))
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                BluetoothEvents.DATA_RETRIEVED -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO))

                    val device = intent.getStringExtra("device")
                    val data = intent.getStringExtra("data")
                    val volt = intent.getStringExtra("volt")
                    val temp = intent.getStringExtra("tempC")

                    try {
                        val bp = getBatteryPercentage(applicationContext)
                        val session = Session(context)

                        val dbVoltwatcherAdapter = DbVoltwatcherAdapter(applicationContext)
                        dbVoltwatcherAdapter.open()
                        dbVoltwatcherAdapter.createRow(device, volt, temp, data, session.getlongitude(), session.getlatitude(), bp.toString())
                        dbVoltwatcherAdapter.close()
                    } catch (e: Exception) {
                        //Log.e(TAG, e.message)
                        val dbIntent = Intent(DatabaseEvents.ERROR)
                        // You can also include some extra data.
                        dbIntent.putExtra("message", e.message)
                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(dbIntent)
                    }

                }
                WebserverEvents.DATA_SENT -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.INFO))
                    // You can also include some extra data.
                    if (debug) {
                        Toast.makeText(context, "Data sent " + intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.DEBUG -> {
                    Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                }

                WebserverEvents.APP_UPDATE -> {

                    //val filepath = intent.getStringExtra("file")
                    val file = File(applicationContext.cacheDir, "app.apk")
                    val apkfile = applicationContext.applicationContext.let { it1 -> FileProvider.getUriForFile(it1, applicationContext.applicationContext.packageName + ".provider", file) }
                    if (file.exists()) {
                        if (debug) {
                            Toast.makeText(context, "Install update", Toast.LENGTH_LONG).show()
                        }
                        val apk = Apk()
                        apk.installApk(applicationContext, apkfile)
                        //install(applicationContext,applicationContext.packageName,file)
                        val fileupdate = File(applicationContext.cacheDir, "app.ava")
                        fileupdate.delete()
                    } else {
                        if (debug) {
                            Toast.makeText(context, "Update file not found", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                WebserverEvents.APP_AVAILABLE -> {
                    val session = Session(context)
                    val updateUrl = intent.getStringExtra("message")
                    session.updateApkUrl = updateUrl
                    mLogs.add(0, Bluelog(now, "Update available at " + updateUrl, Bluelog.logEvents.WARNING))
                    if (debug) {
                        Toast.makeText(context, "Update available at " + updateUrl, Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.APP_CHECK_UPDATE -> {
                    mLogs.add(0, Bluelog(now, "Check for app update", Bluelog.logEvents.INFO))
                    if (debug) {
                        Toast.makeText(context, "Check for app update", Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.APP_NO_AVAILABLE_UPDATE -> {
                    mLogs.add(0, Bluelog(now, "No available update", Bluelog.logEvents.INFO))
                    Toast.makeText(context, "No available update", Toast.LENGTH_SHORT).show()
                }
                DatabaseEvents.ERROR -> {
                    mLogs.add(0, Bluelog(now, intent.getStringExtra("message"), Bluelog.logEvents.ERROR))
                    Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                }
            }
            myRecyclerViewAdapter.notifyDataSetChanged()

        }
    }

    private fun getUpgradeLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.APP_UPDATE)
        return iFilter
    }

    private fun getLogMessagesIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(Bluelog.logEvents.BROADCAST)
        return iFilter
    }

    private fun getCheckUpdateLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.APP_CHECK_UPDATE)
        return iFilter
    }

    private fun getNoUpdateLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.APP_NO_AVAILABLE_UPDATE)
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

    private fun getWebserverInfoDataSentLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.INFO)
        return iFilter
    }

    private fun getWebserverDataSentLocalIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(WebserverEvents.DATA_SENT)
        return iFilter
    }

    private fun getDatabaseErrorIntentFilter(): IntentFilter {
        val iFilter = IntentFilter()
        iFilter.addAction(DatabaseEvents.ERROR)
        return iFilter
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
                val serviceIntent = Intent(this, RebootService::class.java)
                this.startService(serviceIntent)
                return true
            }
            R.id.action_updateapp -> {
                val session = Session(applicationContext)
                val file = File(applicationContext.cacheDir, "app.apk")
                val photoURI = applicationContext.let { it1 -> FileProvider.getUriForFile(it1, applicationContext.packageName + ".provider", file) }

                val updateApp = UpdateApp()
                updateApp.setContext(applicationContext)
                //Log.i("manzolo", file.toString())
                val outputDir = photoURI.toString()
                //Log.e(TAG, session.updateApkUrl)
                //Log.e(TAG, outputDir)
                if (session.updateApkUrl.isEmpty()) {
                    val githubUpdater = GithubUpdater()
                    githubUpdater.checkUpdate(applicationContext)
                } else {
                    updateApp.execute(session.updateApkUrl, outputDir)
                }
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

    /*fun install(context: Context, packageName: String, apkPath: File) {

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
        val fis = apkPath.inputStream()
        fis.copyTo(out)
        session.fsync(out)
        out.close()

        // The app gets killed after installation session commit
        session.commit(PendingIntent.getBroadcast(context, sessionId,
                Intent("android.intent.action.MAIN"), 0).intentSender)
    }*/
}
