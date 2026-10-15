package br.com.coletafacil.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import br.com.coletafacil.SaveFeedback
import br.com.coletafacil.core.EntryRules
import br.com.coletafacil.data.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CollectionTitleDialog(collection: CollectionEntity?, onDismiss: () -> Unit, onSave: suspend (String, String) -> Boolean) {
    val draftId = rememberSaveable(collection?.id) { UUID.randomUUID().toString() }
    var title by rememberSaveable(collection?.id) { mutableStateOf(collection?.title.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (collection == null) "Nova coleta" else "Editar coleta") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Use um nome que ajude a identificar esta conferência.")
            OutlinedTextField(value = title, onValueChange = { if (it.length <= 100) { title = it; error = false } }, label = { Text("Nome da coleta") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth().testTag("collection-title"), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            if (error) Text("Não foi possível salvar. Confira o nome e tente novamente.", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        } },
        confirmButton = { Button(onClick = { scope.launch { saving = true; try { if (onSave(title, draftId)) onDismiss() else error = true } finally { saving = false } } }, enabled = !saving && title.isNotBlank()) { Text(if (saving) "Salvando…" else if (collection == null) "Criar coleta" else "Salvar nome") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemEditor(
    item: ItemEntity?, onDismiss: () -> Unit, onDelete: () -> Unit,
    onSave: suspend (String, String, String, Boolean, String) -> SaveFeedback,
    capturedCode: String? = null, existingItems: List<ItemEntity> = emptyList(),
    onOpenExisting: (ItemEntity) -> Unit = {},
) {
    val draftKey = item?.id ?: capturedCode ?: "manual"
    val draftId = rememberSaveable(draftKey) { UUID.randomUUID().toString() }
    var code by rememberSaveable(draftKey) { mutableStateOf(item?.code ?: capturedCode.orEmpty()) }
    var name by rememberSaveable(draftKey) { mutableStateOf(item?.name.orEmpty()) }
    var status by rememberSaveable(draftKey) { mutableStateOf(item?.status ?: KronosStatus.PENDING) }
    var acknowledgedCode by rememberSaveable(draftKey) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    var openAfterDiscard by remember { mutableStateOf<ItemEntity?>(null) }
    val scope = rememberCoroutineScope()
    val duplicates = if (item != null && code == item.code) emptyList() else existingItems.filter { it.id != item?.id && it.id != draftId && it.code == code }
    val allowDuplicate = acknowledgedCode == code && duplicates.isNotEmpty()
    val validation = EntryRules.validate(code, name, status, duplicates.size, allowDuplicate)
    val dirty = code != (item?.code ?: capturedCode.orEmpty()) || name != item?.name.orEmpty() || status != (item?.status ?: KronosStatus.PENDING)
    val close: () -> Unit = { if (!saving) { if (dirty) { openAfterDiscard = null; discard = true } else onDismiss() } }
    AlertDialog(
        onDismissRequest = close,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(if (item != null) "Editar item" else if (capturedCode != null) "Código identificado" else "Adicionar item") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (capturedCode != null) Text("Leitura pausada. Confira os dados e confirme este item para continuar.", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                OutlinedTextField(value = code, onValueChange = { code = it; acknowledgedCode = null; error = null }, modifier = Modifier.fillMaxWidth().testTag("editor-code"), label = { Text("Código") }, isError = submitted && code.isBlank(), minLines = 1, maxLines = 4, enabled = !saving, supportingText = { Text(if (submitted && code.isBlank()) "Informe o código." else "Código mantido como texto, incluindo zeros iniciais.") })
                if (duplicates.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.WarningAmber, null); Text("Código já presente · ${duplicates.size} registros", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Assertive }) }
                        Text(duplicates.first().name.ifBlank { "Item existente sem nome" })
                        Text("Nada foi adicionado novamente. Você pode editar o item existente ou registrar outra unidade.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { if (dirty) { openAfterDiscard = duplicates.first(); discard = true } else onOpenExisting(duplicates.first()) }, enabled = !saving) { Text("Abrir item existente") }
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !saving) { acknowledgedCode = if (allowDuplicate) null else code }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = allowDuplicate, onCheckedChange = { acknowledgedCode = if (it) code else null }, enabled = !saving, modifier = Modifier.testTag("duplicate-ack"))
                            Text("Registrar outra unidade deste código", modifier = Modifier.weight(1f))
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it; error = null }, modifier = Modifier.fillMaxWidth().testTag("editor-name"), label = { Text("Nome do item *") }, placeholder = { Text("Digite o nome do item") }, isError = submitted && name.isBlank(), supportingText = { Text(if (submitted && name.isBlank()) "Informe o nome para confirmar o item." else "* Campo obrigatório") }, minLines = 1, maxLines = 4, enabled = !saving, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                Text("Confere no Kronos?", style = MaterialTheme.typography.titleMedium)
                Text("Se ainda não conferiu, mantenha Pendente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KronosStatus.values.forEach { option -> FilterChip(selected = status == option, onClick = { status = option; error = null }, label = { Text(option) }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) }
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                if (item != null) {
                    Text("As alterações serão gravadas ao tocar em Salvar alterações.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onDelete, enabled = !saving) { Text("Excluir item", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { Button(onClick = {
            submitted = true
            if (validation != EntryRules.Result.OK) error = validation.message
            else scope.launch {
                saving = true
                try { val result = onSave(code, name, status, allowDuplicate, draftId); if (result.success) onDismiss() else error = result.message }
                finally { saving = false }
            }
        }, enabled = !saving && (duplicates.isEmpty() || allowDuplicate), modifier = Modifier.testTag("editor-save")) { Text(if (saving) "Salvando…" else if (item != null) "Salvar alterações" else if (allowDuplicate) "Confirmar outra unidade" else "Confirmar item") } },
        dismissButton = { TextButton(onClick = close, enabled = !saving) { Text("Cancelar") } },
    )
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Descartar alterações?") }, text = { Text("Os dados deste formulário ainda não foram gravados. Os itens confirmados continuam salvos.") }, confirmButton = { TextButton(onClick = { val target = openAfterDiscard; discard = false; if (target == null) onDismiss() else onOpenExisting(target) }) { Text("Descartar") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Continuar preenchendo") } })
}

@Composable
fun DeleteConfirmation(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = { TextButton(onClick = onConfirm) { Text("Excluir", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Manter") } })
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Como usar o Coleta Fácil") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("1. Crie uma coleta e abra Escanear.")
            Text("2. A leitura vibra e pausa. Digite o nome, informe a conferência e toque em Confirmar item. Cancelar não cria um registro.")
            Text("3. Se o código já estiver na coleta, abra o item existente. Para outra unidade física, marque Registrar outra unidade e confirme. Cada unidade ocupa uma linha.")
            Text("4. Os contadores mostram registros confirmados e códigos únicos. Um mesmo código pode ter várias unidades registradas.")
            Text("5. Use o menu de três pontos para editar ou excluir coletas e itens. Após excluir, Desfazer restaura os dados enquanto a mensagem estiver disponível.")
            Text("6. Exporte em XLSX ou CSV. Alterar itens de uma coleta finalizada reabre a coleta; arquivos já exportados não são alterados.")
            Text("O código fica em texto. A conferência no Kronos é manual. A vibração depende do suporte e das configurações do aparelho.")
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Entendi") } })
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
