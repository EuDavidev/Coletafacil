package br.com.coletafacil

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.com.coletafacil.ui.ColetaTheme
import br.com.coletafacil.ui.ItemEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manualEntryReturnsExactThreeFields() {
        var saved: List<String>? = null
        compose.setContent {
            ColetaTheme {
                ItemEditor(item = null, onDismiss = {}, onDelete = {}, onSave = { code, name, status ->
                    saved = listOf(code, name, status); true
                })
            }
        }
        compose.onNodeWithTag("editor-code").performTextInput("000123")
        compose.onNodeWithTag("editor-name").performTextInput("Bomba do setor A")
        compose.onNodeWithText("Sim").performClick()
        compose.onNodeWithTag("editor-save").performClick()
        compose.waitUntil { saved != null }
        assertEquals(listOf("000123", "Bomba do setor A", "Sim"), saved)
    }

    @Test fun newItemKeepsPendingWithoutClaimingKronosVerification() {
        var saved: List<String>? = null
        compose.setContent {
            ColetaTheme {
                ItemEditor(item = null, onDismiss = {}, onDelete = {}, onSave = { code, name, status ->
                    saved = listOf(code, name, status); true
                })
            }
        }
        compose.onNodeWithTag("editor-code").performTextInput("QR-0001")
        compose.onNodeWithTag("editor-save").performClick()
        compose.waitUntil { saved != null }
        assertEquals(listOf("QR-0001", "", "Pendente"), saved)
    }
}
