package io.github.flagrant99.romulist

enum class AppDestinations(
    val label: String,
    val icon: Int
)
{
    BACK("Back", R.drawable.baseline_arrow_back_24),
    HOME("Home", R.drawable.outline_castle_24),
    SETTINGS("Settings", R.drawable.outline_settings_24);
}
