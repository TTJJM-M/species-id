package com.ttjjm.speciesid.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ttjjm.speciesid.data.guide.RecognitionHistoryDao
import com.ttjjm.speciesid.data.guide.RecognitionRecord

@Database(entities = [RecognitionRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recognitionHistoryDao(): RecognitionHistoryDao
}
