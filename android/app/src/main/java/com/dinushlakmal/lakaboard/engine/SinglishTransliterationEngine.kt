package com.dinushlakmal.lakaboard.engine

/**
 * SinglishTransliterationEngine
 * ---------------------------------------------------------------------
 * Rule-based, syllable-driven Singlish (romanized Sinhala) -> Sinhala
 * Unicode transliteration engine, with a common-word dictionary layer
 * for high-frequency accuracy and a fallback grapheme parser for
 * everything else.
 *
 * The algorithm mirrors the logic used by the web simulator
 * (web/src/App.tsx -> transliterateSinglish) so behaviour stays
 * identical between the IME and the preview.
 *
 * Design:
 *  1. Split input into words on whitespace / punctuation boundaries.
 *  2. Look each word up (case-insensitive) in [COMMON_WORDS]. If found,
 *     use the curated mapping (covers irregular / high frequency words
 *     that the generic rules render imperfectly).
 *  3. Otherwise, run the word through [transliterateWord], a greedy
 *     longest-match consonant+vowel syllable parser.
 *  4. Live candidates are produced for the *current, uncommitted* word
 *     so the suggestion bar can show progressive results while typing.
 */
object SinglishTransliterationEngine {

    // ---------------------------------------------------------------
    // 1. Consonant table (base glyph carries the inherent vowel 'a')
    //    Ordered so longer keys are matched first by [CONSONANT_KEYS].
    // ---------------------------------------------------------------
    private val CONSONANTS: LinkedHashMap<String, String> = linkedMapOf(
        // Prenasalized / bandi akuru (must precede plain n/m matches)
        "ndh" to "ඳ", "nd" to "ඬ", "mb" to "ඹ", "ngg" to "ඟ", "ng" to "ඞ්ග",
        // Aspirated / digraphs (must precede single-letter matches)
        "kh" to "ඛ", "gh" to "ඝ", "chh" to "ඡ", "ch" to "ච", "jh" to "ඣ",
        "th" to "ත", "dh" to "ද", "ph" to "ඵ", "bh" to "භ",
        "sh" to "ශ", "Sh" to "ෂ", "SH" to "ෂ", "gn" to "ඤ", "ny" to "ඤ",
        // Retroflex (capitalised singlish convention)
        "T" to "ට", "D" to "ඩ", "N" to "ණ", "L" to "ළ", "R" to "ර",
        // Base consonants
        "k" to "ක", "g" to "ග", "c" to "ච", "j" to "ජ",
        "t" to "ට", "d" to "ඩ", "n" to "න", "p" to "ප",
        "b" to "බ", "m" to "ම", "y" to "ය", "r" to "ර",
        "l" to "ල", "v" to "ව", "w" to "ව", "s" to "ස",
        "h" to "හ", "f" to "ෆ", "z" to "ස", "x" to "ක්ස්",
        "q" to "ක"
    )

    // Longest-match-first ordering of consonant keys.
    private val CONSONANT_KEYS: List<String> =
        CONSONANTS.keys.sortedByDescending { it.length }

    // ---------------------------------------------------------------
    // 2. Vowel sign table (applied AFTER a consonant, replacing the
    //    inherent 'a'). Empty string = keep inherent 'a' (alapilla).
    // ---------------------------------------------------------------
    private val VOWEL_SIGNS: LinkedHashMap<String, String> = linkedMapOf(
        "aa" to "ා", "A" to "ා",           // kaa  -> කා (alapilla long)
        "aE" to "ැ", "ae" to "ැ",           // (kael) short æ
        "aEE" to "ෑ", "aee" to "ෑ",         // long æ:
        "ii" to "ී", "I" to "ී",
        "i" to "ි",
        "ee" to "ී",
        "uu" to "ූ", "U" to "ූ",
        "oo" to "ූ",
        "u" to "ු",
        "E" to "ේ",                        // kE -> කේ
        "e" to "ෙ",                        // ke -> කෙ
        "O" to "ෝ",                        // kO -> කෝ
        "o" to "ො",                        // ko -> කො
        "ai" to "ෙයි",
        "au" to "ෞ"
    )

    private val VOWEL_KEYS: List<String> = VOWEL_SIGNS.keys.sortedByDescending { it.length }

    // Independent vowels (word-initial / standalone, no preceding consonant)
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

    // Yansaya (්‍ය) / Rakaransaya (්‍ර) modifiers, e.g. "kya" -> ක්‍ය, "kra" -> ක්‍ර
    private const val YANSAYA = "්‍ය"
    private const val RAKARANSAYA = "්‍ර"
    private const val HAL_KIRIMA = "්"
    private const val ANUSVARA = "ං"      // trailing "ng"/"m" nasal, e.g. lanka -> ලංකා
    private const val VISARGA = "ඃ"

