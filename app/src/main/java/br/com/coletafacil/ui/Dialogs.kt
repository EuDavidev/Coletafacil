package br.com.coletafacil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.coletafacil.data.ItemEntity
import br.com.coletafacil.data.KronosStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NewCollectionDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Comece uma nova coleta") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Dê um nome para encontrar esta conferência depois.")
            OutlinedTextField(value = title, onValueChange = { if (it.length <= 100) title = it }, label = { Text("Nome da coleta") }, placeholder = { Text("Ex.: Conferência do almoxarifado") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        } },
        confirmButton = { Button(onClick = { onCreate(title) }, enabled = title.isNotBlank()) { Text("Criar coleta") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemEditor(item: ItemEntity?, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: suspend (String, String, String) -> Boolean) {
    var code by rememberSaveable(item?.id) { mutableStateOf(item?.code.orEmpty()) }
    var name by rememberSaveable(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var status by rememberSaveable(item?.id) { mutableStateOf(item?.status ?: KronosStatus.PENDING) }
    var savedFields by remember(item?.id) { mutableStateOf(listOf(item?.code.orEmpty(), item?.name.orEmpty(), item?.status ?: KronosStatus.PENDING)) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentSave by rememberUpdatedState(onSave)
    val fields = listOf(code, name, status)
    val dirty = fields != savedFields
    val saveAndClose: () -> Unit = {
        if (!saving) {
            if (code.isBlank()) error = true
            else scope.launch {
                saving = true
                try {
                    val snapshot = listOf(code,name,status)
                    if (currentSave(snapshot[0], snapshot[1], snapshot[2])) { savedFields = snapshot; onDismiss() }
                    else error = true
                } finally { saving = false }
            }
        }
    }
    // Existing records autosave after a brief typing pause. Closing flushes immediately.
    LaunchedEffect(code, name, status) {
        if (item != null && dirty && code.isNotBlank()) {
            delay(500)
            val snapshot = listOf(code, name, status)
            saving = true
            try {
                if (currentSave(snapshot[0], snapshot[1], snapshot[2])) { savedFields = snapshot; error = false }
                else error = true
            } finally { saving = false }
        }
    }
    val close: () -> Unit = {
        when {
            saving -> Unit
            item != null && dirty -> saveAndClose()
            item == null && dirty -> discard = true
            else -> onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (item == null) "Adicionar registro" else "Editar registro") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = code, onValueChange = { code = it; error = false }, modifier = Modifier.fillMaxWidth().testTag("editor-code"), label = { Text("Código") }, isError = code.isBlank() && error, minLines = 1, maxLines = 4, enabled = !saving, supportingText = { if (code.isBlank() && error) Text("Informe o código.") else Text("Zeros iniciais serão preservados no app e no XLSX.") })
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth().testTag("editor-name"), label = { Text("Nome") }, placeholder = { Text("Digite o nome do item") }, minLines = 1, maxLines = 4, enabled = !saving, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                Text("Confere no Kronos?", style = MaterialTheme.typography.titleMedium)
                Text("Consulte o Kronos e informe o resultado da conferência.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KronosStatus.values.forEach { option -> FilterChip(selected = status == option, onClick = { status = option }, label = { Text(option) }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) }
                }
                if (item != null) Text(if (saving) "Salvando alterações…" else if (error) "Não foi possível salvar. Tente novamente." else if (dirty) "Alterações aguardando salvamento" else "Alterações salvas no celular", style = MaterialTheme.typography.bodySmall, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                if (item == null && error && code.isNotBlank()) Text("Não foi possível salvar. Tente novamente.", color = MaterialTheme.colorScheme.error)
                if (item != null) TextButton(onClick = onDelete, enabled = !saving) { Text("Excluir registro", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(onClick = saveAndClose, enabled = !saving, modifier = Modifier.testTag("editor-save")) { Text(if (saving) "Salvando…" else if (item == null) "Adicionar" else "Concluído") } },
        dismissButton = { TextButton(onClick = { if (item == null && dirty) discard = true else close() }, enabled = !saving) { Text(if (item == null) "Cancelar" else "Fechar") } },
    )
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Descartar este rascunho?") }, text = { Text("O registro ainda não foi adicionado à coleta.") }, confirmButton = { TextButton(onClick = onDismiss) { Text("Descartar") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Continuar preenchendo") } })
}

@Composable
fun ExportDialog(total: Int, pending: Int, unnamed: Int, onDismiss: () -> Unit, onChoose: (Boolean) -> Unit) {
    var xlsx by rememberSaveable { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Sua coleta, em planilha") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("$total registros • 3 colunas")
            if (pending > 0 || unnamed > 0) Text("Há $unnamed registros sem nome e $pending com conferência pendente. Você pode exportar assim e continuar depois.")
            Card(onClick = { xlsx = true }, colors = CardDefaults.cardColors(containerColor = if (xlsx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(8.dp)) { RadioButton(selected = xlsx, onClick = { xlsx = true }); Column(Modifier.weight(1f).padding(top = 8.dp, end = 8.dp)) { Text("Excel · XLSX", style = MaterialTheme.typography.titleMedium); Text("Recomendado. Preserva os códigos como texto.", style = MaterialTheme.typography.bodySmall) } }
            }
            Card(onClick = { xlsx = false }, colors = CardDefaults.cardColors(containerColor = if (!xlsx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(8.dp)) { RadioButton(selected = !xlsx, onClick = { xlsx = false }); Column(Modifier.weight(1f).padding(top = 8.dp, end = 8.dp)) { Text("Texto · CSV", style = MaterialTheme.typography.titleMedium); Text("UTF-8, separado por ponto e vírgula. Ao importar no Excel, defina Código como texto.", style = MaterialTheme.typography.bodySmall) } }
            }
            Text("Escolha onde salvar na próxima tela. Sua coleta continuará disponível neste celular.", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { Button(onClick = { onChoose(xlsx) }) { Text("Escolher destino") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Revisar antes") } },
    )
}
