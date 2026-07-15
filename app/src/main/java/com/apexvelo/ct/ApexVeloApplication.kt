package com.apexvelo.ct

import android.app.Application
import com.apexvelo.ct.feature.device.connection.ApexVeloBleClient
import org.maplibre.android.MapLibre

class ApexVeloApplication : Application() {

    val navigationBleClient: ApexVeloBleClient by lazy {
        ApexVeloBleClient(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()

        // No API key is needed for the public demo style we currently use.
        MapLibre.getInstance(this)
    }
}
