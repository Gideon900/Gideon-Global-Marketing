package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "gideon_session_prefs")

class SessionPreferences(private val context: Context) {

    val loggedInEmail: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN_EMAIL]
    }

    val currencyCode: Flow<String> = context.sessionDataStore.data.map { prefs ->
        prefs[KEY_CURRENCY_CODE] ?: "XAF"
    }

    suspend fun setLoggedInEmail(email: String?) {
        context.sessionDataStore.edit { prefs ->
            if (email.isNullOrBlank()) {
                prefs.remove(KEY_LOGGED_IN_EMAIL)
            } else {
                prefs[KEY_LOGGED_IN_EMAIL] = email.trim().lowercase()
            }
        }
    }

    suspend fun setCurrencyCode(code: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_CURRENCY_CODE] = code
        }
    }

    private companion object {
        val KEY_LOGGED_IN_EMAIL = stringPreferencesKey("logged_in_email")
        val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
    }
}
