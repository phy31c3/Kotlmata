@file:Suppress("FunctionName")

package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataMachine.*
import kr.co.plasticcity.kotlmata.KotlmataMachine.Companion.By
import kr.co.plasticcity.kotlmata.KotlmataMachine.Companion.Extends
import kr.co.plasticcity.kotlmata.KotlmataMachine.RuleDefinable.*
import kr.co.plasticcity.kotlmata.KotlmataMachine.StateDefinable.StateTemplates
import kr.co.plasticcity.kotlmata.KotlmataMutableMachine.Update
import kr.co.plasticcity.kotlmata.KotlmataMutableMachine.Update.Delete
import kr.co.plasticcity.kotlmata.KotlmataMutableMachine.Update.Has
import kr.co.plasticcity.kotlmata.Log.normal
import kr.co.plasticcity.kotlmata.Log.simple
import kotlin.reflect.KClass

interface KotlmataMachine
{
	companion object
	{
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			block: MachineDefine
		): KotlmataMachine = KotlmataMachineImpl(name, logLevel, block = block)
		
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG
		) = object : Extends<KotlmataMachine>
		{
			override fun extends(template: MachineTemplate) = object : By<KotlmataMachine>
			{
				override fun by(define: MachineDefine) = invoke(name, logLevel) { machine ->
					template(machine)
					define(machine)
				}
			}
			
			override fun extends(templates: MachineTemplates) = object : By<KotlmataMachine>
			{
				override fun by(define: MachineDefine) = invoke(name, logLevel) { machine ->
					templates.forEach { template ->
						template(machine)
					}
					define(machine)
				}
			}
			
			override fun by(define: MachineDefine) = invoke(name, logLevel, define)
		}
		
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			block: MachineDefine
		) = lazy {
			invoke(name, logLevel, block)
		}
		
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG
		) = object : Extends<Lazy<KotlmataMachine>>
		{
			override fun extends(template: MachineTemplate) = object : By<Lazy<KotlmataMachine>>
			{
				override fun by(define: MachineDefine) = lazy {
					invoke(name, logLevel) extends template by define
				}
			}
			
			override fun extends(templates: MachineTemplates) = object : By<Lazy<KotlmataMachine>>
			{
				override fun by(define: MachineDefine) = lazy {
					invoke(name, logLevel) extends templates by define
				}
			}
			
			override fun by(define: MachineDefine) = lazy {
				invoke(name, logLevel, define)
			}
		}
		
		interface Extends<R> : By<R>
		{
			infix fun extends(template: MachineTemplate): By<R>
			infix fun extends(templates: MachineTemplates): By<R>
		}
		
		interface By<R>
		{
			infix fun by(define: MachineDefine): R
		}
	}
	
	@KotlmataMarker
	interface Base : StateDefinable, RuleDefinable
	{
		val on: On
		
		interface On
		{
			infix fun transition(callback: TransitionCallback): Catch
			infix fun error(block: MachineFallback)
		}
		
		interface Catch : Finally
		{
			infix fun catch(fallback: TransitionFallback): Finally
		}
		
		interface Finally
		{
			infix fun finally(finally: TransitionCallback)
		}
	}
	
	interface Init : Base
	{
		val start: Start
		
		interface Start
		{
			infix fun at(state: STATE): End
		}
		
		class End internal constructor()
	}
	
	interface StateDefinable
	{
		operator fun <S : STATE> S.invoke(block: StateDefine<S>)
		infix fun <S : STATE> S.by(block: StateDefine<S>) = invoke(block)
		infix fun <S : STATE> S.extends(template: StateTemplate): By<S>
		infix fun <S : STATE> S.extends(templates: StateTemplates): By<S>
		infix fun <S : STATE> S.update(block: KotlmataMutableState.Update.(state: S) -> Unit)
		
		interface By<S : STATE>
		{
			infix fun by(block: StateDefine<S>)
		}
		
		interface StateTemplates : List<StateTemplate>
		
		operator fun StateTemplate.plus(template: StateTemplate): StateTemplates
		operator fun StateTemplates.plus(template: StateTemplate): StateTemplates
		
		infix fun <S : STATE> S.action(action: EntryAction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL> = function(action)
		infix fun <S : STATE> S.function(function: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: T): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(signals: Signals<T>): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(predicate: (T) -> Boolean): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T> S.via(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = via { t: T -> range.contains(t) }
	}
	
	interface RuleDefinable : SignalsDefinable
	{
		interface States : List<STATE>
		
		infix fun STATE.AND(state: STATE): States
		infix fun States.AND(state: STATE): States
		
		interface AnyExcept : List<SIGNAL>
		
		operator fun any.invoke(vararg except: SIGNAL): AnyExcept
		
		infix fun STATE.x(signal: SIGNAL): RuleLeft
		infix fun STATE.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun STATE.x(signals: Signals<*>): RuleAssignable
		infix fun STATE.x(any: any): RuleLeft
		infix fun STATE.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> STATE.x(predicate: (T) -> Boolean): RuleLeft
		infix fun <T> STATE.x(range: ClosedRange<T>): RuleAssignable where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
		infix fun States.x(signal: SIGNAL): RuleAssignable
		infix fun States.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun States.x(signals: Signals<*>): RuleAssignable
		infix fun States.x(any: any): RuleAssignable
		infix fun States.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> States.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T> States.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
		infix fun any.x(signal: SIGNAL): RuleLeft
		infix fun any.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun any.x(signals: Signals<*>): RuleAssignable
		infix fun any.x(any: any): RuleLeft
		infix fun any.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> any.x(predicate: (T) -> Boolean): RuleLeft
		infix fun <T> any.x(range: ClosedRange<T>): RuleAssignable where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
		infix fun AnyExcept.x(signal: SIGNAL): RuleAssignable
		infix fun AnyExcept.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun AnyExcept.x(signals: Signals<*>): RuleAssignable
		infix fun AnyExcept.x(any: any): RuleAssignable
		infix fun AnyExcept.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> AnyExcept.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T> AnyExcept.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
		@Deprecated("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).", level = DeprecationLevel.ERROR)
		infix fun Signals<*>.x(signal: SIGNAL)
		
		@Deprecated("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).", level = DeprecationLevel.ERROR)
		infix fun Signals<*>.x(signal: KClass<out SIGNAL>)
		
		@Deprecated("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).", level = DeprecationLevel.ERROR)
		infix fun Signals<*>.x(signals: Signals<*>)
		
		@Deprecated("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).", level = DeprecationLevel.ERROR)
		infix fun Signals<*>.x(any: any)
		
		@Deprecated("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).", level = DeprecationLevel.ERROR)
		infix fun Signals<*>.x(anyExcept: AnyExcept)
		
		@Deprecated("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).", level = DeprecationLevel.ERROR)
		infix fun <T : SIGNAL> Signals<*>.x(predicate: (T) -> Boolean)
		
		@Deprecated("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).", level = DeprecationLevel.ERROR)
		infix fun <T> Signals<*>.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T>
		
		/**
		 * For chaining transition rule
		 *
		 * `chain from "state1" to "state2" to "state3" to ... via "signal"`
		 */
		val chain: Chain
		
		interface Chain
		{
			infix fun from(state: STATE): To
			
			interface To
			{
				infix fun to(state: STATE): Via
			}
			
			interface Via : To
			{
				infix fun via(signal: SIGNAL)
				infix fun via(signal: KClass<out SIGNAL>)
				infix fun via(signals: Signals<*>)
				infix fun via(any: any)
				infix fun via(anyExcept: AnyExcept)
				infix fun <T : SIGNAL> via(predicate: (T) -> Boolean)
				infix fun <T> via(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this via { t: T -> range.contains(t) }
			}
		}
	}
	
	interface RuleAssignable
	{
		operator fun remAssign(to: STATE)
		operator fun remAssign(self: self) = remAssign(to = self)
		operator fun remAssign(stay: stay) = remAssign(to = stay)
	}
	
	interface RuleLeft : RuleAssignable
	{
		val from: STATE
		val signal: SIGNAL
	}
	
	val name: String
	val isReleased: Boolean
	
	fun release()
	
	@Suppress("UNCHECKED_CAST")
	fun input(signal: SIGNAL, payload: Any? = null) = input(signal, signal::class as KClass<SIGNAL>, payload)
	fun <S : T, T : SIGNAL> input(signal: S, type: KClass<T>, payload: Any? = null)
	
	@Deprecated("KClass<*> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<*>, payload: Any? = null)
}

interface KotlmataMutableMachine : KotlmataMachine
{
	companion object
	{
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			block: MachineDefine
		): KotlmataMutableMachine = KotlmataMachineImpl(name, logLevel, block = block)
		
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG
		) = object : Extends<KotlmataMutableMachine>
		{
			override fun extends(template: MachineTemplate) = object : By<KotlmataMutableMachine>
			{
				override fun by(define: MachineDefine) = invoke(name, logLevel) { machine ->
					template(machine)
					define(machine)
				}
			}
			
			override fun extends(templates: MachineTemplates) = object : By<KotlmataMutableMachine>
			{
				override fun by(define: MachineDefine) = invoke(name, logLevel) { machine ->
					templates.forEach { template ->
						template(machine)
					}
					define(machine)
				}
			}
			
			override fun by(define: MachineDefine) = invoke(name, logLevel, define)
		}
		
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			block: MachineDefine
		) = lazy {
			invoke(name, logLevel, block)
		}
		
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG
		) = object : Extends<Lazy<KotlmataMutableMachine>>
		{
			override fun extends(template: MachineTemplate) = object : By<Lazy<KotlmataMutableMachine>>
			{
				override fun by(define: MachineDefine) = lazy {
					invoke(name, logLevel) extends template by define
				}
			}
			
			override fun extends(templates: MachineTemplates) = object : By<Lazy<KotlmataMutableMachine>>
			{
				override fun by(define: MachineDefine) = lazy {
					invoke(name, logLevel) extends templates by define
				}
			}
			
			override fun by(define: MachineDefine) = lazy {
				invoke(name, logLevel, define)
			}
		}
		
		internal fun create(
			name: String,
			logLevel: Int,
			prefix: String,
			block: MachineDefine
		): KotlmataMutableMachine = KotlmataMachineImpl(name, logLevel, prefix, block)
	}
	
	@KotlmataMarker
	interface Update : StateDefinable, RuleDefinable
	{
		val currentState: STATE
		val has: Has
		val delete: Delete
		
		interface Has
		{
			infix fun state(state: STATE): Boolean
			infix fun rule(ruleLeft: RuleLeft): Boolean
		}
		
		interface Delete
		{
			infix fun state(state: STATE)
			infix fun state(all: all)
			
			infix fun rule(ruleLeft: RuleLeft)
			infix fun rule(all: all)
		}
	}
	
	operator fun invoke(block: Update.() -> Unit) = update(block)
	infix fun update(block: Update.() -> Unit)
}

