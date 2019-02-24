package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataMachine<T : MACHINE>
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: Initializer.(machine: String) -> Initializer.End
		): KotlmataMachine<String> = KotlmataMachineImpl(name, block)
		
		internal operator fun invoke(
				name: String,
				logLevel: Int,
				block: Initializer.(machine: String) -> Initializer.End
		): KotlmataMachine<String> = KotlmataMachineImpl(name, block, logLevel = logLevel)
	}
	
	@KotlmataMarker
	interface Initializer : StateDefine, RuleDefine
	{
		val log: Log
		val on: On
		val start: Start
		
		interface Log
		{
			/**
			 * @param level **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **2**)
			 */
			infix fun level(level: Int)
		}
		
		interface On
		{
			infix fun error(block: Kotlmata.Marker.(Throwable) -> Unit)
		}
		
		interface Start
		{
			infix fun at(state: STATE): Initializer.End
		}
		
		class End internal constructor()
	}
	
	interface StateDefine
	{
		operator fun <S : STATE> S.invoke(block: KotlmataState.Initializer.(state: S) -> Unit)
		infix fun <S : STATE, R> S.action(action: Kotlmata.Marker.(signal: SIGNAL) -> R)
		infix fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: T): KotlmataState.Entry.Action<T>
	}
	
	interface RuleDefine
	{
		infix fun STATE.x(signal: SIGNAL): RuleLeft
		infix fun STATE.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun STATE.x(keyword: any): RuleLeft
		
		infix fun any.x(signal: SIGNAL): RuleLeft
		infix fun any.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun any.x(keyword: any): RuleLeft
		
		fun any.of(vararg args: STATE_OR_SIGNAL): AnyOf
		fun any.except(vararg args: STATE_OR_SIGNAL): AnyExcept
		
		/* any.xxx(...) x "signal" %= "to" */
		
		infix fun AnyOf.x(signal: SIGNAL): RuleAnyOfSignal
		infix fun AnyOf.x(signal: KClass<out SIGNAL>): RuleAnyOfSignal
		infix fun AnyOf.x(keyword: any): RuleAnyOfSignal
		infix fun AnyExcept.x(signal: SIGNAL): RuleAnyExceptSignal
		infix fun AnyExcept.x(signal: KClass<out SIGNAL>): RuleAnyExceptSignal
		infix fun AnyExcept.x(keyword: any): RuleAnyExceptSignal
		
		/* "from" x any.xxx(...) %= "to" */
		
		infix fun STATE.x(anyOf: AnyOf): RuleStateAnyOf
		infix fun any.x(anyOf: AnyOf): RuleStateAnyOf
		infix fun STATE.x(anyExcept: AnyExcept): RuleStateAnyExcept
		infix fun any.x(anyExcept: AnyExcept): RuleStateAnyExcept
		
		/* any.xxx(...) x any.xxx(...) %= "to" */
		
		infix fun AnyOf.x(anyOf: AnyOf): RuleAnyOfAnyOf
		infix fun AnyOf.x(anyExcept: AnyExcept): RuleAnyOfAnyExcept
		infix fun AnyExcept.x(anyOf: AnyOf): RuleAnyExceptAnyOf
		infix fun AnyExcept.x(anyExcept: AnyExcept): RuleAnyExceptAnyExcept
		
		interface AnyOf : List<STATE_OR_SIGNAL>
		
		interface AnyExcept : List<STATE_OR_SIGNAL>
		
		interface RuleAny
		{
			operator fun remAssign(state: STATE)
		}
		
		interface RuleAnyOfSignal : RuleAny
		interface RuleAnyExceptSignal : RuleAny
		interface RuleStateAnyOf : RuleAny
		interface RuleStateAnyExcept : RuleAny
		interface RuleAnyOfAnyOf : RuleAny
		interface RuleAnyOfAnyExcept : RuleAny
		interface RuleAnyExceptAnyOf : RuleAny
		interface RuleAnyExceptAnyExcept : RuleAny
	}
	
	interface RuleLeft
	{
		val state: STATE
		val signal: SIGNAL
		
		operator fun remAssign(state: STATE)
	}
	
	val key: T
	
	/**
	 * @param block Called if the state is switched and the next state's entry function returns an signal.
	 */
	fun input(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	
	fun input(signal: SIGNAL)
	
	/**
	 * @param block Called if the state is switched and the next state's entry function returns an signal.
	 */
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
}

