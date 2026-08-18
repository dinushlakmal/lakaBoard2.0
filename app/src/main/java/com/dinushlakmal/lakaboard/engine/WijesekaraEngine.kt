package com.dinushlakmal.lakaboard.engine

/**
 * WijesekaraEngine
 * ---------------------------------------------------------------------
 * Direct key -> glyph mapping for the standard Wijesekara Sinhala
 * keyboard layout.
 */
object WijesekaraEngine {

    val LOWER: Map<Char, String> = mapOf(
        '1' to "1", '2' to "2", '3' to "3", '4' to "4", '5' to "5",
        '6' to "6", '7' to "7", '8' to "8", '9' to "9", '0' to "0",
        'q' to "ං", 'w' to "ඉ", 'e' to "ට", 'r' to "එ", 't' to "ර",
        'y' to "ත", 'u' to "ය", 'i' to "උ", 'o' to "ි", 'p' to "ප",
        'a' to "ෙ", 's' to "ස", 'd' to "ද", 'f' to "ෆ", 'g' to "ග",
        'h' to "හ", 'j' to "ජ", 'k' to "ක", 'l' to "ල",
        'z' to "ෘ", 'x' to "ං", 'c' to "ච", 'v' to "ව", 'b' to "බ",
        'n' to "න", 'm' to "ම", ',' to ",", '.' to "."
    )

    val UPPER: Map<Char, String> = mapOf(
        'q' to "ඃ", 'w' to "ඊ", 'e' to "ඨ", 'r' to "ඒ", 't' to "ණ",
        'y' to "ථ", 'u' to "ූ", 'i' to "ඌ", 'o' to "ී", 'p' to "ඵ",
        'a' to "ේ", 's' to "ෂ", 'd' to "ධ", 'f' to "ඩ", 'g' to "ඝ",
        'h' to "ඃ", 'j' to "ඣ", 'k' to "ඛ", 'l' to "ළ",
        'z' to "ෲ", 'x' to "඼", 'c' to "ඡ", 'v' to "ඝ", 'b' to "භ",
        'n' to "ඤ", 'm' to "ං", ',' to "<", '.' to ">"
    )

    const val HAL_KIRIMA = "්"
    const val YANSAYA = "්‍ය"
    const val RAKARANSAYA = "්‍ර"
    const val REPHAYA = "ර්‍"

    fun map(char: Char, shifted: Boolean): String {
        val table = if (shifted) UPPER else LOWER
        return table[char.lowercaseChar()] ?: char.toString()
    }
}
