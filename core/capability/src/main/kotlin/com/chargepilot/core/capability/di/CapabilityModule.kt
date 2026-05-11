package com.chargepilot.core.capability.di

import com.chargepilot.core.capability.BundledCapabilityRegistryLoader
import com.chargepilot.core.capability.CapabilityRegistryLoader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CapabilityModule {
    @Binds
    @Singleton
    abstract fun bindCapabilityRegistryLoader(
        impl: BundledCapabilityRegistryLoader
    ): CapabilityRegistryLoader
}
