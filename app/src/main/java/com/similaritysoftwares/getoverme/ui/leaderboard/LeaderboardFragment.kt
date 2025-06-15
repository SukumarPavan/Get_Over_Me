package com.similaritysoftwares.getoverme.ui.leaderboard

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.similaritysoftwares.getoverme.R
import com.similaritysoftwares.getoverme.databinding.FragmentLeaderboardBinding
import com.similaritysoftwares.getoverme.data.UserPreferences
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class LeaderboardFragment : Fragment() {
    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Fragment view has been destroyed.")
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var userPreferences: UserPreferences
    private val requestOptions = RequestOptions().placeholder(R.drawable.default_profile).error(R.drawable.default_profile)
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private val interstitialAdUnitId = "ca-app-pub-6441750745135526/8804481265" // Your Leaderboard Interstitial Ad Unit ID
    private val bannerAdUnitId = "ca-app-pub-6441750745135526/4755740836" // Your Leaderboard Banner Ad Unit ID
    private var loadingOverlay: View? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        setupLeaderboard()
        loadLeaderboardData()
        loadInterstitialAd()

        // Initialize loading overlay
        loadingOverlay = LayoutInflater.from(requireContext()).inflate(R.layout.loading_overlay, null)
        (requireActivity().window.decorView as FrameLayout).addView(loadingOverlay)
        loadingOverlay?.visibility = View.GONE

        // Load banner ad
        val adRequest = AdRequest.Builder().build()
        _binding?.adView?.adUnitId = bannerAdUnitId
        _binding?.adView?.loadAd(adRequest)

        _binding?.adView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("AdMob", "Banner Ad loaded successfully!")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("AdMob", "Banner Ad failed to load: ${adError.message}")
                // Attempt to reload the ad if it fails
                _binding?.adView?.postDelayed({ 
                    _binding?.adView?.loadAd(AdRequest.Builder().build()) 
                }, 5000)
            }
        }
    }

    private fun loadInterstitialAd() {
        if (isLoadingAd) return
        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(), interstitialAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isLoadingAd = false
            }
            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
                isLoadingAd = false
                // Retry after a delay to avoid spamming requests
                Handler(Looper.getMainLooper()).postDelayed({ loadInterstitialAd() }, 10000)
            }
        })
    }

    private fun showInterstitialAd() {
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
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.adView?.resume()
        // Show interstitial ad when returning to the leaderboard
        showLoading()
        handler.postDelayed({ 
            showInterstitialAd()
        }, 500) // Show loading for 0.5 seconds
    }

    override fun onPause() {
        super.onPause()
        _binding?.adView?.pause()
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

    private fun setupLeaderboard() {
        leaderboardAdapter = LeaderboardAdapter()
        binding.topPlayersList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = leaderboardAdapter
        }
    }

    private fun loadLeaderboardData() {
        // Get current user data
        val currentUserName = userPreferences.getUserName() ?: "Anonymous"
        val currentUserStreak = userPreferences.getHighestStreak() // Use current streak for ranking
        val currentUserImageUri = userPreferences.getProfileImageUri()

        // TODO: Replace with actual data from your data source
        val topPlayers = listOf(
            LeaderboardEntry("Arjun Sharma", 15, 1, profileImageResId = R.drawable.profile_1),
            LeaderboardEntry("Priya Patel", 12, 2, profileImageResId = R.drawable.profile_2),
            LeaderboardEntry("Rahul Gupta", 10, 3, profileImageResId = R.drawable.profile_3),
            LeaderboardEntry("Ananya Singh", 8, 4, profileImageResId = R.drawable.profile_4),
            LeaderboardEntry("Vikram Malhotra", 7, 5, profileImageResId = R.drawable.profile_5),
            LeaderboardEntry("Meera Kapoor", 6, 6, profileImageResId = R.drawable.profile_6),
            LeaderboardEntry("Karan Verma", 5, 7, profileImageResId = R.drawable.profile_7),
            LeaderboardEntry("Neha Reddy", 4, 8, profileImageResId = R.drawable.profile_8),
            LeaderboardEntry("Aditya Joshi", 3, 9, profileImageResId = R.drawable.profile_9),
            LeaderboardEntry("Sanya Mehta", 2, 10, profileImageResId = R.drawable.profile_10)
        ).toMutableList()

        // Add current user to the list (temporary for ranking), pass the actual image URI
        val currentUserEntry = LeaderboardEntry(currentUserName, currentUserStreak, 0, profileImageUri = currentUserImageUri)
        topPlayers.add(currentUserEntry)

        // Sort the list by streak in descending order
        topPlayers.sortByDescending { it.streak }

        // Assign ranks based on sorted order
        var currentRank = 1
        var previousStreak = -1
        val rankedPlayers = topPlayers.mapIndexed { index, entry ->
            if (entry.streak != previousStreak) {
                currentRank = index + 1
            }
            previousStreak = entry.streak
            entry.copy(rank = currentRank)
        }

        // Find the user's updated rank entry
        val userUpdatedRankEntry = rankedPlayers.find { it.name == currentUserName && it.streak == currentUserStreak }

        // Display the top 10 (or fewer if less than 10 players)
        leaderboardAdapter.submitList(rankedPlayers.take(10))

        // Update user's rank section
        if (userUpdatedRankEntry != null) {
             updateUserRank(userUpdatedRankEntry)
             // Load user's profile image in the user rank section using the URI and RequestOptions
             Glide.with(this)
                 .load(currentUserImageUri ?: R.drawable.default_profile)
                 .apply(requestOptions)
                 .into(binding.userImage)

        } else {
            // Fallback for user rank section if entry not found (shouldn't happen with current logic)
             val userRank = LeaderboardEntry(currentUserName, currentUserStreak, -1, profileImageUri = currentUserImageUri)
             updateUserRank(userRank)
             Glide.with(this)
                 .load(currentUserImageUri ?: R.drawable.default_profile)
                 .apply(requestOptions)
                 .into(binding.userImage)
        }
    }

    private fun updateUserRank(userRank: LeaderboardEntry) {
        binding.userRankNumber.text = if (userRank.rank != -1) "#${userRank.rank}" else "--"
        binding.userName.text = userRank.name
        binding.userStreak.text = "Streak: ${userRank.streak}"
        // Note: Profile image for the user rank section is handled directly in loadLeaderboardData
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
}

