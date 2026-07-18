package com.ttjjm.speciesid.data.guide

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ttjjm.speciesid.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecognitionHistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RecognitionHistoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.recognitionHistoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun record(
        species: String,
        domain: String = "植物",
        createdAt: Long,
    ) = RecognitionRecord(
        domain = domain,
        species = species,
        description = "$species 的简介",
        confidence = 90,
        originalImagePath = "/img/$species.jpg",
        thumbnailPath = "/thumb/$species.jpg",
        createdAt = createdAt,
    )

    @Test
    fun `records come back newest first`() = runTest {
        dao.insert(record(species = "银杏", createdAt = 1000))
        dao.insert(record(species = "柯基", domain = "动物", createdAt = 3000))
        dao.insert(record(species = "宫保鸡丁", domain = "菜品", createdAt = 2000))

        val records = dao.observe(domain = null, query = "").first()

        assertEquals(listOf("柯基", "宫保鸡丁", "银杏"), records.map { it.species })
    }

    @Test
    fun `filter by domain returns only that domain`() = runTest {
        dao.insert(record(species = "银杏", domain = "植物", createdAt = 1000))
        dao.insert(record(species = "柯基", domain = "动物", createdAt = 2000))
        dao.insert(record(species = "宫保鸡丁", domain = "菜品", createdAt = 3000))

        val records = dao.observe(domain = "动物", query = "").first()

        assertEquals(listOf("柯基"), records.map { it.species })
    }

    @Test
    fun `search matches species name substring`() = runTest {
        dao.insert(record(species = "银杏", createdAt = 1000))
        dao.insert(record(species = "水杉", createdAt = 2000))
        dao.insert(record(species = "杏树", createdAt = 3000))

        val records = dao.observe(domain = null, query = "杏").first()

        assertEquals(listOf("杏树", "银杏"), records.map { it.species })
    }

    @Test
    fun `domain filter and search combine`() = runTest {
        dao.insert(record(species = "杏鲍菇炒肉", domain = "菜品", createdAt = 1000))
        dao.insert(record(species = "银杏", domain = "植物", createdAt = 2000))

        val records = dao.observe(domain = "植物", query = "杏").first()

        assertEquals(listOf("银杏"), records.map { it.species })
    }

    @Test
    fun `deleted record no longer appears`() = runTest {
        val keepId = dao.insert(record(species = "银杏", createdAt = 1000))
        val dropId = dao.insert(record(species = "柯基", domain = "动物", createdAt = 2000))

        dao.deleteById(dropId)

        val records = dao.observe(domain = null, query = "").first()
        assertEquals(listOf(keepId), records.map { it.id })
    }
}
