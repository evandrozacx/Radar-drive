package com.example

import android.app.Application
import com.example.data.RadarDatabase
import com.example.data.RadarRepository
import com.example.service.RadarRuntimeController
import com.example.service.VoiceAnnouncer

class RadarDriverApp : Application() {

    lateinit var database: RadarDatabase
        private set

    lateinit var repository: RadarRepository
        private set

    lateinit var voiceAnnouncer: VoiceAnnouncer
        private set

    lateinit var runtimeController: RadarRuntimeController
        private set

    override fun onCreate() {
        super.onCreate()
        database = RadarDatabase.getInstance(this)
        repository = RadarRepository(database)
        voiceAnnouncer = VoiceAnnouncer(this)
        runtimeController = RadarRuntimeController(
            context = this,
            repository = repository,
            voiceAnnouncer = voiceAnnouncer
        )
    }

    override fun onTerminate() {
        voiceAnnouncer.shutdown()
        super.onTerminate()
    }
}
