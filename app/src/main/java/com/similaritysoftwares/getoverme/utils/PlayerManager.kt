package com.similaritysoftwares.getoverme.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class PlayerManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    val playerId: String
        get() = prefs.getString(KEY_PLAYER_ID, null) ?: generateAndSavePlayerId()
    
    var playerName: String
        get() = prefs.getString(KEY_PLAYER_NAME, "Player") ?: "Player"
        set(value) = prefs.edit().putString(KEY_PLAYER_NAME, value).apply()
    
    var highestStreak: Int
        get() = prefs.getInt(KEY_HIGHEST_STREAK, 0)
        set(value) {
            if (value > highestStreak) {
                prefs.edit().putInt(KEY_HIGHEST_STREAK, value).apply()
            }
        }
    
    private fun generateAndSavePlayerId(): String {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_PLAYER_ID, newId).apply()
        return newId
    }
    
    companion object {
        private const val PREFS_NAME = "player_prefs"
        private const val KEY_PLAYER_ID = "player_id"
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_HIGHEST_STREAK = "highest_streak"
    }
} 