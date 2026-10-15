package br.com.coletafacil

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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

data class AppState(
    val collections: List<CollectionSummary> = emptyList(),
    val selectedId: String? = null,
    val items: List<ItemEntity> = emptyList(),
    val loading: Boolean = true,
) {
    val selected: CollectionSummary? get() = collections.firstOrNull { it.collection.id == selectedId }
}
data class UiMessage(val text: String, val undo: ItemEntity? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val dao = ColetaDatabase.get(application).dao()
    private val lock = Mutex()
    private val scanSessions = ScanSessions()
    fun scanGate(id: String) = scanSessions.forCollection(id)
    private val selection = saved.getStateFlow<String?>("selected", null)
    private val messagesChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages = messagesChannel.receiveAsFlow()
    private val activeItems = selection.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.observeItems(id)
    }
    val state = combine(dao.observeCollections(), selection, activeItems) { collections, id, items ->
        AppState(collections, id, items.filter { it.collectionId == id }, false)
    }.catch { e ->
        notify("Não foi possível abrir os dados. ${e.localizedMessage.orEmpty()}")
        emit(AppState(loading = false))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())
    val exporting = MutableStateFlow(false)

    fun select(id: String?) { saved["selected"] = id }
    fun notify(text: String) { messagesChannel.trySend(UiMessage(text)) }

    private fun mutation(block: suspend () -> Unit) = viewModelScope.launch {
        try { withContext(Dispatchers.IO) { lock.withLock { block() } } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { notify("Não foi possível salvar. ${e.localizedMessage.orEmpty()}") }
    }

    fun create(title: String) = mutation {
        val collection = CollectionEntity(title = title.trim().ifBlank { "Nova coleta" })
        dao.insertCollection(collection)
        withContext(Dispatchers.Main) { select(collection.id) }
    }

    suspend fun addCode(collectionId: String, raw: String): Boolean {
        if (raw.isBlank()) return false
        return try {
            // Finish an already recognized scan even if its preview is disposed.
            // Updating the gate beside the committed insert avoids reoffering a
            // persisted code when cancellation prevents a camera acknowledgement.
            withContext(NonCancellable + Dispatchers.IO) {
                lock.withLock {
                    val collection = dao.collection(collectionId)
                    if (collection == null || collection.closed) false
                    else {
                        dao.insertItem(ItemEntity(collectionId = collectionId, code = raw))
                        scanSessions.forCollection(collectionId).accepted(raw)
                        true
                    }
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { notify("Leitura não salva. Pause e tente novamente."); false }
    }

    suspend fun updateItem(id: String, code: String, name: String, status: String): Boolean {
        if (code.isBlank() || status !in KronosStatus.values) return false
        return try {
            withContext(Dispatchers.IO) {
                lock.withLock {
                    val item = dao.item(id) ?: return@withLock false
                    if (dao.collection(item.collectionId)?.closed != false) return@withLock false
                    dao.updateItem(id, code, name, status)
                    true
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { notify("Não foi possível salvar o registro. Tente novamente."); false }
    }

    fun delete(item: ItemEntity) = mutation {
        if (dao.collection(item.collectionId)?.closed != false) return@mutation
        dao.deleteItem(item.id)
        messagesChannel.send(UiMessage("Registro excluído", item))
    }

    fun restore(item: ItemEntity) = mutation {
        // Undo is permitted even if the user has since exported: restore and reopen.
        dao.insertItem(item)
        dao.setClosed(item.collectionId, false)
    }

    fun reopen(id: String) = mutation { dao.setClosed(id, false) }

    suspend fun addManual(collectionId: String, code: String, name: String, status: String): Boolean {
        if (code.isBlank() || status !in KronosStatus.values) return false
        return try {
            withContext(Dispatchers.IO) {
                lock.withLock {
                    if (dao.collection(collectionId)?.closed != false) false
                    else { dao.insertItem(ItemEntity(collectionId = collectionId, code = code, name = name, status = status)); true }
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { notify("Não foi possível adicionar o registro. Tente novamente."); false }
    }

    // Never start an export on picker launch. Cancelled picker = no state change.
    fun export(uri: Uri, id: String, xlsx: Boolean) {
        if (exporting.value) return
        exporting.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    lock.withLock {
                        val rows = dao.itemsSnapshot(id).map { ScanRecord(it.code, it.name, it.status) }
                        val resolver = getApplication<Application>().contentResolver
                        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Destino indisponível.")
                        stream.use { if (xlsx) SpreadsheetWriter.writeXlsx(it, rows) else SpreadsheetWriter.writeCsv(it, rows) }
                        dao.setClosed(id, true)
                    }
                }
                notify("Planilha salva. A coleta continua disponível no app.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { notify("Exportação não concluída. ${e.localizedMessage.orEmpty()} Você pode tentar novamente.") }
            finally { exporting.value = false }
        }
    }
}
