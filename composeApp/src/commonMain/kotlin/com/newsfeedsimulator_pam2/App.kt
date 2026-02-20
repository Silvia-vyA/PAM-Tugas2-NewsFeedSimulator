package com.newsfeedsimulator_pam2

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// ---------------- THEME (biar enak dipake ulang) ----------------
private val newsVerseScheme = darkColorScheme(
    primary = Color(0xFF8E24AA),
    primaryContainer = Color(0xFF6A1B9A),
    background = Color(0xFF0F0B14),
    surface = Color(0xFF15121C),
    onPrimary = Color.White,
    onBackground = Color(0xFFEDE7F6),
    onSurface = Color(0xFFEDE7F6),
)

// ---------------- LABEL CATEGORY ----------------
private fun label(cat: Category): String = when (cat) {
    Category.ALL -> "All"
    Category.KPOP -> "K-Pop"
    Category.TEKNOLOGI -> "Tech"
    Category.LIFESTYLE -> "Lifestyle"
}

// ---------------- APP (DATA LIVE DARI SIMULATOR) ----------------
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val simulator = remember { NewsFeedSimulator(scope) }

    val readCount by simulator.readCount.collectAsState()

    val selectedFlow = remember { MutableStateFlow(Category.ALL) }
    var selected by remember { mutableStateOf(Category.ALL) }

    val feed = remember { mutableStateListOf<News>() }
    val readNews = remember { mutableStateListOf<News>() }

    var showDetail by remember { mutableStateOf(false) }
    var detailText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        simulator.startCollecting(selectedFlow) { news ->
            feed.add(0, news)
            if (feed.size > 200) feed.removeLast()
        }
    }

    val shown = remember(feed, selected) {
        if (selected == Category.ALL) feed
        else feed.filter { it.category == selected }
    }

    MaterialTheme(colorScheme = newsVerseScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NewsVerseUI(
                selected = selected,
                onSelectCategory = { cat ->
                    selected = cat
                    selectedFlow.value = cat
                },
                shownFeed = shown,
                lanjutBaca = readNews,
                totalDibaca = readCount,
                onClickBaca = { news ->
                    scope.launch {
                        val d = simulator.fetchDetail(news.id)
                        simulator.markRead(news.id)

                        if (!readNews.contains(news)) {
                            readNews.add(0, news)
                            if (readNews.size > 10) readNews.removeLast()
                        }

                        detailText = "${news.title}\n\n${d.content}\n\nSumber: ${d.author}"
                        showDetail = true
                    }
                },
                onClickLanjut = { news ->
                    scope.launch {
                        val d = simulator.fetchDetail(news.id)
                        detailText = "${news.title}\n\n${d.content}\n\nSumber: ${d.author}"
                        showDetail = true
                    }
                }
            )

            if (showDetail) {
                AlertDialog(
                    onDismissRequest = { showDetail = false },
                    confirmButton = {
                        TextButton(onClick = { showDetail = false }) { Text("Tutup") }
                    },
                    title = { Text("Detail") },
                    text = { Text(detailText) }
                )
            }
        }
    }
}

// ---------------- UI UTAMA (DIPAKE APP + PREVIEW) ----------------
@Composable
private fun NewsVerseUI(
    selected: Category,
    onSelectCategory: (Category) -> Unit,
    shownFeed: List<News>,
    lanjutBaca: List<News>,
    totalDibaca: Int,
    onClickBaca: (News) -> Unit,
    onClickLanjut: (News) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NewsVerse", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text("🔔")
            }
        }

        // FILTER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Category.entries.forEach { c ->
                FilterChip(
                    selected = selected == c,
                    onClick = { onSelectCategory(c) },
                    label = { Text(label(c)) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // LANJUTKAN MEMBACA (HORIZONTAL)
        Text(
            "Lanjutkan Membaca",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(10.dp))

        if (lanjutBaca.isEmpty()) {
            Text(
                "Belum ada yang kamu baca. Klik BACA dulu ya.",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    lanjutBaca,
                    key = { index, news -> "${news.id}-${news.title}-$index" }
                ) { _, news ->
                    Card(
                        modifier = Modifier.width(220.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onClickLanjut(news) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                news.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                label(news.category),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // FEED
        Text(
            "Feed",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                shownFeed,
                key = { index, news -> "${news.id}-${news.title}-$index" }
            ) { _, news ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(news.title)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                label(news.category),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Button(onClick = { onClickBaca(news) }) {
                            Text("BACA")
                        }
                    }
                }
            }
        }

        // TOTAL DIBACA (INI AJA YANG DISISA)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(
                modifier = Modifier.padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "TOTAL DIBACA: $totalDibaca",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// ---------------- PREVIEW (PAKE DATA CONTOH, BUKAN SIMULATOR) ----------------
@Preview(showBackground = true, widthDp = 393, heightDp = 851)
@Composable
fun AppPreview() {
    val dummyFeed = listOf(
        News(1, "BTS comeback dengan album baru", Category.KPOP),
        News(2, "Android Studio update fitur baru", Category.TEKNOLOGI),
        News(3, "Gaya hidup minimalis biar nggak capek", Category.LIFESTYLE),
        News(4, "aespa rilis MV terbaru", Category.KPOP),
        News(5, "Kotlin Flow Modern", Category.TEKNOLOGI)
    )

    val dummyRead = listOf(
        dummyFeed[0],
        dummyFeed[3],
        dummyFeed[2]
    )

    MaterialTheme(colorScheme = newsVerseScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NewsVerseUI(
                selected = Category.ALL,
                onSelectCategory = {},
                shownFeed = dummyFeed,
                lanjutBaca = dummyRead,
                totalDibaca = 3,
                onClickBaca = {},
                onClickLanjut = {}
            )
        }
    }
}