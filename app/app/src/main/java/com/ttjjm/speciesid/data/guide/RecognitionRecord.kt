package com.ttjjm.speciesid.data.guide

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 图鉴中的一条识别记录 */
@Entity(tableName = "recognition_history")
data class RecognitionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val species: String,
    val description: String,
    val confidence: Int,
    @ColumnInfo(name = "original_image_path") val originalImagePath: String,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
