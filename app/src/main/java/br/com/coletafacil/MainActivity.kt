package br.com.coletafacil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.coletafacil.ui.ColetaApp
import br.com.coletafacil.ui.ColetaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ColetaTheme { val model: AppViewModel = viewModel(); ColetaApp(model) } }
    }
}