data class LeaderboardEntry(
    val name: String,
    val streak: Int,
    val rank: Int,
    val profileImageResId: Int? = null, // Make resource ID nullable
    val profileImageUri: Uri? = null // Add Uri field
) {
    // Helper to determine if using resource ID or Uri
    fun hasProfileImage(): Boolean = profileImageResId != null || profileImageUri != null
}

class LeaderboardAdapter : androidx.recyclerview.widget.ListAdapter<LeaderboardEntry, LeaderboardAdapter.ViewHolder>(LeaderboardDiffCallback()) {

    private val requestOptions = RequestOptions().placeholder(R.drawable.default_profile).error(R.drawable.default_profile)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        private val rankNumber = view.findViewById<android.widget.TextView>(R.id.rankNumber)
        private val playerName = view.findViewById<android.widget.TextView>(R.id.playerName)
        private val playerStreak = view.findViewById<android.widget.TextView>(R.id.playerStreak)
        private val playerImage = view.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.playerImage)
        private val requestOptions = RequestOptions().placeholder(R.drawable.default_profile).error(R.drawable.default_profile)

        fun bind(entry: LeaderboardEntry) {
            rankNumber.text = "#${entry.rank}"
            playerName.text = entry.name
            playerStreak.text = "Streak: ${entry.streak}"

            // Load profile image using Glide, checking for both URI and Resource ID
            val imageLoad = when {
                entry.profileImageUri != null -> {
                    Glide.with(itemView.context).load(entry.profileImageUri)
                }
                entry.profileImageResId != null -> {
                    Glide.with(itemView.context).load(entry.profileImageResId)
                }
                else -> {
                    Glide.with(itemView.context).load(R.drawable.default_profile)
                }
            }
            
            imageLoad.apply(requestOptions).into(playerImage)
        }
    }
}

class LeaderboardDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<LeaderboardEntry>() {
    override fun areItemsTheSame(oldItem: LeaderboardEntry, newItem: LeaderboardEntry): Boolean {
        // Assuming name is unique for simplicity in this example
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: LeaderboardEntry, newItem: LeaderboardEntry): Boolean {
        return oldItem == newItem
    }
} 