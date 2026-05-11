package com.chargepilot.core.control.writesettings.di

import com.chargepilot.core.control.ControlStrategy
import com.chargepilot.core.control.di.ControlMethodKey
import com.chargepilot.core.control.writesettings.WriteSettingsStrategy
import com.chargepilot.core.model.ControlMethod
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class WriteSettingsModule {
    @Binds
    @IntoMap
    @ControlMethodKey(ControlMethod.WRITE_SETTINGS_KEY)
    abstract fun bindWriteSettings(impl: WriteSettingsStrategy): ControlStrategy
}
