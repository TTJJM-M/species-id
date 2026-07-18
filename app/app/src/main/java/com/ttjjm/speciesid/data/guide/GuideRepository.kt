package com.ttjjm.speciesid.data.guide

import com.ttjjm.speciesid.data.RecognitionResponse
import kotlinx.coroutines.flow.Flow

/** 图鉴数据入口:识别成功自动写入,列表按条件观察,可删除 */
interface GuideRepository {

    suspend fun saveRecognition(imageBytes: ByteArray, response: RecognitionResponse)

    fun observe(domain: String?, query: String): Flow<List<RecognitionRecord>>

    suspend fun delete(record: RecognitionRecord)
}
