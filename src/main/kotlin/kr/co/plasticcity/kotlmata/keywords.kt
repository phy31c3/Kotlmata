package kr.co.plasticcity.kotlmata

object all
object any
object of

@KotlmataMarker
interface KotlmataDSL

typealias KotlmataAction = KotlmataDSL.(signal: SIGNAL) -> Unit
typealias KotlmataAction1<T> = KotlmataDSL.(signal: T) -> Unit
typealias KotlmataAction2<T, R> = KotlmataDSL.(signal: T) -> R

typealias KotlmataFallback = KotlmataDSL.(throwable: Throwable) -> Unit
typealias KotlmataFallbackR<R> = KotlmataDSL.(throwable: Throwable) -> R
typealias KotlmataFallback1<T> = KotlmataDSL.(throwable: Throwable, signal: T) -> Unit
typealias KotlmataFallback2<T, R> = KotlmataDSL.(throwable: Throwable, signal: T) -> R

typealias KotlmataCallback = KotlmataDSL.(payload: Any?) -> Unit