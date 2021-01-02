package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

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
 * "state" x "signal" %= stay
 * any x "signal" %= stay
 * ```
 */
object stay

/**
 * In Machine:
 * ```
 * "state" x "signal" %= self
 * any x "signal" %= self
 * ```
 */
object self

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
 * delete rule of state "state1"
 * ```
 */
object of

interface StatesOrSignals<T : STATE_OR_SIGNAL> : MutableList<STATE_OR_SIGNAL>
interface StatesOrSignalsDefinable
{
	infix fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> T1.or(stateOrSignal: T2): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> T1.or(stateOrSignal: KClass<T2>): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> KClass<T1>.or(stateOrSignal: T2): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> KClass<T1>.or(stateOrSignal: KClass<T2>): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> StatesOrSignals<T1>.or(stateOrSignal: T2): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> StatesOrSignals<T1>.or(stateOrSignal: KClass<T2>): StatesOrSignals<R>
}

@KotlmataMarker
interface ActionDSL

interface FunctionDSL : ActionDSL
{
	open class Sync internal constructor(val signal: SIGNAL, val type: KClass<SIGNAL>? = null, val payload: Any? = null)
	class TypedSync internal constructor(signal: SIGNAL, type: KClass<SIGNAL>) : Sync(signal, type)
	
	@Suppress("UNCHECKED_CAST")
	infix fun <T : SIGNAL> T.`as`(type: KClass<in T>) = TypedSync(this, type as KClass<SIGNAL>)
	
	infix fun <T : SIGNAL> T.with(payload: Any?) = Sync(this, null, payload)
	infix fun TypedSync.with(payload: Any?) = Sync(signal, type, payload)
}

interface ErrorActionDSL : ActionDSL
{
	val throwable: Throwable
}

interface TransitionDSL : ActionDSL
{
	val count: Int
}

interface PayloadDSL : ActionDSL
{
	val payload: Any?
}

interface PayloadFunctionDSL : PayloadDSL, FunctionDSL
interface ErrorFunctionDSL : ErrorActionDSL, FunctionDSL
interface ErrorPayloadDSL : ErrorActionDSL, PayloadDSL
interface ErrorPayloadFunctionDSL : ErrorPayloadDSL, FunctionDSL
interface ErrorTransitionDSL : ErrorActionDSL, TransitionDSL

typealias EntryAction<T> = ActionDSL.(signal: T) -> Unit
typealias EntryFunction<T> = FunctionDSL.(signal: T) -> Any?
typealias EntryErrorAction<T> = ErrorActionDSL.(signal: T) -> Unit
typealias EntryErrorFunction<T> = ErrorFunctionDSL.(signal: T) -> Any?

typealias InputAction<T> = PayloadDSL.(signal: T) -> Unit
typealias InputFunction<T> = PayloadFunctionDSL.(signal: T) -> Any?
typealias InputErrorAction<T> = ErrorPayloadDSL.(signal: T) -> Unit
typealias InputErrorFunction<T> = ErrorPayloadFunctionDSL.(signal: T) -> Any?

typealias ExitAction<T> = ActionDSL.(signal: T) -> Unit
typealias ExitErrorAction<T> = ErrorActionDSL.(signal: T) -> Unit

typealias StateError = ErrorActionDSL.(signal: SIGNAL) -> Unit
typealias MachineError = ErrorActionDSL.() -> Unit

typealias TransitionCallback = TransitionDSL.(from: STATE, signal: SIGNAL, to: STATE) -> Unit
typealias TransitionFallback = ErrorTransitionDSL.(from: STATE, signal: SIGNAL, to: STATE) -> Unit

typealias DaemonCallback = PayloadDSL.() -> Unit
typealias DaemonFallback = ErrorPayloadDSL.() -> Unit

typealias StateTemplate<T> = KotlmataState.Init.(tag: T) -> Unit
typealias MachineTemplate<T> = KotlmataMachine.Init.(tag: T) -> KotlmataMachine.Init.End
typealias DaemonTemplate<T> = KotlmataDaemon.Init.(tag: T, daemon: KotlmataDaemon<T>) -> KotlmataMachine.Init.End
typealias ForkTemplate<T> = KotlmataDaemon.Init.(tag: T) -> KotlmataMachine.Init.End
