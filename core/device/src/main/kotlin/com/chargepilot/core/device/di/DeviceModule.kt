package com.chargepilot.core.device.di

import com.chargepilot.core.device.DefaultSystemPropertyReader
import com.chargepilot.core.device.SystemPropertyReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceModule {
    @Binds
    @Singleton
    abstract fun bindSystemPropertyReader(impl: DefaultSystemPropertyReader): SystemPropertyReader
}
