package com.apexvelo.ct

import android.app.Application
import org.maplibre.android.MapLibre

class ApexVeloApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // No API key is needed for the public demo style we currently use.
        MapLibre.getInstance(this)
    }
}