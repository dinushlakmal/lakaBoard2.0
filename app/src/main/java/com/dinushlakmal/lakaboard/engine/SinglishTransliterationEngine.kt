package com.dinushlakmal.lakaboard.engine

/**
 * SinglishTransliterationEngine
 * ---------------------------------------------------------------------
 * Rule-based, syllable-driven Singlish (romanized Sinhala) -> Sinhala
 * Unicode transliteration engine with a common-word dictionary layer
 * and progressive candidate suggestions for typing.
 */
object SinglishTransliterationEngine {

    private val CONSONANTS: LinkedHashMap<String, String> = linkedMapOf(
        "ndh" to "ඳ", "nd" to "ඬ", "mb" to "ඹ", "ngg" to "ඟ", "ng" to "ඞ්ග",
        "kh" to "ඛ", "gh" to "ඝ", "chh" to "ඡ", "ch" to "ච", "jh" to "ඣ",
        "th" to "ත", "dh" to "ද", "ph" to "ඵ", "bh" to "භ",
        "sh" to "ශ", "Sh" to "ෂ", "SH" to "ෂ", "gn" to "ඤ", "ny" to "ඤ",
        "T" to "ට", "D" to "ඩ", "N" to "ණ", "L" to "ළ", "R" to "ර",
        "k" to "ක", "g" to "ග", "c" to "ච", "j" to "ජ",
        "t" to "ට", "d" to "ඩ", "n" to "න", "p" to "ප",
        "b" to "බ", "m" to "ම", "y" to "ය", "r" to "ර",
        "l" to "ල", "v" to "ව", "w" to "ව", "s" to "ස",
        "h" to "හ", "f" to "ෆ", "z" to "ස", "x" to "ක්ස්",
        "q" to "ක"
    )

    private val CONSONANT_KEYS: List<String> =
        CONSONANTS.keys.sortedByDescending { it.length }

    private val VOWEL_SIGNS: LinkedHashMap<String, String> = linkedMapOf(
        "aa" to "ා", "A" to "ා",
        "aE" to "ැ", "ae" to "ැ",
        "aEE" to "ෑ", "aee" to "ෑ",
        "ii" to "ී", "I" to "ී",
        "i" to "ි",
        "ee" to "ී",
        "uu" to "ූ", "U" to "ූ",
        "oo" to "ූ",
        "u" to "ු",
        "E" to "ේ",
        "e" to "ෙ",
        "O" to "ෝ",
        "o" to "ො",
        "ai" to "ෙයි",
        "au" to "ෞ"
    )

    private val VOWEL_KEYS: List<String> = VOWEL_SIGNS.keys.sortedByDescending { it.length }

    private val INDEPENDENT_VOWELS: LinkedHashMap<String, String> = linkedMapOf(
        "aa" to "ආ", "A" to "ආ",
        "aE" to "ඇ", "ae" to "ඇ",
        "aEE" to "ඈ", "aee" to "ඈ",
        "ii" to "ඊ", "I" to "ඊ",
        "i" to "ඉ",
        "uu" to "ඌ", "U" to "ඌ",
        "u" to "උ",
        "E" to "ඒ",
        "e" to "එ",
        "O" to "ඕ",
        "o" to "ඔ",
        "ai" to "ඓ",
        "au" to "ඖ",
        "a" to "අ"
    )

    private val INDEPENDENT_VOWEL_KEYS: List<String> =
        INDEPENDENT_VOWELS.keys.sortedByDescending { it.length }

    private const val YANSAYA = "්‍ය"
    private const val RAKARANSAYA = "්‍ර"
    private const val HAL_KIRIMA = "්"
    private const val ANUSVARA = "ං"

    private val COMMON_WORDS: Map<String, String> = mapOf(
        "ayubowan" to "ආයුබෝවන්", "suba" to "සුබ", "udasanak" to "උදෑසනක්",
        "udesanak" to "උදෑසනක්", "amma" to "අම්මා", "thaththa" to "තාත්තා",
        "thatha" to "තාත්තා", "lanka" to "ලංකා", "sinhala" to "සිංහල",
        "sthuthiyi" to "ස්තූතියි", "sthutiyi" to "ස්තූතියි", "kohomada" to "කොහොමද",
        "kohomadha" to "කොහොමද", "mama" to "මම", "oyaa" to "ඔයා", "oya" to "ඔයා",
        "api" to "අපි", "eyaa" to "එයා", "eya" to "එයා", "mokakda" to "මොකක්ද",
        "monawada" to "මොනවද", "hodai" to "හොඳයි", "hondai" to "හොඳයි",
        "narakai" to "නරකයි", "namaskara" to "නමස්කාර", "sriyai" to "ශ්‍රීයයි",
        "sri" to "ශ්‍රී", "lankawa" to "ලංකාව", "wadak" to "වැඩක්",
        "wadaa" to "වැඩ", "sellam" to "සෙල්ලම්", "iskole" to "ඉස්කෝලේ",
        "gedara" to "ගෙදර", "gedhara" to "ගෙදර", "kaema" to "කෑම",
        "bath" to "බත්", "watura" to "වතුර", "vathura" to "වතුර",
        "hitha" to "හිත", "hithanawa" to "හිතනවා", "yanawa" to "යනවා",
        "enawa" to "එනවා", "karanawa" to "කරනවා", "puluwan" to "පුළුවන්",
        "baha" to "බෑ", "ne" to "නෑ", "nedha" to "නැද්ද",
        "puthaa" to "පුතා", "duwa" to "දුව", "malli" to "මල්ලි",
        "akka" to "අක්කා", "aiya" to "අයියා", "seeya" to "සීයා",
        "aachchi" to "ආච්චි", "hitapan" to "හිටපන්", "enna" to "එන්න",
        "yanna" to "යන්න", "kiyanawa" to "කියනවා", "adha" to "අද",
        "heta" to "හෙට", "iiye" to "ඊයේ", "davasak" to "දවසක්",
        "subha" to "සුබ", "aluth" to "අලුත්", "avurudu" to "අවුරුදු",
        "puthu" to "පුතු", "wathura" to "වතුර", "gaha" to "ගහ",
        "gasa" to "ගස", "kolamba" to "කොළඹ", "rate" to "රටේ",
        "rata" to "රට", "deshaya" to "දේශය"
    )

