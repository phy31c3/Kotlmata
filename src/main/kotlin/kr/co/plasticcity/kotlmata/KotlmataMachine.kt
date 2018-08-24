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
			infix fun to(state: Any): End
		}
		
		class End internal constructor()
	}
	
	interface StateDefine
	{
		operator fun Any.invoke(block: KotlmataState.Initializer.() -> Unit)
	}
	
	interface TransitionDefine
	{
		infix fun Any.x(signal: Any): TransitionLeft
		infix fun Any.x(signal: KClass<out Any>): TransitionLeft
		infix fun Any.x(keyword: any): TransitionLeft
		
		infix fun any.x(signal: Any): TransitionLeft
		infix fun any.x(signal: KClass<out Any>): TransitionLeft
		infix fun any.x(keyword: any): TransitionLeft
	}
	
	interface TransitionLeft
	{
		val state: STATE
		val signal: SIGNAL
		
		operator fun remAssign(state: Any)
	}
	
	val key: Any
	
	fun input(signal: Any)
	
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
		val has: Has
		val insert: Insert
		val replace: Replace
		val update: Update
		val delete: Delete
		
		interface Has
		{
			infix fun state(state: Any): then
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
			infix fun state(state: Any): of
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): remAssign
			infix fun or(keyword: Replace): state
			infix fun or(keyword: Update): transition
			
			interface state
			{
				infix fun state(state: Any): of
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
				operator fun remAssign(state: Any)
			}
		}
		
		interface Replace
		{
			infix fun state(state: Any): of
			
			interface of
			{
				infix fun of(block: KotlmataState.Initializer.() -> Unit)
			}
		}
		
		interface Update
		{
			infix fun state(state: Any): set
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
				operator fun remAssign(state: Any)
			}
		}
		
		interface Delete
		{
			infix fun state(state: Any)
			infix fun state(keyword: all)
			
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft)
			infix fun transition(keyword: of): state
			infix fun transition(keyword: all)
			
			interface state
			{
				infix fun state(state: Any)
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
	
	override fun input(signal: Any)
	{
		fun MutableMap<SIGNAL, STATE>.getNext(): STATE?
		{
			return this[signal] ?: this[signal::class] ?: this[any]
		}
		
		let {
			state.input(signal)
			transitionMap[state.key]?.getNext() ?: transitionMap[any]?.getNext()
		}?.let {
			stateMap[it]
		}?.let {
			state.exit()
			state = it
			state.entry(signal) { signal ->
				input(signal)
			}
		}
	}
	
	override fun <T : Any> input(signal: T, type: KClass<in T>)
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
			state.exit()
			state = it
			state.entry(signal, type) { signal ->
				input(signal)
			}
		}
	}
	
	private inner class ModifierImpl internal constructor(
			init: (KotlmataMachine.Initializer.() -> KotlmataMachine.Initialize.End)? = null,
			modify: (KotlmataMutableMachine.Modifier.() -> Unit)? = null
	) : KotlmataMachine.Initializer, KotlmataMutableMachine.Modifier
	{
		@Volatile
		private var expired: Boolean = false
		
		override val initialize = object : KotlmataMachine.Initialize
		{
			override fun origin(keyword: state) = object : KotlmataMachine.Initialize.to
			{
				override fun to(state: Any): KotlmataMachine.Initialize.End
				{
					expired should { return KotlmataMachine.Initialize.End() }
					
					stateMap[state]?.also {
						this@KotlmataMachineImpl.state = it
					} ?: KotlmataMutableState().also {
						this@KotlmataMachineImpl.state = it
						Logger.e(key, state) { INVALID_ORIGIN_STATE }
					}
					
					return KotlmataMachine.Initialize.End()
				}
			}
		}
		
		override val has = object : KotlmataMutableMachine.Modifier.Has
		{
			override fun state(state: Any) = object : KotlmataMutableMachine.Modifier.Has.then
			{
				override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.or
				{
					expired should { return stop() }
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
					expired should { return stop() }
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
		
		override val insert = object : KotlmataMutableMachine.Modifier.Insert
		{
			override fun state(state: Any) = object : KotlmataMutableMachine.Modifier.Insert.of
			{
				override fun of(block: KotlmataState.Initializer.() -> Unit)
				{
					expired should { return }
					stateMap[state] ?: state.invoke(block)
				}
			}
			
			override fun transition(transitionLeft: KotlmataMachine.TransitionLeft) = object : KotlmataMutableMachine.Modifier.Insert.remAssign
			{
				override fun remAssign(state: Any)
				{
					expired should { return }
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
				override fun state(state: Any) = object : KotlmataMutableMachine.Modifier.Insert.of
				{
					override fun of(block: KotlmataState.Initializer.() -> Unit)
					{
						expired should { return }
						state.invoke(block)
					}
				}
			}
			
			override fun or(keyword: KotlmataMutableMachine.Modifier.Update) = object : KotlmataMutableMachine.Modifier.Insert.transition
			{
				override fun transition(transitionLeft: KotlmataMachine.TransitionLeft) = object : KotlmataMutableMachine.Modifier.Insert.remAssign
				{
					override fun remAssign(state: Any)
					{
						expired should { return }
						transitionLeft %= state
					}
				}
			}
		}
		
		override val replace: KotlmataMutableMachine.Modifier.Replace
			get() = TODO("not implemented")
		override val update: KotlmataMutableMachine.Modifier.Update
			get() = TODO("not implemented")
		override val delete: KotlmataMutableMachine.Modifier.Delete
			get() = TODO("not implemented")
		
		override fun Any.invoke(block: KotlmataState.Initializer.() -> Unit)
		{
			expired should { return }
			stateMap[this] = KotlmataMutableState(this) { block() }
		}
		
		override fun Any.x(signal: Any) = transitionLeft(this, signal)
		override fun Any.x(signal: KClass<out Any>) = transitionLeft(this, signal)
		override fun Any.x(keyword: any) = transitionLeft(this, keyword)
		
		override fun any.x(signal: Any) = transitionLeft(this, signal)
		override fun any.x(signal: KClass<out Any>) = transitionLeft(this, signal)
		override fun any.x(keyword: any) = transitionLeft(this, keyword)
		
		private fun transitionLeft(from: STATE, signal: SIGNAL) = object : KotlmataMachine.TransitionLeft
		{
			override val state: STATE = from
			override val signal: SIGNAL = signal
			
			override fun remAssign(state: Any)
			{
				expired should { return }
				(transitionMap[from] ?: HashMap<SIGNAL, STATE>().let {
					transitionMap[from] = it
					it
				})[signal] = state
			}
		}
		
		init
		{
			init?.also { it() } ?: modify?.also { it() }
			expired = true
		}
		
		private inline infix fun Boolean.should(block: () -> Unit)
		{
			if (expired)
			{
				Logger.e(key) { INVALID_MACHINE_SETTER }
				block()
			}
		}
		
		private inline infix fun Boolean.not(block: () -> Unit)
		{
			if (!expired)
			{
				block()
			}
		}
	}
}