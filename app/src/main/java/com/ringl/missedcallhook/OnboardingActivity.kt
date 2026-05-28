package com.ringl.missedcallhook

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ringl.missedcallhook.databinding.ActivityOnboardingBinding
import com.ringl.missedcallhook.util.PrefsManager

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGetStarted.setOnClickListener {
            PrefsManager(this).setFirstRunComplete()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
