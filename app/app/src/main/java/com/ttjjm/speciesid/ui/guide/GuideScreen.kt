package com.ttjjm.speciesid.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.ttjjm.speciesid.data.guide.RecognitionRecord
import com.ttjjm.speciesid.ui.theme.AccentGreen
import com.ttjjm.speciesid.ui.theme.DividerGray
import com.ttjjm.speciesid.ui.theme.FieldBg
import com.ttjjm.speciesid.ui.theme.Ink
import com.ttjjm.speciesid.ui.theme.MagazineSearchField
import com.ttjjm.speciesid.ui.theme.MutedGray
import com.ttjjm.speciesid.ui.theme.PaperBg
import com.ttjjm.speciesid.ui.theme.domainGradient
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 图鉴:档案集布局(最新收录 hero + 领域统计卡即筛选 + 列表),杂志风 */
@Composable
fun GuideScreen(
    onOpenDetail: (Long) -> Unit,
) {
    val viewModel: GuideViewModel = viewModel()
    val records by viewModel.records.collectAsState()
    val domainFilter by viewModel.domainFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val domainCounts by viewModel.domainCounts.collectAsState()

    val hero = records.firstOrNull()
    val domains = remember(domainCounts) { domainCounts.keys.sorted() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PaperBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
    ) {
        item {
            Text("我的图鉴", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
        }

        if (hero != null) {
            item {
                HeroCard(record = hero, onClick = { onOpenDetail(hero.id) })
                Spacer(Modifier.height(16.dp))
            }
        }

        if (domains.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(domains) { domain ->
                        DomainStatCard(
                            domain = domain,
                            count = domainCounts[domain] ?: 0,
                            selected = domainFilter == domain,
                            onClick = {
                                viewModel.setDomainFilter(if (domainFilter == domain) null else domain)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        item {
            MagazineSearchField(searchQuery, viewModel::setSearchQuery)
            Spacer(Modifier.height(8.dp))
        }

        if (records.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (searchQuery.isEmpty() && domainFilter == null)
                            "还没有识别记录,去拍一张吧"
                        else "没有符合条件的记录",
                        color = MutedGray,
                    )
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                GuideListRow(record = record, onClick = { onOpenDetail(record.id) })
                HorizontalDivider(color = DividerGray.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun HeroCard(record: RecognitionRecord, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = File(record.thumbnailPath),
            contentDescription = record.species,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f),
                    )
                )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                "最新收录",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                record.species,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                formatTime(record.createdAt),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DomainStatCard(
    domain: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = FieldBg,
        border = if (selected) BorderStroke(2.dp, Ink) else null,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(domainGradient(domain))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$count",
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                domain,
                color = MutedGray,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun GuideListRow(record: RecognitionRecord, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        AsyncImage(
            model = File(record.thumbnailPath),
            contentDescription = record.species,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                record.species,
                color = Ink,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${record.domain} · ${formatTime(record.createdAt)}",
                color = MutedGray,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            "${record.confidence}%",
            color = AccentGreen,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMillis))
