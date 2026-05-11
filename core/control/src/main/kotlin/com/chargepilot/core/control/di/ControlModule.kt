package com.chargepilot.core.control.di

import com.chargepilot.core.control.ControlStrategy
import com.chargepilot.core.control.OfficialGuidanceStrategy
import com.chargepilot.core.model.ControlMethod
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@MapKey
annotation class ControlMethodKey(val value: ControlMethod)

@Module
@InstallIn(SingletonComponent::class)
abstract class ControlModule {
    @Binds
    @IntoMap
    @ControlMethodKey(ControlMethod.OFFICIAL_GUIDANCE)
    abstract fun bindOfficialGuidance(impl: OfficialGuidanceStrategy): ControlStrategy
}
