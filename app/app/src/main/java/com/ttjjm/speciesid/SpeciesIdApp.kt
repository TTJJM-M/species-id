package com.ttjjm.speciesid

import android.app.Application
import com.ttjjm.speciesid.data.GuideGraph
import com.ttjjm.speciesid.net.RetrofitClient

class SpeciesIdApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        GuideGraph.init(this)
    }
}