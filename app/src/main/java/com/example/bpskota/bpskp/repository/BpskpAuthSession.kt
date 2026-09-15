package com.example.bpskota.bpskp.repository

import android.content.Context

class BpskpAuthSession(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    fun saveLogin(
        token: String,
        userId: Int,
        name: String,
        username: String,
        role: String
    ) {

        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(
            KEY_IS_LOGGED_IN,
            false
        )
    }

    fun getToken(): String? {
        return preferences.getString(
            KEY_TOKEN,
            null
        )
    }

    fun getUserId(): Int {
        return preferences.getInt(
            KEY_USER_ID,
            -1
        )
    }

    fun getName(): String? {
        return preferences.getString(
            KEY_NAME,
            null
        )
    }

    fun getUsername(): String? {
        return preferences.getString(
            KEY_USERNAME,
            null
        )
    }

    fun getRole(): String? {
        return preferences.getString(
            KEY_ROLE,
            null
        )
    }

    fun clearSession() {

        preferences.edit()
            .clear()
            .apply()
    }

    companion object {

        private const val PREF_NAME =
            "bps_auth_session"

        private const val KEY_IS_LOGGED_IN =
            "is_logged_in"

        private const val KEY_TOKEN =
            "token"

        private const val KEY_USER_ID =
            "user_id"

        private const val KEY_NAME =
            "name"

        private const val KEY_USERNAME =
            "username"

        private const val KEY_ROLE =
            "role"
    }
}