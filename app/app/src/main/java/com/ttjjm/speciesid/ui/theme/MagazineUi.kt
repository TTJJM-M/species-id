package com.ttjjm.speciesid.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// 杂志风设计语言(经 UI 原型比选定稿):白底 / 大黑标题 / pill / 绿青 accent
// ============================================================================

val Ink = Color(0xFF111827)
val BodyGray = Color(0xFF4B5563)
val MutedGray = Color(0xFF9CA3AF)
val DividerGray = Color(0xFFE5E7EB)
val AccentGreen = Color(0xFF10B981)
val AccentBlue = Color(0xFF0EA5E9)
val ErrorRed = Color(0xFFDC2626)
val FieldBg = Color(0xFFF3F4F6)
val PaperBg = Color.White

val ShutterGradient = Brush.linearGradient(listOf(Color(0xFF34D399), AccentBlue))

/** 领域配色（统计卡色点等），基于领域名确定性哈希到 8 组渐变色之一 */
private val DOMAIN_PALETTE = listOf(
    Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF059669))), // green
    Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF97316))), // amber
    Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFFE11D48))), // rose
    Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB))), // blue
    Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))), // purple
    Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0D9488))), // teal
    Brush.linearGradient(listOf(Color(0xFFFB923C), Color(0xFFEA580C))), // orange
    Brush.linearGradient(listOf(Color(0xFFA3E635), Color(0xFF65A30D))), // lime
)

fun domainGradient(domain: String): Brush =
    DOMAIN_PALETTE[kotlin.math.abs(domain.hashCode()) % DOMAIN_PALETTE.size]

/** 灰底胶囊搜索框 */
@Composable
fun MagazineSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("搜索物种名", color = MutedGray) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MutedGray) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, "清空搜索", tint = MutedGray)
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Ink,
        ),
    )
}

/** 筛选 pill:选中黑底白字 */
@Composable
fun MagazinePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Ink else PaperBg,
        border = if (selected) null else BorderStroke(1.dp, DividerGray),
    ) {
        Text(
            label,
            color = if (selected) Color.White else BodyGray,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** 置信度圆环 */
@Composable
fun ConfidenceRing(value: Int, size: Dp = 52.dp, strokeWidth: Dp = 5.dp) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { value / 100f },
            color = AccentGreen,
            trackColor = DividerGray,
            strokeWidth = strokeWidth,
            modifier = Modifier.size(size),
        )
        Text(
            "$value",
            color = Ink,
            fontSize = if (size < 48.dp) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
