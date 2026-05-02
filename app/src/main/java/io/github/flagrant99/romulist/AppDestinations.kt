package io.github.flagrant99.romulist

enum class AppDestinations(
    val label: String,
    val icon: Int
)
{
    BACK("Back", R.drawable.baseline_arrow_back_24),
    HOME("Home", R.drawable.ic_home),
    FAVORITE("Favorite", R.drawable.ic_favorite);
}
