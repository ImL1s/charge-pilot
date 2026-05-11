package com.chargepilot.core.control.root.di

import com.chargepilot.core.control.ControlStrategy
import com.chargepilot.core.control.di.ControlMethodKey
import com.chargepilot.core.control.root.RootStrategy
import com.chargepilot.core.model.ControlMethod
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class RootModule {
    @Binds
    @IntoMap
    @ControlMethodKey(ControlMethod.ROOT_SHELL)
    abstract fun bindRoot(impl: RootStrategy): ControlStrategy
}
