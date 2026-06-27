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

    var useNavRail: Boolean
        get() = sharedPrefs.getBoolean("use_nav_rail", true)
        set(value) {
            sharedPrefs.edit().putBoolean("use_nav_rail", value).apply()
        }

    var swapAB: Boolean
        get() = sharedPrefs.getBoolean("swap_ab", false)
        set(value) {
            sharedPrefs.edit().putBoolean("swap_ab", value).apply()
        }

    var useLargeFont: Boolean
        get() = sharedPrefs.getBoolean("use_large_font", false)
        set(value) {
            sharedPrefs.edit().putBoolean("use_large_font", value).apply()
        }
}
