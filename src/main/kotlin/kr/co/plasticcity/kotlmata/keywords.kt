package kr.co.plasticcity.kotlmata

object all
object any
object of

typealias KotlmataAction = Kotlmata.Marker.(signal: SIGNAL) -> Unit
typealias KotlmataAction1<T> = Kotlmata.Marker.(signal: T) -> Unit
typealias KotlmataAction2<T, R> = Kotlmata.Marker.(signal: T) -> R
typealias KotlmataCallback = Kotlmata.Marker.() -> Unit
typealias KotlmataFallback = Kotlmata.Marker.(Throwable) -> Unit