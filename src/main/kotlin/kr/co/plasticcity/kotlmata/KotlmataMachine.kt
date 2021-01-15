package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataMachine.*
import kr.co.plasticcity.kotlmata.KotlmataMachine.Companion.By
import kr.co.plasticcity.kotlmata.KotlmataMachine.Companion.Extends
import kr.co.plasticcity.kotlmata.KotlmataMachine.RuleDefine.*
import kr.co.plasticcity.kotlmata.KotlmataMutableMachine.Update
import kr.co.plasticcity.kotlmata.KotlmataMutableMachine.Update.*
import kr.co.plasticcity.kotlmata.Log.normal
import kr.co.plasticcity.kotlmata.Log.simple
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

interface KotlmataMachine
{
	companion object
	{
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			block: MachineTemplate
		): KotlmataMachine = KotlmataMachineImpl(name, logLevel, block = block)
		
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG
		) = object : Extends<MachineBase, MachineTemplate, KotlmataMachine>
		{
			override fun extends(base: MachineBase) = object : By<MachineTemplate, KotlmataMachine>
			{
				override fun by(template: MachineTemplate) = invoke(name, logLevel) { machine ->
					base(machine)
					template(machine)
				}
			}
			
			override fun by(template: MachineTemplate) = invoke(name, logLevel, template)
		}
		
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			block: MachineTemplate
		) = lazy {
			invoke(name, logLevel, block)
		}
		
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG
		) = object : Extends<MachineBase, MachineTemplate, Lazy<KotlmataMachine>>
		{
			override fun extends(base: MachineBase) = object : By<MachineTemplate, Lazy<KotlmataMachine>>
			{
				override fun by(template: MachineTemplate) = lazy {
					invoke(name, logLevel) extends base by template
				}
			}
			
			override fun by(template: MachineTemplate) = lazy {
				invoke(name, logLevel, template)
			}
		}
		
		interface Extends<B, T, R> : By<T, R>
		{
			infix fun extends(base: B): By<T, R>
		}
		
		interface By<T, R>
		{
			infix fun by(template: T): R
		}
	}
	
	@KotlmataMarker
	interface Base : StateDefine, RuleDefine
	{
		val on: On
		
		interface On
		{
			infix fun transition(callback: TransitionCallback): Catch
			infix fun error(block: MachineErrorCallback)
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
	
	interface StateDefine
	{
		operator fun <S : STATE> S.invoke(block: StateTemplate<S>)
		infix fun <S : T, T : STATE> S.extends(template: StateTemplate<T>): With<S>
		infix fun <S : T, T : STATE> S.update(block: KotlmataMutableState.Update.(state: T) -> Unit)
		
		interface With<S : STATE>
		{
			infix fun with(block: StateTemplate<S>)
		}
		
		infix fun <S : STATE> S.action(action: EntryAction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL> = function(action)
		infix fun <S : STATE> S.function(function: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: T): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(signals: StatesOrSignals<T>): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(predicate: (T) -> Boolean): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T> S.via(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = via { t: T -> range.contains(t) }
	}
	
	interface RuleDefine : StatesOrSignalsDefinable
	{
		fun any.of(vararg args: `STATE or SIGNAL`): AnyOf
		fun any.except(vararg args: `STATE or SIGNAL`): AnyExcept
		
		interface AnyOf : List<`STATE or SIGNAL`>
		interface AnyExcept : List<`STATE or SIGNAL`>
		
		infix fun STATE.x(signal: SIGNAL): RuleLeft
		infix fun STATE.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun STATE.x(signals: StatesOrSignals<*>): RuleAssignable
		infix fun STATE.x(any: any): RuleLeft
		infix fun STATE.x(anyOf: AnyOf): RuleAssignable
		infix fun STATE.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> STATE.x(predicate: (T) -> Boolean): RuleLeft
		infix fun <T> STATE.x(range: ClosedRange<T>): RuleAssignable where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
		infix fun StatesOrSignals<*>.x(signal: SIGNAL): RuleAssignable
		infix fun StatesOrSignals<*>.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun StatesOrSignals<*>.x(signals: StatesOrSignals<*>): RuleAssignable
		infix fun StatesOrSignals<*>.x(any: any): RuleAssignable
		infix fun StatesOrSignals<*>.x(anyOf: AnyOf): RuleAssignable
		infix fun StatesOrSignals<*>.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> StatesOrSignals<*>.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T> StatesOrSignals<*>.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
		infix fun any.x(signal: SIGNAL): RuleLeft
		infix fun any.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun any.x(signals: StatesOrSignals<*>): RuleAssignable
		infix fun any.x(any: any): RuleLeft
		infix fun any.x(anyOf: AnyOf): RuleAssignable
		infix fun any.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> any.x(predicate: (T) -> Boolean): RuleLeft
		infix fun <T> any.x(range: ClosedRange<T>): RuleAssignable where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
		infix fun AnyOf.x(signal: SIGNAL): RuleAssignable
		infix fun AnyOf.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun AnyOf.x(signals: StatesOrSignals<*>): RuleAssignable
		infix fun AnyOf.x(any: any): RuleAssignable
		infix fun AnyOf.x(anyOf: AnyOf): RuleAssignable
		infix fun AnyOf.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> AnyOf.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T> AnyOf.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
		infix fun AnyExcept.x(signal: SIGNAL): RuleAssignable
		infix fun AnyExcept.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun AnyExcept.x(signals: StatesOrSignals<*>): RuleAssignable
		infix fun AnyExcept.x(any: any): RuleAssignable
		infix fun AnyExcept.x(anyOf: AnyOf): RuleAssignable
		infix fun AnyExcept.x(anyExcept: AnyExcept): RuleAssignable
		infix fun <T : SIGNAL> AnyExcept.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T> AnyExcept.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
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
				infix fun via(signals: StatesOrSignals<*>)
				infix fun via(any: any)
				infix fun via(anyOf: AnyOf)
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
	
	fun input(signal: SIGNAL, payload: Any? = null)
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null)
	
	/**
	 * @param block Called if the state is switched and the next state's entry function returns a signal.
	 */
	fun input(signal: SIGNAL, payload: Any? = null, block: (FunctionDSL.Sync) -> Unit)
	
	/**
	 * @param block Called if the state is switched and the next state's entry function returns a signal.
	 */
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null, block: (FunctionDSL.Sync) -> Unit)
	
	@Deprecated("KClass<T> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<out Any>, payload: Any? = null)
	
	@Deprecated("KClass<T> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<out Any>, payload: Any? = null, block: (FunctionDSL.Sync) -> Unit)
}

interface KotlmataMutableMachine : KotlmataMachine
{
	companion object
	{
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			block: MachineTemplate
		): KotlmataMutableMachine = KotlmataMachineImpl(name, logLevel, block = block)
		
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG
		) = object : Extends<MachineBase, MachineTemplate, KotlmataMutableMachine>
		{
			override fun extends(base: MachineBase) = object : By<MachineTemplate, KotlmataMutableMachine>
			{
				override fun by(template: MachineTemplate) = invoke(name, logLevel) { machine ->
					base(machine)
					template(machine)
				}
			}
			
			override fun by(template: MachineTemplate) = invoke(name, logLevel, template)
		}
		
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			block: MachineTemplate
		) = lazy {
			invoke(name, logLevel, block)
		}
		
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG
		) = object : Extends<MachineBase, MachineTemplate, Lazy<KotlmataMutableMachine>>
		{
			override fun extends(base: MachineBase) = object : By<MachineTemplate, Lazy<KotlmataMutableMachine>>
			{
				override fun by(template: MachineTemplate) = lazy {
					invoke(name, logLevel) extends base by template
				}
			}
			
			override fun by(template: MachineTemplate) = lazy {
				invoke(name, logLevel, template)
			}
		}
		
		internal fun create(
			name: String,
			logLevel: Int,
			prefix: String,
			block: MachineTemplate
		): KotlmataMutableMachine = KotlmataMachineImpl(name, logLevel, prefix, block)
	}
	
	@KotlmataMarker
	interface Update : StateDefine, RuleDefine
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
			infix fun state(from: STATE)
			infix fun state(all: all)
			
			infix fun rule(ruleLeft: RuleLeft)
			infix fun rule(all: all)
		}
	}
	
	operator fun invoke(block: Update.() -> Unit) = update(block)
	infix fun update(block: Update.() -> Unit)
}

