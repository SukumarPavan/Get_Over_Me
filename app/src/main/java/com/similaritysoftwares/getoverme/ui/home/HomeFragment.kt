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
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Fragment view has been destroyed.")
    private var loadingOverlay: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var userPreferences: UserPreferences
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var shouldShowAdOnResume = false
    private val adUnitId = "ca-app-pub-6441750745135526/8612909574" // Your Home Interstitial Ad Unit ID

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
            handler.postDelayed({
                showAdThen {
                    startActivity(Intent(requireContext(), GameActivity::class.java))
                }
            }, 500) // Show loading for 0.5 seconds
        }

        binding.highScoreButton.setOnClickListener {
            showLoading()
            handler.postDelayed({
                showAdThen {
                    startActivity(Intent(requireContext(), HighScoreActivity::class.java))
                }
            }, 500) // Show loading for 0.5 seconds
        }

        binding.howToPlayButton.setOnClickListener {
            showLoading()
            handler.postDelayed({
                showAdThen {
                    startActivity(Intent(requireContext(), HowToPlayActivity::class.java))
                }
            }, 500) // Show loading for 0.5 seconds
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
                _binding?.adView?.postDelayed({ 
                    _binding?.adView?.loadAd(AdRequest.Builder().build()) 
                }, 5000)
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

        loadInterstitialAd()
    }

    override fun onPause() {
        super.onPause()
        _binding?.adView?.pause()
        // Clean up interstitial ad resources
        interstitialAd?.fullScreenContentCallback = null
    }

    override fun onResume() {
        super.onResume()
        _binding?.adView?.resume()
        // Update high score text when returning to the home screen
        updateHighScoreButtonText()
        // Show interstitial ad if loaded, and always try to load the next one
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                }
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                }
            }
            try {
                interstitialAd?.show(requireActivity())
            } catch (e: Exception) {
                interstitialAd = null
                loadInterstitialAd()
            }
        } else if (!isLoadingAd) {
            loadInterstitialAd()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.adView?.destroy()
        _binding = null
        loadingOverlay?.let {
            (requireActivity().window.decorView as? FrameLayout)?.removeView(it)
        }
        loadingOverlay = null
    }

    private fun updateHighScoreButtonText() {
        val highestStreak = userPreferences.getHighestStreak()
        _binding?.highScoreButton?.text = "High Score: $highestStreak 🔥"
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

    private fun loadInterstitialAd() {
        if (isLoadingAd) return
        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(), adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isLoadingAd = false
            }
            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
                isLoadingAd = false
                // Retry after a delay to avoid spamming requests
                handler.postDelayed({ loadInterstitialAd() }, 10000)
            }
        })
    }

    // Helper to show ad and then run an action
    private fun showAdThen(action: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    action()
                }
                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                    action()
                }
            }
            try {
                interstitialAd?.show(requireActivity())
            } catch (e: Exception) {
                interstitialAd = null
                loadInterstitialAd()
                action()
            }
        } else {
            action()
        }
    }
} 