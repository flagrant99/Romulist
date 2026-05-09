package io.github.flagrant99.romulist

import android.content.Context
import android.content.SharedPreferences

class PersistentSettings(context: Context) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "RomulistPrefs",
        Context.MODE_PRIVATE
    )

    var favoriteFolder: String?
        get() = sharedPrefs.getString("favorite_folder", null)
        set(value) {
            sharedPrefs.edit().putString("favorite_folder", value).apply()
        }

    fun getPreferredIntent(systemName: String, defaultName: String?): String? {
        return sharedPrefs.getString("preferred_intent_$systemName", defaultName)
    }

    fun setPreferredIntent(systemName: String, intentName: String) {
        sharedPrefs.edit().putString("preferred_intent_$systemName", intentName).apply()
    }
}
