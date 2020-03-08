package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

/**
 * In State:
 * ```
 * delete action all
 * delete action entry all
 * delete action input all
 * ```
 *
 * In Machine:
 * ```
 * delete state all
 * delete rule all
 * ```
 */
object all

/**
 * In Machine:
 * ```
 * "state1" x any %= "state2"
 * any x "signal" %= "state2"
 * any x any %= "state2"
 * any.of("state1", "state2") x "signal" %= "state3"
 * any.of("state1", "state2") x any.of("signal1", "signal2") %= "state3"
 * any.except("state1") x "signal" %= "state2"
 * "state1" x any.of("signal1", "signal2") %= "state2"
 * "state1" x any.except("signal1", "signal2") %= "state2"
 * ```
 */
object any

/**
 * In Machine:
 * ```
 * delete rule of state "state1"
 * ```
 */
object of

/**
 * In Machine:
 * ```
 * "state" x "signal" %= self
 * any x "signal" %= self
 * ```
 */
object self

@KotlmataMarker
interface KotlmataDSL
{
	open class Sync internal constructor(val signal: SIGNAL, val type: KClass<SIGNAL>? = null, val payload: Any? = null)
	class TypedSync internal constructor(signal: SIGNAL, type: KClass<SIGNAL>) : Sync(signal, type)
	
	@Suppress("UNCHECKED_CAST")
	infix fun <T : SIGNAL> T.type(type: KClass<in T>) = TypedSync(this, type as KClass<SIGNAL>)
	
	infix fun <T : SIGNAL> T.payload(payload: Any?) = Sync(this, null, payload)
	infix fun TypedSync.payload(payload: Any?) = Sync(signal, type, payload)
}

typealias KotlmataAction = KotlmataDSL.(signal: SIGNAL) -> Unit
typealias KotlmataActionR<R> = KotlmataDSL.(signal: SIGNAL) -> R
typealias KotlmataAction1<T> = KotlmataDSL.(signal: T) -> Unit
typealias KotlmataAction1R<T, R> = KotlmataDSL.(signal: T) -> R
typealias KotlmataAction2R<T, R> = KotlmataDSL.(signal: T, payload: Any?) -> R

typealias KotlmataError = KotlmataDSL.(throwable: Throwable) -> Unit
typealias KotlmataErrorR<R> = KotlmataDSL.(throwable: Throwable) -> R
typealias KotlmataError1<T> = KotlmataDSL.(throwable: Throwable, signal: T) -> Unit
typealias KotlmataError1R<T, R> = KotlmataDSL.(throwable: Throwable, signal: T) -> R

typealias KotlmataCallback = KotlmataDSL.(payload: Any?) -> Unit
typealias KotlmataFallback = KotlmataDSL.(throwable: Throwable) -> Unit
typealias KotlmataFallback1 = KotlmataDSL.(throwable: Throwable, payload: Any?) -> Unit

typealias KotlmataStateDef<S> = KotlmataState.Init.(state: S) -> Unit
typealias KotlmataMachineDef<M> = KotlmataMachine.Init.(machine: M) -> KotlmataMachine.Init.End
typealias KotlmataDaemonDef<D> = KotlmataDaemon.Init.(daemon: D) -> KotlmataMachine.Init.End