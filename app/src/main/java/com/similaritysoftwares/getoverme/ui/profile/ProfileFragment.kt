package com.similaritysoftwares.getoverme.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.similaritysoftwares.getoverme.R
import com.similaritysoftwares.getoverme.databinding.FragmentProfileBinding
import com.similaritysoftwares.getoverme.data.UserPreferences
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreferences: UserPreferences

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

        // Load saved name and profile picture
        loadProfileData()

        binding.changeNameButton.setOnClickListener {
            showChangeNameDialog()
        }

        binding.changeProfilePicButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun loadProfileData() {
        val savedName = userPreferences.getUserName() ?: "Player Name"
        binding.playerName.text = savedName

        val savedImageUri = userPreferences.getProfileImageUri()
        if (savedImageUri != null) {
            binding.profilePicture.load(savedImageUri) {
                crossfade(true)
                placeholder(R.drawable.default_profile)
                transformations(CircleCropTransformation())
            }
        } else {
            binding.profilePicture.load(R.drawable.default_profile) {
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
                binding.profilePicture.load(internalUri) {
                    crossfade(true)
                    placeholder(R.drawable.default_profile)
                    transformations(CircleCropTransformation())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the error (e.g., show a toast message)
        }
    }

    private fun showChangeNameDialog() {
        val inputEditText = EditText(requireContext())
        inputEditText.setText(binding.playerName.text)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Name")
            .setView(inputEditText)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = inputEditText.text.toString()
                if (newName.isNotBlank()) {
                    binding.playerName.text = newName
                    userPreferences.setUserName(newName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        selectImageLauncher.launch(intent)
    }

    // Helper function to get file size from Uri
    private fun getFileSizeFromUri(uri: Uri): Long {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        var fileSize: Long = -1L
        cursor?.use { it ->
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && it.moveToFirst()) {
                fileSize = it.getLong(sizeIndex)
            }
        }
        return fileSize
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 