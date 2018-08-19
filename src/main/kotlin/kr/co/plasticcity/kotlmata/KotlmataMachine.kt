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
		val templates: Templates
		val initialize: Initialize
	}
	
	interface Templates
	{
		operator fun invoke(block: TemplateDefine.() -> Unit)
		
		interface TemplateDefine
		{
			operator fun Any.invoke(block: KotlmataState.Initializer.() -> Unit)
		}
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
		operator fun Any.invoke(block: StateInitializer.() -> Unit)
		
		interface StateInitializer : KotlmataState.Initializer
		{
			val extends: Extends
		}
		
		interface Extends
		{
			infix fun template(template: Any)
		}
	}
	
	interface TransitionDefine
	{
		infix fun Any.x(signal: Any): TransitionLeft
		infix fun Any.x(signal: KClass<out Any>): TransitionLeft
		infix fun Any.x(keyword: any): TransitionLeft
		
		interface TransitionLeft
		{
			operator fun remAssign(state: Any)
		}
	}
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
			infix fun transition(state: Any): X
			
			interface X
			{
				infix fun x(signal: Any): Then
			}
			
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
			infix fun transition(state: Any): X
			infix fun or(keyword: Replace): State
			infix fun or(keyword: Update): Transition
			
			interface State
			{
				infix fun state(state: Any): Of
			}
			
			interface Transition
			{
				infix fun transition(state: Any): X
			}
			
			interface Of
			{
				infix fun of(block: KotlmataMachine.StateDefine.StateInitializer.() -> Unit)
			}
			
			interface X
			{
				infix fun x(signal: Any): TransitionLeft
			}
			
			interface TransitionLeft
			{
				operator fun remAssign(state: Any)
			}
		}
		
		interface Replace
		{
			infix fun state(state: Any): Of
			
			interface Of
			{
				infix fun of(block: KotlmataMachine.StateDefine.StateInitializer.() -> Unit)
			}
		}
		
		interface Update
		{
			infix fun state(state: Any): Set
			infix fun transition(state: Any): X
			
			interface Set
			{
				infix fun set(block: KotlmataMutableState.Modifier.() -> Unit): Or
			}
			
			interface Or
			{
				infix fun or(block: KotlmataMachine.StateDefine.StateInitializer.() -> Unit)
			}
			
			interface X
			{
				infix fun x(signal: Any): TransitionLeft
			}
			
			interface TransitionLeft
			{
				operator fun remAssign(state: Any)
			}
		}
		
		interface Delete
		{
			infix fun state(state: Any)
			infix fun state(keyword: all)
			
			infix fun transition(state: Any): X
			infix fun transition(keyword: any): AnyLeft
			infix fun transition(keyword: all)
			
			interface X
			{
				infix fun x(signal: Any)
				infix fun x(keyword: any)
			}
			
			interface AnyLeft
			{
				infix fun x(signal: Any)
				infix fun x(keyword: any)
				operator fun remAssign(state: Any)
				operator fun remAssign(keyword: any)
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
	override fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		TODO("not implemented")
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		TODO("not implemented")
	}
}