private class TransitionDef(val callback: TransitionCallback, val fallback: TransitionFallback? = null, val finally: TransitionCallback? = null)

private class KotlmataMachineImpl(
	override val name: String,
	val logLevel: Int,
	val prefix: String = "Machine[$name]:",
	block: MachineTemplate
) : KotlmataMutableMachine
{
	private val stateMap: MutableMap<STATE, KotlmataMutableState<out STATE>> = HashMap()
	private val ruleMap: Mutable2DMap<STATE, SIGNAL, STATE> = Mutable2DMap()
	private val testerMap: MutableMap<STATE, Tester> = HashMap()
	
	private var onTransition: TransitionDef? = null
	private var onError: MachineErrorCallback? = null
	
	private var transitionCounter: Long = 0
	
	private lateinit var currentTag: STATE
	private val currentState: KotlmataState<out STATE>
		get() = stateMap[currentTag] ?: Log.e(prefix.trimEnd(), currentTag, currentTag) { FAILED_TO_GET_STATE }
	
	init
	{
		logLevel.normal(prefix, name) { MACHINE_BUILD }
		UpdateImpl(init = block)
		logLevel.normal(prefix) { MACHINE_END }
	}
	
	private inline fun tryCatchReturn(block: () -> Any?): Any? = try
	{
		block()
	}
	catch (e: Throwable)
	{
		onError?.let { onError ->
			ErrorActionReceiver(e).onError()
		} ?: throw e
	}
	
	private fun TransitionDef.call(from: STATE, signal: SIGNAL, to: STATE)
	{
		val transitionCount = transitionCounter++
		try
		{
			callback.also { callback ->
				TransitionActionReceiver(transitionCount).callback(from, signal, to)
			}
		}
		catch (e: Throwable)
		{
			fallback?.also { fallback ->
				TransitionErrorActionReceiver(transitionCount, e).fallback(from, signal, to)
			} ?: onError?.also { onError ->
				ErrorActionReceiver(e).onError()
			} ?: throw e
		}
		finally
		{
			finally?.also { finally ->
				TransitionActionReceiver(transitionCount).finally(from, signal, to)
			}
		}
	}
	
	private fun defaultInput(begin: FunctionDSL.Sync)
	{
		var next: FunctionDSL.Sync? = begin
		while (next != null) next.also {
			next = null
			if (it.type == null) input(it.signal, it.payload) { sync ->
				next = sync
			}
			else input(it.signal, it.type, it.payload) { sync ->
				next = sync
			}
		}
	}
	
	override fun input(signal: SIGNAL, payload: Any?)
	{
		defaultInput(FunctionDSL.Sync(signal, null, payload))
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?)
	{
		defaultInput(FunctionDSL.Sync(signal, type as KClass<SIGNAL>, payload))
	}
	
	override fun input(signal: SIGNAL, payload: Any?, block: (FunctionDSL.Sync) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.test(from: STATE, signal: SIGNAL): STATE?
		{
			return testerMap[from]?.test(signal)?.let { predicate ->
				this[predicate]
			}
		}
		
		fun next(from: STATE): STATE? = ruleMap[from]?.let { map2 ->
			return map2[signal] ?: map2.test(from, signal) ?: map2[signal::class] ?: map2[any]
		}
		
		val from = currentTag
		val currentState = currentState
		
		logLevel.normal(prefix, signal, payload) { MACHINE_INPUT }
		tryCatchReturn {
			currentState.input(signal, payload)
		}.also { inputReturn ->
			if (inputReturn == stay) return
		}.convertToSync()?.also { sync ->
			logLevel.normal(prefix, sync.signal, sync.typeString, sync.payload) { MACHINE_RETURN_SYNC_INPUT }
			block(sync)
		} ?: run {
			next(from) ?: next(any)
		}?.let { to ->
			when (to)
			{
				is stay -> null
				is self -> currentState
				in stateMap -> stateMap[to]
				else ->
				{
					Log.w(prefix.trimEnd(), from, signal, to) { TRANSITION_FAILED }
					null
				}
			}
		}?.also { nextState ->
			val to = nextState.tag
			tryCatchReturn { currentState.exit(signal, to) }
			logLevel.simple(prefix, from, signal, to) {
				if (logLevel >= NORMAL)
					MACHINE_TRANSITION_TAB
				else
					MACHINE_TRANSITION
			}
			onTransition?.call(from, signal, to)
			currentTag = to
			tryCatchReturn { nextState.entry(from, signal) }.convertToSync()?.also { sync ->
				logLevel.normal(prefix, sync.signal, sync.typeString, sync.payload) { MACHINE_RETURN_SYNC_INPUT }
				block(sync)
			}
		}
		logLevel.normal(prefix) { MACHINE_END }
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?, block: (FunctionDSL.Sync) -> Unit)
	{
		fun next(from: STATE): STATE? = ruleMap[from]?.let { map2 ->
			return map2[type] ?: map2[any]
		}
		
		val from = currentTag
		val currentState = currentState
		
		logLevel.normal(prefix, signal, "${type.simpleName}::class", payload) { MACHINE_TYPED_INPUT }
		tryCatchReturn {
			currentState.input(signal, type, payload)
		}.also { inputReturn ->
			if (inputReturn == stay) return
		}.convertToSync()?.also { sync ->
			logLevel.normal(prefix, sync.signal, sync.typeString, sync.payload) { MACHINE_RETURN_SYNC_INPUT }
			block(sync)
		} ?: run {
			next(from) ?: next(any)
		}?.let { to ->
			when (to)
			{
				is stay -> null
				is self -> currentState
				in stateMap -> stateMap[to]
				else ->
				{
					Log.w(prefix.trimEnd(), from, "${type.simpleName}::class", to) { TRANSITION_FAILED }
					null
				}
			}
		}?.also { nextState ->
			val to = nextState.tag
			tryCatchReturn { currentState.exit(signal, type, to) }
			logLevel.simple(prefix, from, "${type.simpleName}::class", to) {
				if (logLevel > SIMPLE)
					MACHINE_TRANSITION_TAB
				else
					MACHINE_TRANSITION
			}
			onTransition?.call(from, signal, to)
			currentTag = to
			tryCatchReturn { nextState.entry(from, signal, type) }.convertToSync()?.also { sync ->
				logLevel.normal(prefix, sync.signal, sync.typeString, sync.payload) { MACHINE_RETURN_SYNC_INPUT }
				block(sync)
			}
		}
		logLevel.normal(prefix) { MACHINE_END }
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<out Any>, payload: Any?)
	{
		throw IllegalArgumentException("KClass<T> type cannot be used as input.")
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<out Any>, payload: Any?, block: (FunctionDSL.Sync) -> Unit)
	{
		throw IllegalArgumentException("KClass<T> type cannot be used as input.")
	}
	
	override fun update(block: Update.() -> Unit)
	{
		logLevel.normal(prefix, currentTag) { MACHINE_UPDATE }
		UpdateImpl(update = block)
		logLevel.normal(prefix) { MACHINE_END }
	}
	
	override fun toString(): String
	{
		return "KotlmataMachine[$name]{${hashCode().toString(16)}}"
	}
	
	private inner class UpdateImpl(
		init: (MachineTemplate)? = null,
		update: (Update.() -> Unit)? = null
	) : Init, Update, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_OBJECT } })
	{
		override val on = object : Base.On
		{
			override fun transition(callback: TransitionCallback): Base.Catch
			{
				this@UpdateImpl shouldNot expired
				onTransition = TransitionDef(callback)
				logLevel.normal(prefix) { MACHINE_REGISTER_ON_TRANSITION }
				return object : Base.Catch
				{
					override fun catch(fallback: TransitionFallback): Base.Finally
					{
						this@UpdateImpl shouldNot expired
						onTransition = TransitionDef(callback, fallback)
						return object : Base.Finally
						{
							override fun finally(finally: TransitionCallback)
							{
								this@UpdateImpl shouldNot expired
								onTransition = TransitionDef(callback, fallback, finally)
							}
						}
					}
					
					override fun finally(finally: TransitionCallback)
					{
						this@UpdateImpl shouldNot expired
						onTransition = TransitionDef(callback, null, finally)
					}
				}
			}
			
			override fun error(block: MachineErrorCallback)
			{
				this@UpdateImpl shouldNot expired
				onError = block
				logLevel.normal(prefix) { MACHINE_REGISTER_ON_ERROR }
			}
		}
		
		override val start = object : Init.Start
		{
			override fun at(state: STATE): Init.End
			{
				this@UpdateImpl shouldNot expired
				
				stateMap[state]?.also {
					currentTag = state
					if (state !== `Initial state for KotlmataDaemon`)
					{
						logLevel.normal(prefix, state) { MACHINE_START_AT }
					}
				} ?: Log.e(prefix.trimEnd(), state) { UNDEFINED_START_STATE }
				
				return Init.End()
			}
		}
		
		override fun <S : STATE> S.invoke(block: StateTemplate<S>)
		{
			this@UpdateImpl shouldNot expired
			stateMap[this] = KotlmataMutableState(this, logLevel, "$prefix$tab", block)
			if (this !== `Initial state for KotlmataDaemon`)
			{
				logLevel.normal(prefix, this) { MACHINE_ADD_STATE }
			}
		}
		
		override fun <S : T, T : STATE> S.extends(template: StateTemplate<T>) = object : StateDefine.With<S>
		{
			val state: KotlmataMutableState<S>
			
			init
			{
				this@UpdateImpl shouldNot expired
				state = KotlmataMutableState(this@extends, logLevel, "$prefix$tab", template)
				stateMap[this@extends] = state
				if (this@extends !== `Initial state for KotlmataDaemon`)
				{
					logLevel.normal(prefix, this@extends) { MACHINE_ADD_STATE }
				}
			}
			
			override fun with(block: StateTemplate<S>)
			{
				this@UpdateImpl shouldNot expired
				state.update(block)
			}
		}
		
		@Suppress("UNCHECKED_CAST")
		override fun <S : T, T : STATE> S.update(block: KotlmataMutableState.Update.(state: T) -> Unit)
		{
			this@UpdateImpl shouldNot expired
			stateMap[this]?.update(block as KotlmataMutableState.Update.(STATE) -> Unit)
			logLevel.normal(prefix, this) { MACHINE_UPDATE_STATE }
		}
		
		override fun <S : STATE> S.function(function: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
		{
			this@UpdateImpl shouldNot expired
			val state = KotlmataMutableState(this, logLevel, "$prefix$tab") {
				entry function function
			}
			stateMap[this] = state
			if (this !== `Initial state for KotlmataDaemon`)
			{
				logLevel.normal(prefix, this) { MACHINE_ADD_STATE }
			}
			return object : KotlmataState.Entry.Catch<SIGNAL>
			{
				override fun intercept(intercept: EntryErrorFunction<SIGNAL>): KotlmataState.Entry.Finally<SIGNAL>
				{
					this@UpdateImpl shouldNot expired
					state {
						entry function function intercept intercept
					}
					return object : KotlmataState.Entry.Finally<SIGNAL>
					{
						override fun finally(finally: EntryAction<SIGNAL>)
						{
							state {
								entry function function intercept intercept finally finally
							}
						}
					}
				}
				
				override fun finally(finally: EntryAction<SIGNAL>)
				{
					state {
						entry function function finally finally
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@UpdateImpl shouldNot expired
				val state = KotlmataMutableState(this@via, logLevel, "$prefix$tab") {
					entry via signal function function
				}
				stateMap[this@via] = state
				if (this@via !== `Initial state for KotlmataDaemon`)
				{
					logLevel.normal(prefix, this@via) { MACHINE_ADD_STATE }
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(intercept: EntryErrorFunction<T>): KotlmataState.Entry.Finally<T>
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signal function function intercept intercept
						}
						return object : KotlmataState.Entry.Finally<T>
						{
							override fun finally(finally: EntryAction<T>)
							{
								this@UpdateImpl shouldNot expired
								state {
									entry via signal function function intercept intercept finally finally
								}
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
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: T) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@UpdateImpl shouldNot expired
				val state = KotlmataMutableState(this@via, logLevel, "$prefix$tab") {
					entry via signal function function
				}
				stateMap[this@via] = state
				if (this@via !== `Initial state for KotlmataDaemon`)
				{
					logLevel.normal(prefix, this@via) { MACHINE_ADD_STATE }
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(intercept: EntryErrorFunction<T>): KotlmataState.Entry.Finally<T>
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signal function function intercept intercept
						}
						return object : KotlmataState.Entry.Finally<T>
						{
							override fun finally(finally: EntryAction<T>)
							{
								this@UpdateImpl shouldNot expired
								state {
									entry via signal function function intercept intercept finally finally
								}
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
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signals: StatesOrSignals<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@UpdateImpl shouldNot expired
				val state = KotlmataMutableState(this@via, logLevel, "$prefix$tab") {
					entry via signals function function
				}
				stateMap[this@via] = state
				if (this@via !== `Initial state for KotlmataDaemon`)
				{
					logLevel.normal(prefix, this@via) { MACHINE_ADD_STATE }
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(intercept: EntryErrorFunction<T>): KotlmataState.Entry.Finally<T>
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via signals function function intercept intercept
						}
						return object : KotlmataState.Entry.Finally<T>
						{
							override fun finally(finally: EntryAction<T>)
							{
								this@UpdateImpl shouldNot expired
								state {
									entry via signals function function intercept intercept finally finally
								}
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
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(predicate: (T) -> Boolean) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@UpdateImpl shouldNot expired
				val state = KotlmataMutableState(this@via, logLevel, "$prefix$tab") {
					entry via predicate function function
				}
				stateMap[this@via] = state
				if (this@via !== `Initial state for KotlmataDaemon`)
				{
					logLevel.normal(prefix, this@via) { MACHINE_ADD_STATE }
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(intercept: EntryErrorFunction<T>): KotlmataState.Entry.Finally<T>
					{
						this@UpdateImpl shouldNot expired
						state {
							entry via predicate function function intercept intercept
						}
						return object : KotlmataState.Entry.Finally<T>
						{
							override fun finally(finally: EntryAction<T>)
							{
								this@UpdateImpl shouldNot expired
								state {
									entry via predicate function function intercept intercept finally finally
								}
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
		}
		
		/*###################################################################################################################################
		 * Transition rules
		 *###################################################################################################################################*/
		
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> T1.OR(stateOrSignal: T2) = OR(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> T1.OR(stateOrSignal: KClass<T2>) = OR(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> KClass<T1>.OR(stateOrSignal: T2) = OR(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> KClass<T1>.OR(stateOrSignal: KClass<T2>) = OR(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> StatesOrSignals<T1>.OR(stateOrSignal: T2) = OR(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> StatesOrSignals<T1>.OR(stateOrSignal: KClass<T2>) = OR(this, stateOrSignal)
		
		override fun any.of(vararg args: `STATE or SIGNAL`): AnyOf = object : AnyOf, List<`STATE or SIGNAL`> by listOf(*args)
		{ /* empty */ }
		
		override fun any.except(vararg args: `STATE or SIGNAL`): AnyExcept = object : AnyExcept, List<`STATE or SIGNAL`> by listOf(*args)
		{ /* empty */ }
		
		private fun StatesOrSignals<*>.toAnyOf() = object : AnyOf, List<`STATE or SIGNAL`> by this
		{ /* empty */ }
		
		private fun ruleLeft(from: STATE, signal: SIGNAL) = object : RuleLeft
		{
			override val from: STATE = from
			override val signal: SIGNAL = signal
			
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				ruleMap[from, signal] = to
				logLevel.normal(prefix, from, signal, to) { MACHINE_ADD_RULE }
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
		
		@Suppress("FunctionName")
		private fun `state x anyOf`(from: STATE, anyOfSignal: AnyOf) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				anyOfSignal.forEach { signal ->
					from x signal %= to
				}
			}
		}
		
		@Suppress("FunctionName")
		private fun `state x anyExcept`(from: STATE, anyExceptSignal: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				from x any %= to
				anyExceptSignal.forEach { signal ->
					ruleMap[from, signal] ?: run {
						from x signal %= stay
					}
				}
			}
		}
		
		@Suppress("FunctionName")
		private fun `anyOf x signal`(anyOfFrom: AnyOf, signal: SIGNAL) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				anyOfFrom.forEach { from ->
					from x signal %= to
				}
			}
		}
		
		@Suppress("FunctionName")
		private fun `anyOf x anyOf`(anyOfFrom: AnyOf, anyOfSignal: AnyOf) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				anyOfFrom.forEach { from ->
					from x anyOfSignal %= to
				}
			}
		}
		
		@Suppress("FunctionName")
		private fun `anyOf x anyExcept`(anyOfFrom: AnyOf, anyExceptSignal: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				anyOfFrom.forEach { from ->
					from x anyExceptSignal %= to
				}
			}
		}
		
		@Suppress("FunctionName")
		private fun `anyExcept x signal`(anyExceptFrom: AnyExcept, signal: SIGNAL) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				any x signal %= to
				anyExceptFrom.forEach { from ->
					ruleMap[from, signal] ?: run {
						from x signal %= stay
					}
				}
			}
		}
		
		@Suppress("FunctionName")
		private fun `anyExcept x anyOf`(anyExceptFrom: AnyExcept, anyOfSignal: AnyOf) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				anyOfSignal.forEach { signal ->
					anyExceptFrom x signal %= to
				}
			}
		}
		
		@Suppress("FunctionName")
		private fun `anyExcept x anyExcept`(anyExceptFrom: AnyExcept, anyExceptSignal: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				anyExceptFrom x any %= to
				any x anyExceptSignal %= to
			}
		}
		
		override fun STATE.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun STATE.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun STATE.x(signals: StatesOrSignals<*>) = this x signals.toAnyOf()
		override fun STATE.x(any: any) = ruleLeft(this, any)
		override fun STATE.x(anyOf: AnyOf) = `state x anyOf`(this, anyOf)
		override fun STATE.x(anyExcept: AnyExcept) = `state x anyExcept`(this, anyExcept)
		override fun <T : SIGNAL> STATE.x(predicate: (T) -> Boolean) = this x this.store(predicate)
		
		override fun StatesOrSignals<*>.x(signal: SIGNAL) = toAnyOf() x signal
		override fun StatesOrSignals<*>.x(signal: KClass<out SIGNAL>) = toAnyOf() x signal
		override fun StatesOrSignals<*>.x(signals: StatesOrSignals<*>) = toAnyOf() x signals.toAnyOf()
		override fun StatesOrSignals<*>.x(any: any) = toAnyOf() x any
		override fun StatesOrSignals<*>.x(anyOf: AnyOf) = toAnyOf() x anyOf
		override fun StatesOrSignals<*>.x(anyExcept: AnyExcept) = toAnyOf() x anyExcept
		override fun <T : SIGNAL> StatesOrSignals<*>.x(predicate: (T) -> Boolean) = this.toAnyOf() x predicate
		
		override fun any.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun any.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun any.x(signals: StatesOrSignals<*>) = this x signals.toAnyOf()
		override fun any.x(any: any) = ruleLeft(this, any)
		override fun any.x(anyOf: AnyOf) = `state x anyOf`(this, anyOf)
		override fun any.x(anyExcept: AnyExcept) = `state x anyExcept`(this, anyExcept)
		override fun <T : SIGNAL> any.x(predicate: (T) -> Boolean) = this x this.store(predicate)
		
		override fun AnyOf.x(signal: SIGNAL) = `anyOf x signal`(this, signal)
		override fun AnyOf.x(signal: KClass<out SIGNAL>) = `anyOf x signal`(this, signal)
		override fun AnyOf.x(signals: StatesOrSignals<*>) = this x signals.toAnyOf()
		override fun AnyOf.x(any: any) = `anyOf x signal`(this, any)
		override fun AnyOf.x(anyOf: AnyOf) = `anyOf x anyOf`(this, anyOf)
		override fun AnyOf.x(anyExcept: AnyExcept) = `anyOf x anyExcept`(this, anyExcept)
		override fun <T : SIGNAL> AnyOf.x(predicate: (T) -> Boolean) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				this@x.forEach { from ->
					from x predicate %= to
				}
			}
		}
		
		override fun AnyExcept.x(signal: SIGNAL) = `anyExcept x signal`(this, signal)
		override fun AnyExcept.x(signal: KClass<out SIGNAL>) = `anyExcept x signal`(this, signal)
		override fun AnyExcept.x(signals: StatesOrSignals<*>) = this x signals.toAnyOf()
		override fun AnyExcept.x(any: any) = `anyExcept x signal`(this, any)
		override fun AnyExcept.x(anyOf: AnyOf) = `anyExcept x anyOf`(this, anyOf)
		override fun AnyExcept.x(anyExcept: AnyExcept) = `anyExcept x anyExcept`(this, anyExcept)
		override fun <T : SIGNAL> AnyExcept.x(predicate: (T) -> Boolean) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@UpdateImpl shouldNot expired
				any x predicate %= to
				this@x.forEach { from ->
					ruleMap[from, predicate] ?: run {
						from x predicate %= stay
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Chaining transition rule
		 *###################################################################################################################################*/
		
		override val chain: Chain = object : Chain
		{
			override fun from(state: STATE): Chain.To
			{
				this@UpdateImpl shouldNot expired
				val states: MutableList<STATE> = mutableListOf()
				states.add(state)
				return object : Chain.To, Chain.Via
				{
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
					
					override fun via(signals: StatesOrSignals<*>) = via(signals.toAnyOf())
					
					override fun via(any: any)
					{
						this@UpdateImpl shouldNot expired
						loop { from, to -> from x any %= to }
					}
					
					override fun via(anyOf: AnyOf)
					{
						this@UpdateImpl shouldNot expired
						loop { from, to -> from x anyOf %= to }
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
			override fun state(from: STATE)
			{
				this@UpdateImpl shouldNot expired
				stateMap -= from
				logLevel.normal(prefix, from) { MACHINE_DELETE_STATE }
			}
			
			override fun state(all: all)
			{
				this@UpdateImpl shouldNot expired
				stateMap.clear()
				logLevel.normal(prefix) { MACHINE_DELETE_STATE_ALL }
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
		
		init
		{
			init?.also { it(this@KotlmataMachineImpl) } ?: update?.also { it() }
			expire()
		}
	}
}
