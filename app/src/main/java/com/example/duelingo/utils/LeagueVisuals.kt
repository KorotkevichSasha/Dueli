package com.example.duelingo.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.duelingo.R

data class LeagueVisual(
    @DrawableRes val icon: Int,
    @StringRes val name: Int
)

object LeagueVisuals {
    fun forId(id: String?): LeagueVisual = when (id) {
        "COPPER" -> LeagueVisual(R.drawable.league_copper, R.string.league_copper)
        "SILVER" -> LeagueVisual(R.drawable.league_silver, R.string.league_silver)
        "GOLD" -> LeagueVisual(R.drawable.league_gold, R.string.league_gold)
        "SAPPHIRE" -> LeagueVisual(R.drawable.league_sapphire, R.string.league_sapphire)
        "EMERALD" -> LeagueVisual(R.drawable.league_emerald, R.string.league_emerald)
        "RUBY" -> LeagueVisual(R.drawable.league_ruby, R.string.league_ruby)
        "OBSIDIAN" -> LeagueVisual(R.drawable.league_obsidian, R.string.league_obsidian)
        "DIAMOND" -> LeagueVisual(R.drawable.league_diamond, R.string.league_diamond)
        "LEGEND" -> LeagueVisual(R.drawable.league_legend, R.string.league_legend)
        else -> LeagueVisual(R.drawable.league_ember, R.string.league_ember)
    }
}
