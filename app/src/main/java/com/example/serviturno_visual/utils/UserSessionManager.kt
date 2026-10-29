package com.example.serviturno_visual.utils

import android.content.Context

object UserSessionManager {
    private const val PREFS_NAME = "serviturno_user_prefs"
    private const val KEY_LOGGED_EMAIL = "key_logged_email"

    fun saveSession(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOGGED_EMAIL, email.trim().lowercase()).apply()
    }

    fun getSavedEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_LOGGED_EMAIL, null)
        return if (!email.isNullOrBlank()) email else null
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
