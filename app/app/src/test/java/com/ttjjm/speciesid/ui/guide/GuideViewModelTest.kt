package com.ttjjm.speciesid.ui.guide

import app.cash.turbine.test
import com.ttjjm.speciesid.data.RecognitionResponse
import com.ttjjm.speciesid.data.guide.GuideRepository
import com.ttjjm.speciesid.data.guide.RecognitionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GuideViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun record(
        id: Long,
        species: String,
        domain: String = "植物",
        createdAt: Long,
    ) = RecognitionRecord(
        id = id,
        domain = domain,
        species = species,
        description = "$species 的简介",
        confidence = 90,
        originalImagePath = "/img/$species.jpg",
        thumbnailPath = "/thumb/$species.jpg",
        createdAt = createdAt,
    )

    private val ginkgo = record(id = 1, species = "银杏", domain = "植物", createdAt = 1000)
    private val corgi = record(id = 2, species = "柯基", domain = "动物", createdAt = 2000)
    private val kungPao = record(id = 3, species = "宫保鸡丁", domain = "菜品", createdAt = 3000)

    private fun viewModel(vararg records: RecognitionRecord): Pair<GuideViewModel, FakeGuideRepository> {
        val repo = FakeGuideRepository(records.toList())
        return GuideViewModel(repositoryProvider = { repo }) to repo
    }

    @Test
    fun `records stream from repository newest first`() = runTest {
        val (viewModel, _) = viewModel(ginkgo, corgi, kungPao)

        viewModel.records.test {
            assertEquals(
                listOf("宫保鸡丁", "柯基", "银杏"),
                expectMostRecentItem().map { it.species },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `domain filter narrows the list and clearing restores it`() = runTest {
        val (viewModel, _) = viewModel(ginkgo, corgi, kungPao)

        viewModel.records.test {
            viewModel.setDomainFilter("动物")
            assertEquals(listOf("柯基"), expectMostRecentItem().map { it.species })

            viewModel.setDomainFilter(null)
            assertEquals(3, expectMostRecentItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search narrows the list by species name`() = runTest {
        val (viewModel, _) = viewModel(ginkgo, corgi, kungPao)

        viewModel.records.test {
            viewModel.setSearchQuery("杏")
            assertEquals(listOf("银杏"), expectMostRecentItem().map { it.species })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting a record removes it from the list`() = runTest {
        val (viewModel, repo) = viewModel(ginkgo, corgi)

        viewModel.records.test {
            viewModel.delete(corgi)
            assertEquals(listOf("银杏"), expectMostRecentItem().map { it.species })
            assertEquals(listOf(corgi), repo.deleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `domain counts cover all records and ignore the active filter`() = runTest {
        val (viewModel, _) = viewModel(ginkgo, corgi, kungPao)

        viewModel.domainCounts.test {
            assertEquals(
                mapOf("植物" to 1, "动物" to 1, "菜品" to 1),
                expectMostRecentItem(),
            )

            viewModel.setDomainFilter("动物")
            viewModel.setSearchQuery("柯")
            expectNoEvents() // 计数与筛选/搜索无关,不应重新发射

            cancelAndIgnoreRemainingEvents()
        }
    }
}

/** 模拟 GuideRepository 契约:倒序 + 领域筛 + 物种名子串搜索(真实现由 DAO 测试保证) */
private class FakeGuideRepository(
    initial: List<RecognitionRecord>,
) : GuideRepository {
    private val store = MutableStateFlow(initial)
    val deleted = mutableListOf<RecognitionRecord>()

    override suspend fun saveRecognition(imageBytes: ByteArray, response: RecognitionResponse) = Unit

    override fun observe(domain: String?, query: String): Flow<List<RecognitionRecord>> =
        store.map { list ->
            list.filter { (domain == null || it.domain == domain) && it.species.contains(query) }
                .sortedByDescending { it.createdAt }
        }

    override suspend fun delete(record: RecognitionRecord) {
        deleted += record
        store.value = store.value - record
    }
}
