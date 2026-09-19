package com.homesajja.app

import android.app.Application
import com.homesajja.app.di.AppContainer

class HomeSajjaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        container.sessionServices.start()
    }
}
