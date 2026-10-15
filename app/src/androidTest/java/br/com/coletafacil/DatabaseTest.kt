package br.com.coletafacil

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.coletafacil.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @Test fun collectionRenameDeletionAndRestorePreserveItems() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ColetaDatabase::class.java).build()
        try {
            val dao = db.dao()
            val collection = CollectionEntity(id = "c", title = "Antes", createdAt = 1)
            val item = ItemEntity(id = "i", collectionId = "c", code = "0001", name = "Bomba", createdAt = 2)
            dao.insertCollection(collection); dao.insertItem(item)
            dao.renameCollection("c", "Depois")
            assertEquals("Depois", dao.collection("c")!!.title)
            assertEquals(1, dao.duplicates("c", "0001", "").size)
            assertTrue(dao.duplicates("c", "0001", "i").isEmpty())
            assertTrue(dao.duplicates("c", "1", "").isEmpty())
            val snapshot = dao.collection("c")!!
            dao.deleteCollection("c")
            assertNull(dao.collection("c")); assertTrue(dao.itemsSnapshot("c").isEmpty())
            dao.insertCollection(snapshot); dao.insertItems(listOf(item))
            assertEquals(listOf(item), dao.itemsSnapshot("c"))
        } finally { db.close() }
    }

    @Test fun exactFieldsArePersistedAndCompletionIsExplicit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ColetaDatabase::class.java).build()
        try {
            val dao = db.dao()
            dao.insertCollection(CollectionEntity(id = "c", title = "Setor A", createdAt = 1))
            dao.insertItem(ItemEntity(id = "i", collectionId = "c", code = "000123", createdAt = 2))
            assertEquals("Pendente", dao.itemsSnapshot("c").single().status)
            dao.updateItem("i", "000123", "Bomba d’água", "Sim")
            val saved = dao.itemsSnapshot("c").single()
            assertEquals("000123", saved.code)
            assertEquals("Bomba d’água", saved.name)
            assertEquals("Sim", saved.status)
            assertFalse(dao.collection("c")!!.closed)
            assertEquals(1, dao.observeCollections().first().single().total)
            dao.setClosed("c", true)
            assertTrue(dao.collection("c")!!.closed)
            dao.setClosed("c", false)
            assertFalse(dao.collection("c")!!.closed)
            dao.deleteItem("i")
            assertTrue(dao.itemsSnapshot("c").isEmpty())
            dao.insertItem(saved)
            assertEquals(saved, dao.itemsSnapshot("c").single())
        } finally { db.close() }
    }
}
