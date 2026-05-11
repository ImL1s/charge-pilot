package com.chargepilot.core.domain.di

import com.chargepilot.core.domain.DefaultPreconditionChecker
import com.chargepilot.core.domain.PreconditionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    @Binds
    @Singleton
    abstract fun bindPreconditionChecker(impl: DefaultPreconditionChecker): PreconditionChecker
}
