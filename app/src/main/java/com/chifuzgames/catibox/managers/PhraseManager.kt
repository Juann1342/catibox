package com.chifuzgames.catibox.managers

import android.content.Context
import com.chifuzgames.catibox.R

class PhraseManager(private val context: Context) {

    private var lastStartIndex = -1
    private var lastGameOverIndex = -1
    private var lastFinalIndex = -1

    private fun getRandomIndex(arraySize: Int, lastIndex: Int): Int {
        var index: Int
        do {
            index = (0 until arraySize).random()
        } while (index == lastIndex && arraySize > 1)
        return index
    }

    fun getStartPhrase(): String {
        val phrases = context.resources.getStringArray(R.array.phrase_start_game)
        val index = getRandomIndex(phrases.size, lastStartIndex)
        lastStartIndex = index
        return phrases[index]
    }

    fun getGameOverPhrase(): String {
        val phrases = context.resources.getStringArray(R.array.phrase_game_over)
        val index = getRandomIndex(phrases.size, lastGameOverIndex)
        lastGameOverIndex = index
        return phrases[index]
    }

    fun getFinalPhrase(): String {
        val phrases = context.resources.getStringArray(R.array.phrase_finish_win)
        val index = getRandomIndex(phrases.size, lastFinalIndex)
        lastFinalIndex = index
        return phrases[index]
    }
}
