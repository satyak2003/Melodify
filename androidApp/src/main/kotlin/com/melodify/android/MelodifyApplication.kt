package com.melodify.android

import android.app.Application
import com.melodify.android.di.appModule
import com.melodify.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MelodifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MelodifyApplication)
            androidLogger()
            modules(appModule, sharedModule)
        }
    }
}
