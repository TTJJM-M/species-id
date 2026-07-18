package com.ttjjm.speciesid.ui.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ttjjm.speciesid.data.RecognitionResponse
import com.ttjjm.speciesid.net.RetrofitClient
import com.ttjjm.speciesid.ui.settings.SettingsDialog
import com.ttjjm.speciesid.ui.theme.AccentBlue
import com.ttjjm.speciesid.ui.theme.BodyGray
import com.ttjjm.speciesid.ui.theme.ConfidenceRing
import com.ttjjm.speciesid.ui.theme.DividerGray
import com.ttjjm.speciesid.ui.theme.ErrorRed
import com.ttjjm.speciesid.ui.theme.Ink
import com.ttjjm.speciesid.ui.theme.MutedGray
import com.ttjjm.speciesid.ui.theme.ShutterGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CameraScreen(
    onOpenSettings: () -> Unit,
) {
    val viewModel: CameraViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val pendingBytes by viewModel.pendingBytes.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var compressionError by remember { mutableStateOf<String?>(null) }

    fun compressAndRecognize(uri: Uri) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { ImageCompressor.compress(context, uri) }
            }
            result
                .onSuccess { bytes ->
                    compressionError = null
                    viewModel.recognizeImage(bytes)
                }
                .onFailure { error ->
                    compressionError = "图片处理失败: ${error.message}"
                }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) pendingCameraUri?.let { compressAndRecognize(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            val uri = createTempImageUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) compressAndRecognize(uri) }

    fun takePhoto() {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            val uri = createTempImageUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    fun pickImage() {
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            initialUrl = RetrofitClient.getCurrentBaseUrl() ?: "",
            onSave = { url -> RetrofitClient.saveBaseUrl(context, url) },
            onDismiss = { showSettingsDialog = false },
        )
    }

    val previewBitmap: Bitmap? = remember(pendingBytes) {
        pendingBytes?.let { bytes ->
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    val state = uiState
    if (state is RecognitionUiState.Success) {
        SpeciesArchiveContent(
            result = state.result,
            previewBitmap = previewBitmap,
            onReset = viewModel::reset,
        )
    } else {
        CaptureContent(
            uiState = state,
            compressionError = compressionError,
            onTakePhoto = ::takePhoto,
            onPickImage = ::pickImage,
            onOpenSettings = { showSettingsDialog = true },
            onRetry = viewModel::retryWithLastBytes,
        )
    }
}

/** 识别前/中:白底极简,一颗渐变大快门 */
@Composable
private fun CaptureContent(
    uiState: RecognitionUiState,
    compressionError: String?,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
        ) {
            Icon(Icons.Default.Settings, contentDescription = "设置", tint = MutedGray)
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(ShutterGradient)
                    .clickable(
                        enabled = uiState !is RecognitionUiState.Loading,
                        onClick = onTakePhoto,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState is RecognitionUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                } else {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "拍照",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                if (uiState is RecognitionUiState.Loading) "识别中…" else "轻点拍照,认识它",
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onPickImage) {
                Text("或从相册选一张", color = AccentBlue)
            }
            when (uiState) {
                RecognitionUiState.Unrecognized -> Text("认不准,换个角度试试", color = BodyGray)
                is RecognitionUiState.Error -> {
                    Text(uiState.message, color = ErrorRed)
                    TextButton(onClick = onRetry) { Text("重试") }
                }
                else -> {}
            }
            compressionError?.let { Text(it, color = ErrorRed) }
        }
    }
}

/** 识别后:整页物种档案排版 */
@Composable
private fun SpeciesArchiveContent(
    result: RecognitionResponse,
    previewBitmap: Bitmap?,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        previewBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = result.species,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
            )
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(22.dp))
            Text(
                result.species ?: "",
                color = Ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 40.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Ink) {
                    Text(
                        result.domain ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                ConfidenceRing(result.confidence ?: 0)
                Spacer(Modifier.width(8.dp))
                Text("置信度", color = BodyGray, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = DividerGray)
            Spacer(Modifier.height(20.dp))
            Text(
                result.description ?: "",
                color = BodyGray,
                fontSize = 17.sp,
                lineHeight = 30.sp,
            )
            Spacer(Modifier.height(28.dp))
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("再拍一张")
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
