package com.vatsalp.transitlens.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vatsalp.transitlens.core.model.GuidanceAction
import com.vatsalp.transitlens.core.model.SceneClass
import com.vatsalp.transitlens.ui.navigation.NavUiState
import com.vatsalp.transitlens.ui.navigation.NavigationViewModel
import java.util.concurrent.Executors

@Composable
fun NavigationScreen(viewModel: NavigationViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            val analyzer = remember { viewModel.createAnalyzer() }
            CameraPreview(analyzer, Modifier.fillMaxSize())
            GuidanceOverlay(
                ui = ui,
                modelsReady = viewModel.modelsReady,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else {
            PermissionPrompt { permissionLauncher.launch(Manifest.permission.CAMERA) }
        }
    }
}

@Composable
private fun CameraPreview(analyzer: ImageAnalysis.Analyzer, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }
            provider.unbindAll()
            runCatching {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose { executor.shutdown() }
    }
}

@Composable
private fun GuidanceOverlay(ui: NavUiState, modelsReady: Boolean, modifier: Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = guidanceLabel(ui.action),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Scene: ${sceneLabel(ui.scene)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!modelsReady) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "On-device models are not bundled in this build; showing camera only.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "TransitLens needs the camera to read your surroundings.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant camera access") }
    }
}

private fun guidanceLabel(action: GuidanceAction): String = when (action) {
    GuidanceAction.BOARD -> "Board now"
    GuidanceAction.ALIGHT -> "This is your stop"
    GuidanceAction.WAIT -> "Please wait"
    GuidanceAction.SEEK_ELEVATOR -> "Elevator ahead"
    GuidanceAction.CROSS_WAIT -> "Wait to cross"
    GuidanceAction.CROSS_NOW -> "Cross now"
    GuidanceAction.TURN_LEFT -> "Turn left"
    GuidanceAction.TURN_RIGHT -> "Turn right"
    GuidanceAction.ARRIVED -> "You have arrived"
    GuidanceAction.RECALCULATING -> "Recalculating route"
    GuidanceAction.ALERT -> "Attention"
    GuidanceAction.CONTINUE -> "Continue straight"
}

private fun sceneLabel(scene: SceneClass): String = when (scene) {
    SceneClass.BUS_STOP -> "bus stop"
    SceneClass.TRAIN_PLATFORM -> "train platform"
    SceneClass.STREET_CORNER -> "street corner"
    SceneClass.VEHICLE_INTERIOR -> "vehicle interior"
    SceneClass.TRANSFER_HUB -> "transfer hub"
    SceneClass.UNKNOWN -> "looking around"
}
