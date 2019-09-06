package com.hoan.samples.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(val app: Application) {
    private val mApplication = app

    @Provides
    @Singleton
    fun provideApplication(): Context = mApplication
}