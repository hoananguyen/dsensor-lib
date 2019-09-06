package com.hoan.samples

import android.app.Application
import com.hoan.samples.di.AppComponent
import com.hoan.samples.di.AppModule
import com.hoan.samples.di.DaggerAppComponent

class SamplesApplication : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = initDagger(this)
    }

    private fun initDagger(samplesApplication: SamplesApplication): AppComponent {
        return DaggerAppComponent.builder()
            .appModule(AppModule(samplesApplication))
            .build()
    }
}