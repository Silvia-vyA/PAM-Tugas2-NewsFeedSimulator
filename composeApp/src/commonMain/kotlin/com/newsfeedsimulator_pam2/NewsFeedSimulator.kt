package com.newsfeedsimulator_pam2

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

enum class Category {
    ALL, KPOP, TEKNOLOGI, LIFESTYLE
}

data class News(
    val id: Int,
    val title: String,
    val category: Category
)

data class NewsDetail(
    val id: Int,
    val content: String,
    val author: String
)

class NewsFeedSimulator(private val scope: CoroutineScope) {

    private val _readCount = MutableStateFlow(0)
    val readCount: StateFlow<Int> = _readCount.asStateFlow()

    private val readIds = mutableSetOf<Int>()

    fun newsStream(): Flow<News> = flow {
        var id = 1

        val pool = listOf(
            Category.KPOP to listOf(
                "BTS comeback dengan album baru",
                "BLACKPINK world tour diumumkan",
                "NewJeans trending global di streaming",
                "IU rilis lagu ballad terbaru",
                "Stray Kids konser di Jakarta",
                "TWICE masuk chart Billboard",
                "aespa rilis MV terbaru"
            ),
            Category.TEKNOLOGI to listOf(
                "Kotlin Flow Modern",
                "Compose Multiplatform makin rame",
                "Tips coroutine biar gak ngelag",
                "StateFlow vs LiveData singkat",
                "Android Studio update fitur baru"
            ),
            Category.LIFESTYLE to listOf(
                "Gaya hidup minimalis",
                "Tips tidur biar lebih nyenyak",
                "Olahraga ringan di rumah",
                "Journaling biar pikiran rapi",
                "Produktif tanpa overthinking"
            )
        )

        while (true) {
            delay(800)

            val (cat, titles) = pool.random()
            emit(
                News(
                    id = id++,
                    title = titles.random(),
                    category = cat
                )
            )
        }
    }

    suspend fun fetchDetail(newsId: Int): NewsDetail {
        delay(Random.nextLong(300, 900))
        return NewsDetail(
            id = newsId,
            content = "Detail berita #$newsId (simulasi). Ini ceritanya lebih panjang sedikit biar kelihatan kayak isi artikel.",
            author = listOf("Dispatch", "Soompi", "TechToday", "Daily Life").random()
        )
    }

    fun markRead(id: Int) {
        if (readIds.add(id)) _readCount.value++
    }

    fun startCollecting(
        selected: StateFlow<Category>,
        onNews: (News) -> Unit
    ): Job = scope.launch {
        val selectedNow = selected

        newsStream()
            .combine(selectedNow) { news, cat -> news to cat }
            .filter { (news, cat) -> cat == Category.ALL || news.category == cat }
            .map { it.first }
            .collect { onNews(it) }
    }
}