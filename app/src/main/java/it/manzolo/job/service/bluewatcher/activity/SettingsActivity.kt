package it.manzolo.job.service.bluewatcher.activity

import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import it.manzolo.job.service.bluewatcher.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)


            val webserviceUrlPreference: EditTextPreference? = findPreference("webserviceurl")

            webserviceUrlPreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            }

            val passwordPreference: EditTextPreference? = findPreference("webservicepassword")

            passwordPreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            val webserviceEverySecondsPreference: EditTextPreference? = findPreference("webservice_service_every_seconds")

            webserviceEverySecondsPreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            val locationEverySecondsPreference: EditTextPreference? = findPreference("location_service_every_seconds")

            locationEverySecondsPreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            val updateEverySecondsPreference: EditTextPreference? = findPreference("update_service_every_seconds")

            updateEverySecondsPreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

            val restartAppEverySecondsPreference: EditTextPreference? = findPreference("restart_app_service_every_seconds")

            restartAppEverySecondsPreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }


        }
    }


}