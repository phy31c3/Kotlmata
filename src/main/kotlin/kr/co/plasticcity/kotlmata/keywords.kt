package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

object all
object any
object of

data class TypedSignal<T : SIGNAL>(val signal: T, val type: KClass<in T>)

infix fun <T : SIGNAL> T.asType(type: KClass<in T>) = TypedSignal(this, type)

@KotlmataMarker
interface KotlmataDSL

typealias KotlmataAction = KotlmataAction1R<SIGNAL, Unit>
typealias KotlmataActionR<R> = KotlmataAction1R<SIGNAL, R>
typealias KotlmataAction1<T> = KotlmataAction1R<T, Unit>
typealias KotlmataAction1R<T, R> = KotlmataDSL.(signal: T) -> R

typealias KotlmataFallback = KotlmataFallbackR<Unit>
typealias KotlmataFallbackR<R> = KotlmataDSL.(throwable: Throwable) -> R
typealias KotlmataFallback1<T> = KotlmataFallback1R<T, Unit>
typealias KotlmataFallback1R<T, R> = KotlmataDSL.(throwable: Throwable, signal: T) -> R

typealias KotlmataCallback = KotlmataDSL.(payload: Any?) -> Unit