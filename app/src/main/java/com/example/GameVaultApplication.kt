package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class GameVaultApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase App & App Check safely
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }

            if (BuildConfig.DEBUG) {
                val appCheck = FirebaseAppCheck.getInstance()
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            }
        } catch (e: Exception) {
            Log.w("GameVaultApplication", "Firebase / App Check initialization warning: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024) // 150 MB disk cache
                    .build()
            }
            .allowHardware(true) // Fast GPU rendering
            .crossfade(150) // Fast and smooth 150ms transition
            .respectCacheHeaders(false)
            .build()
    }
}
