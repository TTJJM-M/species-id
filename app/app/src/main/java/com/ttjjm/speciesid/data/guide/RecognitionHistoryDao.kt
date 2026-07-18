package com.ttjjm.speciesid.data.guide

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecognitionHistoryDao {

    @Insert
    suspend fun insert(record: RecognitionRecord): Long

    /**
     * 图鉴列表:时间倒序,可按领域筛(null = 全部),按物种名子串搜索("" = 不过滤)。
     * Room 要求 @Query 用上所有参数,所以筛选/搜索直接落在这条 SQL 里。
     */
    @Query(
        """
        SELECT * FROM recognition_history
        WHERE (:domain IS NULL OR domain = :domain)
          AND species LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        """
    )
    fun observe(domain: String?, query: String): Flow<List<RecognitionRecord>>

    @Query("DELETE FROM recognition_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
