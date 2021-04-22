package com.example.camerax

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_fragment_container, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /*class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_eyesee, rootKey)
        }
    }*/

    override fun onOptionsItemSelected(item: MenuItem) : Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
        }

        return super.onOptionsItemSelected(item)
    }
}