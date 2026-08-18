package com.dinushlakmal.lakaboard

import com.dinushlakmal.lakaboard.engine.SinglishTransliterationEngine
import com.dinushlakmal.lakaboard.engine.WijesekaraEngine
import com.dinushlakmal.lakaboard.viewmodel.KeyboardMode
import com.dinushlakmal.lakaboard.viewmodel.KeyboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineUnitTest {

    @Test
    fun testCommonWordTransliteration() {
        val result = SinglishTransliterationEngine.transliterateWord("ayubowan")
        assertEquals("ආයුබෝවන්", result)
    }

    @Test
    fun testSyllableTransliteration() {
        val mama = SinglishTransliterationEngine.transliterateWord("mama")
        assertEquals("මම", mama)

        val lanka = SinglishTransliterationEngine.transliterateWord("lanka")
        assertEquals("ලංකා", lanka)
    }

    @Test
    fun testSuggestionsForPartial() {
        val suggestions = SinglishTransliterationEngine.suggestionsFor("ayu")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.contains("ආයුබෝවන්"))
    }

    @Test
    fun testWijesekaraMapping() {
        val k = WijesekaraEngine.map('k', shifted = false)
        assertEquals("ක", k)

        val upperK = WijesekaraEngine.map('k', shifted = true)
        assertEquals("ඛ", upperK)
    }

    @Test
    fun testViewModelStateTransitions() {
        val vm = KeyboardViewModel()
        var committedText = ""
        vm.onCommitText = { committedText += it }

        vm.switchMode(KeyboardMode.ENGLISH)
        vm.onCharacterKey('h')
        vm.onCharacterKey('i')
        assertEquals("hi", committedText)

        vm.switchMode(KeyboardMode.SINGLISH)
        vm.onCharacterKey('m')
        vm.onCharacterKey('a')
        vm.onSpace()
        assertNotNull(vm.uiState.value)
    }

    @Test
    fun testKeyDefinitionStructure() {
        val key = com.dinushlakmal.lakaboard.ime.KeyDefinition(primary = "q", secondary = "1")
        assertEquals("q", key.primary)
        assertEquals("1", key.secondary)
        assertEquals(1.0f, key.weight)
    }
}
