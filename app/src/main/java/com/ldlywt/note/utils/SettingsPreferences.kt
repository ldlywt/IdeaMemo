package com.ldlywt.note.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ldlywt.note.App
import com.ldlywt.note.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val THEME_PREFERENCES = "THEME_PREFERENCES"
private val Context.themePreferences by preferencesDataStore(name = THEME_PREFERENCES)


object SettingsPreferences {
    enum class ThemeMode(@StringRes val resId: Int) {
        LIGHT(R.string.light_mode), DARK(R.string.dark_mode), SYSTEM(R.string.use_device_theme),
    }

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }


    private val themePreferences = App.instance.themePreferences

    val themeMode = themePreferences.getEnum(PreferencesKeys.THEME_MODE, ThemeMode.SYSTEM)
    val dynamicColor = themePreferences.getBoolean(PreferencesKeys.DYNAMIC_COLOR, false)
    val firstLaunch = themePreferences.getBoolean(PreferencesKeys.FIRST_LAUNCH, true)

    private suspend fun <T> updatePreference(key:Preferences.Key<T>,value: T){
        themePreferences.edit { preferences  ->
            preferences[key]=value
        }
    }

    suspend fun changeThemeMode(themeMode: ThemeMode) {
      updatePreference(PreferencesKeys.THEME_MODE,themeMode.name)
    }


    suspend fun changeDynamicColor(dynamicTheme: Boolean) {
      updatePreference(PreferencesKeys.DYNAMIC_COLOR,dynamicTheme)
    }

    suspend fun changeFirstLaunch(isFirst: Boolean) {
       updatePreference(PreferencesKeys.FIRST_LAUNCH,isFirst)
    }
}


inline fun <reified T : Enum<T>> DataStore<Preferences>.getEnum(key: Preferences.Key<String>, defaultValue: T): Flow<T> {
    return this.data.map { preferences ->
        preferences[key]?.let {
            try {
                enumValueOf<T>(it)
            } catch (e: IllegalArgumentException) {
                defaultValue
            }
        } ?: defaultValue
    }
}


fun DataStore<Preferences>.getBoolean(key: Preferences.Key<Boolean>, defaultValue: Boolean): Flow<Boolean> {
    return this.data.map { preferences ->
        preferences[key] ?: defaultValue
    }
}

fun DataStore<Preferences>.getInt(key: Preferences.Key<Int>, defaultValue: Int?): Flow<Int?> {
    return this.data.map { preferences ->
        preferences[key]?: defaultValue
    }
}

fun DataStore<Preferences>.getString(key: Preferences.Key<String>, defaultValue: String?): Flow<String?> {
    return this.data.map { preferences ->
        preferences[key]?: defaultValue
    }
}

