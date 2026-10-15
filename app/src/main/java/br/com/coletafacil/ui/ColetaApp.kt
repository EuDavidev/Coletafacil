package br.com.coletafacil.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColetaApp(model: AppViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val busy by model.exporting.collectAsStateWithLifecycle()
    val selected = state.selected
    var page by rememberSaveable(state.selectedId) { mutableIntStateOf(0) }
    var newCollection by rememberSaveable { mutableStateOf(false) }
    var manual by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportDialog by rememberSaveable { mutableStateOf(false) }
    var exportId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(model) {
        model.messages.collect { message ->
            val result = snackbar.showSnackbar(message.text, actionLabel = if (message.undo != null) "Desfazer" else null, withDismissAction = true, duration = if (message.undo != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) message.undo?.let { model.restore(it) }
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
    BackHandler(enabled = state.selectedId != null && !busy) { if (page != 0) page = 0 else model.select(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(
            title = { Text(selected?.collection?.title ?: "Coleta Fácil", maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                if (state.selectedId != null) IconButton(onClick = { if (!busy) model.select(null) }) { Icon(Icons.Outlined.ArrowBack, "Voltar para coletas") }
                else Icon(Icons.Outlined.QrCodeScanner, null, Modifier.padding(start = 20.dp, end = 8.dp), tint = MaterialTheme.colorScheme.primary)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        ) },
        bottomBar = {
            if (selected != null) NavigationBar {
                val labels = listOf("Resumo", "Escanear", "Registros")
                val icons = listOf(Icons.Outlined.SpaceDashboard, Icons.Outlined.QrCodeScanner, Icons.Outlined.FactCheck)
                labels.forEachIndexed { index, label -> NavigationBarItem(selected = page == index, onClick = { if (!busy) page = index }, icon = { Icon(icons[index], null) }, label = { Text(label) }, enabled = !busy) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                selected == null -> HomeScreen(state.collections, onNew = { newCollection = true }, onOpen = { model.select(it) })
                else -> {
                    val onEdit: (br.com.coletafacil.data.ItemEntity) -> Unit = { item ->
                        if (selected.collection.closed) model.notify("Reabra a coleta no resumo para editar.") else { page = 2; editingId = item.id }
                    }
                    when (page) {
                        1 -> ScannerScreen(collectionId = selected.collection.id, total = selected.total, closed = selected.collection.closed, gate = model.scanGate(selected.collection.id),
                            onCode = { model.addCode(selected.collection.id, it) }, onReview = { page = 2 }, onManual = { page = 2; manual = true })
                        2 -> RecordsScreen(selected.collection.id, state.items, selected.collection.closed, onEdit, onManual = { manual = true }, onExport = { exportDialog = true })
                        else -> SummaryScreen(selected, state.items, onScan = { page = 1 }, onRecords = { page = 2 }, onExport = { exportDialog = true }, onReopen = { model.reopen(selected.collection.id) }, onEdit = onEdit)
                    }
                }
            }
        }
    }
    if (newCollection) NewCollectionDialog(onDismiss = { newCollection = false }, onCreate = { model.create(it); newCollection = false })
    val item = state.items.firstOrNull { it.id == editingId }
    if ((manual || item != null) && selected != null && !selected.collection.closed) {
        ItemEditor(item = if (manual) null else item,
            onDismiss = { manual = false; editingId = null },
            onDelete = { item?.let { model.delete(it) }; editingId = null },
            onSave = { code, name, status ->
                if (manual) model.addManual(selected.collection.id, code, name, status)
                else if (item != null) model.updateItem(item.id, code, name, status) else false
            },
        )
    }
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
