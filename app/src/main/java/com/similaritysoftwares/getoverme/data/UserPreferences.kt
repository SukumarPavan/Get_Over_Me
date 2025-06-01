package com.similaritysoftwares.getoverme.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getCurrentStreak(): Int {
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }

    fun setCurrentStreak(streak: Int) {
        prefs.edit().putInt(KEY_CURRENT_STREAK, streak).apply()
    }

    fun getHighestStreak(): Int {
        return prefs.getInt(KEY_HIGHEST_STREAK, 0)
    }

    fun setHighestStreak(streak: Int) {
        prefs.edit().putInt(KEY_HIGHEST_STREAK, streak).apply()
    }

    fun getProfileImageUri(): Uri? {
        val uriString = prefs.getString(KEY_PROFILE_IMAGE, null)
        return uriString?.let { Uri.parse(it) }
    }

    fun setProfileImageUri(uri: Uri?) {
        prefs.edit().putString(KEY_PROFILE_IMAGE, uri?.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_HIGHEST_STREAK = "highest_streak"
        private const val KEY_PROFILE_IMAGE = "profile_image"
    }
} 