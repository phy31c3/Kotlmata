@file:Suppress("unused", "FunctionName")

package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataMachine.RuleAssignable
import kr.co.plasticcity.kotlmata.KotlmataMachine.RuleDefine.AnyExcept
import kr.co.plasticcity.kotlmata.KotlmataMachine.RuleDefine.AnyOf
import kr.co.plasticcity.kotlmata.KotlmataMachine.RuleLeft
import kr.co.plasticcity.kotlmata.KotlmataMutableMachine.Modifier.*
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
			infix fun error(block: MachineError)
			infix fun transition(block: TransitionCallback)
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
		infix fun <S : STATE> S.extends(template: StateTemplate<S>): With<S>
		
		interface With<S : STATE>
		{
			infix fun with(block: StateTemplate<S>)
		}
		
		infix fun <S : STATE> S.action(action: EntryAction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL> = function(action)
		infix fun <S : STATE> S.function(action: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: T): KotlmataState.Entry.Action<T>
		infix fun <S : STATE> S.via(signals: StatesOrSignals): KotlmataState.Entry.Action<SIGNAL>
	}
	
	interface RuleDefine
	{
		/* Basic rule interface */
		
		infix fun STATE.x(signal: SIGNAL): RuleLeft
		infix fun STATE.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun STATE.x(keyword: any): RuleLeft
		
		infix fun any.x(signal: SIGNAL): RuleLeft
		infix fun any.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun any.x(keyword: any): RuleLeft
		
		/* For 'AnyXX' interface */
		
		interface AnyOf : List<STATE_OR_SIGNAL>
		interface AnyExcept : List<STATE_OR_SIGNAL>
		
		fun any.of(vararg args: STATE_OR_SIGNAL): AnyOf
		fun any.except(vararg args: STATE_OR_SIGNAL): AnyExcept
		
		/* any.xxx(...) x "signal" %= "to" */
		
		infix fun AnyOf.x(signal: SIGNAL): RuleAssignable
		infix fun AnyOf.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun AnyOf.x(keyword: any): RuleAssignable
		infix fun AnyExcept.x(signal: SIGNAL): RuleAssignable
		infix fun AnyExcept.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun AnyExcept.x(keyword: any): RuleAssignable
		
		/* "from" x any.xxx(...) %= "to" */
		
		infix fun STATE.x(anyOf: AnyOf): RuleAssignable
		infix fun any.x(anyOf: AnyOf): RuleAssignable
		infix fun STATE.x(anyExcept: AnyExcept): RuleAssignable
		infix fun any.x(anyExcept: AnyExcept): RuleAssignable
		
		/* any.xxx(...) x any.xxx(...) %= "to" */
		
		infix fun AnyOf.x(anyOf: AnyOf): RuleAssignable
		infix fun AnyOf.x(anyExcept: AnyExcept): RuleAssignable
		infix fun AnyExcept.x(anyOf: AnyOf): RuleAssignable
		infix fun AnyExcept.x(anyExcept: AnyExcept): RuleAssignable
		
		/* For StatesOrSignals interface */
		infix fun STATE_OR_SIGNAL.or(stateOrSignal: STATE_OR_SIGNAL): StatesOrSignals
		
		infix fun STATE.x(signals: StatesOrSignals): RuleAssignable
		infix fun any.x(signals: StatesOrSignals): RuleAssignable
		infix fun AnyOf.x(signals: StatesOrSignals): RuleAssignable
		infix fun AnyExcept.x(signals: StatesOrSignals): RuleAssignable
		infix fun StatesOrSignals.x(signal: SIGNAL): RuleAssignable
		infix fun StatesOrSignals.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun StatesOrSignals.x(keyword: any): RuleAssignable
		infix fun StatesOrSignals.x(anyExcept: AnyExcept): RuleAssignable
		infix fun StatesOrSignals.x(anyOf: AnyOf): RuleAssignable
		infix fun StatesOrSignals.x(signals: StatesOrSignals): RuleAssignable
		
		/**
		 * For chaining transition rule
		 *
		 * `chain from "state1" to "state2" to "state3" to ... via "signal"`
		 */
		val chain: Chain
		
		interface Chain
		{
			infix fun from(state: STATE): To
		}
		
		interface To
		{
			infix fun to(state: STATE): Via
		}
		
		interface Via : To
		{
			infix fun via(signal: SIGNAL)
			infix fun via(signal: KClass<out SIGNAL>)
			infix fun via(keyword: any)
		}
		
		/* For Predicate rule */
		infix fun <T : SIGNAL> STATE.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T : SIGNAL> any.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T : SIGNAL> AnyOf.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T : SIGNAL> AnyExcept.x(predicate: (T) -> Boolean): RuleAssignable
		infix fun <T : SIGNAL> StatesOrSignals.x(predicate: (T) -> Boolean): RuleAssignable
		
		infix fun <T> STATE.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		infix fun <T> any.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		infix fun <T> AnyOf.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		infix fun <T> AnyExcept.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
		infix fun <T> StatesOrSignals.x(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = this x { t: T -> range.contains(t) }
	}
	
	interface RuleAssignable
	{
		operator fun remAssign(state: STATE)
		operator fun remAssign(keyword: self) = remAssign(state = keyword)
	}
	
	interface RuleLeft : RuleAssignable
	{
		val state: STATE
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
	interface Modifier : KotlmataMachine.StateDefine, KotlmataMachine.RuleDefine
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

private class KotlmataMachineImpl<T : MACHINE>(
	override val tag: T,
	val logLevel: Int = NO_LOG,
	val prefix: String = "Machine[$tag]:",
	block: MachineTemplate<T>
) : KotlmataMutableMachine<T>
{
	private val stateMap: MutableMap<STATE, KotlmataMutableState<out STATE>> = HashMap()
	private val ruleMap: MutableMap<STATE, MutableMap<SIGNAL, STATE>> = HashMap()
	private val predicateMap: MutableMap<STATE, Predicates> = HashMap()
	
	private var onTransition: TransitionCallback? = null
	private var onError: MachineError? = null
	
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
			ErrorAction(e).onError()
		} ?: throw e
		null
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
			return predicateMap[from]?.test(signal)?.let { predicate ->
				this[predicate]
			}
		}
		
		fun next(from: STATE): STATE? = ruleMap[from]?.run {
			return this[signal] ?: this[signal::class] ?: test(from, signal) ?: this[any]
		}
		
		tryCatchReturn {
			if (current.tag !== CREATED)
			{
				logLevel.normal(prefix, signal, payload, current.tag) { MACHINE_START_INPUT }
			}
			current.input(signal, payload)
		}.also {
			if (current.tag !== CREATED)
			{
				logLevel.normal(prefix, signal, payload, current.tag) { MACHINE_END_INPUT }
			}
		}.convertToSync()?.also { sync ->
			block(sync)
		} ?: run {
			next(current.tag) ?: next(any)
		}?.let {
			when (it)
			{
				is stay ->
				{
					null
				}
				is self ->
				{
					current
				}
				!in stateMap ->
				{
					Log.w(prefix.trimEnd(), current.tag, signal, it) { TRANSITION_FAILED }
					null
				}
				else ->
				{
					stateMap[it]
				}
			}
		}?.also { next ->
			logLevel.simple(prefix, current.tag, signal, next.tag) { MACHINE_START_TRANSITION }
			tryCatchReturn { current.exit(signal) }
			onTransition?.invoke(Transition(), current.tag, signal, next.tag)
			current = next
			tryCatchReturn { current.entry(signal) }.convertToSync()?.also(block)
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?, block: (FunctionDSL.Sync) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[type] ?: this[any]
		}
		
		tryCatchReturn {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", payload, current.tag) { MACHINE_START_TYPED_INPUT }
			current.input(signal, type, payload)
		}.also {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", payload, current.tag) { MACHINE_END_TYPED_INPUT }
		}.convertToSync()?.also { sync ->
			block(sync)
		} ?: ruleMap.let {
			it[current.tag]?.next() ?: it[any]?.next()
		}?.let {
			when (it)
			{
				is stay ->
				{
					null
				}
				is self ->
				{
					current
				}
				!in stateMap ->
				{
					Log.w(prefix.trimEnd(), current.tag, "${type.simpleName}::class", it) { TRANSITION_FAILED }
					null
				}
				else ->
				{
					stateMap[it]
				}
			}
		}?.also { next ->
			logLevel.simple(prefix, current.tag, "${type.simpleName}::class", next.tag) { MACHINE_START_TRANSITION }
			tryCatchReturn { current.exit(signal, type) }
			onTransition?.invoke(Transition(), current.tag, signal, next.tag)
			current = next
			tryCatchReturn { current.entry(signal, type) }.convertToSync()?.also(block)
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
	
	override fun modify(block: KotlmataMutableMachine.Modifier.(T) -> Unit)
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
		modify: (KotlmataMutableMachine.Modifier.(T) -> Unit)? = null
	) : KotlmataMachine.Init, KotlmataMutableMachine.Modifier, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		override val on = object : KotlmataMachine.Init.On
		{
			override fun error(block: MachineError)
			{
				this@ModifierImpl shouldNot expired
				onError = block
			}
			
			override fun transition(block: TransitionCallback)
			{
				this@ModifierImpl shouldNot expired
				onTransition = block
			}
		}
		
		override val start = object : KotlmataMachine.Init.Start
		{
			override fun at(state: STATE): KotlmataMachine.Init.End
			{
				this@ModifierImpl shouldNot expired
				
				stateMap[state]?.also {
					this@KotlmataMachineImpl.current = it
				} ?: Log.e(prefix.trimEnd(), state) { UNDEFINED_START_STATE }
				
				return KotlmataMachine.Init.End()
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
					return ruleMap.let {
						it[ruleLeft.state]
					}?.let {
						it[ruleLeft.signal]
					}?.let {
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
					ruleMap.let {
						it[ruleLeft.state]
					}?.let {
						it[ruleLeft.signal]
					} ?: let {
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
					ruleMap.let {
						it[ruleLeft.state]
					}?.let {
						it[ruleLeft.signal]
					}?.let {
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
				ruleMap.let {
					it[ruleLeft.state]
				}?.let {
					it -= ruleLeft.signal
				}
			}
			
			override fun rule(keyword: of) = object : Delete.State
			{
				override fun state(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap -= state
					predicateMap -= state
				}
			}
			
			override fun rule(keyword: all)
			{
				this@ModifierImpl shouldNot expired
				ruleMap.clear()
				predicateMap.clear()
			}
		}
		
		override fun <S : STATE> S.invoke(block: StateTemplate<S>)
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState.create(this, logLevel, "$prefix$tab", block)
		}
		
		override fun <S : STATE> S.extends(template: StateTemplate<S>) = object : KotlmataMachine.StateDefine.With<S>
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
		
		override fun <S : STATE> S.function(action: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState.create(this, logLevel, "$prefix$tab") {
				entry function action
			}
			return object : KotlmataState.Entry.Catch<SIGNAL>
			{
				override fun intercept(error: EntryErrorFunction<SIGNAL>)
				{
					this@ModifierImpl shouldNot expired
					stateMap[this@function]?.modify {
						entry function action intercept error
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(action: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signal function action
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(error: EntryErrorFunction<T>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal function action intercept error
						}
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: T) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(action: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signal function action
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(error: EntryErrorFunction<T>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal function action intercept error
						}
					}
				}
			}
		}
		
		override fun <S : STATE> S.via(signals: StatesOrSignals) = object : KotlmataState.Entry.Action<SIGNAL>
		{
			override fun function(action: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signals function action
				}
				return object : KotlmataState.Entry.Catch<SIGNAL>
				{
					override fun intercept(error: EntryErrorFunction<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signals function action intercept error
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Basic transition rules
		 *###################################################################################################################################*/
		override fun STATE.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun STATE.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun STATE.x(keyword: any) = ruleLeft(this, keyword)
		
		override fun any.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun any.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun any.x(keyword: any) = ruleLeft(this, keyword)
		
		private fun ruleLeft(from: STATE, signal: SIGNAL) = object : RuleLeft
		{
			override val state: STATE = from
			override val signal: SIGNAL = signal
			
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				(ruleMap[from] ?: HashMap<SIGNAL, STATE>().also {
					ruleMap[from] = it
				})[signal] = state
			}
		}
		
		/*###################################################################################################################################
		 * 'AnyXX' transition rules
		 *###################################################################################################################################*/
		override fun any.of(vararg args: STATE_OR_SIGNAL): AnyOf = object : AnyOf, List<STATE_OR_SIGNAL> by listOf(*args)
		{
			/* empty */
		}
		
		override fun any.except(vararg args: STATE_OR_SIGNAL): AnyExcept = object : AnyExcept, List<STATE_OR_SIGNAL> by listOf(*args)
		{
			/* empty */
		}
		
		override fun AnyOf.x(signal: SIGNAL) = anyOf_x_signal(this, signal)
		
		override fun AnyOf.x(signal: KClass<out SIGNAL>) = anyOf_x_signal(this, signal)
		
		override fun AnyOf.x(keyword: any) = anyOf_x_signal(this, keyword)
		
		override fun AnyExcept.x(signal: SIGNAL) = anyExcept_x_signal(this, signal)
		
		override fun AnyExcept.x(signal: KClass<out SIGNAL>) = anyExcept_x_signal(this, signal)
		
		override fun AnyExcept.x(keyword: any) = anyExcept_x_signal(this, keyword)
		
		override fun STATE.x(anyOf: AnyOf) = state_x_anyOf(this, anyOf)
		
		override fun any.x(anyOf: AnyOf) = state_x_anyOf(this, anyOf)
		
		override fun STATE.x(anyExcept: AnyExcept) = state_x_anyExcept(this, anyExcept)
		
		override fun any.x(anyExcept: AnyExcept) = state_x_anyExcept(this, anyExcept)
		
		override fun AnyOf.x(anyOf: AnyOf) = anyOf_x_anyOf(this, anyOf)
		
		override fun AnyOf.x(anyExcept: AnyExcept) = anyOf_x_anyExcept(this, anyExcept)
		
		override fun AnyExcept.x(anyOf: AnyOf) = anyExcept_x_anyOf(this, anyOf)
		
		override fun AnyExcept.x(anyExcept: AnyExcept) = anyExcept_x_anyExcept(this, anyExcept)
		
		private fun anyOf_x_signal(anyOf: AnyOf, signal: SIGNAL) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOf.forEach { from ->
					from x signal %= state
				}
			}
		}
		
		private fun anyExcept_x_signal(anyExcept: AnyExcept, signal: SIGNAL) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				any x signal %= state
				anyExcept.forEach { from ->
					ruleMap.let {
						it[from]
					}?.let {
						it[signal]
					} ?: run {
						from x signal %= stay
					}
				}
			}
		}
		
		private fun state_x_anyOf(from: STATE, anyOf: AnyOf) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOf.forEach { signal ->
					from x signal %= state
				}
			}
		}
		
		private fun state_x_anyExcept(from: STATE, anyExcept: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				from x any %= state
				anyExcept.forEach { signal ->
					ruleMap.let {
						it[from]
					}?.let {
						it[signal]
					} ?: run {
						from x signal %= stay
					}
				}
			}
		}
		
		private fun anyOf_x_anyOf(anyOfState: AnyOf, anyOfSignal: AnyOf) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfState.forEach { from ->
					from x anyOfSignal %= state
				}
			}
		}
		
		private fun anyOf_x_anyExcept(anyOfState: AnyOf, anyExceptSignal: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfState.forEach { from ->
					from x anyExceptSignal %= state
				}
			}
		}
		
		private fun anyExcept_x_anyOf(anyExceptState: AnyExcept, anyOfSignal: AnyOf) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfSignal.forEach { signal ->
					anyExceptState x signal %= state
				}
			}
		}
		
		private fun anyExcept_x_anyExcept(anyExceptState: AnyExcept, anyExceptSignal: AnyExcept) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyExceptState x any %= state
				any x anyExceptSignal %= state
			}
		}
		
		/*###################################################################################################################################
		 * StatesOrSignals transition rule
		 *###################################################################################################################################*/
		override fun SIGNAL.or(stateOrSignal: SIGNAL): StatesOrSignals = object : StatesOrSignals, MutableList<SIGNAL> by mutableListOf(this, stateOrSignal)
		{
			override fun or(stateOrSignal: SIGNAL): StatesOrSignals
			{
				this@ModifierImpl shouldNot expired
				add(stateOrSignal)
				return this
			}
		}
		
		private fun StatesOrSignals.toAnyOf() = object : AnyOf, List<STATE_OR_SIGNAL> by this
		{
			/* empty */
		}
		
		override fun STATE.x(signals: StatesOrSignals) = this x signals.toAnyOf()
		
		override fun any.x(signals: StatesOrSignals) = this x signals.toAnyOf()
		
		override fun AnyOf.x(signals: StatesOrSignals) = this x signals.toAnyOf()
		
		override fun AnyExcept.x(signals: StatesOrSignals) = this x signals.toAnyOf()
		
		override fun StatesOrSignals.x(signal: SIGNAL) = toAnyOf() x signal
		
		override fun StatesOrSignals.x(signal: KClass<out SIGNAL>) = toAnyOf() x signal
		
		override fun StatesOrSignals.x(keyword: any) = toAnyOf() x keyword
		
		override fun StatesOrSignals.x(anyExcept: AnyExcept) = toAnyOf() x anyExcept
		
		override fun StatesOrSignals.x(anyOf: AnyOf) = toAnyOf() x anyOf
		
		override fun StatesOrSignals.x(signals: StatesOrSignals) = toAnyOf() x signals.toAnyOf()
		
		/*###################################################################################################################################
		 * Chaining transition rule
		 *###################################################################################################################################*/
		override val chain = object : KotlmataMachine.RuleDefine.Chain
		{
			val states: MutableList<STATE> = mutableListOf()
			
			override fun from(state: STATE): KotlmataMachine.RuleDefine.To
			{
				this@ModifierImpl shouldNot expired
				states.add(state)
				return object : KotlmataMachine.RuleDefine.To
				{
					override fun to(state: STATE): KotlmataMachine.RuleDefine.Via
					{
						this@ModifierImpl shouldNot expired
						states.add(state)
						return object : KotlmataMachine.RuleDefine.Via, KotlmataMachine.RuleDefine.To by this
						{
							override fun via(signal: SIGNAL)
							{
								this@ModifierImpl shouldNot expired
								done(signal)
							}
							
							override fun via(signal: KClass<out SIGNAL>)
							{
								this@ModifierImpl shouldNot expired
								done(signal)
							}
							
							override fun via(keyword: any)
							{
								this@ModifierImpl shouldNot expired
								done(keyword)
							}
							
							private fun done(signal: Any) = (1 until states.size).forEach { i ->
								val from = states[i - 1]
								val to = states[i]
								(ruleMap[from] ?: HashMap<SIGNAL, STATE>().also {
									ruleMap[from] = it
								})[signal] = to
							}
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Predicate transition rule
		 *###################################################################################################################################*/
		private fun <T : SIGNAL> store(state: STATE, predicate: (T) -> Boolean): SIGNAL
		{
			(predicateMap[state] ?: Predicates().also {
				predicateMap[state] = it
			}).store(predicate)
			return predicate
		}
		
		override fun <T : SIGNAL> STATE.x(predicate: (T) -> Boolean) = this x store(this, predicate)
		
		override fun <T : SIGNAL> any.x(predicate: (T) -> Boolean) = this x store(this, predicate)
		
		override fun <T : SIGNAL> AnyOf.x(predicate: (T) -> Boolean) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				this@x.forEach { from ->
					from x store(this, predicate) %= state
				}
			}
		}
		
		override fun <T : SIGNAL> AnyExcept.x(predicate: (T) -> Boolean) = object : RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				any x store(any, predicate) %= state
				this@x.forEach { from ->
					store(from, predicate)
					ruleMap.let {
						it[from]
					}?.let {
						it[predicate]
					} ?: run {
						from x predicate %= stay
					}
				}
			}
		}
		
		override fun <T : SIGNAL> StatesOrSignals.x(predicate: (T) -> Boolean) = this.toAnyOf() x predicate
		
		init
		{
			init?.also { it(tag) } ?: modify?.also { it(tag) }
			expire()
		}
	}
}
