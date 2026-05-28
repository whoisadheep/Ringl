package com.ringl.missedcallhook.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ringl.missedcallhook.databinding.FragmentSettingsBinding
import com.ringl.missedcallhook.util.PrefsManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsManager: PrefsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsManager = PrefsManager(requireContext())
        setupUI()
    }

    private fun setupUI() {
        // Load initial values
        val currentDelay = prefsManager.getReplyDelay()
        val currentMessage = prefsManager.getReplyMessage()

        binding.tvDelayValue.text = "$currentDelay seconds"
        binding.sliderDelay.value = currentDelay.toFloat()
        binding.etReplyMessage.setText(currentMessage)

        // Slider listener
        binding.sliderDelay.addOnChangeListener { _, value, _ ->
            binding.tvDelayValue.text = "${value.toInt()} seconds"
        }

        // Save button
        binding.btnSaveSettings.setOnClickListener {
            val delay = binding.sliderDelay.value.toInt()
            val message = binding.etReplyMessage.text.toString().trim()

            if (message.isEmpty()) {
                Toast.makeText(context, "⚠️ Message cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefsManager.setReplyDelay(delay)
            prefsManager.setReplyMessage(message)
            Toast.makeText(context, "✅ Settings updated!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