    // ---------------------------------------------------------------
    // 3. Curated high-frequency dictionary (overrides generic rules
    //    for common / irregular words so everyday typing feels right)
    // ---------------------------------------------------------------
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

    // Quick-access phrase chips for the Greetings mode.
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

    /**
     * Transliterate a full input buffer (may contain multiple words /
     * trailing punctuation). Preserves whitespace and punctuation.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return input
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
        return sb.toString()
    }

    /** Transliterate a single word (dictionary first, then algorithmic). */
    fun transliterateWord(word: String): String {
        if (word.isEmpty()) return word
        COMMON_WORDS[word.lowercase()]?.let { return it }
        return transliterateSyllables(word)
    }

    /**
     * Live suggestion candidates for a partially-typed word, used to
     * populate the smart suggestion bar while the user is still typing.
     * Returns up to 3 candidates: dictionary exact/prefix matches first,
     * then the algorithmic best-effort render.
     */
    fun suggestionsFor(partial: String): List<String> {
        if (partial.isEmpty()) return emptyList()
        val lower = partial.lowercase()
        val results = LinkedHashSet<String>()

        COMMON_WORDS[lower]?.let { results.add(it) }
        for ((key, value) in COMMON_WORDS) {
            if (results.size >= 3) break
            if (key.startsWith(lower) && key != lower) results.add(value)
        }
        results.add(transliterateSyllables(partial))
        return results.take(3).toList()
    }

    // ---------------------------------------------------------------
    // Core syllable parser
    // ---------------------------------------------------------------
    private fun transliterateSyllables(word: String): String {
        val out = StringBuilder()
        var i = 0
        var atWordStart = true

        while (i < word.length) {
            // Trailing anusvara: "...ng" / "...ank" style nasal endings
            // handled inline via consonant table ("ng" digraph above).

            var matchedConsonant: String? = null
            for (key in CONSONANT_KEYS) {
                if (word.regionMatches(i, key, 0, key.length, ignoreCase = false)) {
                    matchedConsonant = key
                    break
                }
            }

            if (matchedConsonant != null) {
                val base = CONSONANTS.getValue(matchedConsonant)
                var pos = i + matchedConsonant.length

                // Special multi-glyph result from digraph (e.g. "ng" -> ඞ්ග)
                if (base.contains(HAL_KIRIMA)) {
                    out.append(base)
                    i = pos
                    atWordStart = false
                    continue
                }

                // Yansaya / Rakaransaya modifiers: kya -> ක්‍ය, kra -> ක්‍ර
                var glyph = base
                if (pos < word.length && word[pos] == 'y' &&
                    matchedConsonant != "y"
                ) {
                    glyph = base + YANSAYA
                    pos += 1
                } else if (pos + 1 < word.length && word[pos] == 'r' &&
                    matchedConsonant != "r" && matchedConsonant != "R"
                ) {
                    glyph = base + RAKARANSAYA
                    pos += 1
                }

                // Now match a vowel sign following the consonant
                var matchedVowel: String? = null
                for (vKey in VOWEL_KEYS) {
                    if (word.regionMatches(pos, vKey, 0, vKey.length, ignoreCase = false)) {
                        matchedVowel = vKey
                        break
                    }
                }

                if (matchedVowel != null) {
                    out.append(glyph.let {
                        // strip base to insert vowel sign correctly when yansaya/rakaransaya present
                        it
                    })
                    out.append(VOWEL_SIGNS.getValue(matchedVowel))
                    pos += matchedVowel.length
                } else {
                    // No vowel found: check for end-of-syllable hal kirima cases
                    val isLastChar = pos >= word.length
                    val nextIsConsonantStart = pos < word.length && !isVowelStart(word, pos)
                    if (isLastChar || nextIsConsonantStart) {
                        out.append(glyph).append(HAL_KIRIMA)
                    } else {
                        // inherent 'a' (alapilla)
                        out.append(glyph)
                    }
                }
                i = pos
                atWordStart = false
                continue
            }

            // No consonant matched -> try independent vowel (word start
            // or after another vowel)
            var matchedIndepVowel: String? = null
            for (vKey in INDEPENDENT_VOWEL_KEYS) {
                if (word.regionMatches(i, vKey, 0, vKey.length, ignoreCase = false)) {
                    matchedIndepVowel = vKey
                    break
                }
            }
            if (matchedIndepVowel != null) {
                out.append(INDEPENDENT_VOWELS.getValue(matchedIndepVowel))
                i += matchedIndepVowel.length
                atWordStart = false
                continue
            }

            // Trailing nasal "ng"/"m" at word end -> anusvara
            if (word[i] == 'n' && i + 1 < word.length && word[i + 1] == 'g' &&
                i + 2 >= word.length
            ) {
                out.append(ANUSVARA)
                i += 2
                continue
            }

            // Unknown character (digits, stray punctuation inside word) -> passthrough
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
