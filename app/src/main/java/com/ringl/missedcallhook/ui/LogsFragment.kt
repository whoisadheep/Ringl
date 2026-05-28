package com.ringl.missedcallhook.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ringl.missedcallhook.databinding.FragmentLogsBinding
import com.ringl.missedcallhook.util.PrefsManager

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsManager: PrefsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsManager = PrefsManager(requireContext())
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val json = prefsManager.getCallLogs()
        val type = object : TypeToken<List<CallLogEntry>>() {}.type
        val logs: List<CallLogEntry> = try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        binding.rvLogs.layoutManager = LinearLayoutManager(context)
        binding.rvLogs.adapter = LogAdapter(logs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
