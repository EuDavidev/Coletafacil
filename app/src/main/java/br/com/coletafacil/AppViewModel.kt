package br.com.coletafacil

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import br.com.coletafacil.core.EntryRules
import br.com.coletafacil.core.ScanRecord
import br.com.coletafacil.core.ScanSessions
import br.com.coletafacil.core.SpreadsheetWriter
import br.com.coletafacil.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.io.Serializable

data class AppState(
    val collections: List<CollectionSummary> = emptyList(),
    val selectedId: String? = null,
    val items: List<ItemEntity> = emptyList(),
    val loading: Boolean = true,
) {
    val selected: CollectionSummary? get() = collections.firstOrNull { it.collection.id == selectedId }
}
data class DeletedCollection(val collection: CollectionEntity, val items: List<ItemEntity>)
data class UiMessage(val text: String, val undo: ItemEntity? = null, val undoCollection: DeletedCollection? = null)
data class PendingScan(val collectionId: String, val code: String) : Serializable
data class SaveFeedback(val success: Boolean, val message: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val database = ColetaDatabase.get(application)
    private val dao = database.dao()
    private val lock = Mutex()
    private val scanSessions = ScanSessions()
    fun scanGate(id: String) = scanSessions.forCollection(id)
    private val selection = saved.getStateFlow<String?>("selected", null)
    val pendingScan = saved.getStateFlow<PendingScan?>("pendingScan", null)
    private val messagesChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages = messagesChannel.receiveAsFlow()
    private val activeItems = selection.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeItems(id) }
    val state = combine(dao.observeCollections(), selection, activeItems) { collections, id, items ->
        AppState(collections, id, items.filter { it.collectionId == id }, false)
    }.catch {
        notify("Não foi possível abrir os dados. Feche e abra o aplicativo para tentar novamente.")
        emit(AppState(loading = false))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())
    val exporting = MutableStateFlow(false)

    fun select(id: String?) { saved["selected"] = id }
    fun notify(text: String) { messagesChannel.trySend(UiMessage(text)) }
    fun beginScan(collectionId: String, code: String): Boolean {
        if (code.isBlank() || pendingScan.value != null || exporting.value || selection.value != collectionId) return false
        saved["pendingScan"] = PendingScan(collectionId, code)
        return true
    }
    fun cancelScan() { saved["pendingScan"] = null }

    private fun mutation(block: suspend () -> Unit) = viewModelScope.launch {
        try { withContext(Dispatchers.IO) { lock.withLock { block() } } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { notify("Não foi possível salvar a alteração. Tente novamente.") }
    }

    suspend fun saveCollectionTitle(id: String?, title: String): Boolean {
        val value = title.trim()
        if (value.isBlank() || value.length > 100) return false
        return try {
            val selected = withContext(Dispatchers.IO) {
                lock.withLock {
                    if (id == null) {
                        val collection = CollectionEntity(title = value)
                        dao.insertCollection(collection)
                        collection.id
                    } else {
                        if (dao.collection(id) == null) return@withLock null
                        dao.renameCollection(id, value)
                        id
                    }
                }
            } ?: return false
            if (id == null) select(selected)
            notify(if (id == null) "Coleta criada." else "Nome da coleta atualizado.")
            true
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { false }
    }

    suspend fun submitItem(collectionId: String, itemId: String?, code: String, name: String, status: String, allowDuplicate: Boolean): SaveFeedback {
        return try {
            val result = withContext(Dispatchers.IO) {
                lock.withLock {
                    database.withTransaction {
                        val collection = dao.collection(collectionId)
                            ?: return@withTransaction SaveFeedback(false, "Esta coleta não está mais disponível.")
                        if (itemId == null && collection.closed) return@withTransaction SaveFeedback(false, "Reabra a coleta antes de adicionar itens.")
                        val current = itemId?.let { dao.item(it) }
                        if (itemId != null && current?.collectionId != collectionId) return@withTransaction SaveFeedback(false, "O item foi excluído. Volte à lista de registros.")
                        val duplicates = if (current?.code == code) 0 else dao.duplicates(collectionId, code, itemId.orEmpty()).size
                        val validation = EntryRules.validate(code, name, status, duplicates, allowDuplicate)
                        if (validation != EntryRules.Result.OK) return@withTransaction SaveFeedback(false, validation.message)
                        if (itemId == null) dao.insertItem(ItemEntity(collectionId = collectionId, code = code, name = name.trim(), status = status))
                        else { dao.updateItem(itemId, code, name.trim(), status); dao.setClosed(collectionId, false) }
                        val count = dao.itemsSnapshot(collectionId).size
                        SaveFeedback(true, if (itemId == null) "Item confirmado · $count registros na coleta." else "Item atualizado. A coleta está aberta para revisão.")
                    }
                }
            }
            if (result.success) result.message?.let { notify(it) }
            result
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { SaveFeedback(false, "Não foi possível salvar. Seus dados continuam neste formulário. Tente novamente.") }
    }

    fun delete(item: ItemEntity) = mutation {
        val current = dao.item(item.id) ?: return@mutation
        database.withTransaction { dao.deleteItem(current.id); dao.setClosed(current.collectionId, false) }
        messagesChannel.send(UiMessage("Item excluído.", current))
    }
    fun restore(item: ItemEntity) = mutation {
        database.withTransaction {
            if (dao.collection(item.collectionId) == null) return@withTransaction
            dao.insertItem(item); dao.setClosed(item.collectionId, false)
        }
    }
    fun deleteCollection(id: String) = mutation {
        val collection = dao.collection(id) ?: return@mutation
        val backup = DeletedCollection(collection, dao.itemsSnapshot(id))
        database.withTransaction { dao.deleteCollection(id) }
        withContext(Dispatchers.Main) {
            if (selection.value == id) select(null)
            if (pendingScan.value?.collectionId == id) cancelScan()
        }
        messagesChannel.send(UiMessage("Coleta e seus ${backup.items.size} itens excluídos.", undoCollection = backup))
    }
    fun restoreCollection(backup: DeletedCollection) = mutation {
        database.withTransaction { dao.insertCollection(backup.collection); dao.insertItems(backup.items) }
        notify("Coleta restaurada com ${backup.items.size} itens.")
    }
    fun reopen(id: String) = mutation { dao.setClosed(id, false); notify("Coleta reaberta.") }

    fun export(uri: Uri, id: String, xlsx: Boolean) {
        if (exporting.value) return
        exporting.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    lock.withLock {
                        if (dao.collection(id) == null) throw IOException("Coleta indisponível.")
                        val rows = dao.itemsSnapshot(id).map { ScanRecord(it.code, it.name, it.status) }
                        val resolver = getApplication<Application>().contentResolver
                        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Escolha outro destino.")
                        stream.use { if (xlsx) SpreadsheetWriter.writeXlsx(it, rows) else SpreadsheetWriter.writeCsv(it, rows) }
                        dao.setClosed(id, true)
                    }
                }
                notify("Planilha salva. A coleta continua disponível no app.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { notify("Exportação não concluída. ${e.localizedMessage.orEmpty()} Tente novamente.") }
            finally { exporting.value = false }
        }
    }
}
