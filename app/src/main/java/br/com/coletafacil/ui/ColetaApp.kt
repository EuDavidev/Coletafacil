package br.com.coletafacil.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.coletafacil.AppViewModel
import br.com.coletafacil.camera.ScannerScreen
import br.com.coletafacil.data.ItemEntity
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColetaApp(model: AppViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val busy by model.exporting.collectAsStateWithLifecycle()
    val pending by model.pendingScan.collectAsStateWithLifecycle()
    val selected = state.selected
    var page by rememberSaveable(state.selectedId) { mutableIntStateOf(0) }
    var newCollection by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var manual by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var help by rememberSaveable { mutableStateOf(false) }
    var exportDialog by rememberSaveable { mutableStateOf(false) }
    var exportId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val overlay = pending != null || manual || editingId != null || help || renameId != null || deleteCollectionId != null || deleteItemId != null || exportDialog || busy

    LaunchedEffect(model) {
        model.messages.collectLatest { message ->
            val undo = message.undo != null || message.undoCollection != null
            val result = snackbar.showSnackbar(message.text, actionLabel = if (undo) "Desfazer" else null, withDismissAction = true, duration = if (undo) SnackbarDuration.Long else SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) {
                message.undo?.let { model.restore(it) }
                message.undoCollection?.let { model.restoreCollection(it) }
            }
        }
    }
    val xlsxPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        val id = exportId; exportId = null
        if (uri != null && id != null) model.export(uri, id, true)
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val id = exportId; exportId = null
        if (uri != null && id != null) model.export(uri, id, false)
    }
    BackHandler(enabled = state.selectedId != null && !overlay) { if (page != 0) page = 0 else model.select(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(
            title = { Text(selected?.collection?.title ?: "Coleta Fácil", maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                if (state.selectedId != null) IconButton(onClick = { if (!overlay) model.select(null) }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar para coletas") }
                else Icon(Icons.Outlined.QrCodeScanner, null, Modifier.padding(start = 20.dp, end = 8.dp), tint = MaterialTheme.colorScheme.primary)
            },
            actions = {
                IconButton(onClick = { help = true }) { Icon(Icons.Outlined.HelpOutline, "Ajuda para usar o aplicativo") }
                if (selected != null) ActionMenu("coleta", "Opções da coleta ${selected.collection.title}", onEdit = { renameId = selected.collection.id }, onDelete = { deleteCollectionId = selected.collection.id })
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        ) },
        bottomBar = {
            if (selected != null) NavigationBar {
                val labels = listOf("Resumo", "Escanear", "Registros")
                val icons = listOf(Icons.Outlined.SpaceDashboard, Icons.Outlined.QrCodeScanner, Icons.Outlined.FactCheck)
                labels.forEachIndexed { index, label -> NavigationBarItem(selected = page == index, onClick = { if (!overlay) page = index }, icon = { Icon(icons[index], null) }, label = { Text(label) }, enabled = !overlay) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                selected == null -> HomeScreen(state.collections, onNew = { newCollection = true }, onOpen = { model.select(it) }, onRename = { renameId = it.id }, onDelete = { deleteCollectionId = it.collection.id })
                else -> {
                    val edit: (ItemEntity) -> Unit = { item -> page = 2; editingId = item.id }
                    val delete: (ItemEntity) -> Unit = { deleteItemId = it.id }
                    when (page) {
                        1 -> ScannerScreen(collectionId = selected.collection.id, total = selected.total, closed = selected.collection.closed, gate = model.scanGate(selected.collection.id), knownCodes = state.items.map { it.code }.toSet(), reviewing = overlay,
                            onCode = { if (overlay) false else model.beginScan(selected.collection.id, it) }, onReview = { page = 2 }, onManual = { manual = true })
                        2 -> RecordsScreen(selected.collection.id, state.items, selected.collection.closed, edit, delete, onManual = { manual = true }, onExport = { exportDialog = true })
                        else -> SummaryScreen(selected, state.items, onScan = { page = 1 }, onRecords = { page = 2 }, onExport = { exportDialog = true }, onReopen = { model.reopen(selected.collection.id) }, onEdit = edit, onDelete = delete)
                    }
                }
            }
        }
    }
    val renamed = state.collections.firstOrNull { it.collection.id == renameId }?.collection
    if (newCollection || renamed != null) CollectionTitleDialog(collection = if (newCollection) null else renamed,
        onDismiss = { newCollection = false; renameId = null },
        onSave = { model.saveCollectionTitle(if (newCollection) null else renamed?.id, it) })
    val item = state.items.firstOrNull { it.id == editingId }
    if ((manual || item != null || pending != null) && selected != null) {
        val scanned = pending
        ItemEditor(item = if (manual || scanned != null) null else item,
            capturedCode = scanned?.code, existingItems = state.items,
            onDismiss = { manual = false; editingId = null; model.cancelScan() },
            onDelete = { if (item != null) deleteItemId = item.id },
            onOpenExisting = { existing -> model.cancelScan(); manual = false; page = 2; editingId = existing.id },
            onSave = { code, name, status, allowDuplicate ->
                model.submitItem(scanned?.collectionId ?: selected.collection.id, if (manual || scanned != null) null else item?.id, code, name, status, allowDuplicate)
            },
        )
    }
    val deletedCollection = state.collections.firstOrNull { it.collection.id == deleteCollectionId }
    if (deletedCollection != null) DeleteConfirmation("Excluir coleta?", "A coleta “${deletedCollection.collection.title}” e seus ${deletedCollection.total} itens serão excluídos. Você poderá tocar em Desfazer na mensagem seguinte.", onDismiss = { deleteCollectionId = null }, onConfirm = { model.deleteCollection(deletedCollection.collection.id); deleteCollectionId = null })
    val deletedItem = state.items.firstOrNull { it.id == deleteItemId }
    if (deletedItem != null) DeleteConfirmation("Excluir item?", "Excluir “${deletedItem.name.ifBlank { deletedItem.code }}” (código ${deletedItem.code}) desta coleta? Você poderá desfazer na mensagem seguinte.", onDismiss = { deleteItemId = null }, onConfirm = { model.delete(deletedItem); deleteItemId = null; editingId = null })
    if (help) HelpDialog { help = false }
    if (exportDialog && selected != null) ExportDialog(selected.total, selected.pending, selected.unnamed, onDismiss = { exportDialog = false }, onChoose = { xlsx ->
        exportDialog = false; exportId = selected.collection.id
        val cleanTitle = selected.collection.title.replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").take(65).ifBlank { "coleta" }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val name = "${cleanTitle}_$timestamp.${if (xlsx) "xlsx" else "csv"}"
        try { if (xlsx) xlsxPicker.launch(name) else csvPicker.launch(name) }
        catch (_: Exception) { exportId = null; model.notify("Nenhum seletor de arquivos disponível neste aparelho.") }
    })
    if (busy) AlertDialog(onDismissRequest = {}, title = { Text("Salvando sua planilha") }, text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(Modifier.size(28.dp)); Text("Aguarde a conclusão da exportação.") } }, confirmButton = {})
}
