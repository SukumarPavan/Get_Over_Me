package com.similaritysoftwares.getoverme.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.similaritysoftwares.getoverme.R
import com.similaritysoftwares.getoverme.databinding.FragmentHomeBinding
import com.similaritysoftwares.getoverme.ui.game.GameActivity
import com.similaritysoftwares.getoverme.ui.highscore.HighScoreActivity
import com.similaritysoftwares.getoverme.data.UserPreferences
import com.similaritysoftwares.getoverme.ui.howtoplay.HowToPlayActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var loadingOverlay: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var userPreferences: UserPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())

        // Initialize loading overlay
        loadingOverlay = LayoutInflater.from(requireContext()).inflate(R.layout.loading_overlay, null)
        (requireActivity().window.decorView as FrameLayout).addView(loadingOverlay)
        loadingOverlay?.visibility = View.GONE

        updateHighScoreButtonText()

        binding.newGameButton.setOnClickListener {
            showLoading()
            startActivity(Intent(requireContext(), GameActivity::class.java))
        }

        binding.highScoreButton.setOnClickListener {
            showLoading()
            startActivity(Intent(requireContext(), HighScoreActivity::class.java))
        }

        binding.howToPlayButton.setOnClickListener {
            showLoading()
            startActivity(Intent(requireContext(), HowToPlayActivity::class.java))
        }

        // Load AdMob banner ad 1
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        binding.adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("AdMob", "Banner Ad 1 loaded successfully!")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("AdMob", "Banner Ad 1 failed to load: ${adError.message}")
                // Attempt to reload the ad if it fails
                binding.adView.postDelayed({ binding.adView.loadAd(AdRequest.Builder().build()) }, 5000)
            }

            override fun onAdOpened() {
                Log.d("AdMob", "Banner Ad 1 opened!")
            }

            override fun onAdClicked() {
                Log.d("AdMob", "Banner Ad 1 clicked!")
            }

            override fun onAdClosed() {
                Log.d("AdMob", "Banner Ad 1 closed!")
            }
        }
    }

    override fun onPause() {
        binding.adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.adView.resume()
        // Update high score text when returning to the home screen
        updateHighScoreButtonText()
    }

    private fun updateHighScoreButtonText() {
        val highestStreak = userPreferences.getHighestStreak()
        binding.highScoreButton.text = "High Score: $highestStreak 🔥"
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.adView.destroy()
        handler.removeCallbacksAndMessages(null)
        loadingOverlay?.let {
            (requireActivity().window.decorView as FrameLayout).removeView(it)
        }
        _binding = null
    }
} 