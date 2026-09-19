package com.homesajja.app

import android.app.Application
import android.os.StrictMode
import com.homesajja.app.di.AppContainer

class HomeSajjaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) enableStrictMode()
        container = AppContainer(applicationContext)
        container.warmUp()
    }

    /** Debug builds only: logs (never crashes on) disk or network work on the main thread and other slips, so they get fixed. */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedSqlLiteObjects().penaltyLog().build())
    }
}
