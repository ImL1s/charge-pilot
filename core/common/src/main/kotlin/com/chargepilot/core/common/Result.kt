package com.chargepilot.core.common

/**
 * Lightweight, exception-free Result used at module boundaries. Prefer this over
 * `kotlin.Result` for repository APIs because it survives serialization and pattern
 * matching cleanly.
 */
sealed interface CpResult<out T> {
    data class Success<T>(val value: T) : CpResult<T>
    data class Failure(val error: Throwable, val message: String? = null) : CpResult<Nothing>
}

inline fun <T, R> CpResult<T>.map(transform: (T) -> R): CpResult<R> = when (this) {
    is CpResult.Success -> CpResult.Success(transform(value))
    is CpResult.Failure -> this
}

inline fun <T> CpResult<T>.getOrElse(default: (CpResult.Failure) -> T): T = when (this) {
    is CpResult.Success -> value
    is CpResult.Failure -> default(this)
}

inline fun <T> runCatchingCp(block: () -> T): CpResult<T> = try {
    CpResult.Success(block())
} catch (t: Throwable) {
    CpResult.Failure(t)
}
