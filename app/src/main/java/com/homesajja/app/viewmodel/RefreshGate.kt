package com.homesajja.app.viewmodel

/**
 * Lets a screen refresh whenever it resumes without loading twice in a row. Compose re-creates a
 * screen when you navigate away and back, while its ViewModel (and stale data) survives — so
 * "is the data old?" has to be asked of the ViewModel, not remembered by the screen.
 */
class RefreshGate(private val minIntervalMillis: Long = 1_500L) {
    private var lastLoadMillis = 0L

    /** Call when a load starts and again when it ends. */
    fun markLoaded() {
        lastLoadMillis = System.currentTimeMillis()
    }

    fun isStale(): Boolean = System.currentTimeMillis() - lastLoadMillis > minIntervalMillis
}
