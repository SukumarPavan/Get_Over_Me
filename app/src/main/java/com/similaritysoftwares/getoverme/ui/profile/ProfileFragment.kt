package com.similaritysoftwares.getoverme.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.similaritysoftwares.getoverme.R
import com.similaritysoftwares.getoverme.databinding.FragmentProfileBinding
import com.similaritysoftwares.getoverme.data.UserPreferences
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Fragment view has been destroyed.")

    private lateinit var userPreferences: UserPreferences
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private val interstitialAdUnitId = "ca-app-pub-6441750745135526/8914493606" // Your Profile Interstitial Ad Unit ID
    private val bannerAdUnitId = "ca-app-pub-6441750745135526/3176750066" // Your Profile Banner Ad Unit ID
    private var loadingOverlay: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { uri ->
                // Check image size
                val fileSize = getFileSizeFromUri(uri)
                val maxSize = 10 * 1024 * 1024 // 10 MB

                if (fileSize != -1L && fileSize > maxSize) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Image Too Large")
                        .setMessage("Please select an image smaller than 10MB.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    // Save the image to internal storage and update UI
                    saveProfileImage(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())

        // Initialize loading overlay
        loadingOverlay = LayoutInflater.from(requireContext()).inflate(R.layout.loading_overlay, null)
        (requireActivity().window.decorView as FrameLayout).addView(loadingOverlay)
        loadingOverlay?.visibility = View.GONE

        // Load saved name and profile picture
        loadProfileData()

        binding.changeNameButton.setOnClickListener {
            showLoading()
            handler.postDelayed({
                showAdThen { showChangeNameDialog() }
            }, 500) // Show loading for 0.5 seconds
        }

        binding.changeProfilePicButton.setOnClickListener {
            showLoading()
            handler.postDelayed({
                showAdThen { openImagePicker() }
            }, 500) // Show loading for 0.5 seconds
        }

        binding.privacyPolicyButton.setOnClickListener {
            showLoading()
            handler.postDelayed({
                showAdThen {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://getoverme-privacypolicy.blogspot.com/2025/06/privacy-policy-for-getoverme-last.html"))
                    startActivity(browserIntent)
                }
            }, 500) // Show loading for 0.5 seconds
        }

        // Load AdMob banner ad 2
        val adRequest = AdRequest.Builder().build()
        _binding?.adView2?.adUnitId = bannerAdUnitId
        _binding?.adView2?.loadAd(adRequest)

        _binding?.adView2?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("AdMob", "Banner Ad 2 loaded successfully!")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("AdMob", "Banner Ad 2 failed to load: ${adError.message}")
                // Attempt to reload the ad if it fails
                _binding?.adView2?.postDelayed({ 
                    _binding?.adView2?.loadAd(AdRequest.Builder().build()) 
                }, 5000)
            }
        }

        // Load interstitial ad
        loadInterstitialAd()
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

    override fun onResume() {
        super.onResume()
        _binding?.adView2?.resume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.adView2?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.adView2?.destroy()
        _binding = null
        loadingOverlay?.let {
            (requireActivity().window.decorView as? FrameLayout)?.removeView(it)
        }
        loadingOverlay = null
    }

    private fun loadProfileData() {
        val savedName = userPreferences.getUserName() ?: "Player Name"
        _binding?.playerName?.text = savedName

        val savedImageUri = userPreferences.getProfileImageUri()
        if (savedImageUri != null) {
            _binding?.profilePicture?.load(savedImageUri) {
                crossfade(true)
                placeholder(R.drawable.default_profile)
                transformations(CircleCropTransformation())
            }
        } else {
            _binding?.profilePicture?.load(R.drawable.default_profile) {
                transformations(CircleCropTransformation())
            }
        }
    }

    private fun saveProfileImage(uri: Uri) {
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val outputDir = requireContext().getDir("profile_images", Context.MODE_PRIVATE)
                val outputFile = File(outputDir, "user_profile_image.jpg")
                val outputStream = FileOutputStream(outputFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                // Save the internal storage URI in UserPreferences
                val internalUri = Uri.fromFile(outputFile)
                userPreferences.setProfileImageUri(internalUri)

                // Load the saved image into the ImageView
                _binding?.profilePicture?.load(internalUri) {
                    crossfade(true)
                    placeholder(R.drawable.default_profile)
                    transformations(CircleCropTransformation())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showChangeNameDialog() {
        val inputEditText = EditText(requireContext())
        inputEditText.setText(_binding?.playerName?.text)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Name")
            .setView(inputEditText)
            .setPositiveButton("Save") { _, _ ->
                val newName = inputEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    userPreferences.setUserName(newName)
                    _binding?.playerName?.text = newName
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        selectImageLauncher.launch(intent)
    }

    private fun getFileSizeFromUri(uri: Uri): Long {
        var size: Long = -1
        try {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
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