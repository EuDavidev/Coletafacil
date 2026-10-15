package br.com.coletafacil

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.com.coletafacil.data.ItemEntity
import br.com.coletafacil.ui.ColetaTheme
import br.com.coletafacil.ui.ItemEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manualEntryReturnsExactThreeFields() {
        var saved: List<String>? = null
        compose.setContent { ColetaTheme { ItemEditor(item = null, onDismiss = {}, onDelete = {}, onSave = { code, name, status, _, _ -> saved = listOf(code, name, status); SaveFeedback(true) }) } }
        compose.onNodeWithTag("editor-code").performTextInput("000123")
        compose.onNodeWithTag("editor-name").performTextInput("Bomba do setor A")
        compose.onNodeWithText("Sim").performScrollTo().performClick()
        compose.onNodeWithText("Sim").assertIsSelected()
        compose.onNodeWithTag("editor-save").performClick()
        compose.waitUntil { saved != null }
        assertEquals(listOf("000123", "Bomba do setor A", "Sim"), saved)
    }

    @Test fun newItemKeepsPendingWithoutClaimingKronosVerification() {
        var saved: List<String>? = null
        compose.setContent { ColetaTheme { ItemEditor(item = null, onDismiss = {}, onDelete = {}, onSave = { code, name, status, _, _ -> saved = listOf(code, name, status); SaveFeedback(true) }) } }
        compose.onNodeWithTag("editor-code").performTextInput("QR-0001")
        compose.onNodeWithTag("editor-save").performClick()
        compose.waitForIdle()
        assertEquals(null, saved)
        compose.onNodeWithTag("editor-name").performTextInput("Motor")
        compose.onNodeWithTag("editor-save").performClick()
        compose.waitUntil { saved != null }
        assertEquals(listOf("QR-0001", "Motor", "Pendente"), saved)
    }

    @Test fun editingWaitsForConfirmationAndSupportsCancel() {
        var saves = 0
        val item = ItemEntity(id = "i", collectionId = "c", code = "001", name = "Bomba")
        compose.setContent { ColetaTheme { ItemEditor(item = item, onDismiss = {}, onDelete = {}, onSave = { _, _, _, _, _ -> saves++; SaveFeedback(true) }) } }
        compose.onNodeWithTag("editor-name").performTextReplacement("Bomba corrigida")
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        assertEquals(0, saves)
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        assertEquals(0, saves)
        compose.onNodeWithText("Continuar preenchendo").performClick()
        compose.onNodeWithTag("editor-save").performClick()
        compose.waitUntil { saves == 1 }
    }

    @Test fun openingExistingProtectsTypedName() {
        var opened = false
        val old = ItemEntity(id = "i", collectionId = "c", code = "001", name = "Bomba")
        compose.setContent { ColetaTheme { ItemEditor(item = null, capturedCode = "001", existingItems = listOf(old), onDismiss = {}, onDelete = {}, onOpenExisting = { opened = true }, onSave = { _, _, _, _, _ -> SaveFeedback(true) }) } }
        compose.onNodeWithTag("editor-name").performTextInput("Nome ainda não salvo")
        compose.onNodeWithText("Abrir item existente").performScrollTo().performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        assertEquals(false, opened)
        compose.onNodeWithText("Continuar preenchendo").performClick()
        compose.onNodeWithTag("editor-name").assertTextContains("Nome ainda não salvo")
    }

    @Test fun duplicateNeedsExplicitSecondUnitAcknowledgement() {
        var acknowledged: Boolean? = null
        val old = ItemEntity(id = "i", collectionId = "c", code = "001", name = "Bomba")
        compose.setContent { ColetaTheme { ItemEditor(item = null, capturedCode = "001", existingItems = listOf(old), onDismiss = {}, onDelete = {}, onSave = { _, _, _, allow, _ -> acknowledged = allow; SaveFeedback(true) }) } }
        compose.onNodeWithTag("editor-save").assertIsNotEnabled()
        compose.onNodeWithTag("editor-name").performTextInput("Outra bomba")
        compose.onNodeWithTag("duplicate-ack").performClick()
        compose.onNodeWithTag("editor-save").performClick()
        compose.waitUntil { acknowledged != null }
        assertEquals(true, acknowledged)
    }
}
