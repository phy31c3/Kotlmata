package kr.co.plasticcity.kotlmata

object all
object any
object of

@KotlmataMarker
interface KotlmataDSL

typealias KotlmataAction = KotlmataDSL.(signal: SIGNAL) -> Unit
typealias KotlmataAction1<T> = KotlmataDSL.(signal: T) -> Unit
typealias KotlmataAction2<T, R> = KotlmataDSL.(signal: T) -> R
typealias KotlmataCallback = KotlmataDSL.(payload: Any?) -> Unit
typealias KotlmataFallback = KotlmataDSL.(Throwable) -> Unit