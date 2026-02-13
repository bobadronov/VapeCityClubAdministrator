package org.bigblackowl.vccadmin.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext

class DiApplication : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        GlobalContext.startKoin {
            androidContext(this@DiApplication)
            modules(coreModules)
        }
    }
}