package br.com.coletafacil.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun ActionMenu(subject: String, description: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.MoreVert, description) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Editar $subject") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { expanded = false; onEdit() })
            DropdownMenuItem(text = { Text("Excluir $subject", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }, onClick = { expanded = false; onDelete() })
        }
    }
}
