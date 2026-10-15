package com.aquagrow.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aquagrow.R
import com.aquagrow.domain.repository.CaptureTarget
import com.aquagrow.domain.repository.PhotoRepository
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.ModuleColors
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

/**
 * Shows a stored photo (relative path) or the placeholder. A missing/corrupt file simply
 * shows the placeholder instead of crashing.
 */
@Composable
fun AppPhoto(
    path: String?,
    @DrawableRes placeholderIcon: Int,
    colors: ModuleColors,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
) {
    val context = LocalContext.current
    Box(modifier.clip(shape)) {
        PhotoPlaceholder(placeholderIcon, colors, Modifier.fillMaxSize(), iconSize = 40.dp, shape = shape)
        if (path != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(context.filesDir, path))
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@HiltViewModel
class PhotoInputViewModel @Inject constructor(
    private val photos: PhotoRepository,
) : ViewModel() {
    suspend fun import(uri: Uri): String? = runCatching { photos.importPhoto(uri.toString()) }.getOrNull()
    fun newTarget(): CaptureTarget = photos.newCaptureTarget()
    suspend fun discard(path: String) = photos.discardFile(path)
}

/**
 * Photo area for forms: shows the current photo and offers Take photo (in-app CameraX)
 * or Choose from gallery (system Photo Picker; no storage permission needed).
 */
@Composable
fun PhotoField(
    path: String?,
    onPathChange: (String?) -> Unit,
    @DrawableRes placeholderIcon: Int,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {},
    viewModel: PhotoInputViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val colors = AquaGrowTheme.module
    val failed = stringResource(R.string.photo_import_failed)
    var busy by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<CaptureTarget?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val imported = viewModel.import(uri)
                busy = false
                if (imported != null) onPathChange(imported) else onError(failed)
            }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            AppPhoto(path, placeholderIcon, colors, Modifier.size(132.dp))
            if (busy) CircularProgressIndicator()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { cameraTarget = viewModel.newTarget() }) {
                Icon(painterResource(R.drawable.ic_photo_camera), null, Modifier.size(18.dp))
                Text(" " + stringResource(R.string.photo_take))
            }
            TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(painterResource(R.drawable.ic_photo_library), null, Modifier.size(18.dp))
                Text(" " + stringResource(R.string.photo_choose))
            }
        }
        if (path != null) {
            TextButton(onClick = { onPathChange(null) }) { Text(stringResource(R.string.photo_remove)) }
        }
    }

    cameraTarget?.let { target ->
        CameraCaptureDialog(
            target = target,
            onCaptured = {
                onPathChange(target.relativePath)
                cameraTarget = null
            },
            onDismiss = {
                scope.launch { viewModel.discard(target.relativePath) }
                cameraTarget = null
            },
        )
    }
}

/** Full-screen in-app camera (CameraX). Asks for the camera permission with an explanation. */
@Composable
fun CameraCaptureDialog(
    target: CaptureTarget,
    onCaptured: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        denied = !it
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            if (granted) {
                CameraContent(target, onCaptured, onDismiss)
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .systemBarsPadding()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(painterResource(R.drawable.ic_photo_camera), null, tint = AquaGrowTheme.module.primary, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(if (denied) R.string.camera_permission_denied else R.string.camera_permission_body),
                        textAlign = TextAlign.Center,
                        color = AquaGrowTheme.colors.mutedText,
                    )
                    PrimaryButton(stringResource(R.string.camera_permission_grant), onClick = { permission.launch(Manifest.permission.CAMERA) })
                    if (denied) {
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }) { Text(stringResource(R.string.camera_open_settings)) }
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                }
            }
        }
    }
}

@Composable
private fun CameraContent(target: CaptureTarget, onCaptured: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var capturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    val preview = remember { Preview.Builder().build().apply { setSurfaceProvider { surfaceRequest = it } } }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // Plenty for plant/fish photos and keeps files small.
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy(Size(1600, 1200), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                    .build(),
            )
            .build()
    }

    LaunchedEffect(lensFacing) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            awaitCancellation()
        } catch (_: IllegalArgumentException) {
            // Requested lens not available (e.g. no front camera): fall back to back camera.
            if (lensFacing != CameraSelector.LENS_FACING_BACK) lensFacing = CameraSelector.LENS_FACING_BACK else error = true
        } finally {
            provider.unbindAll()
        }
    }

    Box(Modifier.fillMaxSize()) {
        surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize()) }
        Row(
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close), tint = Color.White) }
            IconButton(onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            }) { Icon(painterResource(R.drawable.ic_flip_camera_android), stringResource(R.string.camera_switch), tint = Color.White) }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (error) Text(stringResource(R.string.camera_failed), color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
            Surface(
                onClick = {
                    if (capturing) return@Surface
                    capturing = true
                    error = false
                    imageCapture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(File(target.absolutePath)).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                capturing = false
                                onCaptured()
                            }

                            override fun onError(exception: ImageCaptureException) {
                                capturing = false
                                error = true
                            }
                        },
                    )
                },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(76.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (capturing) {
                        CircularProgressIndicator()
                    } else {
                        Icon(painterResource(R.drawable.ic_photo_camera), stringResource(R.string.camera_capture), tint = Color.Black)
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet offering "Take photo" (in-app CameraX) or "Choose from gallery" (Photo Picker).
 * Calls [onPicked] with the stored relative path.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceSheet(
    onPicked: (String) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit = {},
    viewModel: PhotoInputViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val failed = stringResource(R.string.photo_import_failed)
    var cameraTarget by remember { mutableStateOf<CaptureTarget?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            onDismiss()
        } else {
            scope.launch {
                val imported = viewModel.import(uri)
                if (imported != null) {
                    onPicked(imported)
                } else {
                    onError(failed)
                    onDismiss()
                }
            }
        }
    }
    if (cameraTarget == null) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.padding(bottom = 24.dp)) {
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.photo_take)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_photo_camera), null) },
                    modifier = Modifier.clickable { cameraTarget = viewModel.newTarget() },
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.photo_choose)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_photo_library), null) },
                    modifier = Modifier.clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
            }
        }
    }
    cameraTarget?.let { target ->
        CameraCaptureDialog(
            target = target,
            onCaptured = { onPicked(target.relativePath) },
            onDismiss = {
                scope.launch { viewModel.discard(target.relativePath) }
                cameraTarget = null
                onDismiss()
            },
        )
    }
}
