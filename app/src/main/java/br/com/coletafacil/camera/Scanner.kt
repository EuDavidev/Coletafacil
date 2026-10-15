package br.com.coletafacil.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.coletafacil.core.ScanGate
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen(
    collectionId: String, total: Int, closed: Boolean, gate: ScanGate,
    knownCodes: Set<String>, reviewing: Boolean,
    onCode: (String) -> Boolean, onReview: () -> Unit, onManual: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    fun permissionGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(permissionGranted()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var paused by rememberSaveable(collectionId) { mutableStateOf(false) }
    var lastCode by rememberSaveable(collectionId) { mutableStateOf("") }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var flash by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; asked = true }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) granted = permissionGranted() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Uma leitura. Uma confirmação.", style = MaterialTheme.typography.headlineSmall)
        Text("A câmera pausa em cada leitura. Confira o nome e o Kronos antes de confirmar o item.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(310.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF0F172A))) {
            when {
                closed -> CameraMessage("Coleta finalizada", "Reabra a coleta no resumo para adicionar registros.")
                !granted -> CameraMessage("Sua câmera, sua coleta", "Permita o acesso para ler QR Codes e códigos de barras.")
                cameraError != null -> CameraMessage("Câmera indisponível", cameraError!!)
                reviewing -> CameraMessage("Confira o item identificado", "A câmera está pausada até confirmar ou cancelar o formulário.")
                paused -> CameraMessage("Leitura pausada", "Seus registros estão salvos. Retome quando quiser.")
                else -> CameraPreview(
                    key = collectionId, gate = gate, torch = flash, onHasFlash = { hasFlash = it },
                    onFailure = { cameraError = "Use a entrada manual ou tente abrir a câmera novamente." },
                    onCode = { code ->
                        val recognized = onCode(code)
                        if (recognized) {
                            lastCode = code
                            val vibrator = context.getSystemService(Vibrator::class.java)
                            if (vibrator?.hasVibrator() == true) {
                                try {
                                    vibrator.vibrate(if (code in knownCodes) VibrationEffect.createWaveform(longArrayOf(0,35,70,35), -1) else VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
                                } catch (_: Exception) { /* Visible confirmation remains available. */ }
                            }
                        }
                        recognized
                    },
                )
            }
            if (granted && !paused && !closed && !reviewing && cameraError == null) {
                Canvas(Modifier.align(Alignment.Center).size(215.dp)) {
                    val stroke = 4.dp.toPx(); val length = 32.dp.toPx(); val c = Color(0xFF6EE7B7)
                    listOf(Offset.Zero, Offset(size.width,0f), Offset(0f,size.height), Offset(size.width,size.height)).forEach { corner ->
                        val dx = if (corner.x == 0f) 1 else -1; val dy = if (corner.y == 0f) 1 else -1
                        drawLine(c, corner, Offset(corner.x+dx*length,corner.y), stroke)
                        drawLine(c, corner, Offset(corner.x,corner.y+dy*length), stroke)
                    }
                }
                Surface(Modifier.align(Alignment.TopStart).padding(16.dp), color = Color(0xDD0F172A), shape = RoundedCornerShape(24.dp)) {
                    Text("CÂMERA ATIVA", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                if (hasFlash) FilledTonalIconButton(onClick = { flash = !flash }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    Icon(if (flash) Icons.Outlined.FlashOff else Icons.Outlined.FlashOn, if (flash) "Desligar lanterna" else "Ligar lanterna")
                }
            }
        }
        if (!granted && !closed) {
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Permitir câmera") }
            if (asked) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Abrir permissões do aplicativo") }
        }
        if (cameraError != null && !closed) OutlinedButton(onClick = { cameraError = null }) { Text("Tentar câmera novamente") }
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$total registros confirmados · ${knownCodes.size} códigos únicos", style = MaterialTheme.typography.titleMedium)
                Text(if (lastCode.isEmpty()) "O código identificado aparecerá no formulário de confirmação." else "Último código identificado: $lastCode", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, maxLines = 3)
            }
        }
        if (granted && !closed && cameraError == null) OutlinedButton(onClick = { paused = !paused; flash = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, null)
            Spacer(Modifier.width(8.dp)); Text(if (paused) "Retomar leitura" else "Pausar leitura")
        }
        Button(onClick = onReview, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Conferir registros") }
        if (!closed) TextButton(onClick = onManual, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Digitar código manualmente") }
        Text("Após conferir, retire o código do enquadramento. Repetições mostram um aviso antes de adicionar outra unidade.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CameraMessage(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.QrCodeScanner, null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodyMedium)
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(key: String, gate: ScanGate, torch: Boolean, onHasFlash: (Boolean) -> Unit, onFailure: () -> Unit, onCode: (String) -> Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val currentFailure by rememberUpdatedState(onFailure)
    val preview = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var camera by remember { mutableStateOf<Camera?>(null) }
    LaunchedEffect(camera, torch) { camera?.cameraControl?.enableTorch(torch) }
    AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
    DisposableEffect(key, lifecycleOwner, preview) {
        val alive = AtomicBoolean(true)
        val executor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val reader = BarcodeScanning.getClient()
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        val display = Preview.Builder().build().also { it.setSurfaceProvider(preview.surfaceProvider) }
        var provider: ProcessCameraProvider? = null
        analysis.setAnalyzer(executor) { proxy ->
            val image = proxy.image
            if (image == null || !alive.get()) { proxy.close(); return@setAnalyzer }
            try {
                reader.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener(mainExecutor) { barcodes ->
                        if (!alive.get() || !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@addOnSuccessListener
                        val visible = barcodes.mapNotNull { it.rawValue }.toCollection(linkedSetOf())
                        val code = gate.candidate(visible, SystemClock.elapsedRealtime())
                        if (code != null) {
                            try {
                                if (alive.get() && currentOnCode(code)) gate.accepted(code)
                            } finally {
                                // A detected code opens a draft; it does not insert a row.
                                gate.failed(code)
                            }
                        }
                    }
                    .addOnFailureListener(mainExecutor) { if (alive.get()) currentFailure() }
                    .addOnCompleteListener { proxy.close() }
            } catch (_: Exception) { proxy.close(); if (alive.get()) mainExecutor.execute { if (alive.get()) currentFailure() } }
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (alive.get()) try {
                provider = future.get()
                camera = provider!!.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, display, analysis)
                onHasFlash(camera!!.cameraInfo.hasFlashUnit())
            } catch (_: Exception) { currentFailure() }
        }, mainExecutor)
        onDispose {
            alive.set(false)
            analysis.clearAnalyzer()
            provider?.unbind(display, analysis)
            camera = null
            reader.close()
            executor.shutdown()
        }
    }
}