interface KotlmataMutableMachine<T : MACHINE> : KotlmataMachine<T>
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: KotlmataMachine.Initializer.(machine: String) -> KotlmataMachine.Initializer.End
		): KotlmataMutableMachine<String> = KotlmataMachineImpl(name, block)
		
		internal operator fun <T : MACHINE> invoke(
				key: T,
				prefix: String,
				block: KotlmataMachine.Initializer.(machine: T) -> KotlmataMachine.Initializer.End
		): KotlmataMutableMachine<T> = KotlmataMachineImpl(key, block, prefix)
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
			infix fun state(state: STATE): then
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): then
			
			interface then
			{
				infix fun then(block: () -> Unit): or
			}
			
			interface or
			{
				infix fun or(block: () -> Unit)
			}
		}
		
		interface Insert
		{
			infix fun <T : STATE> state(state: T): of<T>
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): remAssign
			infix fun or(keyword: Replace): state
			infix fun or(keyword: Update): rule
			
			interface state
			{
				infix fun <T : STATE> state(state: T): of<T>
			}
			
			interface rule
			{
				infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): remAssign
			}
			
			interface of<T : STATE>
			{
				infix fun of(block: KotlmataState.Initializer.(state: T) -> Unit)
			}
			
			interface remAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Replace
		{
			infix fun <T : STATE> state(state: T): of<T>
			
			interface of<T : STATE>
			{
				infix fun of(block: KotlmataState.Initializer.(state: T) -> Unit)
			}
		}
		
		interface Update
		{
			infix fun <T : STATE> state(state: T): set<T>
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): remAssign
			
			interface set<T : STATE>
			{
				infix fun set(block: KotlmataMutableState.Modifier.(state: T) -> Unit): or<T>
			}
			
			interface or<T : STATE>
			{
				infix fun or(block: KotlmataState.Initializer.(state: T) -> Unit)
			}
			
			interface remAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Delete
		{
			infix fun state(state: STATE)
			infix fun state(keyword: all)
			
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft)
			infix fun rule(keyword: of): state
			infix fun rule(keyword: all)
			
			interface state
			{
				infix fun state(state: STATE)
			}
		}
	}
	
	infix fun modify(block: Modifier.(machine: T) -> Unit)
	
	operator fun invoke(block: Modifier.(machine: T) -> Unit) = modify(block)
}

