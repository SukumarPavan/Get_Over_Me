package com.similaritysoftwares.getoverme.ui.highscore

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.similaritysoftwares.getoverme.R
import com.similaritysoftwares.getoverme.databinding.ActivityHighScoreBinding
import com.similaritysoftwares.getoverme.data.UserPreferences

class HighScoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHighScoreBinding
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHighScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "High Scores"

        // Initialize UserPreferences
        userPreferences = UserPreferences(this)

        // Set up the fire animation
        binding.fireAnimation.setAnimation(R.raw.fire_animation)
        binding.fireAnimation.playAnimation()
        binding.fireAnimation.repeatCount = LottieDrawable.INFINITE

        // Display the user's current streak
        val currentUserStreak = userPreferences.getHighestStreak()
        binding.currentUserStreakTextView.text = "Your Current Streak: $currentUserStreak 🔥"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 