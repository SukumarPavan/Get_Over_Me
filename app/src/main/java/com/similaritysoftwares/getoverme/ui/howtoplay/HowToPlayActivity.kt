package com.similaritysoftwares.getoverme.ui.howtoplay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.similaritysoftwares.getoverme.databinding.FragmentHowToPlayBinding

class HowToPlayActivity : AppCompatActivity() {
    private lateinit var binding: FragmentHowToPlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentHowToPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "How to Play"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 