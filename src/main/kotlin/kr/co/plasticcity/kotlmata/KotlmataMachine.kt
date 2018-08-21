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
		infix fun origin(keyword: state): To
		
		interface To
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
			infix fun state(state: Any): Then
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): Then
			
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
			infix fun state(state: Any): Of
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): RemAssign
			infix fun or(keyword: Replace): State
			infix fun or(keyword: Update): Transition
			
			interface State
			{
				infix fun state(state: Any): Of
			}
			
			interface Transition
			{
				infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): RemAssign
			}
			
			interface Of
			{
				infix fun of(block: KotlmataState.Initializer.() -> Unit)
			}
			
			interface RemAssign
			{
				operator fun remAssign(state: Any)
			}
		}
		
		interface Replace
		{
			infix fun state(state: Any): Of
			
			interface Of
			{
				infix fun of(block: KotlmataState.Initializer.() -> Unit)
			}
		}
		
		interface Update
		{
			infix fun state(state: Any): Set
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft): RemAssign
			
			interface Set
			{
				infix fun set(block: KotlmataMutableState.Modifier.() -> Unit): Or
			}
			
			interface Or
			{
				infix fun or(block: KotlmataState.Initializer.() -> Unit)
			}
			
			interface RemAssign
			{
				operator fun remAssign(state: Any)
			}
		}
		
		interface Delete
		{
			infix fun state(state: Any)
			infix fun state(keyword: all)
			
			infix fun transition(transitionLeft: KotlmataMachine.TransitionLeft)
			infix fun transition(keyword: of): State
			infix fun transition(keyword: all)
			
			interface State
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
	private val key: Any = key ?: this
	private val stateMap: MutableMap<Any, KotlmataMutableState> = HashMap()
	private val transitionMap: MutableMap<Any, MutableMap<Any, Any>> = HashMap()
	
	override fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		TODO("not implemented")
	}
	
	override fun input(signal: Any)
	{
		TODO("not implemented")
	}
	
	override fun <T : Any> input(signal: T, type: KClass<in T>)
	{
		TODO("not implemented")
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		TODO("not implemented")
	}
}