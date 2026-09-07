package com.example.clipgallery.ml

import android.content.Context
import org.json.JSONObject

/**
 * Minimal byte-level BPE tokenizer matching CLIP's SimpleTokenizer.
 * Loads vocab.json + merges.txt from assets/tokenizer/.
 */
class ClipTokenizer(context: Context) {

    private val byteEncoder: Map<Int, Char> = bytesToUnicode()
    private val encoder: Map<String, Int>
    private val bpeRanks: Map<Pair<String, String>, Int>
    private val cache = HashMap<String, List<String>>()
    private val pattern = Regex(
        "<\\|startoftext\\|>|<\\|endoftext\\|>|'s|'t|'re|'ve|'m|'ll|'d|[A-Za-z]+|[0-9]+|[^\\sA-Za-z0-9]+"
    )

    init {
        val vocabText = context.assets.open("tokenizer/vocab.json").bufferedReader().readText()
        val obj = JSONObject(vocabText)
        val map = HashMap<String, Int>()
        obj.keys().forEach { key -> map[key] = obj.getInt(key) }
        encoder = map

        val mergesLines = context.assets.open("tokenizer/merges.txt").bufferedReader().readLines()
            .drop(1) // header comment
            .filter { it.isNotBlank() }
        val ranks = HashMap<Pair<String, String>, Int>()
        mergesLines.forEachIndexed { idx, line ->
            val parts = line.trim().split(" ")
            if (parts.size == 2) ranks[Pair(parts[0], parts[1])] = idx
        }
        bpeRanks = ranks
    }

    private fun bytesToUnicode(): Map<Int, Char> {
        val bs = mutableListOf<Int>()
        bs.addAll('!'.code..'~'.code)
        bs.addAll(0xA1..0xAC)
        bs.addAll(0xAE..0xFF)
        val cs = bs.toMutableList()
        var n = 0
        for (b in 0..255) {
            if (b !in bs) {
                bs.add(b)
                cs.add(256 + n)
                n++
            }
        }
        return bs.zip(cs).associate { (b, c) -> b to c.toChar() }
    }

    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        val pairs = mutableSetOf<Pair<String, String>>()
        for (i in 0 until word.size - 1) pairs.add(word[i] to word[i + 1])
        return pairs
    }

    private fun bpe(token: String): List<String> {
        cache[token]?.let { return it }
        if (token.isEmpty()) return emptyList()
        var word = token.dropLast(1).map { it.toString() }.toMutableList()
        word.add(token.last().toString() + "</w>")
        var pairs = getPairs(word)
        if (pairs.isEmpty()) return word

        while (true) {
            val bigram = pairs.minByOrNull { bpeRanks[it] ?: Int.MAX_VALUE } ?: break
            if (bpeRanks[bigram] == null) break
            val (first, second) = bigram
            val newWord = mutableListOf<String>()
            var i = 0
            while (i < word.size) {
                val relativeIndex = word.subList(i, word.size).indexOf(first)
                val j = if (relativeIndex == -1) -1 else i + relativeIndex
                if (j == -1) {
                    newWord.addAll(word.subList(i, word.size))
                    break
                }
                newWord.addAll(word.subList(i, j))
                if (j < word.size - 1 && word[j] == first && word[j + 1] == second) {
                    newWord.add(first + second)
                    i = j + 2
                } else {
                    newWord.add(word[j])
                    i = j + 1
                }
            }
            word = newWord
            if (word.size == 1) break
            pairs = getPairs(word)
        }
        cache[token] = word
        return word
    }

    /** Returns a fixed-length (contextLength) array of token ids, padded with 0. */
    fun encode(text: String, contextLength: Int = 77): LongArray {
        val cleaned = text.lowercase().trim()
        val ids = mutableListOf<Int>()
        encoder["<|startoftext|>"]?.let { ids.add(it) }

        for (m in pattern.findAll(cleaned)) {
            val bytes = m.value.toByteArray(Charsets.UTF_8)
            val mapped = buildString { for (b in bytes) append(byteEncoder[b.toInt() and 0xFF]) }
            for (sub in bpe(mapped)) {
                encoder[sub]?.let { ids.add(it) }
            }
        }
        encoder["<|endoftext|>"]?.let { ids.add(it) }

        val out = LongArray(contextLength)
        for (i in 0 until contextLength) out[i] = if (i < ids.size) ids[i].toLong() else 0L
        return out
    }
}
