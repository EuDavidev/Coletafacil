package br.com.coletafacil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.coletafacil.data.CollectionSummary
import br.com.coletafacil.data.CollectionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun dateLabel(time: Long): String = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time))

@Composable
fun HomeScreen(collections: List<CollectionSummary>, onNew: () -> Unit, onOpen: (String) -> Unit, onRename: (CollectionEntity) -> Unit, onDelete: (CollectionSummary) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.OfflineBolt, null, Modifier.size(20.dp)); Text("DISPONÍVEL SEM INTERNET", style = MaterialTheme.typography.labelMedium) }
                    Text("Cada código.\nTudo em ordem.", style = MaterialTheme.typography.headlineLarge)
                    Text("Leia, identifique e confira seus itens. Sua próxima planilha começa aqui.", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onNew, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Nova coleta") }
                }
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Suas coletas", style = MaterialTheme.typography.titleLarge); Text("${collections.size} coletas · ${collections.sumOf { it.total }} registros", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) } }
        if (collections.isEmpty()) item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Inventory2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Text("Tudo pronto para começar", style = MaterialTheme.typography.titleMedium)
                    Text("Crie uma coleta. Cada leitura pede o nome do item e sua conferência antes de salvar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(collections, key = { it.collection.id }) { summary ->
            Card(onClick = { onOpen(summary.collection.id) }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(summary.collection.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        ActionMenu("coleta", "Opções da coleta ${summary.collection.title}", onEdit = { onRename(summary.collection) }, onDelete = { onDelete(summary) })
                    }
                    Text(dateLabel(summary.collection.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${summary.total} registros · ${summary.pending} pendentes", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(if (summary.collection.closed) Icons.Outlined.CheckCircle else Icons.Outlined.EditNote, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if (summary.collection.closed) "Finalizada · disponível para exportar" else "Em andamento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