internal interface KotlmataInternalMachine : KotlmataMutableMachine
{
	@Suppress("UNCHECKED_CAST")
	fun input(signal: SIGNAL, payload: Any? = null, block: (FunctionDSL.Return) -> Unit) = input(signal, signal::class as KClass<SIGNAL>, payload, block)
	fun <S : T, T : SIGNAL> input(signal: S, type: KClass<T>, payload: Any? = null, block: (FunctionDSL.Return) -> Unit)
}

private object Released

private class TransitionDef(
	val callback: TransitionCallback,
	val fallback: TransitionFallback? = null,
	val finally: TransitionCallback? = null
)

private class Except(
	val exceptStates: List<STATE>?,
	val exceptSignals: List<SIGNAL>?,
	val to: STATE
)
{
	override fun toString() = to.toString()
}

private class KotlmataMachineImpl(
	override val name: String,
	val logLevel: Int,
	val prefix: String = "Machine[$name]:",
	block: MachineDefine
) : KotlmataInternalMachine
{
	private val prefixWithTab = if (logLevel < NORMAL) prefix else prefix + tab
	
	private val stateMap: MutableMap<STATE, KotlmataMutableState<out STATE>> = HashMap()
	private val ruleMap: Mutable2DMap<STATE, SIGNAL, STATE> = Mutable2DMap()
	private val testerMap: MutableMap<STATE, Tester> = HashMap()
	
	private var onTransition: TransitionDef? = null
	private var onError: MachineFallback? = null
	
	private var transitionCounter: Long = 0
	
	private lateinit var currentTag: STATE
	
	override val isReleased: Boolean
		get() = currentTag == Released
	
	init
	{
		try
		{
			logLevel.normal(prefix, name) { MACHINE_BUILD }
			UpdateImpl().use {
				it.block(this)
			}
		}
		finally
		{
			logLevel.normal(prefix) { MACHINE_END }
		}
	}
	
	private inline fun ifReleased(block: () -> Unit)
	{
		if (currentTag == Released)
		{
			Log.w(prefix) { MACHINE_USING_RELEASED_MACHINE }
			block()
		}
	}
	
	private inline fun runStateFunction(block: () -> Any?): Any?
	{
		return try
		{
			block()
		}
		catch (e: Throwable)
		{
			onError?.let { onError ->
				ErrorActionReceiver(e).onError()
			} ?: throw e
		}
	}
	
	override fun release()
	{
		if (currentTag != Released)
		{
			try
			{
				logLevel.normal(prefix) { MACHINE_RELEASE }
				runStateFunction { stateMap[currentTag]?.clear() }
			}
			finally
			{
				stateMap.clear()
				ruleMap.clear()
				testerMap.clear()
				onTransition = null
				onError = null
				currentTag = Released
				logLevel.normal(prefix) { MACHINE_DONE }
				logLevel.normal(prefix) { MACHINE_END }
			}
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <S : T, T : SIGNAL> input(signal: S, type: KClass<T>, payload: Any?)
	{
		var next: FunctionDSL.Return? = FunctionDSL.Return(signal, type as KClass<SIGNAL>, payload)
		while (next != null) next.also {
			next = null
			input(it.signal, it.type, it.payload) { sync ->
				next = sync
			}
		}
	}
	
	override fun <S : T, T : SIGNAL> input(signal: S, type: KClass<T>, payload: Any?, block: (FunctionDSL.Return) -> Unit)
	{
		ifReleased { return }
		
		val from = currentTag
		
		fun TransitionDef.call(to: STATE)
		{
			try
			{
				callback.also { callback ->
					TransitionActionReceiver(transitionCounter).callback(from, signal, to)
				}
			}
			catch (e: Throwable)
			{
				fallback?.also { fallback ->
					TransitionErrorActionReceiver(transitionCounter, e).fallback(from, signal, to)
				} ?: onError?.also { onError ->
					ErrorActionReceiver(e).onError()
				} ?: throw e
			}
			finally
			{
				finally?.also { finally ->
					TransitionActionReceiver(transitionCounter).finally(from, signal, to)
				}
			}
		}
		
		fun STATE.filterExcept() = when
		{
			this !is Except -> this
			exceptStates?.contains(from) == true -> null
			exceptSignals == null -> this.to
			exceptSignals.contains(signal) -> null
			exceptSignals.contains(type) -> null
			else -> this.to
		}
		
		fun MutableMap<SIGNAL, STATE>.predicateFilterExcept(): STATE?
		{
			testerMap[any]?.test(signal) { predicate ->
				this[predicate]?.filterExcept()?.let { to ->
					return to
				}
			}
			return null
		}
		
		fun MutableMap<SIGNAL, STATE>.predicate(): STATE?
		{
			return testerMap[from]?.test(signal)?.let { predicate ->
				this[predicate]
			}
		}
		
		try
		{
			logLevel.normal(prefix, signal, type.string, payload) { MACHINE_INPUT }
			stateMap[from]?.also { currentState ->
				runStateFunction {
					currentState.input(signal, type, transitionCounter, payload)
				}.also { inputReturn ->
					if (inputReturn == stay)
					{
						return
					}
				}.convertToSync()?.also { sync ->
					logLevel.normal(prefix, sync.signal, sync.type.string, sync.payload) { MACHINE_RETURN_SYNC_INPUT }
					block(sync)
				} ?: run {
					ruleMap[from]?.let { `from x` ->
						`from x`[signal]
							?: `from x`.predicate()
							?: `from x`[type]
							?: `from x`[any]?.filterExcept()
					} ?: ruleMap[any]?.let { `any x` ->
						`any x`[signal]?.filterExcept()
							?: `any x`.predicateFilterExcept()
							?: `any x`[type]?.filterExcept()
							?: `any x`[any]?.filterExcept()
					}
				}?.let { to ->
					when (to)
					{
						is stay -> null
						is self -> currentState
						in stateMap -> stateMap[to]
						else ->
						{
							Log.w(prefixWithTab, from, signal, to) { MACHINE_TRANSITION_FAILED }
							null
						}
					}
				}?.also { nextState ->
					val to = nextState.tag
					runStateFunction { currentState.exit(signal, type, transitionCounter, payload, to) }
					runStateFunction { currentState.clear() }
					logLevel.simple(prefixWithTab, from, signal, to) { MACHINE_TRANSITION }
					currentTag = to
					++transitionCounter
					onTransition?.call(to)
					runStateFunction { nextState.entry(from, signal, type, transitionCounter, payload) }.convertToSync()?.also { sync ->
						logLevel.normal(prefix, sync.signal, sync.type.string, sync.payload) { MACHINE_RETURN_SYNC_INPUT }
						block(sync)
					}
				}
			} ?: Log.e(prefixWithTab, from, from) { FAILED_TO_GET_STATE }
		}
		finally
		{
			logLevel.normal(prefix) { MACHINE_END }
		}
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<*>, payload: Any?)
	{
		throw IllegalArgumentException("KClass<*> type cannot be used as input.")
	}
	
	override fun update(block: Update.() -> Unit)
	{
		ifReleased { return }
		
		try
		{
			logLevel.normal(prefix, currentTag) { MACHINE_UPDATE }
			UpdateImpl().use {
				it.block()
			}
		}
		finally
		{
			logLevel.normal(prefix) { MACHINE_DONE }
			logLevel.normal(prefix) { MACHINE_END }
		}
	}
	
	override fun toString(): String
	{
		return "KotlmataMachine[$name]{${hashCode().toString(16)}}"
	}
	
	private inner class UpdateImpl : Init, Update, SignalsDefinable by SignalsDefinableImpl, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_OBJECT } })
	{
		override val on = object : Base.On
		{
			override fun transition(callback: TransitionCallback) = object : Base.Catch
			{
				init
				{
					this@UpdateImpl shouldNot expired
					onTransition = TransitionDef(callback)
					logLevel.normal(prefix) { MACHINE_SET_ON_TRANSITION }
				}
				
				override fun catch(fallback: TransitionFallback) = object : Base.Finally
				{
					init
					{
						this@UpdateImpl shouldNot expired
						onTransition = TransitionDef(callback, fallback)
					}
					
					override fun finally(finally: TransitionCallback)
					{
						this@UpdateImpl shouldNot expired
						onTransition = TransitionDef(callback, fallback, finally)
					}
				}
				
				override fun finally(finally: TransitionCallback)
				{
					this@UpdateImpl shouldNot expired
					onTransition = TransitionDef(callback, null, finally)
				}
			}
			
			override fun error(block: MachineFallback)
			{
				this@UpdateImpl shouldNot expired
				onError = block
				logLevel.normal(prefix) { MACHINE_SET_ON_ERROR }
			}
		}
		
		override val start = object : Init.Start
		{
			override fun at(state: STATE): Init.End
			{
				this@UpdateImpl shouldNot expired
				
				stateMap[state]?.also {
					currentTag = state
					if (state !== Initial_state_for_KotlmataDaemon)
					{
						logLevel.normal(prefix, state) { MACHINE_START_AT }
					}
				} ?: Log.e(prefixWithTab, state) { UNDEFINED_START_STATE }
				
				return Init.End()
			}
		}
		
		override fun <S : STATE> S.invoke(block: StateDefine<S>)
		{
			this@UpdateImpl shouldNot expired
			stateMap[this] = KotlmataMutableState(this, logLevel, "$prefix$tab", block)
			if (this !== Initial_state_for_KotlmataDaemon)
			{
				logLevel.normal(prefix, this) { MACHINE_ADD_STATE }
			}
		}
		
		override fun <S : STATE> S.extends(template: StateTemplate) = object : StateDefinable.By<S>
		{
			val state: KotlmataMutableState<S>
			
			init
			{
				this@UpdateImpl shouldNot expired
				state = KotlmataMutableState(this@extends, logLevel, "$prefix$tab", template)
				stateMap[this@extends] = state
				if (this@extends !== Initial_state_for_KotlmataDaemon)
				{
					logLevel.normal(prefix, this@extends) { MACHINE_ADD_STATE }
				}
			}
			
			override fun by(block: StateDefine<S>)
			{
				this@UpdateImpl shouldNot expired
				state.update(block)
			}
		}
		
		override fun <S : STATE> S.extends(templates: StateTemplates) = object : StateDefinable.By<S>
		{
			val state: KotlmataMutableState<S>
			
			init
			{
				this@UpdateImpl shouldNot expired
				state = KotlmataMutableState(this@extends, logLevel, "$prefix$tab", templates[0])
				stateMap[this@extends] = state
				if (this@extends !== Initial_state_for_KotlmataDaemon)
				{
					logLevel.normal(prefix, this@extends) { MACHINE_ADD_STATE }
				}
				for (i in 1..templates.lastIndex)
				{
					state.update(templates[i])
				}
			}
			
			override fun by(block: StateDefine<S>)
			{
				this@UpdateImpl shouldNot expired
				state.update(block)
			}
		}
		
		override fun StateTemplate.plus(template: StateTemplate): StateTemplates = object : StateTemplates, List<StateTemplate> by listOf(this, template)
		{ /* empty */ }
		
		override fun StateTemplates.plus(template: StateTemplate): StateTemplates = object : StateTemplates, List<StateTemplate> by (this as List<StateTemplate>) + template
		{ /* empty */ }
		
		@Suppress("UNCHECKED_CAST")
		override fun <S : STATE> S.update(block: KotlmataMutableState.Update.(state: S) -> Unit)
		{
			this@UpdateImpl shouldNot expired
			stateMap[this]?.update(block as KotlmataMutableState.Update.(STATE) -> Unit)
			logLevel.normal(prefix, this) { MACHINE_UPDATE_STATE }
		}
		
		override fun <S : STATE> S.function(function: EntryFunction<SIGNAL>) = object : KotlmataState.Entry.Catch<SIGNAL>
		{
			val state = KotlmataMutableState(this@function, logLevel, "$prefix$tab") {
				entry function function
			}
			
			init
			{
				this@UpdateImpl shouldNot expired
				stateMap[this@function] = state
				if (this@function !== Initial_state_for_KotlmataDaemon)
				{
					logLevel.normal(prefix, this) { MACHINE_ADD_STATE }
				}
			}
			
			override fun intercept(intercept: EntryErrorFunction<SIGNAL>) = object : KotlmataState.Entry.Finally<SIGNAL>
			{
				init
				{
					this@UpdateImpl shouldNot expired
					state {
						entry function function intercept intercept
					}
				}
				
				override fun finally(finally: EntryAction<SIGNAL>)
				{
					this@UpdateImpl shouldNot expired
					state {
						entry function function intercept intercept finally finally
					}
				}
			}
			
			override fun finally(finally: EntryAction<SIGNAL>)
			{
				this@UpdateImpl shouldNot expired
				state {
					entry function function finally finally
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>) = object : KotlmataState.Entry.Catch<T>
			{
				val state = KotlmataMutableState(this@via, logLevel, "$prefix$tab") {
					entry via signal function function
				}
				
				init
				{
					this@UpdateImpl shouldNot expired
					stateMap[this@via] = state
					if (this@via !== Initial_state_for_KotlmataDaemon)
					{
						logLevel.normal(prefix, this@via) { MACHINE_ADD_STATE }
					}
				}
				
				override fun intercept(intercept: EntryErrorFunction<T>) = object : KotlmataState.Entry.Finally<T>
				{
					init
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signal function function intercept intercept
						}
					}
					
					override fun finally(finally: EntryAction<T>)
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signal function function intercept intercept finally finally
						}
					}
				}
				
				override fun finally(finally: EntryAction<T>)
				{
					this@UpdateImpl shouldNot expired
					state {
						entry via signal function function finally finally
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: T) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>) = object : KotlmataState.Entry.Catch<T>
			{
				val state = KotlmataMutableState(this@via, logLevel, "$prefix$tab") {
					entry via signal function function
				}
				
				init
				{
					this@UpdateImpl shouldNot expired
					stateMap[this@via] = state
					if (this@via !== Initial_state_for_KotlmataDaemon)
					{
						logLevel.normal(prefix, this@via) { MACHINE_ADD_STATE }
					}
				}
				
				override fun intercept(intercept: EntryErrorFunction<T>) = object : KotlmataState.Entry.Finally<T>
				{
					init
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signal function function intercept intercept
						}
					}
					
					override fun finally(finally: EntryAction<T>)
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signal function function intercept intercept finally finally
						}
					}
				}
				
				override fun finally(finally: EntryAction<T>)
				{
					this@UpdateImpl shouldNot expired
					state {
						entry via signal function function finally finally
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signals: Signals<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>) = object : KotlmataState.Entry.Catch<T>
			{
				val state = KotlmataMutableState(this@via, logLevel, "$prefix$tab") {
					entry via signals function function
				}
				
				init
				{
					this@UpdateImpl shouldNot expired
					stateMap[this@via] = state
					if (this@via !== Initial_state_for_KotlmataDaemon)
					{
						logLevel.normal(prefix, this@via) { MACHINE_ADD_STATE }
					}
				}
				
				override fun intercept(intercept: EntryErrorFunction<T>) = object : KotlmataState.Entry.Finally<T>
				{
					init
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signals function function intercept intercept
						}
					}
					
					override fun finally(finally: EntryAction<T>)
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signals function function intercept intercept finally finally
						}
					}
				}
				
				override fun finally(finally: EntryAction<T>)
				{
					this@UpdateImpl shouldNot expired
					state {
						entry via signals function function finally finally
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(predicate: (T) -> Boolean) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>) = object : KotlmataState.Entry.Catch<T>
			{
				val state = KotlmataMutableState(this@via, logLevel, "$prefix$tab") {
					entry via predicate function function
				}
				
				init
				{
					this@UpdateImpl shouldNot expired
					stateMap[this@via] = state
					if (this@via !== Initial_state_for_KotlmataDaemon)
					{
						logLevel.normal(prefix, this@via) { MACHINE_ADD_STATE }
					}
				}
				
				override fun intercept(intercept: EntryErrorFunction<T>) = object : KotlmataState.Entry.Finally<T>
				{
					init
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via predicate function function intercept intercept
						}
					}
					
					override fun finally(finally: EntryAction<T>)
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via predicate function function intercept intercept finally finally
						}
					}
				}
				
				override fun finally(finally: EntryAction<T>)
				{
					this@UpdateImpl shouldNot expired
					state {
						entry via predicate function function finally finally
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Transition rules
		 *###################################################################################################################################*/
		override fun STATE.AND(state: STATE): States = object : States, List<SIGNAL> by listOf(this, state)
		{ /* empty */ }
		
		override fun States.AND(state: STATE): States = object : States, List<SIGNAL> by this + state
		{ /* empty */ }
		
		override fun any.invoke(vararg except: SIGNAL): AnyExcept = object : AnyExcept, List<SIGNAL> by listOf(*except)
		{ /* empty */ }
		
		private fun ruleLeft(from: STATE, signal: SIGNAL) = object : RuleLeft
		{
			override val from: STATE = from
			override val signal: SIGNAL = signal
			
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				ruleMap[from, signal] = to
				if (from !== Initial_state_for_KotlmataDaemon)
				{
					logLevel.normal(prefix, from, signal, to) { MACHINE_ADD_RULE }
				}
			}
		}
		
		private fun <T : SIGNAL> STATE.store(predicate: (T) -> Boolean): SIGNAL
		{
			this@UpdateImpl shouldNot expired
			testerMap[this]?.also { tester ->
				tester += predicate
			} ?: Tester().also { tester ->
				tester += predicate
				testerMap[this] = tester
			}
			return predicate
		}
		
		private fun state_x_signals(from: STATE, signals: Signals<*>) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				signals.forEach { signal ->
					from x signal %= to
				}
			}
		}
		
		private fun state_x_except(from: STATE, exceptSignals: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				from x any %= Except(null, exceptSignals, to)
			}
		}
		
		private fun states_x_signal(states: States, signal: SIGNAL) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				states.forEach { from ->
					from x signal %= to
				}
			}
		}
		
		private fun states_x_signals(states: States, signals: Signals<*>) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				states.forEach { from ->
					from x signals %= to
				}
			}
		}
		
		private fun states_x_except(states: States, exceptSignals: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				states.forEach { from ->
					from x exceptSignals %= to
				}
			}
		}
		
		private fun <T : SIGNAL> states_x_predicate(states: States, predicate: (T) -> Boolean) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				states.forEach { from ->
					from x predicate %= to
				}
			}
		}
		
		private fun except_x_signal(exceptStates: AnyExcept, signal: SIGNAL) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				any x signal %= Except(exceptStates, null, to)
			}
		}
		
		private fun except_x_signals(exceptStates: AnyExcept, signals: Signals<*>) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				signals.forEach { signal ->
					exceptStates x signal %= to
				}
			}
		}
		
		private fun except_x_except(exceptStates: AnyExcept, exceptSignals: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				any x any %= Except(exceptStates, exceptSignals, to)
			}
		}
		
		private fun <T : SIGNAL> except_x_predicate(exceptStates: AnyExcept, predicate: (T) -> Boolean) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				any x predicate %= Except(exceptStates, null, to)
			}
		}
		
		override fun STATE.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun STATE.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun STATE.x(signals: Signals<*>) = state_x_signals(this, signals)
		override fun STATE.x(any: any) = ruleLeft(this, any)
		override fun STATE.x(anyExcept: AnyExcept) = state_x_except(this, anyExcept)
		override fun <T : SIGNAL> STATE.x(predicate: (T) -> Boolean) = this x this.store(predicate)
		
		override fun States.x(signal: SIGNAL) = states_x_signal(this, signal)
		override fun States.x(signal: KClass<out SIGNAL>) = states_x_signal(this, signal)
		override fun States.x(signals: Signals<*>) = states_x_signals(this, signals)
		override fun States.x(any: any) = states_x_signal(this, any)
		override fun States.x(anyExcept: AnyExcept) = states_x_except(this, anyExcept)
		override fun <T : SIGNAL> States.x(predicate: (T) -> Boolean) = states_x_predicate(this, predicate)
		
		override fun any.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun any.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun any.x(signals: Signals<*>) = state_x_signals(this, signals)
		override fun any.x(any: any) = ruleLeft(this, any)
		override fun any.x(anyExcept: AnyExcept) = state_x_except(this, anyExcept)
		override fun <T : SIGNAL> any.x(predicate: (T) -> Boolean) = this x this.store(predicate)
		
		override fun AnyExcept.x(signal: SIGNAL) = except_x_signal(this, signal)
		override fun AnyExcept.x(signal: KClass<out SIGNAL>) = except_x_signal(this, signal)
		override fun AnyExcept.x(signals: Signals<*>) = except_x_signals(this, signals)
		override fun AnyExcept.x(any: any) = except_x_signal(this, any)
		override fun AnyExcept.x(anyExcept: AnyExcept) = except_x_except(this, anyExcept)
		override fun <T : SIGNAL> AnyExcept.x(predicate: (T) -> Boolean) = except_x_predicate(this, predicate)
		
		@Suppress("OverridingDeprecatedMember")
		override fun Signals<*>.x(signal: SIGNAL)
		{
			throw IllegalArgumentException("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).")
		}
		
		@Suppress("OverridingDeprecatedMember")
		override fun Signals<*>.x(signal: KClass<out SIGNAL>)
		{
			throw IllegalArgumentException("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).")
		}
		
		@Suppress("OverridingDeprecatedMember")
		override fun Signals<*>.x(signals: Signals<*>)
		{
			throw IllegalArgumentException("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).")
		}
		
		@Suppress("OverridingDeprecatedMember")
		override fun Signals<*>.x(any: any)
		{
			throw IllegalArgumentException("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).")
		}
		
		@Suppress("OverridingDeprecatedMember")
		override fun Signals<*>.x(anyExcept: AnyExcept)
		{
			throw IllegalArgumentException("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).")
		}
		
		@Suppress("OverridingDeprecatedMember")
		override fun <T : SIGNAL> Signals<*>.x(predicate: (T) -> Boolean)
		{
			throw IllegalArgumentException("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).")
		}
		
		@Suppress("OverridingDeprecatedMember")
		override fun <T> Signals<*>.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T>
		{
			throw IllegalArgumentException("Signals cannot be used as lhs of transition rule. Use (a AND b) instead of (a OR b).")
		}
		
		/*###################################################################################################################################
		 * Chaining transition rule
		 *###################################################################################################################################*/
		override val chain: Chain = object : Chain
		{
			override fun from(state: STATE) = object : Chain.To, Chain.Via
			{
				val states: MutableList<STATE> = mutableListOf()
				
				init
				{
					this@UpdateImpl shouldNot expired
					states.add(state)
				}
				
				override fun to(state: STATE): Chain.Via
				{
					this@UpdateImpl shouldNot expired
					states.add(state)
					return this
				}
				
				override fun via(signal: SIGNAL)
				{
					this@UpdateImpl shouldNot expired
					loop { from, to -> from x signal %= to }
				}
				
				override fun via(signal: KClass<out SIGNAL>)
				{
					this@UpdateImpl shouldNot expired
					loop { from, to -> from x signal %= to }
				}
				
				override fun via(signals: Signals<*>)
				{
					this@UpdateImpl shouldNot expired
					loop { from, to -> from x signals %= to }
				}
				
				override fun via(any: any)
				{
					this@UpdateImpl shouldNot expired
					loop { from, to -> from x any %= to }
				}
				
				override fun via(anyExcept: AnyExcept)
				{
					this@UpdateImpl shouldNot expired
					loop { from, to -> from x anyExcept %= to }
				}
				
				override fun <T : SIGNAL> via(predicate: (T) -> Boolean)
				{
					this@UpdateImpl shouldNot expired
					loop { from, to -> from x predicate %= to }
				}
				
				private fun loop(block: (from: STATE, to: STATE) -> Unit)
				{
					for (i in 0 until states.lastIndex)
					{
						block(states[i], states[i + 1])
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Update
		 *###################################################################################################################################*/
		override val currentState: STATE
			get()
			{
				this@UpdateImpl shouldNot expired
				return currentTag
			}
		
		override val has = object : Has
		{
			override fun state(state: STATE) = state in stateMap
			override fun rule(ruleLeft: RuleLeft) = ruleMap[ruleLeft.from, ruleLeft.signal] != null
		}
		
		override val delete = object : Delete
		{
			override fun state(state: STATE)
			{
				this@UpdateImpl shouldNot expired
				if (state == currentTag)
				{
					Log.w(prefixWithTab, state) { MACHINE_CANNOT_DELETE_CURRENT_STATE }
				}
				else
				{
					stateMap -= state
					logLevel.normal(prefix, state) { MACHINE_DELETE_STATE }
				}
			}
			
			override fun state(all: all)
			{
				this@UpdateImpl shouldNot expired
				stateMap[currentTag]?.also { currentState ->
					stateMap.clear()
					stateMap[currentTag] = currentState
					logLevel.normal(prefix) { MACHINE_DELETE_STATE_ALL }
				} ?: Log.e(prefixWithTab, currentTag, currentTag) { FAILED_TO_GET_STATE }
			}
			
			override fun rule(ruleLeft: RuleLeft) = ruleLeft.run {
				this@UpdateImpl shouldNot expired
				ruleMap[from]?.let { map2 ->
					map2 -= signal
				}
				testerMap[from]?.remove(signal)
				logLevel.normal(prefix, from, signal) { MACHINE_DELETE_RULE }
			}
			
			override fun rule(all: all)
			{
				this@UpdateImpl shouldNot expired
				ruleMap.clear()
				testerMap.clear()
				logLevel.normal(prefix) { MACHINE_DELETE_RULE_ALL }
			}
		}
	}
}
