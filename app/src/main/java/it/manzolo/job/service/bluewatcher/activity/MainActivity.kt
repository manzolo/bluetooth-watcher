package it.manzolo.job.service.bluewatcher.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import it.manzolo.job.service.bluewatcher.R
import it.manzolo.job.service.bluewatcher.utils.*
import it.manzolo.job.service.enums.BluetoothEvents
import it.manzolo.job.service.enums.WebserverEvents
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    var session: Session? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationRequest: LocationRequest? = null
    private lateinit var locationCallback: LocationCallback
    private val mPowerManager: PowerManager? = null
    private var mWakeLock: PowerManager.WakeLock? = null

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


                    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

                    val url = preferences.getString("webserviceurl", "http://localhost:8080/api/sendvolt")

                    val device = intent.getStringExtra("device")
                    val data = intent.getStringExtra("data")
                    val volt = intent.getStringExtra("volt")
                    val temp = intent.getStringExtra("temp")

                    try {
                        val bp = getBatteryPercentage(applicationContext)
                        val session = Session(context)

                        val dbVoltwatcherAdapter = DbVoltwatcherAdapter(applicationContext)
                        dbVoltwatcherAdapter.open()
                        dbVoltwatcherAdapter.createRow(device, volt, temp, data, session.getlongitude(), session.getlatitude(), bp.toString())
                        dbVoltwatcherAdapter.close()

                        if (isNetworkAvailable(applicationContext)) {
                            Log.d(TAG, "Send data to webserver")
                            val sender = WebserverSender(context, url)
                            sender.send()
                            val intentWs = Intent(WebserverEvents.DATA_SENT)
                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentWs)
                            // You can also include some extra data.
                            if (debug) {
                                Toast.makeText(context, "Data sent", Toast.LENGTH_LONG).show()
                            }
                            Log.d(TAG, "Data sent")
                        } else {
                            if (debug) {
                                Toast.makeText(context, "No internet connection", Toast.LENGTH_LONG).show()
                            }
                            Log.e(TAG, "No internet connection")
                        }

                    } catch (e: Exception) {
                        //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, e.message)
                        val intent = Intent(WebserverEvents.ERROR)
                        // You can also include some extra data.
                        intent.putExtra("message", e.message)
                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                        //e.printStackTrace();
                    }

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


    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var activeNetworkInfo: NetworkInfo? = null
        activeNetworkInfo = cm.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
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

    //GPS
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

        //GPS
        mLocationRequest = LocationRequest()
        mLocationRequest?.interval = 120000 // two minute interval
        mLocationRequest?.fastestInterval = 120000
        mLocationRequest?.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        session = Session(applicationContext)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    session!!.setlongitude(location?.longitude.toString())
                    session!!.setlatitude(location?.latitude.toString())
                    val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    val debug = preferences.getBoolean("debug", false)
                    if (debug) {
                        Toast.makeText(applicationContext, location?.longitude.toString() + " " + location?.latitude.toString(), Toast.LENGTH_LONG).show()
                    }

                }
            }
        }

        //Get location
        obtieneLocalizacion()

    }

    private fun obtieneLocalizacion() {
        fusedLocationClient?.requestLocationUpdates(mLocationRequest, locationCallback, Looper.getMainLooper())
    }

}


