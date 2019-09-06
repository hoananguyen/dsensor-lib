package com.hoan.samples.di

import com.hoan.samples.fragments.FragmentSensorList
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, AdapterModule::class])
interface AppComponent {
    fun inject(fragmentSensorList: FragmentSensorList)
}