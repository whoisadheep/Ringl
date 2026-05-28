package com.ringl.missedcallhook.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ringl.missedcallhook.R
import com.ringl.missedcallhook.databinding.FragmentDashboardBinding
import com.ringl.missedcallhook.service.MissedCallService
import com.ringl.missedcallhook.util.PrefsManager

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsManager: PrefsManager

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateStats()
            refreshHandler.postDelayed(this, 5000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsManager = PrefsManager(requireContext())
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        updateStats()
        refreshHandler.postDelayed(refreshRunnable, 5000)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun setupUI() {
        // Webhook fields
        binding.etWebhookUrl.setText(prefsManager.getWebhookUrl())
        binding.etWebhookSecret.setText(prefsManager.getWebhookSecret())
        binding.etTenantId.setText(prefsManager.getTenantId())

        // Save button
        binding.btnSave.setOnClickListener {
            val url = binding.etWebhookUrl.text.toString().trim()
            val secret = binding.etWebhookSecret.text.toString().trim()
            val tenantId = binding.etTenantId.text.toString().trim()

            if (url.isEmpty()) {
                Toast.makeText(context, "⚠️ Webhook URL cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefsManager.setWebhookUrl(url)
            prefsManager.setWebhookSecret(secret)
            prefsManager.setTenantId(tenantId)
            Toast.makeText(context, "✅ Settings saved!", Toast.LENGTH_SHORT).show()
        }

        // Toggle monitoring
        binding.switchMonitoring.isChecked = prefsManager.isMonitoringEnabled()
        binding.switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setMonitoringEnabled(isChecked)
            if (isChecked) {
                startMonitoring()
            } else {
                stopMonitoring()
            }
            updateStatusUI(isChecked)
        }
        
        updateStatusUI(prefsManager.isMonitoringEnabled())
    }

    private fun startMonitoring() {
        val serviceIntent = Intent(activity, MissedCallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(serviceIntent)
        } else {
            activity?.startService(serviceIntent)
        }
    }

    private fun stopMonitoring() {
        val serviceIntent = Intent(activity, MissedCallService::class.java)
        activity?.stopService(serviceIntent)
    }

    private fun updateStatusUI(active: Boolean) {
        binding.tvStatus.text = if (active) "🟢 Monitoring Active" else "🔴 Monitoring Stopped"
        context?.let { ctx ->
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(ctx, if (active) R.color.status_active else R.color.status_inactive)
            )
        }
        updateStats()
    }

    private fun updateStats() {
        val totalSent = prefsManager.getTotalWebhooksSent()
        val lastCall = prefsManager.getLastMissedCallNumber()
        val lastTime = prefsManager.getLastMissedCallTime()

        binding.tvTotalSent.text = totalSent.toString()
        binding.tvLastCall.text = if (lastCall.isNotEmpty()) {
            "$lastCall\n$lastTime"
        } else {
            "No missed calls"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
