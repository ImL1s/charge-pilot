package com.chargepilot.core.control.shizuku.di

import com.chargepilot.core.control.ControlStrategy
import com.chargepilot.core.control.di.ControlMethodKey
import com.chargepilot.core.control.shizuku.ShizukuStrategy
import com.chargepilot.core.model.ControlMethod
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class ShizukuModule {
    @Binds
    @IntoMap
    @ControlMethodKey(ControlMethod.SHIZUKU_RPC)
    abstract fun bindShizuku(impl: ShizukuStrategy): ControlStrategy
}
