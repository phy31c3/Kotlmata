package kr.co.plasticcity.kotlmata

object all
object any
object of

@KotlmataMarker
interface KotlmataDSL

typealias KotlmataAction = KotlmataAction1R<SIGNAL, Unit>
typealias KotlmataAction1<T> = KotlmataAction1R<T, Unit>
typealias KotlmataActionR<R> = KotlmataAction1R<SIGNAL, R>
typealias KotlmataAction1R<T, R> = KotlmataDSL.(signal: T) -> R

typealias KotlmataFallback = KotlmataDSL.(throwable: Throwable) -> Unit
typealias KotlmataFallbackR<R> = KotlmataDSL.(throwable: Throwable) -> R
typealias KotlmataFallback1<T> = KotlmataDSL.(throwable: Throwable, signal: T) -> Unit
typealias KotlmataFallback2<T, R> = KotlmataDSL.(throwable: Throwable, signal: T) -> R

typealias KotlmataCallback = KotlmataDSL.(payload: Any?) -> Unit