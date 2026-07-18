package com.ttjjm.speciesid.data

import android.content.Context
import androidx.room.Room
import com.ttjjm.speciesid.data.guide.GuideRepository
import com.ttjjm.speciesid.data.guide.GuideRepositoryImpl

/** 图鉴依赖的组装点,App.onCreate 里 init 一次 */
object GuideGraph {

    lateinit var repository: GuideRepository
        private set

    fun init(context: Context) {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "species_id.db").build()
        repository = GuideRepositoryImpl(context, db.recognitionHistoryDao())
    }
}
