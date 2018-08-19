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
		interface Insert
		interface Replace
		interface Update
		interface Delete
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