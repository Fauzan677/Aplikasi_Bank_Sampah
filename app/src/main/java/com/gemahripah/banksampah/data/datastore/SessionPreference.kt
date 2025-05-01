package com.gemahripah.banksampah.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gemahripah.banksampah.data.remote.SessionData
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.sessionDataStore by preferencesDataStore(name = "supabase_session")

class SessionPreference private constructor(private val context: Context) {

    companion object {
        private val SESSION_KEY = stringPreferencesKey("session_json")

        @Volatile
        private var INSTANCE: SessionPreference? = null

        fun getInstance(context: Context): SessionPreference {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionPreference(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun saveSession(sessionData: SessionData) {
        // Mengonversi objek sessionData menjadi JSON string
        val sessionJson = Gson().toJson(sessionData)

        context.sessionDataStore.edit { preferences ->
            preferences[SESSION_KEY] = sessionJson
        }
    }

    fun getSession(): Flow<SessionData?> {
        return context.sessionDataStore.data.map { preferences ->
            val sessionJson = preferences[SESSION_KEY]
            // Mengonversi kembali JSON string menjadi objek SessionData
            sessionJson?.let { Gson().fromJson(it, SessionData::class.java) }
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(SESSION_KEY)
        }
    }
}