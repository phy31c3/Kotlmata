package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataMachine
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: Initializer.(name: String) -> Initializer.End
		): KotlmataMachine = KotlmataMachineImpl(name, block)
		
		internal operator fun invoke(
				name: String,
				logLevel: Int,
				block: Initializer.(name: String) -> Initializer.End
		): KotlmataMachine = KotlmataMachineImpl(name, block, logLevel = logLevel)
	}
	
	interface Initializer : StateDefine, TransitionDefine
	{
		val log: Log
		val start: Start
		
		interface Log
		{
			/**
			 * @param level **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **2**)
			 */
			infix fun level(level: Int)
		}
		
		interface Start
		{
			infix fun at(state: STATE): Initializer.End
		}
		
		class End internal constructor()
	}
	
	interface StateDefine
	{
		operator fun <T : STATE> T.invoke(block: KotlmataState.Initializer<T>.() -> Unit)
	}
	
	interface TransitionDefine
	{
		infix fun STATE.x(signal: SIGNAL): TransitionLeft
		infix fun STATE.x(signal: KClass<out SIGNAL>): TransitionLeft
		infix fun STATE.x(keyword: any): TransitionLeft
		
		infix fun any.x(signal: SIGNAL): TransitionLeft
		infix fun any.x(signal: KClass<out SIGNAL>): TransitionLeft
		infix fun any.x(keyword: any): TransitionLeft
	}
	
	interface TransitionLeft
	{
		val state: STATE
		val signal: SIGNAL
		
		operator fun remAssign(state: STATE)
	}
	
	val key: MACHINE
	
	/**
	 * @param block Called if the state transitions and the next state's entry function returns an signal.
	 */
	fun input(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	
	fun input(signal: SIGNAL)
	
	/**
	 * @param block Called if the state transitions and the next state's entry function returns an signal.
	 */
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
}

interface KotlmataMutableMachine<out T : MACHINE> : KotlmataMachine
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: KotlmataMachine.Initializer.(name: String) -> KotlmataMachine.Initializer.End
		): KotlmataMutableMachine<String> = KotlmataMachineImpl(name, block)
		
		internal operator fun <T : MACHINE> invoke(
				key: T,
				prefix: String,
				block: KotlmataMachine.Initializer.(key: T) -> KotlmataMachine.Initializer.End
		): KotlmataMutableMachine<T> = KotlmataMachineImpl(key, block, prefix)
	}
	
	interface Modifier : KotlmataMachine.StateDefine, KotlmataMachine.TransitionDefine
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
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): then
			
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
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): remAssign
			infix fun or(keyword: Replace): state
			infix fun or(keyword: Update): transition
			
			interface state
			{
				infix fun <T : STATE> state(state: T): of<T>
			}
			
			interface transition
			{
				infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): remAssign
			}
			
			interface of<T : STATE>
			{
				infix fun of(block: KotlmataState.Initializer<T>.() -> Unit)
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
				infix fun of(block: KotlmataState.Initializer<T>.() -> Unit)
			}
		}
		
		interface Update
		{
			infix fun <T : STATE> state(state: T): set<T>
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): remAssign
			
			interface set<T : STATE>
			{
				infix fun set(block: KotlmataMutableState.Modifier<T>.() -> Unit): or<T>
			}
			
			interface or<T : STATE>
			{
				infix fun or(block: KotlmataState.Initializer<T>.() -> Unit)
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
			
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft)
			infix fun transition(keyword: of): state
			infix fun transition(keyword: all)
			
			interface state
			{
				infix fun state(state: STATE)
			}
		}
	}
	
	infix fun modify(block: Modifier.(key: T) -> Unit)
	
	operator fun invoke(block: Modifier.(key: T) -> Unit) = modify(block)
}

