package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

/**
 * Supertype of KotlmataState tag
 */
typealias STATE = Any

/**
 * Supertype of Kotlmata signal
 */
typealias SIGNAL = Any

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

interface StatesOrSignals<T : `STATE or SIGNAL`> : MutableList<`STATE or SIGNAL`>
interface StatesOrSignalsDefinable
{
	infix fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> T1.or(stateOrSignal: T2): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> T1.or(stateOrSignal: KClass<T2>): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> KClass<T1>.or(stateOrSignal: T2): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> KClass<T1>.or(stateOrSignal: KClass<T2>): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> StatesOrSignals<T1>.or(stateOrSignal: T2): StatesOrSignals<R>
	infix fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> StatesOrSignals<T1>.or(stateOrSignal: KClass<T2>): StatesOrSignals<R>
}

interface ErrorHolder
{
	val throwable: Throwable
}

interface EntryHolder
{
	val previousState: STATE
}

interface ExitHolder
{
	val nextState: STATE
}

interface PayloadHolder
{
	val payload: Any?
}

interface TransitionHolder
{
	val transitionCount: Long
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

interface ErrorActionDSL : ErrorHolder, ActionDSL
interface ErrorFunctionDSL : ErrorActionDSL, FunctionDSL

interface EntryActionDSL : EntryHolder, ActionDSL
interface EntryFunctionDSL : EntryActionDSL, FunctionDSL
interface EntryErrorActionDSL : EntryActionDSL, ErrorActionDSL
interface EntryErrorFunctionDSL : EntryErrorActionDSL, EntryFunctionDSL, ErrorFunctionDSL

interface ExitActionDSL : ExitHolder, ActionDSL
interface ExitErrorActionDSL : ExitActionDSL, ErrorActionDSL

interface PayloadActionDSL : PayloadHolder, ActionDSL
interface PayloadFunctionDSL : PayloadActionDSL, FunctionDSL
interface PayloadErrorActionDSL : PayloadActionDSL, ErrorActionDSL
interface PayloadErrorFunctionDSL : PayloadErrorActionDSL, PayloadFunctionDSL, ErrorFunctionDSL

interface TransitionActionDSL : TransitionHolder, ActionDSL
interface TransitionErrorActionDSL : TransitionActionDSL, ErrorActionDSL

/*###################################################################################################################################
 * typealias for action
 *###################################################################################################################################*/

typealias EntryAction<T> = EntryActionDSL.(signal: T) -> Unit
typealias EntryFunction<T> = EntryFunctionDSL.(signal: T) -> Any?
typealias EntryErrorAction<T> = EntryErrorActionDSL.(signal: T) -> Unit
typealias EntryErrorFunction<T> = EntryErrorFunctionDSL.(signal: T) -> Any?

typealias InputAction<T> = PayloadActionDSL.(signal: T) -> Unit
typealias InputFunction<T> = PayloadFunctionDSL.(signal: T) -> Any?
typealias InputErrorAction<T> = PayloadErrorActionDSL.(signal: T) -> Unit
typealias InputErrorFunction<T> = PayloadErrorFunctionDSL.(signal: T) -> Any?

typealias ExitAction<T> = ExitActionDSL.(signal: T) -> Unit
typealias ExitErrorAction<T> = ExitErrorActionDSL.(signal: T) -> Unit

typealias StateErrorCallback = ErrorActionDSL.(signal: SIGNAL) -> Unit

typealias TransitionCallback = TransitionActionDSL.(from: STATE, signal: SIGNAL, to: STATE) -> Unit
typealias TransitionFallback = TransitionErrorActionDSL.(from: STATE, signal: SIGNAL, to: STATE) -> Unit

typealias MachineErrorCallback = ErrorActionDSL.() -> Unit

typealias DaemonCallback = PayloadActionDSL.() -> Unit
typealias DaemonFallback = PayloadErrorActionDSL.() -> Unit

/*###################################################################################################################################
 * typealias for template
 *###################################################################################################################################*/

typealias StateTemplate<T> = KotlmataState.Init.(state: T) -> Unit

typealias MachineBase = KotlmataMachine.Base.(machine: KotlmataMachine) -> Unit
typealias MachineTemplate = KotlmataMachine.Init.(machine: KotlmataMachine) -> KotlmataMachine.Init.End

typealias DaemonBase = KotlmataDaemon.Base.(daemon: KotlmataDaemon) -> Unit
typealias DaemonTemplate = KotlmataDaemon.Init.(daemon: KotlmataDaemon) -> KotlmataMachine.Init.End
