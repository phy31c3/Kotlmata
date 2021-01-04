package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataMachine.*
import kr.co.plasticcity.kotlmata.KotlmataMachine.RuleDefine.*
import kr.co.plasticcity.kotlmata.KotlmataMutableMachine.Modifier
import kr.co.plasticcity.kotlmata.KotlmataMutableMachine.Modifier.*
import kr.co.plasticcity.kotlmata.Log.normal
import kr.co.plasticcity.kotlmata.Log.simple
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

interface KotlmataMachine<T : MACHINE>
{
	companion object
	{
		operator fun <T : MACHINE> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			block: MachineTemplate<T>
		): KotlmataMachine<T> = KotlmataMachineImpl(tag, logLevel, block = block)
		
		operator fun <T : MACHINE> invoke(
			tag: T,
			logLevel: Int = NO_LOG
		) = object : InvokeBy<T>
		{
			override fun by(block: MachineTemplate<T>) = invoke(tag, logLevel, block)
		}
		
		fun <T : MACHINE> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			block: MachineTemplate<T>
		) = lazy {
			invoke(tag, logLevel, block)
		}
		
		fun <T : MACHINE> lazy(
			tag: T,
			logLevel: Int = NO_LOG
		) = object : LazyBy<T>
		{
			override fun by(block: MachineTemplate<T>) = lazy { invoke(tag, logLevel, block) }
		}
		
		interface InvokeBy<T : MACHINE>
		{
			infix fun by(block: MachineTemplate<T>): KotlmataMachine<T>
		}
		
		interface LazyBy<T : MACHINE>
		{
			infix fun by(block: MachineTemplate<T>): Lazy<KotlmataMachine<T>>
		}
	}
	
	@KotlmataMarker
	interface Init : StateDefine, RuleDefine
	{
		val on: On
		val start: Start
		
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
		infix fun <T : SIGNAL> STATE.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T> STATE.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
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
		infix fun <T : SIGNAL> any.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T> any.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		
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
	
	val tag: T
	
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

interface KotlmataMutableMachine<T : MACHINE> : KotlmataMachine<T>
{
	companion object
	{
		operator fun <T : MACHINE> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			block: MachineTemplate<T>
		): KotlmataMutableMachine<T> = KotlmataMachineImpl(tag, logLevel, block = block)
		
		operator fun <T : MACHINE> invoke(
			tag: T,
			logLevel: Int = NO_LOG
		) = object : InvokeBy<T>
		{
			override fun by(block: MachineTemplate<T>) = invoke(tag, logLevel, block)
		}
		
		fun <T : MACHINE> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			block: MachineTemplate<T>
		) = lazy {
			invoke(tag, logLevel, block)
		}
		
		fun <T : MACHINE> lazy(
			tag: T,
			logLevel: Int = NO_LOG
		) = object : LazyBy<T>
		{
			override fun by(block: MachineTemplate<T>) = lazy { invoke(tag, logLevel, block) }
		}
		
		interface InvokeBy<T : MACHINE>
		{
			infix fun by(block: MachineTemplate<T>): KotlmataMutableMachine<T>
		}
		
		interface LazyBy<T : MACHINE>
		{
			infix fun by(block: MachineTemplate<T>): Lazy<KotlmataMutableMachine<T>>
		}
		
		internal fun <T : MACHINE> create(
			tag: T,
			logLevel: Int,
			prefix: String,
			block: MachineTemplate<T>
		): KotlmataMutableMachine<T> = KotlmataMachineImpl(tag, logLevel, prefix, block)
	}
	
	@KotlmataMarker
	interface Modifier : StateDefine, RuleDefine
	{
		val current: STATE
		val has: Has
		val insert: Insert
		val replace: Replace
		val update: Update
		val delete: Delete
		
		interface Has
		{
			infix fun state(state: STATE): Then
			infix fun rule(ruleLeft: RuleLeft): Then
			
			interface Then
			{
				infix fun then(block: () -> Unit): Or
			}
			
			interface Or
			{
				infix fun or(block: () -> Unit)
			}
		}
		
		interface Insert
		{
			infix fun <T : STATE> state(state: T): By<T>
			infix fun rule(ruleLeft: RuleLeft): RemAssign
			infix fun or(keyword: Replace): State
			infix fun or(keyword: Update): Rule
			
			interface State
			{
				infix fun <T : STATE> state(state: T): By<T>
			}
			
			interface Rule
			{
				infix fun rule(ruleLeft: RuleLeft): RemAssign
			}
			
			interface By<T : STATE>
			{
				infix fun by(block: StateTemplate<T>)
			}
			
			interface RemAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Replace
		{
			infix fun <T : STATE> state(state: T): By<T>
			
			interface By<T : STATE>
			{
				infix fun by(block: StateTemplate<T>)
			}
		}
		
		interface Update
		{
			infix fun <T : STATE> state(state: T): By<T>
			infix fun rule(ruleLeft: RuleLeft): RemAssign
			
			interface By<T : STATE>
			{
				infix fun by(block: KotlmataMutableState.Modifier.(state: T) -> Unit): Or<T>
			}
			
			interface Or<T : STATE>
			{
				infix fun or(block: StateTemplate<T>)
			}
			
			interface RemAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Delete
		{
			infix fun state(state: STATE)
			infix fun state(keyword: all)
			
			infix fun rule(ruleLeft: RuleLeft)
			infix fun rule(keyword: of): State
			infix fun rule(keyword: all)
			
			interface State
			{
				infix fun state(state: STATE)
			}
		}
	}
	
	operator fun invoke(block: Modifier.(machine: T) -> Unit) = modify(block)
	
	infix fun modify(block: Modifier.(machine: T) -> Unit)
}

private class TransitionDef(val callback: TransitionCallback, val fallback: TransitionFallback? = null, val finally: TransitionCallback? = null)

private class KotlmataMachineImpl<T : MACHINE>(
	override val tag: T,
	val logLevel: Int = NO_LOG,
	val prefix: String = "Machine[$tag]:",
	block: MachineTemplate<T>
) : KotlmataMutableMachine<T>
{
	private val stateMap: MutableMap<STATE, KotlmataMutableState<out STATE>> = HashMap()
	private val ruleMap: Mutable2DMap<STATE, SIGNAL, STATE> = Mutable2DMap()
	private val predicatesMap: MutableMap<STATE, Predicates> = HashMap()
	
	private var onTransition: TransitionDef? = null
	private var onError: MachineErrorCallback? = null
	
	private var transitionCounter: Long = 0
	
	private lateinit var current: KotlmataState<out STATE>
	
	init
	{
		logLevel.normal(prefix) { MACHINE_START_BUILD }
		ModifierImpl(init = block)
		logLevel.normal(prefix) { MACHINE_END_BUILD }
	}
	
	private inline fun <T> tryCatchReturn(block: () -> T?): T? = try
	{
		block()
	}
	catch (e: Throwable)
	{
		onError?.also { onError ->
			ErrorActionReceiver(e).onError()
		} ?: throw e
		null
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
			finally?.also {
				TransitionActionReceiver(transitionCount).callback(from, signal, to)
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
			return predicatesMap[from]?.test(signal)?.let { predicate ->
				this[predicate]
			}
		}
		
		fun next(from: STATE): STATE? = ruleMap[from]?.let { map2 ->
			return map2[signal] ?: map2[signal::class] ?: map2.test(from, signal) ?: map2[any]
		}
		
		val currentState = current
		val from = currentState.tag
		
		tryCatchReturn {
			if (from !== CREATED)
			{
				logLevel.normal(prefix, signal, payload, from) { MACHINE_START_INPUT }
			}
			currentState.input(signal, payload)
		}.also {
			if (from !== CREATED)
			{
				logLevel.normal(prefix, signal, payload, from) { MACHINE_END_INPUT }
			}
		}.convertToSync()?.also { sync ->
			block(sync)
		} ?: run {
			next(from) ?: next(any)
		}?.let { nextTag ->
			when (nextTag)
			{
				is stay -> null
				is self -> currentState
				in stateMap -> stateMap[nextTag]
				else ->
				{
					Log.w(prefix.trimEnd(), from, signal, nextTag) { TRANSITION_FAILED }
					null
				}
			}
		}?.also { nextState ->
			val to = nextState.tag
			logLevel.simple(prefix, from, signal, to) { MACHINE_START_TRANSITION }
			tryCatchReturn { currentState.exit(signal, to) }
			onTransition?.call(from, signal, to)
			current = nextState
			tryCatchReturn { nextState.entry(from, signal) }.convertToSync()?.also(block)
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?, block: (FunctionDSL.Sync) -> Unit)
	{
		fun next(from: STATE): STATE? = ruleMap[from]?.let { map2 ->
			return map2[type] ?: map2[any]
		}
		
		val currentState = current
		val from = currentState.tag
		
		tryCatchReturn {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", payload, from) { MACHINE_START_TYPED_INPUT }
			currentState.input(signal, type, payload)
		}.also {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", payload, from) { MACHINE_END_TYPED_INPUT }
		}.convertToSync()?.also { sync ->
			block(sync)
		} ?: run {
			next(from) ?: next(any)
		}?.let { nextTag ->
			when (nextTag)
			{
				is stay -> null
				is self -> currentState
				in stateMap -> stateMap[nextTag]
				else ->
				{
					Log.w(prefix.trimEnd(), from, "${type.simpleName}::class", nextTag) { TRANSITION_FAILED }
					null
				}
			}
		}?.also { nextState ->
			val to = nextState.tag
			logLevel.simple(prefix, from, "${type.simpleName}::class", to) { MACHINE_START_TRANSITION }
			tryCatchReturn { currentState.exit(signal, type, to) }
			onTransition?.call(from, signal, to)
			current = nextState
			tryCatchReturn { nextState.entry(from, signal, type) }.convertToSync()?.also(block)
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
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
	
	override fun modify(block: Modifier.(T) -> Unit)
	{
		logLevel.normal(prefix, current.tag) { MACHINE_START_MODIFY }
		ModifierImpl(modify = block)
		logLevel.normal(prefix, current.tag) { MACHINE_END_MODIFY }
	}
	
	override fun toString(): String
	{
		return "KotlmataMachine[$tag]{${hashCode().toString(16)}}"
	}
	
	private inner class ModifierImpl(
		init: (MachineTemplate<T>)? = null,
		modify: (Modifier.(T) -> Unit)? = null
	) : Init, Modifier, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		override val on = object : Init.On
		{
			override fun transition(callback: TransitionCallback): Init.Catch
			{
				this@ModifierImpl shouldNot expired
				onTransition = TransitionDef(callback)
				return object : Init.Catch
				{
					override fun catch(fallback: TransitionFallback): Init.Finally
					{
						this@ModifierImpl shouldNot expired
						onTransition = TransitionDef(callback, fallback)
						return object : Init.Finally
						{
							override fun finally(finally: TransitionCallback)
							{
								this@ModifierImpl shouldNot expired
								onTransition = TransitionDef(callback, fallback, finally)
							}
						}
					}
					
					override fun finally(finally: TransitionCallback)
					{
						this@ModifierImpl shouldNot expired
						onTransition = TransitionDef(callback, null, finally)
					}
				}
			}
			
			override fun error(block: MachineErrorCallback)
			{
				this@ModifierImpl shouldNot expired
				onError = block
			}
		}
		
		override val start = object : Init.Start
		{
			override fun at(state: STATE): Init.End
			{
				this@ModifierImpl shouldNot expired
				
				stateMap[state]?.also {
					this@KotlmataMachineImpl.current = it
				} ?: Log.e(prefix.trimEnd(), state) { UNDEFINED_START_STATE }
				
				return Init.End()
			}
		}
		
		override val current: STATE
			get()
			{
				this@ModifierImpl shouldNot expired
				return this@KotlmataMachineImpl.current.tag
			}
		
		override val has = object : Has
		{
			val stop = object : Has.Or
			{
				override fun or(block: () -> Unit)
				{
					/* do nothing */
				}
			}
			
			val or = object : Has.Or
			{
				override fun or(block: () -> Unit)
				{
					block()
				}
			}
			
			override fun state(state: STATE) = object : Has.Then
			{
				override fun then(block: () -> Unit): Has.Or
				{
					this@ModifierImpl shouldNot expired
					return if (state in stateMap)
					{
						block()
						stop
					}
					else
					{
						or
					}
				}
			}
			
			override fun rule(ruleLeft: RuleLeft) = object : Has.Then
			{
				override fun then(block: () -> Unit): Has.Or
				{
					this@ModifierImpl shouldNot expired
					return ruleMap[ruleLeft.from, ruleLeft.signal]?.run {
						block()
						stop
					} ?: or
				}
			}
		}
		
		override val insert = object : Insert
		{
			override fun <T : STATE> state(state: T) = object : Insert.By<T>
			{
				override fun by(block: StateTemplate<T>)
				{
					this@ModifierImpl shouldNot expired
					if (state !in stateMap)
					{
						state.invoke(block)
					}
				}
			}
			
			override fun rule(ruleLeft: RuleLeft) = object : Insert.RemAssign
			{
				override fun remAssign(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap[ruleLeft.from, ruleLeft.signal] ?: run {
						ruleLeft %= state
					}
				}
			}
			
			override fun or(keyword: Replace) = object : Insert.State
			{
				override fun <T : STATE> state(state: T) = object : Insert.By<T>
				{
					override fun by(block: StateTemplate<T>)
					{
						this@ModifierImpl shouldNot expired
						state.invoke(block)
					}
				}
			}
			
			override fun or(keyword: Update) = object : Insert.Rule
			{
				override fun rule(ruleLeft: RuleLeft) = object : Insert.RemAssign
				{
					override fun remAssign(state: STATE)
					{
						this@ModifierImpl shouldNot expired
						ruleLeft %= state
					}
				}
			}
		}
		
		override val replace = object : Replace
		{
			override fun <T : STATE> state(state: T) = object : Replace.By<T>
			{
				override fun by(block: StateTemplate<T>)
				{
					this@ModifierImpl shouldNot expired
					if (state in stateMap)
					{
						state.invoke(block)
					}
				}
			}
		}
		
		override val update = object : Update
		{
			override fun <T : STATE> state(state: T) = object : Update.By<T>
			{
				val stop = object : Update.Or<T>
				{
					override fun or(block: StateTemplate<T>)
					{
						/* do nothing */
					}
				}
				
				val or = object : Update.Or<T>
				{
					override fun or(block: StateTemplate<T>)
					{
						state.invoke(block)
					}
				}
				
				@Suppress("UNCHECKED_CAST")
				override fun by(block: KotlmataMutableState.Modifier.(T) -> Unit): Update.Or<T>
				{
					this@ModifierImpl shouldNot expired
					return if (state in stateMap)
					{
						stateMap[state]!!.modify(block as KotlmataMutableState.Modifier.(STATE) -> Unit)
						stop
					}
					else
					{
						or
					}
				}
			}
			
			override fun rule(ruleLeft: RuleLeft) = object : Update.RemAssign
			{
				override fun remAssign(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap[ruleLeft.from, ruleLeft.signal]?.run {
						ruleLeft %= state
					}
				}
			}
		}
		
		override val delete = object : Delete
		{
			override fun state(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				stateMap -= state
			}
			
			override fun state(keyword: all)
			{
				this@ModifierImpl shouldNot expired
				stateMap.clear()
			}
			
			override fun rule(ruleLeft: RuleLeft)
			{
				this@ModifierImpl shouldNot expired
				ruleMap[ruleLeft.from]?.let { map2 ->
					map2 -= ruleLeft.signal
				}
			}
			
			override fun rule(keyword: of) = object : Delete.State
			{
				override fun state(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap -= state
					predicatesMap -= state
				}
			}
			
			override fun rule(keyword: all)
			{
				this@ModifierImpl shouldNot expired
				ruleMap.clear()
				predicatesMap.clear()
			}
		}
		
		override fun <S : STATE> S.invoke(block: StateTemplate<S>)
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState.create(this, logLevel, "$prefix$tab", block)
		}
		
		override fun <S : T, T : STATE> S.extends(template: StateTemplate<T>) = object : StateDefine.With<S>
		{
			val state: KotlmataMutableState<S>
			
			init
			{
				this@ModifierImpl shouldNot expired
				state = KotlmataMutableState.create(this@extends, logLevel, "$prefix$tab", template)
				stateMap[this@extends] = state
			}
			
			override fun with(block: StateTemplate<S>)
			{
				this@ModifierImpl shouldNot expired
				state.modify(block)
			}
		}
		
		override fun <S : STATE> S.function(function: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState.create(this, logLevel, "$prefix$tab") {
				entry function function
			}
			return object : KotlmataState.Entry.Catch<SIGNAL>
			{
				override fun intercept(intercept: EntryErrorFunction<SIGNAL>): KotlmataState.Entry.Finally<SIGNAL>
				{
					this@ModifierImpl shouldNot expired
					stateMap[this@function]?.modify {
						entry function function intercept intercept
					}
					return object : KotlmataState.Entry.Finally<SIGNAL>
					{
						override fun finally(finally: EntryAction<SIGNAL>)
						{
							stateMap[this@function]?.modify {
								entry function function intercept intercept finally finally
							}
						}
					}
				}
				
				override fun finally(finally: EntryAction<SIGNAL>)
				{
					stateMap[this@function]?.modify {
						entry function function finally finally
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(function: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signal function function
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(intercept: EntryErrorFunction<T>): KotlmataState.Entry.Finally<T>
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal function function intercept intercept
						}
						return object : KotlmataState.Entry.Finally<T>
						{
							override fun finally(finally: EntryAction<T>)
							{
								this@ModifierImpl shouldNot expired
								stateMap[this@via]?.modify {
									entry via signal function function intercept intercept finally finally
								}
							}
						}
					}
					
					override fun finally(finally: EntryAction<T>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
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
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signal function function
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(intercept: EntryErrorFunction<T>): KotlmataState.Entry.Finally<T>
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal function function intercept intercept
						}
						return object : KotlmataState.Entry.Finally<T>
						{
							override fun finally(finally: EntryAction<T>)
							{
								this@ModifierImpl shouldNot expired
								stateMap[this@via]?.modify {
									entry via signal function function intercept intercept finally finally
								}
							}
						}
					}
					
					override fun finally(finally: EntryAction<T>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
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
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signals function function
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(intercept: EntryErrorFunction<T>): KotlmataState.Entry.Finally<T>
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signals function function intercept intercept
						}
						return object : KotlmataState.Entry.Finally<T>
						{
							override fun finally(finally: EntryAction<T>)
							{
								this@ModifierImpl shouldNot expired
								stateMap[this@via]?.modify {
									entry via signals function function intercept intercept finally finally
								}
							}
						}
					}
					
					override fun finally(finally: EntryAction<T>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
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
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via predicate function function
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(intercept: EntryErrorFunction<T>): KotlmataState.Entry.Finally<T>
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via predicate function function intercept intercept
						}
						return object : KotlmataState.Entry.Finally<T>
						{
							override fun finally(finally: EntryAction<T>)
							{
								this@ModifierImpl shouldNot expired
								stateMap[this@via]?.modify {
									entry via predicate function function intercept intercept finally finally
								}
							}
						}
					}
					
					override fun finally(finally: EntryAction<T>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via predicate function function finally finally
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Transition rules
		 *###################################################################################################################################*/
		
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> T1.or(stateOrSignal: T2) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> T1.or(stateOrSignal: KClass<T2>) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> KClass<T1>.or(stateOrSignal: T2) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> KClass<T1>.or(stateOrSignal: KClass<T2>) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> StatesOrSignals<T1>.or(stateOrSignal: T2) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> StatesOrSignals<T1>.or(stateOrSignal: KClass<T2>) = or(this, stateOrSignal)
		
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
				this@ModifierImpl shouldNot expired
				ruleMap[from, signal] = to
			}
		}
		
		private fun <T : SIGNAL> store(from: STATE, predicate: (T) -> Boolean): SIGNAL
		{
			this@ModifierImpl shouldNot expired
			predicatesMap[from]?.also { predicates ->
				predicates.store(predicate)
			} ?: Predicates().also { predicates ->
				predicates.store(predicate)
				predicatesMap[from] = predicates
			}
			return predicate
		}
		
		@Suppress("FunctionName")
		private fun `state x anyOf`(from: STATE, anyOfSignal: AnyOf) = object : RuleAssignable
		{
			override fun remAssign(to: STATE)
			{
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
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
		override fun <T : SIGNAL> STATE.x(predicate: (T) -> Boolean) = this x store(this, predicate)
		
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
		override fun <T : SIGNAL> any.x(predicate: (T) -> Boolean) = this x store(this, predicate)
		
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
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
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
				this@ModifierImpl shouldNot expired
				val states: MutableList<STATE> = mutableListOf()
				states.add(state)
				return object : Chain.To, Chain.Via
				{
					override fun to(state: STATE): Chain.Via
					{
						this@ModifierImpl shouldNot expired
						states.add(state)
						return this
					}
					
					override fun via(signal: SIGNAL)
					{
						this@ModifierImpl shouldNot expired
						loop { from, to -> from x signal %= to }
					}
					
					override fun via(signal: KClass<out SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						loop { from, to -> from x signal %= to }
					}
					
					override fun via(signals: StatesOrSignals<*>) = via(signals.toAnyOf())
					
					override fun via(any: any)
					{
						this@ModifierImpl shouldNot expired
						loop { from, to -> from x any %= to }
					}
					
					override fun via(anyOf: AnyOf)
					{
						this@ModifierImpl shouldNot expired
						loop { from, to -> from x anyOf %= to }
					}
					
					override fun via(anyExcept: AnyExcept)
					{
						this@ModifierImpl shouldNot expired
						loop { from, to -> from x anyExcept %= to }
					}
					
					override fun <T : SIGNAL> via(predicate: (T) -> Boolean)
					{
						this@ModifierImpl shouldNot expired
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
		
		init
		{
			init?.also { it(tag) } ?: modify?.also { it(tag) }
			expire()
		}
	}
}