private class KotlmataMachineImpl<T : MACHINE>(
		override val key: T,
		block: KotlmataMachine.Initializer.(key: T) -> KotlmataMachine.Initializer.End,
		val prefix: String = "Machine[$key]:",
		var logLevel: Int = Log.logLevel
) : KotlmataMutableMachine<T>
{
	private val stateMap: MutableMap<STATE, KotlmataMutableState<out STATE>> = HashMap()
	private val transitionMap: MutableMap<STATE, MutableMap<SIGNAL, STATE>> = HashMap()
	
	private lateinit var current: KotlmataState<out STATE>
	
	init
	{
		logLevel.normal(prefix) { MACHINE_START_BUILD }
		ModifierImpl(init = block)
		logLevel.normal(prefix) { MACHINE_END_BUILD }
	}
	
	override fun input(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[signal] ?: this[signal::class] ?: this[any]
		}
		
		transitionMap.let {
			logLevel.normal(prefix, signal, current.key) { MACHINE_START_SIGNAL }
			current.input(signal)
			logLevel.normal(prefix, signal, current.key) { MACHINE_END_SIGNAL }
			it[current.key]?.next() ?: it[any]?.next()
		}?.also {
			if (it !in stateMap)
			{
				Log.w(prefix.trimEnd(), current.key, signal, it) { TRANSITION_FAILED }
			}
		}?.let {
			stateMap[it]
		}?.let { next ->
			logLevel.simple(prefix, current.key, signal, next.key) { MACHINE_START_TRANSITION }
			current.exit(signal)
			current = next
			current.entry(signal, block)
			logLevel.simple(prefix, current.key, signal, next.key) { MACHINE_END_TRANSITION }
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
		
		transitionMap.let {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", current.key) { MACHINE_START_TYPED }
			current.input(signal, type)
			logLevel.normal(prefix, signal, "${type.simpleName}::class", current.key) { MACHINE_END_TYPED }
			it[current.key]?.next() ?: it[any]?.next()
		}.also {
			if (it !in stateMap)
			{
				Log.w(prefix.trimEnd(), current.key, signal, it) { TRANSITION_FAILED }
			}
		}?.let {
			stateMap[it]
		}?.let { next ->
			logLevel.simple(prefix, current.key, signal, next.key) { MACHINE_START_TRANSITION }
			current.exit(signal)
			current = next
			current.entry(signal, type, block)
			logLevel.simple(prefix, current.key, signal, next.key) { MACHINE_END_TRANSITION }
		}
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	{
		input(signal, type) {
			input(it)
		}
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.(key: T) -> Unit)
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
			init: (KotlmataMachine.Initializer.(key: T) -> KotlmataMachine.Initializer.End)? = null,
			modify: (KotlmataMutableMachine.Modifier.(key: T) -> Unit)? = null
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
		
		override val start by lazy {
			object : KotlmataMachine.Initializer.Start
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
		}
		
		override val current: STATE
			get()
			{
				this@ModifierImpl shouldNot expired
				return this@KotlmataMachineImpl.current.key.takeIf {
					it != PreStart
				} ?: Log.w(prefix.trimEnd()) { OBTAIN_PRE_START }
			}
		
		override val has by lazy {
			object : KotlmataMutableMachine.Modifier.Has
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
				
				override fun transition(transitionLeft: KotlmataMachine.TransitionLeft) = object : KotlmataMutableMachine.Modifier.Has.then
				{
					override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.or
					{
						this@ModifierImpl shouldNot expired
						return transitionMap.let {
							it[transitionLeft.state]
						}?.let {
							it[transitionLeft.signal]
						}?.let {
							block()
							stop
						} ?: or
					}
				}
			}
		}
		
		override val insert by lazy {
			object : KotlmataMutableMachine.Modifier.Insert
			{
				override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Insert.of<T>
				{
					override fun of(block: KotlmataState.Initializer<T>.() -> Unit)
					{
						this@ModifierImpl shouldNot expired
						if (state !in stateMap)
						{
							state.invoke(block)
						}
					}
				}
				
				override fun transition(transitionLeft: KotlmataMachine.TransitionLeft) = object : KotlmataMutableMachine.Modifier.Insert.remAssign
				{
					override fun remAssign(state: STATE)
					{
						this@ModifierImpl shouldNot expired
						transitionMap.let {
							it[transitionLeft.state]
						}?.let {
							it[transitionLeft.signal]
						} ?: let {
							transitionLeft %= state
						}
					}
				}
				
				override fun or(keyword: KotlmataMutableMachine.Modifier.Replace) = object : KotlmataMutableMachine.Modifier.Insert.state
				{
					override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Insert.of<T>
					{
						override fun of(block: KotlmataState.Initializer<T>.() -> Unit)
						{
							this@ModifierImpl shouldNot expired
							state.invoke(block)
						}
					}
				}
				
				override fun or(keyword: KotlmataMutableMachine.Modifier.Update) = object : KotlmataMutableMachine.Modifier.Insert.transition
				{
					override fun transition(transitionLeft: KotlmataMachine.TransitionLeft) = object : KotlmataMutableMachine.Modifier.Insert.remAssign
					{
						override fun remAssign(state: STATE)
						{
							this@ModifierImpl shouldNot expired
							transitionLeft %= state
						}
					}
				}
			}
		}
		
		override val replace by lazy {
			object : KotlmataMutableMachine.Modifier.Replace
			{
				override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Replace.of<T>
				{
					override fun of(block: KotlmataState.Initializer<T>.() -> Unit)
					{
						this@ModifierImpl shouldNot expired
						if (state in stateMap)
						{
							state.invoke(block)
						}
					}
				}
			}
		}
		
		override val update by lazy {
			object : KotlmataMutableMachine.Modifier.Update
			{
				override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Update.set<T>
				{
					val stop = object : KotlmataMutableMachine.Modifier.Update.or<T>
					{
						override fun or(block: KotlmataState.Initializer<T>.() -> Unit)
						{
							/* do nothing */
						}
					}
					
					val or = object : KotlmataMutableMachine.Modifier.Update.or<T>
					{
						override fun or(block: KotlmataState.Initializer<T>.() -> Unit)
						{
							state.invoke(block)
						}
					}
					
					@Suppress("UNCHECKED_CAST")
					override fun set(block: KotlmataMutableState.Modifier<T>.() -> Unit): KotlmataMutableMachine.Modifier.Update.or<T>
					{
						this@ModifierImpl shouldNot expired
						return if (state in stateMap)
						{
							stateMap[state]!!.modify(block as KotlmataMutableState.Modifier<out STATE>.() -> Unit)
							stop
						}
						else
						{
							or
						}
					}
				}
				
				override fun transition(transitionLeft: KotlmataMachine.TransitionLeft) = object : KotlmataMutableMachine.Modifier.Update.remAssign
				{
					override fun remAssign(state: STATE)
					{
						this@ModifierImpl shouldNot expired
						transitionMap.let {
							it[transitionLeft.state]
						}?.let {
							it[transitionLeft.signal]
						}.let {
							transitionLeft %= state
						}
					}
				}
			}
		}
		
		override val delete by lazy {
			object : KotlmataMutableMachine.Modifier.Delete
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
				
				override fun transition(transitionLeft: KotlmataMachine.TransitionLeft)
				{
					this@ModifierImpl shouldNot expired
					transitionMap.let {
						it[transitionLeft.state]
					}?.let {
						it -= transitionLeft.signal
					}
				}
				
				override fun transition(keyword: of) = object : KotlmataMutableMachine.Modifier.Delete.state
				{
					override fun state(state: STATE)
					{
						this@ModifierImpl shouldNot expired
						transitionMap -= state
					}
				}
				
				override fun transition(keyword: all)
				{
					this@ModifierImpl shouldNot expired
					transitionMap.clear()
				}
			}
		}
		
		override fun <T : STATE> T.invoke(block: KotlmataState.Initializer<T>.() -> Unit)
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState(this, "$prefix   ", logLevel, block)
		}
		
		override fun STATE.x(signal: SIGNAL) = transitionLeft(this, signal)
		override fun STATE.x(signal: KClass<out SIGNAL>) = transitionLeft(this, signal)
		override fun STATE.x(keyword: any) = transitionLeft(this, keyword)
		
		override fun any.x(signal: SIGNAL) = transitionLeft(this, signal)
		override fun any.x(signal: KClass<out SIGNAL>) = transitionLeft(this, signal)
		override fun any.x(keyword: any) = transitionLeft(this, keyword)
		
		private fun transitionLeft(from: STATE, signal: SIGNAL) = object : KotlmataMachine.TransitionLeft
		{
			override val state: STATE = from
			override val signal: SIGNAL = signal
			
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				(transitionMap[from] ?: HashMap<SIGNAL, STATE>().also {
					transitionMap[from] = it
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