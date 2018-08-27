package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataMachine
{
	companion object
	{
		operator fun invoke(
				name: String? = null,
				block: Initializer.() -> Initialize.End
		): KotlmataMachine = KotlmataMachineImpl(name, block)
	}
	
	interface Initializer : StateDefine, TransitionDefine
	{
		val initialize: Initialize
	}
	
	interface Initialize
	{
		infix fun origin(keyword: state): to
		
		interface to
		{
			infix fun to(state: STATE): End
		}
		
		class End internal constructor()
	}
	
	interface StateDefine
	{
		operator fun STATE.invoke(block: KotlmataState.Initializer.() -> Unit)
	}
	
	interface TransitionDefine
	{
		infix fun STATE.x(signal: SIGNAL): TransitionLeft
		infix fun STATE.x(signal: KClass<out Any>): TransitionLeft
		infix fun STATE.x(keyword: any): TransitionLeft
		
		infix fun any.x(signal: SIGNAL): TransitionLeft
		infix fun any.x(signal: KClass<out Any>): TransitionLeft
		infix fun any.x(keyword: any): TransitionLeft
	}
	
	interface TransitionLeft
	{
		val state: STATE
		val signal: SIGNAL
		
		operator fun remAssign(state: STATE)
	}
	
	val key: Any
	
	/**
	 * @param block Called if the state transitions and the next state's entry function returns an signal.
	 */
	fun input(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	
	fun input(signal: SIGNAL)
	
	/**
	 * @param block Called if the state transitions and the next state's entry function returns an signal.
	 */
	fun <T : Any> input(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	
	fun <T : Any> input(signal: T, type: KClass<in T>)
}

interface KotlmataMutableMachine : KotlmataMachine
{
	companion object
	{
		operator fun invoke(
				name: String? = null,
				block: (KotlmataMachine.Initializer.() -> KotlmataMachine.Initialize.End)? = null
		): KotlmataMutableMachine = KotlmataMachineImpl(name, block)
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
			infix fun state(state: STATE): of
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): remAssign
			infix fun or(keyword: Replace): state
			infix fun or(keyword: Update): transition
			
			interface state
			{
				infix fun state(state: STATE): of
			}
			
			interface transition
			{
				infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): remAssign
			}
			
			interface of
			{
				infix fun of(block: KotlmataState.Initializer.() -> Unit)
			}
			
			interface remAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Replace
		{
			infix fun state(state: STATE): of
			
			interface of
			{
				infix fun of(block: KotlmataState.Initializer.() -> Unit)
			}
		}
		
		interface Update
		{
			infix fun state(state: STATE): set
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): remAssign
			
			interface set
			{
				infix fun set(block: KotlmataMutableState.Modifier.() -> Unit): or
			}
			
			interface or
			{
				infix fun or(block: KotlmataState.Initializer.() -> Unit)
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
	
	operator fun invoke(block: Modifier.() -> Unit)
	
	infix fun modify(block: Modifier.() -> Unit)
}

private class KotlmataMachineImpl(
		key: Any? = null,
		block: (KotlmataMachine.Initializer.() -> KotlmataMachine.Initialize.End)? = null
) : KotlmataMutableMachine
{
	override val key: Any = key ?: this
	private val stateMap: MutableMap<STATE, KotlmataMutableState> = HashMap()
	private val transitionMap: MutableMap<STATE, MutableMap<SIGNAL, STATE>> = HashMap()
	private lateinit var state: KotlmataState
	
	init
	{
		block?.also {
			ModifierImpl(init = it)
		}
	}
	
	override fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		modify(block)
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		ModifierImpl(modify = block)
	}
	
	override fun input(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[signal] ?: this[signal::class] ?: this[any]
		}
		
		let {
			state.input(signal)
			transitionMap[state.key]?.next() ?: transitionMap[any]?.next()
		}?.let {
			stateMap[it]
		}?.let {
			Log.d(key, state.key, signal, it.key) { MACHINE_TRANSITION }
			state.exit()
			state = it
			state.entry(signal, block)
		}
	}
	
	override fun input(signal: SIGNAL)
	{
		input(signal) {
			input(it)
		}
	}
	
	override fun <T : Any> input(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[type] ?: this[any]
		}
		
		let {
			state.input(signal, type)
			transitionMap[state.key]?.next() ?: transitionMap[any]?.next()
		}?.let {
			stateMap[it]
		}?.let {
			Log.d(key, state.key, signal, it.key) { MACHINE_TRANSITION }
			state.exit()
			state = it
			state.entry(signal, type, block)
		}
	}
	
	override fun <T : Any> input(signal: T, type: KClass<in T>)
	{
		input(signal, type) {
			input(signal)
		}
	}
	
	private inner class ModifierImpl internal constructor(
			init: (KotlmataMachine.Initializer.() -> KotlmataMachine.Initialize.End)? = null,
			modify: (KotlmataMutableMachine.Modifier.() -> Unit)? = null
	) : KotlmataMachine.Initializer, KotlmataMutableMachine.Modifier, CanExpire({ Log.e(key) { EXPIRED_MACHINE_MODIFIER } })
	{
		override val current: STATE
			get()
			{
				this@ModifierImpl shouldNot expired
				return state.key
			}
		
		override val initialize by lazy {
			object : KotlmataMachine.Initialize
			{
				override fun origin(keyword: state) = object : KotlmataMachine.Initialize.to
				{
					override fun to(state: STATE): KotlmataMachine.Initialize.End
					{
						this@ModifierImpl shouldNot expired
						
						stateMap[state]?.also {
							this@KotlmataMachineImpl.state = it
						} ?: Log.e(key, state) { NULL_ORIGIN_STATE }
						
						return KotlmataMachine.Initialize.End()
					}
				}
			}
		}
		
		override val has by lazy {
			object : KotlmataMutableMachine.Modifier.Has
			{
				override fun state(state: STATE) = object : KotlmataMutableMachine.Modifier.Has.then
				{
					override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.or
					{
						this@ModifierImpl shouldNot expired
						return stateMap.let {
							it[state]
						}?.let {
							block()
							stop()
						} ?: or()
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
							stop()
						} ?: or()
					}
				}
				
				private fun stop() = object : KotlmataMutableMachine.Modifier.Has.or
				{
					override fun or(block: () -> Unit)
					{
						/* do nothing */
					}
				}
				
				private fun or() = object : KotlmataMutableMachine.Modifier.Has.or
				{
					override fun or(block: () -> Unit)
					{
						block()
					}
				}
			}
		}
		
		override val insert by lazy {
			object : KotlmataMutableMachine.Modifier.Insert
			{
				override fun state(state: STATE) = object : KotlmataMutableMachine.Modifier.Insert.of
				{
					override fun of(block: KotlmataState.Initializer.() -> Unit)
					{
						this@ModifierImpl shouldNot expired
						stateMap[state] ?: state.invoke(block)
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
						} ?: also {
							transitionLeft %= state
						}
					}
				}
				
				override fun or(keyword: KotlmataMutableMachine.Modifier.Replace) = object : KotlmataMutableMachine.Modifier.Insert.state
				{
					override fun state(state: STATE) = object : KotlmataMutableMachine.Modifier.Insert.of
					{
						override fun of(block: KotlmataState.Initializer.() -> Unit)
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
				override fun state(state: STATE) = object : KotlmataMutableMachine.Modifier.Replace.of
				{
					override fun of(block: KotlmataState.Initializer.() -> Unit)
					{
						this@ModifierImpl shouldNot expired
						stateMap[state]?.also { state.invoke(block) }
					}
				}
			}
		}
		
		override val update by lazy {
			object : KotlmataMutableMachine.Modifier.Update
			{
				override fun state(state: STATE) = object : KotlmataMutableMachine.Modifier.Update.set
				{
					override fun set(block: KotlmataMutableState.Modifier.() -> Unit): KotlmataMutableMachine.Modifier.Update.or
					{
						this@ModifierImpl shouldNot expired
						return stateMap.let {
							it[state]
						}?.let {
							it.modify(block)
							stop()
						} ?: object : KotlmataMutableMachine.Modifier.Update.or
						{
							override fun or(block: KotlmataState.Initializer.() -> Unit)
							{
								state.invoke(block)
							}
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
				
				private fun stop() = object : KotlmataMutableMachine.Modifier.Update.or
				{
					override fun or(block: KotlmataState.Initializer.() -> Unit)
					{
						/* do nothing */
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
		
		override fun STATE.invoke(block: KotlmataState.Initializer.() -> Unit)
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState(this) { block() }
		}
		
		override fun STATE.x(signal: SIGNAL) = transitionLeft(this, signal)
		override fun STATE.x(signal: KClass<out Any>) = transitionLeft(this, signal)
		override fun STATE.x(keyword: any) = transitionLeft(this, keyword)
		
		override fun any.x(signal: SIGNAL) = transitionLeft(this, signal)
		override fun any.x(signal: KClass<out Any>) = transitionLeft(this, signal)
		override fun any.x(keyword: any) = transitionLeft(this, keyword)
		
		private fun transitionLeft(from: STATE, signal: SIGNAL) = object : KotlmataMachine.TransitionLeft
		{
			override val state: STATE = from
			override val signal: SIGNAL = signal
			
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				(transitionMap[from] ?: HashMap<SIGNAL, STATE>().let {
					transitionMap[from] = it
					it
				})[signal] = state
			}
		}
		
		init
		{
			init?.also { it() } ?: modify?.also { it() }
			expire()
		}
	}
}