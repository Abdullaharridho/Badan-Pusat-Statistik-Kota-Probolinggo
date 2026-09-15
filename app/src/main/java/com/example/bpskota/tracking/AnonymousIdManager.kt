package com.example.bpskota.tracking

import android.content.Context
import java.util.UUID

class AnonymousIdManager(context: Context) {

    companion object {
        private const val PREF_NAME = "activity_tracking"
        private const val KEY_ANONYMOUS_ID = "anonymous_id"
    }

    private val preferences = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getAnonymousId(): String {
        val existingId = preferences.getString(KEY_ANONYMOUS_ID, null)

        if (!existingId.isNullOrBlank()) {
            return existingId
        }

        val newId = UUID.randomUUID().toString()

        preferences.edit()
            .putString(KEY_ANONYMOUS_ID, newId)
            .apply()

        return newId
    }
}