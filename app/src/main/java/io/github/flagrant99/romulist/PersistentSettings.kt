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
}
