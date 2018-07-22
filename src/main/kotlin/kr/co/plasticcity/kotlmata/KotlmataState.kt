package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataState
{
	companion object
	{
		infix fun new(tag: Any): KotlmataState = KotlmataStateImpl(tag)
	}
	
	infix fun set(block: DisposableSetter.() -> Unit): KotlmataState
	
	interface DisposableSetter
	{
		val entry: Entry
		val input: Input
		val exit: Exit
		
		interface Entry
		{
			infix fun action(action: () -> Any?)
			infix fun <T : Any> via(signal: KClass<T>): Action<T, Any?>
			infix fun <T : Any> via(signal: T): Action<T, Any?>
		}
		
		interface Input
		{
			infix fun <T : Any> signal(signal: KClass<T>): Action<T, Unit>
			infix fun <T : Any> signal(signal: T): Action<T, Unit>
		}
		
		interface Exit
		{
			infix fun action(action: () -> Unit)
		}
		
		interface Action<T, U>
		{
			infix fun action(action: (T) -> U)
			infix fun action(action: () -> U)
		}
	}
}

internal class KotlmataStateImpl(val tag: Any) : KotlmataState
{
	override fun set(block: KotlmataState.DisposableSetter.() -> Unit): KotlmataState
	{
		DisposableSetterImpl(block)
		return this
	}
	
	private inner class DisposableSetterImpl
	internal constructor(block: KotlmataState.DisposableSetter.() -> Unit)
		: KotlmataState.DisposableSetter
	{
		private var valid: Boolean = true
		
		init
		{
			block()
			valid = false
		}
		
		override val entry: KotlmataState.DisposableSetter.Entry
			get() = TODO("not implemented")
		override val input: KotlmataState.DisposableSetter.Input
			get() = TODO("not implemented")
		override val exit: KotlmataState.DisposableSetter.Exit
			get() = TODO("not implemented")
	}
}