package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class FontSizeScale(val scale: Float, val displayName: String, val description: String) {
    SMALL(0.85f, "Small (85%)", "Compact view for high information density"),
    MEDIUM(1.0f, "Medium (100%)", "Default balanced system scale"),
    LARGE(1.15f, "Large (115%)", "Enhanced readability"),
    EXTRA_LARGE(1.30f, "Extra Large (130%)", "Large text and high contrast"),
    HUGE(1.50f, "Huge (150%)", "Maximum accessibility sizing")
}

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _fontScale = MutableStateFlow(loadFontScale())
    val fontScale: StateFlow<FontSizeScale> = _fontScale.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun loadFontScale(): FontSizeScale {
        val name = prefs.getString(KEY_FONT_SCALE, FontSizeScale.MEDIUM.name) ?: FontSizeScale.MEDIUM.name
        return try {
            FontSizeScale.valueOf(name)
        } catch (e: Exception) {
            FontSizeScale.MEDIUM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun toggleLightDarkTheme() {
        val current = _themeMode.value
        val next = if (current == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(next)
    }

    fun setFontScale(scale: FontSizeScale) {
        _fontScale.value = scale
        prefs.edit().putString(KEY_FONT_SCALE, scale.name).apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_FONT_SCALE = "key_font_scale"

        @Volatile
        private var INSTANCE: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettings(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
