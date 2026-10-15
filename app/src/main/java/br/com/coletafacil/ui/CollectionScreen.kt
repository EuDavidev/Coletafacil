package br.com.coletafacil.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import br.com.coletafacil.data.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryScreen(summary: CollectionSummary, items: List<ItemEntity>, onScan: () -> Unit, onRecords: () -> Unit, onExport: () -> Unit, onReopen: () -> Unit, onEdit: (ItemEntity) -> Unit, onDelete: (ItemEntity) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (summary.collection.closed) "COLETA FINALIZADA" else "COLETA EM ANDAMENTO", style = MaterialTheme.typography.labelLarge)
                    Text(summary.total.toString(), style = MaterialTheme.typography.headlineLarge)
                    Text("registros nesta coleta", style = MaterialTheme.typography.titleMedium)
                    Text("${items.map { it.code }.distinct().size} códigos únicos · ${items.size - items.map { it.code }.distinct().size} unidades adicionais", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onRecords, label = { Text("${summary.unnamed} sem nome") }, leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)) })
                        AssistChip(onClick = onRecords, label = { Text("${summary.pending} pendentes") }, leadingIcon = { Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp)) })
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (summary.collection.closed) "Pronta para continuar?" else "O próximo passo é seu", style = MaterialTheme.typography.titleMedium)
                    Text(if (summary.collection.closed) "Sua coleta está salva. Você pode exportar novamente ou reabrir para corrigir e adicionar itens." else "Escaneie novos códigos ou complete os nomes e a conferência no Kronos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (summary.collection.closed) OutlinedButton(onClick = onReopen, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Outlined.EditNote, null); Spacer(Modifier.width(8.dp)); Text("Reabrir coleta") }
                    else Button(onClick = onScan, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Outlined.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("Escanear códigos") }
                    OutlinedButton(onClick = onRecords, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Outlined.FactCheck, null); Spacer(Modifier.width(8.dp)); Text("Conferir registros") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conferência no Kronos", style = MaterialTheme.typography.titleMedium)
                Text("${items.count { it.status == "Sim" }} conferem · ${items.count { it.status == "Não" }} não conferem · ${summary.pending} pendentes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { if (summary.total == 0) 0f else (summary.total - summary.pending).toFloat() / summary.total }, modifier = Modifier.fillMaxWidth().height(8.dp))
                Text("Resultado informado manualmente por você.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Button(onClick = onExport, enabled = summary.total > 0, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Outlined.FileDownload, null); Spacer(Modifier.width(8.dp)); Text(if (summary.collection.closed) "Exportar novamente" else "Finalizar e exportar") }
        }
        if (items.isNotEmpty()) {
            item { Text("Últimos registros", style = MaterialTheme.typography.titleLarge) }
            items(items.take(3), key = { it.id }) { item -> ItemCard(item, onClick = { onEdit(item) }, onDelete = { onDelete(item) }) }
        }
    }
}

@Composable
fun RecordsScreen(collectionId: String, records: List<ItemEntity>, closed: Boolean, onEdit: (ItemEntity) -> Unit, onDelete: (ItemEntity) -> Unit, onManual: () -> Unit, onExport: () -> Unit) {
    var search by rememberSaveable(collectionId) { mutableStateOf("") }
    var filter by rememberSaveable(collectionId) { mutableStateOf("Todos") }
    val quantities = records.groupingBy { it.code }.eachCount()
    val filtered = records.filter { item ->
        (item.code.contains(search, true) || item.name.contains(search, true)) && when (filter) {
            "Sem nome" -> item.name.isBlank()
            "Pendentes" -> item.status == KronosStatus.PENDING
            "Não confere" -> item.status == "Não"
            "Repetidos" -> (quantities[item.code] ?: 0) > 1
            else -> true
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Cada item no seu lugar", style = MaterialTheme.typography.headlineSmall) }
        item { OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Buscar código ou nome") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Sem nome", "Pendentes", "Não confere", "Repetidos").forEach { option -> FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) }, modifier = Modifier.heightIn(min = 48.dp)) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${filtered.size} de ${records.size} registros · ${quantities.size} códigos únicos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!closed) OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar manualmente") }
                else Text("Editar ou excluir um item reabre esta coleta para revisão.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (filtered.isEmpty()) item {
            Card(shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.Inventory2, null, tint = MaterialTheme.colorScheme.primary); Text(if (records.isEmpty()) "Ainda sem registros" else "Nenhum item neste filtro", style = MaterialTheme.typography.titleMedium); Text(if (records.isEmpty()) "Escaneie um código ou adicione o primeiro item manualmente." else "Tente outro nome, código ou filtro.") } }
        }
        items(filtered, key = { it.id }) { item -> ItemCard(item, onClick = { onEdit(item) }, onDelete = { onDelete(item) }, quantity = quantities[item.code] ?: 1) }
        if (records.isNotEmpty()) item { OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Outlined.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Exportar coleta completa") } }
    }
}

@Composable
fun ItemCard(item: ItemEntity, onClick: () -> Unit, onDelete: () -> Unit, quantity: Int = 1) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name.ifBlank { "Nome a preencher" }, style = MaterialTheme.typography.titleMedium, color = if (item.name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                ActionMenu("item", "Opções do item ${item.name.ifBlank { item.code }}", onEdit = onClick, onDelete = onDelete)
            }
            Text(item.code, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            StatusBadge(item.status)
            if (quantity > 1) Text("$quantity registros com este código nesta coleta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val container = when (status) { "Sim" -> MaterialTheme.colorScheme.primaryContainer; "Não" -> MaterialTheme.colorScheme.errorContainer; else -> MaterialTheme.colorScheme.secondaryContainer }
    val foreground = when (status) { "Sim" -> MaterialTheme.colorScheme.onPrimaryContainer; "Não" -> MaterialTheme.colorScheme.onErrorContainer; else -> MaterialTheme.colorScheme.onSecondaryContainer }
    val icon = when (status) { "Sim" -> Icons.Outlined.CheckCircle; "Não" -> Icons.Outlined.HighlightOff; else -> Icons.Outlined.Schedule }
    Surface(color = container, contentColor = foreground, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(16.dp))
            Text(when (status) { "Sim" -> "Confere no Kronos"; "Não" -> "Não confere no Kronos"; else -> "Conferência pendente" }, style = MaterialTheme.typography.labelMedium)
        }
    }
}
