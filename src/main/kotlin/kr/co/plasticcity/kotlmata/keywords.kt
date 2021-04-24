@file:Suppress("FunctionName", "ClassName")

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
{
	override fun toString(): String = this::class.java.simpleName
}

/**
 * In State:
 * ```
 * input signal String::class function { signal ->
 *     if (signal == "A") stay // do not transition
 *     else Unit // transition if rule exists
 * }
 * ```
 *
 * In Machine:
 * ```
 * "state" x "signal" %= stay
 * any x "signal" %= stay
 * ```
 */
object stay
{
	override fun toString(): String = this::class.java.simpleName
}

/**
 * In Machine:
 * ```
 * "state" x "signal" %= self
 * any x "signal" %= self
 * ```
 */
object self
{
	override fun toString(): String = this::class.java.simpleName
}

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

interface Signals<T : SIGNAL> : List<SIGNAL>
interface SignalsDefinable
{
	infix fun <T : R, U : R, R : SIGNAL> T.OR(signal: U): Signals<R>
	infix fun <T : R, U : R, R : SIGNAL> T.OR(signal: KClass<U>): Signals<R>
	infix fun <T : R, U : R, R : SIGNAL> KClass<T>.OR(signal: U): Signals<R>
	infix fun <T : R, U : R, R : SIGNAL> KClass<T>.OR(signal: KClass<U>): Signals<R>
	infix fun <T : R, U : R, R : SIGNAL> Signals<T>.OR(signal: U): Signals<R>
	infix fun <T : R, U : R, R : SIGNAL> Signals<T>.OR(signal: KClass<U>): Signals<R>
}

interface ErrorHolder
{
	val throwable: Throwable
}

interface PrevHolder
{
	val prevState: STATE
}

interface NextHolder
{
	val nextState: STATE
}

interface TransitionHolder
{
	val transitionCount: Long
}

interface PayloadHolder
{
	val payload: Any?
}

@KotlmataMarker
interface ActionDSL

interface FunctionDSL : ActionDSL
{
	@Suppress("UNCHECKED_CAST")
	open class Return internal constructor(
		val signal: SIGNAL,
		val type: KClass<SIGNAL> = signal::class as KClass<SIGNAL>,
		val payload: Any? = null
	)
	
	class ReturnWithoutPayload internal constructor(signal: SIGNAL, type: KClass<SIGNAL>) : Return(signal, type)
	
	@Suppress("UNCHECKED_CAST")
	infix fun <S : T, T : SIGNAL> S.`as`(type: KClass<T>) = ReturnWithoutPayload(this, type as KClass<SIGNAL>)
	
	infix fun SIGNAL.with(payload: Any?) = Return(this, payload = payload)
	infix fun ReturnWithoutPayload.with(payload: Any?) = Return(signal, type, payload)
}

interface ErrorActionDSL : ErrorHolder, ActionDSL
interface ErrorFunctionDSL : ErrorActionDSL, FunctionDSL

interface EntryActionDSL : PrevHolder, TransitionHolder, PayloadHolder, ActionDSL
interface EntryFunctionDSL : EntryActionDSL, FunctionDSL
interface EntryErrorActionDSL : EntryActionDSL, ErrorActionDSL
interface EntryErrorFunctionDSL : EntryErrorActionDSL, EntryFunctionDSL, ErrorFunctionDSL

interface InputActionDSL : TransitionHolder, PayloadHolder, ActionDSL
interface InputFunctionDSL : InputActionDSL, FunctionDSL
interface InputErrorActionDSL : InputActionDSL, ErrorActionDSL
interface InputErrorFunctionDSL : InputErrorActionDSL, InputFunctionDSL, ErrorFunctionDSL

interface ExitActionDSL : NextHolder, TransitionHolder, PayloadHolder, ActionDSL
interface ExitErrorActionDSL : ExitActionDSL, ErrorActionDSL

interface TransitionActionDSL : TransitionHolder, ActionDSL
interface TransitionErrorActionDSL : TransitionActionDSL, ErrorActionDSL

interface PayloadActionDSL : PayloadHolder, ActionDSL
interface PayloadErrorActionDSL : PayloadActionDSL, ErrorActionDSL

/*###################################################################################################################################
 * typealias for action
 *###################################################################################################################################*/
typealias EntryAction<T> = EntryActionDSL.(signal: T) -> Unit
typealias EntryFunction<T> = EntryFunctionDSL.(signal: T) -> Any?
typealias EntryErrorAction<T> = EntryErrorActionDSL.(signal: T) -> Unit
typealias EntryErrorFunction<T> = EntryErrorFunctionDSL.(signal: T) -> Any?

typealias InputAction<T> = InputActionDSL.(signal: T) -> Unit
typealias InputFunction<T> = InputFunctionDSL.(signal: T) -> Any?
typealias InputErrorAction<T> = InputErrorActionDSL.(signal: T) -> Unit
typealias InputErrorFunction<T> = InputErrorFunctionDSL.(signal: T) -> Any?

typealias ExitAction<T> = ExitActionDSL.(signal: T) -> Unit
typealias ExitErrorAction<T> = ExitErrorActionDSL.(signal: T) -> Unit

typealias StateSimpleCallback = ActionDSL.() -> Unit
typealias StateSimpleFallback = ErrorActionDSL.() -> Unit
typealias StateFallback = ErrorActionDSL.(signal: SIGNAL?) -> Unit

typealias TransitionCallback = TransitionActionDSL.(from: STATE, signal: SIGNAL, to: STATE) -> Unit
typealias TransitionFallback = TransitionErrorActionDSL.(from: STATE, signal: SIGNAL, to: STATE) -> Unit

typealias MachineFallback = ErrorActionDSL.() -> Unit

typealias DaemonSimpleCallback = ActionDSL.() -> Unit
typealias DaemonSimpleFallback = ErrorActionDSL.() -> Unit
typealias DaemonCallback = PayloadActionDSL.() -> Unit
typealias DaemonFallback = PayloadErrorActionDSL.() -> Unit

/*###################################################################################################################################
 * typealias for template
 *###################################################################################################################################*/
typealias StateTemplate = StateDefine<STATE>
typealias StateDefine<T> = KotlmataState.Init.(state: T) -> Unit

typealias MachineTemplate = KotlmataMachine.Base.(machine: KotlmataMachine) -> Unit
typealias MachineDefine = KotlmataMachine.Init.(machine: KotlmataMachine) -> KotlmataMachine.Init.End

typealias DaemonTemplate = KotlmataDaemon.Base.(daemon: KotlmataDaemon) -> Unit
typealias DaemonDefine = KotlmataDaemon.Init.(daemon: KotlmataDaemon) -> KotlmataMachine.Init.End