    val QUICK_PHRASES: List<Pair<String, String>> = listOf(
        "Hello" to "ආයුබෝවන්!",
        "Thanks" to "ස්තූතියි!",
        "Good day" to "සුබ දවසක්!",
        "Good morning" to "සුබ උදෑසනක්!",
        "Good night" to "සුබ රාත්‍රියක්!",
        "How are you?" to "කොහොමද?",
        "I'm fine" to "මම හොඳින් ඉන්නවා",
        "See you" to "පසුව හම්බෙමු",
        "Congratulations" to "සුභ පැතුම්!",
        "Happy birthday" to "සුභ උපන්දිනයක්!"
    )

    fun transliterate(input: String): String {
        if (input.isEmpty()) return input
        return try {
            val sb = StringBuilder()
            var i = 0
            val wordBuf = StringBuilder()

            fun flushWord() {
                if (wordBuf.isNotEmpty()) {
                    sb.append(transliterateWord(wordBuf.toString()))
                    wordBuf.clear()
                }
            }

            while (i < input.length) {
                val ch = input[i]
                if (ch.isLetter()) {
                    wordBuf.append(ch)
                } else {
                    flushWord()
                    sb.append(ch)
                }
                i++
            }
            flushWord()
            sb.toString()
        } catch (_: Throwable) {
            input
        }
    }

    fun transliterateWord(word: String): String {
        if (word.isEmpty()) return word
        return try {
            COMMON_WORDS[word.lowercase()]?.let { return it }
            transliterateSyllables(word)
        } catch (_: Throwable) {
            word
        }
    }

    fun suggestionsFor(partial: String): List<String> {
        if (partial.isEmpty()) return emptyList()
        return try {
            val lower = partial.lowercase()
            val results = LinkedHashSet<String>()

            COMMON_WORDS[lower]?.let { results.add(it) }
            for ((key, value) in COMMON_WORDS) {
                if (results.size >= 3) break
                if (key.startsWith(lower) && key != lower) results.add(value)
            }
            results.add(transliterateSyllables(partial))
            results.take(3).toList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun transliterateSyllables(word: String): String {
        val out = StringBuilder()
        var i = 0

        while (i < word.length) {
            var matchedConsonant: String? = null
            for (key in CONSONANT_KEYS) {
                if (word.regionMatches(i, key, 0, key.length, ignoreCase = false)) {
                    matchedConsonant = key
                    break
                }
            }

            if (matchedConsonant != null) {
                val base = CONSONANTS[matchedConsonant] ?: ""
                var pos = i + matchedConsonant.length

                if (base.contains(HAL_KIRIMA)) {
                    out.append(base)
                    i = pos
                    continue
                }

                var glyph = base
                if (pos < word.length && word[pos] == 'y' && matchedConsonant != "y") {
                    glyph = base + YANSAYA
                    pos += 1
                } else if (pos < word.length && word[pos] == 'r' && matchedConsonant != "r" && matchedConsonant != "R") {
                    glyph = base + RAKARANSAYA
                    pos += 1
                }

                var matchedVowel: String? = null
                for (vKey in VOWEL_KEYS) {
                    if (word.regionMatches(pos, vKey, 0, vKey.length, ignoreCase = false)) {
                        matchedVowel = vKey
                        break
                    }
                }

                if (matchedVowel != null) {
                    out.append(glyph)
                    out.append(VOWEL_SIGNS[matchedVowel] ?: "")
                    pos += matchedVowel.length
                } else {
                    val isLastChar = pos >= word.length
                    val nextIsConsonantStart = pos < word.length && !isVowelStart(word, pos)
                    if (isLastChar || nextIsConsonantStart) {
                        out.append(glyph).append(HAL_KIRIMA)
                    } else {
                        out.append(glyph)
                    }
                }
                i = pos
                continue
            }

            var matchedIndepVowel: String? = null
            for (vKey in INDEPENDENT_VOWEL_KEYS) {
                if (word.regionMatches(i, vKey, 0, vKey.length, ignoreCase = false)) {
                    matchedIndepVowel = vKey
                    break
                }
            }
            if (matchedIndepVowel != null) {
                out.append(INDEPENDENT_VOWELS[matchedIndepVowel] ?: "")
                i += matchedIndepVowel.length
                continue
            }

            if (word[i] == 'n' && i + 1 < word.length && word[i + 1] == 'g' && i + 2 >= word.length) {
                out.append(ANUSVARA)
                i += 2
                continue
            }

            out.append(word[i])
            i++
        }
        return out.toString()
    }

    private fun isVowelStart(word: String, pos: Int): Boolean {
        if (pos >= word.length) return false
        val c = word[pos]
        return c in "aeiouAEIOU"
    }
}
