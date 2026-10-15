package br.com.coletafacil

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.coletafacil.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ScanWorkflowTest {
    @Test fun detectionIsDraftAndDuplicatesRequireConfirmation() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dao = ColetaDatabase.get(app).dao()
        val id = UUID.randomUUID().toString()
        dao.insertCollection(CollectionEntity(id = id, title = "Teste de confirmação"))
        lateinit var model: AppViewModel
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            instrumentation.runOnMainSync { model = AppViewModel(app, SavedStateHandle(mapOf("selected" to id))); assertTrue(model.beginScan(id, "0001")) }
            assertTrue(dao.itemsSnapshot(id).isEmpty())
            assertTrue(model.submitItem(id, null, "0001", "Bomba", "Pendente", false).success)
            assertEquals(1, dao.itemsSnapshot(id).size)
            instrumentation.runOnMainSync { model.cancelScan() }
            assertFalse(model.submitItem(id, null, "0001", "Bomba", "Sim", false).success)
            assertEquals(1, dao.itemsSnapshot(id).size)
            val requestId = UUID.randomUUID().toString()
            assertTrue(model.submitItem(id, null, "0001", "Outra bomba", "Não", true, requestId).success)
            // A recreated form retries the same request without adding another physical unit.
            assertTrue(model.submitItem(id, null, "0001", "Outra bomba", "Não", true, requestId).success)
            assertEquals(2, dao.itemsSnapshot(id).size)
            val collectionRequest = UUID.randomUUID().toString()
            try {
                assertTrue(model.saveCollectionTitle(null, "Coleta estável", collectionRequest))
                assertTrue(model.saveCollectionTitle(null, "Coleta estável", collectionRequest))
                assertNotNull(dao.collection(collectionRequest))
            } finally { dao.deleteCollection(collectionRequest) }
            val item = dao.itemsSnapshot(id).first()
            assertTrue(model.submitItem(id, item.id, "0001", "Nome corrigido", "Sim", false).success)
            assertEquals(2, dao.itemsSnapshot(id).size)
            model.deleteCollection(id).join()
            val undo = model.pendingUndo.value!!
            assertEquals(2, undo.message.undoCollection!!.items.size)
            assertEquals(undo.token, model.pendingUndo.value!!.token)
            model.restoreCollection(undo.message.undoCollection!!).join()
            model.consumeUndo(undo.token)
            assertNull(model.pendingUndo.value)
            assertEquals(2, dao.itemsSnapshot(id).size)
        } finally { dao.deleteCollection(id) }
    }
}
