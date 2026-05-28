package com.ringl.missedcallhook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ringl.missedcallhook.databinding.ActivityMainBinding
import com.ringl.missedcallhook.ui.DashboardFragment
import com.ringl.missedcallhook.ui.LogsFragment
import com.ringl.missedcallhook.ui.AnalyticsFragment
import com.ringl.missedcallhook.ui.SettingsFragment
import com.ringl.missedcallhook.util.PrefsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PrefsManager

    private val requiredPermissions = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "❌ Permissions required for missed call detection", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = PrefsManager(this)
        
        // Check for Onboarding on First Run
        if (prefsManager.isFirstRun()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setupNavigation()
        checkPermissionsAndStart()
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_logs -> LogsFragment()
                R.id.nav_analytics -> AnalyticsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
            true
        }

        // Set initial fragment
        if (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, DashboardFragment())
                .commit()
        }
    }

    private fun checkPermissionsAndStart() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }
}
