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
		
		interface End
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
		block?.let {
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
		TODO("not implemented")
	}
	
	override fun <T : Any> input(signal: T, type: KClass<in T>)
	{
		TODO("not implemented")
	}
	
	private inner class ModifierImpl internal constructor(
			init: (KotlmataMachine.Initializer.() -> KotlmataMachine.Initialize.End)? = null,
			modify: (KotlmataMutableMachine.Modifier.() -> Unit)? = null
	) : KotlmataMachine.Initializer, KotlmataMutableMachine.Modifier
	{
		override val initialize: KotlmataMachine.Initialize
			get() = TODO("not implemented")
		override val has: KotlmataMutableMachine.Modifier.Has
			get() = TODO("not implemented")
		override val insert: KotlmataMutableMachine.Modifier.Insert
			get() = TODO("not implemented")
		override val replace: KotlmataMutableMachine.Modifier.Replace
			get() = TODO("not implemented")
		override val update: KotlmataMutableMachine.Modifier.Update
			get() = TODO("not implemented")
		override val delete: KotlmataMutableMachine.Modifier.Delete
			get() = TODO("not implemented")
		
		init
		{
			TODO("not implemented")
		}
		
		override fun Any.invoke(block: KotlmataState.Initializer.() -> Unit)
		{
			TODO("not implemented")
		}
		
		override fun Any.x(signal: Any): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun Any.x(signal: KClass<out Any>): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun Any.x(keyword: any): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun any.x(signal: Any): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun any.x(signal: KClass<out Any>): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun any.x(keyword: any): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
	}
}