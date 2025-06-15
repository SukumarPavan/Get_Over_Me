package com.similaritysoftwares.getoverme.ui.game

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.similaritysoftwares.getoverme.MainActivity
import com.similaritysoftwares.getoverme.R // Import the generated R class
import com.similaritysoftwares.getoverme.databinding.ActivityGameBinding
import com.similaritysoftwares.getoverme.data.UserPreferences
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class GameActivity : AppCompatActivity(), GameBoardView.GameListener {
    private lateinit var binding: ActivityGameBinding
    private var winStreak = 0
    private var loadingOverlay: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var userPreferences: UserPreferences
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private val interstitialAdUnitId = "ca-app-pub-6441750745135526/3891065936" // Your Game Completion Interstitial Ad Unit ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Game"

        // Initialize UserPreferences
        userPreferences = UserPreferences(this)

        // Get win streak from intent if it exists
        winStreak = intent.getIntExtra("winStreak", 0)

        // Set this activity as the game listener
        binding.gameBoard.setGameListener(this)

        // Initialize loading overlay
        loadingOverlay = LayoutInflater.from(this).inflate(R.layout.loading_overlay, null)
        (window.decorView as FrameLayout).addView(loadingOverlay)
        loadingOverlay?.visibility = View.GONE

        // Load interstitial ad
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        if (isLoadingAd) return
        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, interstitialAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isLoadingAd = false
            }
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("AdMob", "Game Interstitial Ad Load Failed: ${adError.message}")
                interstitialAd = null
                isLoadingAd = false
                // Retry after a delay to avoid spamming requests
                handler.postDelayed({ loadInterstitialAd() }, 10000)
            }
        })
    }

    private fun showInterstitialAdThenFinish() {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd() // Load the next ad
                    finish()
                }
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    loadInterstitialAd() // Load the next ad
                    finish()
                }
            }
            try {
                interstitialAd?.show(this)
            } catch (e: Exception) {
                Log.e("AdMob", "Game Interstitial Ad Show Exception: ${e.message}")
                interstitialAd = null
                loadInterstitialAd()
                finish()
            }
        } else {
            finish()
        }
    }

    override fun onGameOver() {
        // Save current streak
        userPreferences.setCurrentStreak(winStreak)
        showLoading()
        handler.postDelayed({
            showInterstitialAdThenFinish()
        }, 500) // Show loading for 0.5 seconds before showing ad or finishing
    }

    override fun onGameWon() {
        // Save current streak when game is won (it will be incremented in showGameResultDialog)
        userPreferences.setCurrentStreak(winStreak + 1)
        showLoading()
        handler.postDelayed({
            showGameResultDialog(true)
        }, 500) // Show loading for 0.5 seconds before showing ad or finishing
    }

    override fun onSupportNavigateUp(): Boolean {
        // Save current streak when leaving the game
        userPreferences.setCurrentStreak(winStreak)
        onBackPressed()
        return true
    }

    private fun showLoading() {
        loadingOverlay?.visibility = View.VISIBLE
        handler.postDelayed({
            hideLoading()
        }, 500) // Hide after 0.5 seconds
    }

    private fun hideLoading() {
        loadingOverlay?.visibility = View.GONE
    }

    private fun showGameResultDialog(isWin: Boolean) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        // Inflate the custom layout
        val customLayout = LayoutInflater.from(this).inflate(R.layout.dialog_game_result, null)
        dialog.setContentView(customLayout)

        // Set dialog background to transparent to show the custom layout's rounded corners and background
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleTextView = customLayout.findViewById<TextView>(R.id.dialog_title)
        val messageTextView = customLayout.findViewById<TextView>(R.id.dialog_message)
        val newGameButton = customLayout.findViewById<Button>(R.id.button_new_game)
        val homeButton = customLayout.findViewById<Button>(R.id.button_home)

        if (isWin) {
            winStreak++
            titleTextView.text = "Congratulations!"
            messageTextView.text = "You have won!\nWin Streak: $winStreak 🔥"
            newGameButton.text = "Streak on the line! 🔥"

            // Update highest streak if current streak is higher
            val highestStreak = userPreferences.getHighestStreak()
            if (winStreak > highestStreak) {
                userPreferences.setHighestStreak(winStreak)
            }

        } else {
            winStreak = 0
            titleTextView.text = "Game Over"
            messageTextView.text = "You touched an orange ball!"
            newGameButton.text = "New Game"
        }

        // Set click listeners
        newGameButton.setOnClickListener { v ->
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.wiggle_animation))

            // Delay dismissal slightly to allow animation to start
            v.postDelayed({
                dialog.dismiss()
                showLoading()
                // Restart the current activity while preserving the win streak
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("winStreak", winStreak)

                // Save current streak before starting a new game
                userPreferences.setCurrentStreak(winStreak)

                finish()
                startActivity(intent)
            }, 300) // Delay in milliseconds
        }

        homeButton.setOnClickListener { v ->
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.wiggle_animation))

            // Delay dismissal slightly to allow animation to start
            v.postDelayed({
                dialog.dismiss()
                showLoading()

                // Save current streak before going home
                userPreferences.setCurrentStreak(winStreak)

                // Navigate back to the home screen
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }, 300) // Delay in milliseconds
        }

        // Apply wiggle animation to the dialog root when shown
        dialog.setOnShowListener { _ ->
            val dialogRoot = dialog.findViewById<LinearLayout>(R.id.dialog_root_layout)
            dialogRoot?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pop_fade_in))
        }

        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        // Save the current streak when the activity is paused
        userPreferences.setCurrentStreak(winStreak)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        loadingOverlay?.let {
            (window.decorView as? FrameLayout)?.removeView(it)
        }
        loadingOverlay = null
    }
} 