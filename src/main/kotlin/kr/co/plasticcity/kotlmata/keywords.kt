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
interface ActionDSL

interface FunctionDSL : ActionDSL
{
	open class Sync internal constructor(val signal: SIGNAL, val type: KClass<SIGNAL>? = null, val payload: Any? = null)
	class TypedSync internal constructor(signal: SIGNAL, type: KClass<SIGNAL>) : Sync(signal, type)
	
	@Suppress("UNCHECKED_CAST")
	infix fun <T : SIGNAL> T.type(type: KClass<in T>) = TypedSync(this, type as KClass<SIGNAL>)
	
	infix fun <T : SIGNAL> T.payload(payload: Any?) = Sync(this, null, payload)
	infix fun TypedSync.payload(payload: Any?) = Sync(signal, type, payload)
}

interface ErrorDSL : ActionDSL
{
	val throwable: Throwable
}

interface PayloadDSL : ActionDSL
{
	val payload: Any?
}

interface PayloadFunctionDSL : PayloadDSL, FunctionDSL
interface ErrorFunctionDSL : ErrorDSL, FunctionDSL
interface ErrorPayloadDSL : ErrorDSL, PayloadDSL
interface ErrorPayloadFunctionDSL : ErrorFunctionDSL, PayloadDSL

typealias EntryAction<T> = ActionDSL.(signal: T) -> Unit
typealias EntryFunction<T, R> = FunctionDSL.(signal: T) -> R
typealias EntryError<T, R> = ErrorDSL.(signal: T) -> R
typealias EntryCatch<T, R> = ErrorFunctionDSL.(signal: T) -> R
typealias InputAction<T> = PayloadDSL.(signal: T) -> Unit
typealias InputFunction<T, R> = PayloadFunctionDSL.(signal: T) -> R
typealias InputError<T, R> = ErrorPayloadDSL.(signal: T) -> R
typealias InputCatch<T, R> = ErrorPayloadFunctionDSL.(signal: T) -> R
typealias ExitAction = ActionDSL.(signal: SIGNAL) -> Unit
typealias ExitError = ErrorDSL.(signal: SIGNAL) -> Unit

typealias StateError = ErrorDSL.(signal: SIGNAL) -> Unit
typealias MachineError = ErrorDSL.() -> Unit

typealias DaemonCallback = PayloadDSL.() -> Unit
typealias DaemonFallback = ErrorPayloadDSL.() -> Unit

typealias StateTemplate<S> = KotlmataState.Init.(state: S) -> Unit
typealias MachineTemplate<M> = KotlmataMachine.Init.(machine: M) -> KotlmataMachine.Init.End
typealias DaemonTemplate<D> = KotlmataDaemon.Init.(daemon: D) -> KotlmataMachine.Init.End
