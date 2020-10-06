package it.manzolo.job.service.bluewatcher.activity

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
import it.manzolo.job.service.bluewatcher.R
import it.manzolo.job.service.bluewatcher.updater.UpdateApp
import it.manzolo.job.service.bluewatcher.utils.*
import it.manzolo.job.service.enums.BluetoothEvents
import it.manzolo.job.service.enums.DatabaseEvents
import it.manzolo.job.service.enums.WebserverEvents
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val debug = preferences.getBoolean("debug", false)
            val now = DateUtils.now()
            when (intent?.action) {
                BluetoothEvents.ERROR -> {
                    context.run { imageView.setImageResource(android.R.drawable.presence_busy) }
                    context.run { editText.append(now + " " + intent.getStringExtra("message") + "\n") }
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.ERROR -> {
                    context.run { imageView.setImageResource(android.R.drawable.presence_busy) }
                    context.run { editText.append(now + " " + intent.getStringExtra("message") + "\n") }
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.INFO -> {
                    context.run { editText.append(now + " " + intent.getStringExtra("message") + "\n") }
                    if (debug) {
                        Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }
                }
                BluetoothEvents.DATA_RETRIEVED -> {
                    context.run { imageView.setImageResource(android.R.drawable.presence_online) }
                    context.run { editText.append(now + " " + intent.getStringExtra("message") + "\n") }

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
                    context.run { imageView.setImageResource(android.R.drawable.presence_online) }
                    context.run { editText.append(now + " " + intent.getStringExtra("message") + "\n") }
                    // You can also include some extra data.
                    if (debug) {
                        Toast.makeText(context, "Data sent " + intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                    }

                    //context.run { editText.append(intent.getStringExtra("message") + "\n") }
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
                    if (debug) {
                        Toast.makeText(context, "Update available at " + updateUrl, Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.APP_CHECK_UPDATE -> {
                    if (debug) {
                        Toast.makeText(context, "Check for app update", Toast.LENGTH_LONG).show()
                    }
                }
                WebserverEvents.APP_NO_AVAILABLE_UPDATE -> {
                    Toast.makeText(context, "No available update", Toast.LENGTH_SHORT).show()
                }
                DatabaseEvents.ERROR -> {
                    Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
                }
            }
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
                val intent1 = Intent(this, SettingsActivity::class.java)
                this.startActivity(intent1)
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
            R.id.action_updateapp -> {
                val session = Session(applicationContext)
                val file = File(applicationContext.cacheDir, "app.apk")
                val photoURI = applicationContext.let { it1 -> FileProvider.getUriForFile(it1, applicationContext.packageName + ".provider", file) }

                val updateapp = UpdateApp()
                updateapp.setContext(applicationContext)
                //Log.i("manzolo", file.toString())
                val outputDir = photoURI.toString()
                //Log.e(TAG, session.updateApkUrl)
                //Log.e(TAG, outputDir)
                if (session.updateApkUrl.length == 0) {
                    val githubup = GithubUpdater()
                    githubup.checkUpdate(applicationContext)
                } else {
                    updateapp.execute(session.updateApkUrl, outputDir)
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //GPS
    override fun onCreate(savedInstanceState: Bundle?) {
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

        /*DEBUG SEND DATA TO WEBSERVER
        val dbVoltwatcherAdapter = DbVoltwatcherAdapter(applicationContext)
        dbVoltwatcherAdapter.open()
        dbVoltwatcherAdapter.createRow("44:44:09:04:01:CC", "9.99", "30", "2019-10-09 00:00:00", "1.1", "2.2", "50")
        dbVoltwatcherAdapter.close()
        */
        Thread.setDefaultUncaughtExceptionHandler(UnCaughtExceptionHandler(this))
    }

    // Method to show an alert dialog with yes, no and cancel button
    private fun showBackupDialog() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog


        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this)

        // Set a title for alert dialog
        builder.setTitle("Are you sure")

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
        builder.setTitle("Are you sure")

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
