package com.chargepilot.core.common

import javax.inject.Qualifier

/**
 * Hilt qualifiers for [kotlinx.coroutines.CoroutineDispatcher]s so we can swap them in
 * tests. Modules MUST inject via these qualifiers, never via `Dispatchers.IO` directly.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MainDispatcher
