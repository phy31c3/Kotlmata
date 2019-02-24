package kr.co.plasticcity.kotlmata

object all
object any
object of

typealias KotlmataAction0 = Kotlmata.Marker.(signal: SIGNAL) -> Unit
typealias KotlmataAction1<T> = Kotlmata.Marker.(signal: T) -> Unit
typealias KotlmataAction2<T, R> = Kotlmata.Marker.(signal: T) -> R