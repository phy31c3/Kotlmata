package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataMachine
{
	companion object
	{
		operator fun invoke(
				name: String? = null,
				block: Initializer.() -> Initialize.End
		): KotlmataMachine? = null
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
		infix fun Any.X(signal: Any): TransitionLeft = x(signal)
		infix fun Any.X(signal: KClass<out Any>): TransitionLeft = x(signal)
		infix fun Any.X(keyword: any): TransitionLeft = x(keyword)
		
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
		): KotlmataMutableMachine? = null
	}
}