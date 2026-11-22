package gr.kiddo.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen { Intro, Home, Draw, Coloring, Music, Quiz, Puzzle, Memory, Numbers, Owl3D, Games, Balloons, Moles, Catch, Simon }

/** Global, persisted app state: age group, stars earned and music on/off. */
class AppState(context: Context) {
    private val prefs = context.getSharedPreferences("kiddo", Context.MODE_PRIVATE)

    var stars by mutableIntStateOf(prefs.getInt("stars", 0))
        private set
    var older by mutableStateOf(prefs.getBoolean("older", false))
        private set
    var musicOn by mutableStateOf(prefs.getBoolean("music", true))
        private set

    fun addStars(n: Int) {
        stars += n
        prefs.edit().putInt("stars", stars).apply()
    }

    fun updateOlder(value: Boolean) {
        older = value
        prefs.edit().putBoolean("older", value).apply()
    }

    private fun bestKey(game: String) = "best.$game.${if (older) "o" else "y"}"

    fun best(game: String): Int = prefs.getInt(bestKey(game), 0)

    /** Saves the score if it beats the record; returns true for a new record. */
    fun saveBest(game: String, score: Int): Boolean {
        if (score <= best(game)) return false
        prefs.edit().putInt(bestKey(game), score).apply()
        return true
    }

    fun updateMusic(value: Boolean) {
        musicOn = value
        prefs.edit().putBoolean("music", value).apply()
    }
}
