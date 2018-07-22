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

private class KotlmataStateImpl(val tag: Any) : KotlmataState
{
	private var entry: () -> Any? = none
	private val entryMap: MutableMap<Any, (Any) -> Any?> = HashMap()
	private val inputMap: MutableMap<Any, (Any) -> Unit> = HashMap()
	private var exit: () -> Unit = none
	
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
		
		@Suppress("UNCHECKED_CAST")
		override val entry: KotlmataState.DisposableSetter.Entry = object : KotlmataState.DisposableSetter.Entry
		{
			override fun action(action: () -> Any?)
			{
				ifValid { this@KotlmataStateImpl.entry = action }
			}
			
			override fun <T : Any> via(signal: KClass<T>): KotlmataState.DisposableSetter.Action<T, Any?>
			{
				return object : KotlmataState.DisposableSetter.Action<T, Any?>
				{
					override fun action(action: (T) -> Any?)
					{
						ifValid { entryMap[signal] = action as (Any) -> Any? }
					}
					
					override fun action(action: () -> Any?)
					{
						ifValid { entryMap[signal] = { _ -> action() } }
					}
				}
			}
			
			override fun <T : Any> via(signal: T): KotlmataState.DisposableSetter.Action<T, Any?>
			{
				return object : KotlmataState.DisposableSetter.Action<T, Any?>
				{
					override fun action(action: (T) -> Any?)
					{
						ifValid { entryMap[signal] = action as (Any) -> Any? }
					}
					
					override fun action(action: () -> Any?)
					{
						ifValid { entryMap[signal] = { _ -> action() } }
					}
				}
			}
		}
		
		@Suppress("UNCHECKED_CAST")
		override val input: KotlmataState.DisposableSetter.Input = object : KotlmataState.DisposableSetter.Input
		{
			override fun <T : Any> signal(signal: KClass<T>): KotlmataState.DisposableSetter.Action<T, Unit>
			{
				return object : KotlmataState.DisposableSetter.Action<T, Unit>
				{
					override fun action(action: (T) -> Unit)
					{
						ifValid { inputMap[signal] = action as (Any) -> Unit }
					}
					
					override fun action(action: () -> Unit)
					{
						ifValid { inputMap[signal] = { _ -> action() } }
					}
				}
			}
			
			override fun <T : Any> signal(signal: T): KotlmataState.DisposableSetter.Action<T, Unit>
			{
				return object : KotlmataState.DisposableSetter.Action<T, Unit>
				{
					override fun action(action: (T) -> Unit)
					{
						ifValid { inputMap[signal] = action as (Any) -> Unit }
					}
					
					override fun action(action: () -> Unit)
					{
						ifValid { inputMap[signal] = { _ -> action() } }
					}
				}
			}
		}
		
		override val exit: KotlmataState.DisposableSetter.Exit = object : KotlmataState.DisposableSetter.Exit
		{
			override fun action(action: () -> Unit)
			{
				ifValid { this@KotlmataStateImpl.exit = action }
			}
		}
		
		init
		{
			block()
			valid = false
		}
		
		private fun ifValid(block: () -> Unit)
		{
			if (valid)
			{
				block()
			}
			else
			{
				Logger.e(tag) { INVALID_STATE_SETTER }
			}
		}
	}
}

private val none = {}