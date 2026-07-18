package com.ttjjm.speciesid.data.guide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ttjjm.speciesid.data.RecognitionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/** 真实图鉴存储:原图 + 缩略图写 app 私有目录,元数据写 Room */
class GuideRepositoryImpl(
    context: Context,
    private val dao: RecognitionHistoryDao,
) : GuideRepository {

    private val originalsDir = File(context.filesDir, "guide/originals")
    private val thumbsDir = File(context.filesDir, "guide/thumbs")

    override suspend fun saveRecognition(imageBytes: ByteArray, response: RecognitionResponse) {
        withContext(Dispatchers.IO) {
            val createdAt = System.currentTimeMillis()
            originalsDir.mkdirs()
            thumbsDir.mkdirs()

            val original = File(originalsDir, "$createdAt.jpg")
            original.writeBytes(imageBytes)

            val thumb = File(thumbsDir, "$createdAt.jpg")
            writeThumbnail(imageBytes, thumb)

            dao.insert(
                RecognitionRecord(
                    domain = response.domain ?: "",
                    species = response.species ?: "",
                    description = response.description ?: "",
                    confidence = response.confidence ?: 0,
                    originalImagePath = original.absolutePath,
                    thumbnailPath = thumb.absolutePath,
                    createdAt = createdAt,
                )
            )
        }
    }

    override fun observe(domain: String?, query: String): Flow<List<RecognitionRecord>> =
        dao.observe(domain, query)

    override suspend fun delete(record: RecognitionRecord) {
        withContext(Dispatchers.IO) {
            dao.deleteById(record.id)
            // 文件删除尽力而为,失败不影响记录移除
            File(record.originalImagePath).delete()
            File(record.thumbnailPath).delete()
        }
    }

    /** 最长边缩到 [THUMB_MAX_SIZE],JPEG 80;解码失败就直接落原图字节 */
    private fun writeThumbnail(imageBytes: ByteArray, target: File) {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (bitmap == null) {
            target.writeBytes(imageBytes)
            return
        }
        val scale = THUMB_MAX_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val thumb = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            bitmap
        }
        target.outputStream().use { out ->
            thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        if (thumb !== bitmap) thumb.recycle()
        bitmap.recycle()
    }

    private companion object {
        const val THUMB_MAX_SIZE = 512
    }
}
