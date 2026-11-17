package io.github.flagrant99.romulist

enum class AppDestinations(
    val label: String,
    val icon: Int
)
{
    BACK("Back", R.drawable.baseline_arrow_back_24),
    HOME("Home", R.drawable.outline_castle_24),
    DETAIL("Detail", R.drawable.outline_rocket_launch_24),
    SET_HOME("Set Home", R.drawable.ic_home),
    PACKAGE_LIST("List Packages", R.drawable.ic_account_box),
    ACTIVITY_LIST("Activities", R.drawable.outline_settings_24),
    ANDROID_SYSTEM("Android System", R.drawable.outline_settings_24);
}
