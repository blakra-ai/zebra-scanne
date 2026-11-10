package com.stankovic.zebrascanner.config

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.stankovic.zebrascanner.R
import java.util.Locale

/**
 * App-level language, independent of the phone's language. The same code is sent to the backend
 * in the X-Language header so server messages come back in the language shown in the app.
 * Supported set matches the backend's resources/lang folders.
 */
object AppLanguage {
    private const val PREFS = "app_prefs"
    private const val KEY = "app_language"

    /** code to native display name */
    val supported = linkedMapOf(
        "en" to "English",
        "de" to "Deutsch",
        "es" to "Español",
        "hu" to "Magyar",
    )

    fun current(context: Context): String {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (saved != null && saved in supported) return saved
        // First run: follow the phone if we support its language, else English
        val phone = Locale.getDefault().language
        return if (phone in supported) phone else "en"
    }

    /** Applies the saved language to the whole app; call from Application.onCreate. */
    fun apply(context: Context) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(current(context)))
    }

    fun set(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, code).apply()
        // Recreates open AppCompat activities in the new language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }

    /**
     * A context whose resources use the app language. Use it for strings built outside an
     * activity (receivers, background work), where the system locale would otherwise leak in.
     */
    fun localized(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(current(context)))
        return context.createConfigurationContext(config)
    }

    fun showPicker(activity: Activity) {
        val codes = supported.keys.toList()
        val names = supported.values.toTypedArray()
        val selected = codes.indexOf(current(activity))
        AlertDialog.Builder(activity)
            .setTitle(R.string.language)
            .setSingleChoiceItems(names, selected) { dialog, which ->
                dialog.dismiss()
                if (codes[which] != current(activity)) set(activity, codes[which])
            }
            .show()
    }
}
