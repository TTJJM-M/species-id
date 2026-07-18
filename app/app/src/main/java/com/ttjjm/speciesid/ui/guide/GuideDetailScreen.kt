package com.ttjjm.speciesid.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.ttjjm.speciesid.ui.theme.BodyGray
import com.ttjjm.speciesid.ui.theme.ConfidenceRing
import com.ttjjm.speciesid.ui.theme.DividerGray
import com.ttjjm.speciesid.ui.theme.ErrorRed
import com.ttjjm.speciesid.ui.theme.Ink
import com.ttjjm.speciesid.ui.theme.MutedGray
import com.ttjjm.speciesid.ui.theme.PaperBg
import java.io.File

/** 图鉴详情:杂志风物种档案(可缩放原图 + 排版信息区 + 删除) */
@Composable
fun GuideDetailScreen(
    recordId: Long,
    onBack: () -> Unit,
) {
    val viewModel: GuideViewModel = viewModel()
    val records by viewModel.records.collectAsState()
    val record = records.find { it.id == recordId }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && record != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这条记录?") },
            text = { Text("「${record.species}」的照片和信息将一并删除") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(record)
                    onBack()
                }) { Text("删除", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(PaperBg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MutedGray)
            }
        }

        if (record == null) return@Column

        // 原图占满剩余空间,手势独立,不与页面滚动抢事件
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ZoomableImage(
                imagePath = record.originalImagePath,
                contentDescription = record.species,
            )
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
            Text(
                record.species,
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 36.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Ink) {
                    Text(
                        record.domain,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                ConfidenceRing(record.confidence)
                Spacer(Modifier.width(8.dp))
                Text("置信度", color = BodyGray, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    formatTime(record.createdAt),
                    color = MutedGray,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = DividerGray)
            Spacer(Modifier.height(14.dp))
            Text(
                record.description,
                color = BodyGray,
                fontSize = 16.sp,
                lineHeight = 28.sp,
                modifier = Modifier
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

/** 全屏原图:双指缩放 + 放大后单指拖动,双击复位 */
@Composable
private fun ZoomableImage(
    imagePath: String,
    contentDescription: String,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = File(imagePath),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset = if (scale > 1f) offset + pan else Offset.Zero
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offset = Offset.Zero
                })
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
            ),
    )
}