private class KotlmataMachineImpl<T : MACHINE>(
		override val key: T,
		block: KotlmataMachine.Initializer.(T) -> KotlmataMachine.Initializer.End,
		val prefix: String = "Machine[$key]:",
		var logLevel: Int = Log.logLevel
) : KotlmataMutableMachine<T>
{
	private val stateMap: MutableMap<STATE, KotlmataMutableState<out STATE>> = HashMap()
	private val ruleMap: MutableMap<STATE, MutableMap<SIGNAL, STATE>> = HashMap()
	
	private var onError: (Kotlmata.Marker.(Throwable) -> Unit)? = null
	
	private lateinit var current: KotlmataState<out STATE>
	
	init
	{
		logLevel.normal(prefix) { MACHINE_START_BUILD }
		ModifierImpl(init = block)
		logLevel.normal(prefix) { MACHINE_END_BUILD }
	}
	
	private inline fun action(block: () -> Unit)
	{
		try
		{
			block()
		}
		catch (e: Throwable)
		{
			onError?.also {
				Kotlmata.Marker.it(e)
			} ?: throw e
		}
	}
	
	override fun input(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[signal] ?: this[signal::class] ?: this[any]
		}
		
		ruleMap.let {
			logLevel.normal(prefix, signal, current.key) { MACHINE_START_SIGNAL }
			action { current.input(signal) }
			logLevel.normal(prefix, signal, current.key) { MACHINE_END_SIGNAL }
			it[current.key]?.next() ?: it[any]?.next()
		}?.let {
			when (it)
			{
				is stay ->
				{
					null
				}
				!in stateMap ->
				{
					Log.w(prefix.trimEnd(), current.key, signal, it) { TRANSITION_FAILED }
					null
				}
				else ->
				{
					stateMap[it]
				}
			}
		}?.let { next ->
			logLevel.simple(prefix, current.key, signal, next.key) { MACHINE_START_TRANSITION }
			action { current.exit(signal) }
			current = next
			action { current.entry(signal, block) }
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
	}
	
	override fun input(signal: SIGNAL)
	{
		input(signal) {
			input(it)
		}
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[type] ?: this[any]
		}
		
		ruleMap.let {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", current.key) { MACHINE_START_TYPED }
			action { current.input(signal, type) }
			logLevel.normal(prefix, signal, "${type.simpleName}::class", current.key) { MACHINE_END_TYPED }
			it[current.key]?.next() ?: it[any]?.next()
		}?.let {
			when (it)
			{
				is stay ->
				{
					null
				}
				!in stateMap ->
				{
					Log.w(prefix.trimEnd(), current.key, signal, it) { TRANSITION_FAILED }
					null
				}
				else ->
				{
					stateMap[it]
				}
			}
		}?.let { next ->
			logLevel.simple(prefix, current.key, signal, next.key) { MACHINE_START_TRANSITION }
			action { current.exit(signal) }
			current = next
			action { current.entry(signal, type, block) }
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	{
		input(signal, type) {
			input(it)
		}
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.(T) -> Unit)
	{
		logLevel.normal(prefix, current.key) { MACHINE_START_MODIFY }
		ModifierImpl(modify = block)
		logLevel.normal(prefix, current.key) { MACHINE_END_MODIFY }
	}
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
	
	private inner class ModifierImpl internal constructor(
			init: (KotlmataMachine.Initializer.(T) -> KotlmataMachine.Initializer.End)? = null,
			modify: (KotlmataMutableMachine.Modifier.(T) -> Unit)? = null
	) : KotlmataMachine.Initializer, KotlmataMutableMachine.Modifier, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		override val log = object : KotlmataMachine.Initializer.Log
		{
			override fun level(level: Int)
			{
				this@ModifierImpl shouldNot expired
				logLevel = level
			}
		}
		
		override val on = object : KotlmataMachine.Initializer.On
		{
			override fun error(block: Kotlmata.Marker.(Throwable) -> Unit)
			{
				this@ModifierImpl shouldNot expired
				onError = block
			}
		}
		
		override val start = object : KotlmataMachine.Initializer.Start
		{
			override fun at(state: STATE): KotlmataMachine.Initializer.End
			{
				this@ModifierImpl shouldNot expired
				
				stateMap[state]?.also {
					this@KotlmataMachineImpl.current = it
				} ?: Log.e(prefix.trimEnd(), state) { UNDEFINED_INITIAL_STATE }
				
				return KotlmataMachine.Initializer.End()
			}
		}
		
		override val current: STATE
			get()
			{
				this@ModifierImpl shouldNot expired
				return this@KotlmataMachineImpl.current.key.takeIf {
					it != PreStart
				} ?: Log.w(prefix.trimEnd()) { OBTAIN_PRE_START }
			}
		
		override val has = object : KotlmataMutableMachine.Modifier.Has
		{
			val stop = object : KotlmataMutableMachine.Modifier.Has.or
			{
				override fun or(block: () -> Unit)
				{
					/* do nothing */
				}
			}
			
			val or = object : KotlmataMutableMachine.Modifier.Has.or
			{
				override fun or(block: () -> Unit)
				{
					block()
				}
			}
			
			override fun state(state: STATE) = object : KotlmataMutableMachine.Modifier.Has.then
			{
				override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.or
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
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Has.then
			{
				override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.or
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
		
		override val insert = object : KotlmataMutableMachine.Modifier.Insert
		{
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Insert.of<T>
			{
				override fun of(block: KotlmataState.Initializer.(T) -> Unit)
				{
					this@ModifierImpl shouldNot expired
					if (state !in stateMap)
					{
						state.invoke(block)
					}
				}
			}
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Insert.remAssign
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
			
			override fun or(keyword: KotlmataMutableMachine.Modifier.Replace) = object : KotlmataMutableMachine.Modifier.Insert.state
			{
				override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Insert.of<T>
				{
					override fun of(block: KotlmataState.Initializer.(T) -> Unit)
					{
						this@ModifierImpl shouldNot expired
						state.invoke(block)
					}
				}
			}
			
			override fun or(keyword: KotlmataMutableMachine.Modifier.Update) = object : KotlmataMutableMachine.Modifier.Insert.rule
			{
				override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Insert.remAssign
				{
					override fun remAssign(state: STATE)
					{
						this@ModifierImpl shouldNot expired
						ruleLeft %= state
					}
				}
			}
		}
		
		override val replace = object : KotlmataMutableMachine.Modifier.Replace
		{
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Replace.of<T>
			{
				override fun of(block: KotlmataState.Initializer.(T) -> Unit)
				{
					this@ModifierImpl shouldNot expired
					if (state in stateMap)
					{
						state.invoke(block)
					}
				}
			}
		}
		
		override val update = object : KotlmataMutableMachine.Modifier.Update
		{
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Update.set<T>
			{
				val stop = object : KotlmataMutableMachine.Modifier.Update.or<T>
				{
					override fun or(block: KotlmataState.Initializer.(T) -> Unit)
					{
						/* do nothing */
					}
				}
				
				val or = object : KotlmataMutableMachine.Modifier.Update.or<T>
				{
					override fun or(block: KotlmataState.Initializer.(T) -> Unit)
					{
						state.invoke(block)
					}
				}
				
				@Suppress("UNCHECKED_CAST")
				override fun set(block: KotlmataMutableState.Modifier.(T) -> Unit): KotlmataMutableMachine.Modifier.Update.or<T>
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
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Update.remAssign
			{
				override fun remAssign(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap.let {
						it[ruleLeft.state]
					}?.let {
						it[ruleLeft.signal]
					}.let {
						ruleLeft %= state
					}
				}
			}
		}
		
		override val delete = object : KotlmataMutableMachine.Modifier.Delete
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
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft)
			{
				this@ModifierImpl shouldNot expired
				ruleMap.let {
					it[ruleLeft.state]
				}?.let {
					it -= ruleLeft.signal
				}
			}
			
			override fun rule(keyword: of) = object : KotlmataMutableMachine.Modifier.Delete.state
			{
				override fun state(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap -= state
				}
			}
			
			override fun rule(keyword: all)
			{
				this@ModifierImpl shouldNot expired
				ruleMap.clear()
			}
		}
		
		override fun <T : STATE> T.invoke(block: KotlmataState.Initializer.(T) -> Unit)
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState(this, "$prefix   ", logLevel, block)
		}
		
		override fun <S : STATE, R> S.action(action: Kotlmata.Marker.(signal: SIGNAL) -> R)
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState(this, "$prefix   ", logLevel) {
				entry action action
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun <R> action(action: Kotlmata.Marker.(signal: T) -> R)
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState(this@via, "$prefix   ", logLevel) {
					entry via signal action action
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: T) = object : KotlmataState.Entry.Action<T>
		{
			override fun <R> action(action: Kotlmata.Marker.(signal: T) -> R)
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState(this@via, "$prefix   ", logLevel) {
					entry via signal action action
				}
			}
		}
		
		override fun STATE.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun STATE.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun STATE.x(keyword: any) = ruleLeft(this, keyword)
		
		override fun any.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun any.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun any.x(keyword: any) = ruleLeft(this, keyword)
		
		override fun any.of(vararg args: STATE_OR_SIGNAL): KotlmataMachine.RuleDefine.AnyOf = object : KotlmataMachine.RuleDefine.AnyOf, List<STATE_OR_SIGNAL> by listOf(*args)
		{
			/* empty */
		}
		
		override fun any.except(vararg args: STATE_OR_SIGNAL): KotlmataMachine.RuleDefine.AnyExcept = object : KotlmataMachine.RuleDefine.AnyExcept, List<STATE_OR_SIGNAL> by listOf(*args)
		{
			/* empty */
		}
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(signal: SIGNAL) = ruleAnyOfSignal(this, signal)
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(signal: KClass<out SIGNAL>) = ruleAnyOfSignal(this, signal)
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(keyword: any) = ruleAnyOfSignal(this, keyword)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(signal: SIGNAL) = ruleAnyExceptSignal(this, signal)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(signal: KClass<out SIGNAL>) = ruleAnyExceptSignal(this, signal)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(keyword: any) = ruleAnyExceptSignal(this, keyword)
		
		override fun STATE.x(anyOf: KotlmataMachine.RuleDefine.AnyOf) = ruleStateAnyOf(this, anyOf)
		
		override fun any.x(anyOf: KotlmataMachine.RuleDefine.AnyOf) = ruleStateAnyOf(this, anyOf)
		
		override fun STATE.x(anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = ruleStateAnyExcept(this, anyExcept)
		
		override fun any.x(anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = ruleStateAnyExcept(this, anyExcept)
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(anyOf: KotlmataMachine.RuleDefine.AnyOf) = ruleAnyOfAnyOf(this, anyOf)
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = ruleAnyOfAnyExcept(this, anyExcept)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(anyOf: KotlmataMachine.RuleDefine.AnyOf) = ruleAnyExceptAnyOf(this, anyOf)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = ruleAnyExceptAnyExcept(this, anyExcept)
		
		private fun ruleAnyOfSignal(anyOf: KotlmataMachine.RuleDefine.AnyOf, signal: SIGNAL) = object : KotlmataMachine.RuleDefine.RuleAnyOfSignal
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOf.forEach { from ->
					from x signal %= state
				}
			}
		}
		
		private fun ruleAnyExceptSignal(anyExcept: KotlmataMachine.RuleDefine.AnyExcept, signal: SIGNAL) = object : KotlmataMachine.RuleDefine.RuleAnyExceptSignal
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
		
		private fun ruleStateAnyOf(from: STATE, anyOf: KotlmataMachine.RuleDefine.AnyOf) = object : KotlmataMachine.RuleDefine.RuleStateAnyOf
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOf.forEach { signal ->
					from x signal %= state
				}
			}
		}
		
		private fun ruleStateAnyExcept(from: STATE, anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = object : KotlmataMachine.RuleDefine.RuleStateAnyExcept
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
		
		private fun ruleAnyOfAnyOf(anyOfState: KotlmataMachine.RuleDefine.AnyOf, anyOfSignal: KotlmataMachine.RuleDefine.AnyOf) = object : KotlmataMachine.RuleDefine.RuleAnyOfAnyOf
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfState.forEach { from ->
					ruleStateAnyOf(from, anyOfSignal).remAssign(state)
				}
			}
		}
		
		private fun ruleAnyOfAnyExcept(anyOfState: KotlmataMachine.RuleDefine.AnyOf, anyExceptSignal: KotlmataMachine.RuleDefine.AnyExcept) = object : KotlmataMachine.RuleDefine.RuleAnyOfAnyExcept
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfState.forEach { from ->
					ruleStateAnyExcept(from, anyExceptSignal).remAssign(state)
				}
			}
		}
		
		private fun ruleAnyExceptAnyOf(anyExceptState: KotlmataMachine.RuleDefine.AnyExcept, anyOfSignal: KotlmataMachine.RuleDefine.AnyOf) = object : KotlmataMachine.RuleDefine.RuleAnyExceptAnyOf
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfSignal.forEach { signal ->
					ruleAnyExceptSignal(anyExceptState, signal).remAssign(state)
				}
			}
		}
		
		private fun ruleAnyExceptAnyExcept(anyExceptState: KotlmataMachine.RuleDefine.AnyExcept, anyExceptSignal: KotlmataMachine.RuleDefine.AnyExcept) = object : KotlmataMachine.RuleDefine.RuleAnyExceptAnyExcept
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				ruleAnyExceptSignal(anyExceptState, any)
				ruleStateAnyExcept(any, anyExceptSignal)
			}
		}
		
		private fun ruleLeft(from: STATE, signal: SIGNAL) = object : KotlmataMachine.RuleLeft
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
		
		init
		{
			init?.let { it(key) } ?: modify?.let { it(key) }
			expire()
		}
	}
}