package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.UserAllergenPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "galsafe_prefs")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val AVOID_MAMMAL_MEAT = booleanPreferencesKey("avoid_mammal_meat")
        val AVOID_DAIRY = booleanPreferencesKey("avoid_dairy")
        val AVOID_GLUTEN = booleanPreferencesKey("avoid_gluten")
        val FLAG_HIDDEN_DERIVATIVES = booleanPreferencesKey("flag_hidden_derivatives")
        val FLAG_CARRAGEENAN = booleanPreferencesKey("flag_carrageenan")
    }

    val preferencesFlow: Flow<UserAllergenPreferences> = context.dataStore.data.map { prefs ->
        UserAllergenPreferences(
            avoidMammalMeat = prefs[Keys.AVOID_MAMMAL_MEAT] ?: true,
            avoidDairy = prefs[Keys.AVOID_DAIRY] ?: true,
            avoidGluten = prefs[Keys.AVOID_GLUTEN] ?: true,
            flagHiddenMammalDerivatives = prefs[Keys.FLAG_HIDDEN_DERIVATIVES] ?: true,
            flagCarrageenan = prefs[Keys.FLAG_CARRAGEENAN] ?: true
        )
    }

    suspend fun setAvoidMammalMeat(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AVOID_MAMMAL_MEAT] = enabled }
    }

    suspend fun setAvoidDairy(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AVOID_DAIRY] = enabled }
    }

    suspend fun setAvoidGluten(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AVOID_GLUTEN] = enabled }
    }

    suspend fun setFlagHiddenDerivatives(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLAG_HIDDEN_DERIVATIVES] = enabled }
    }

    suspend fun setFlagCarrageenan(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLAG_CARRAGEENAN] = enabled }
    }
}
