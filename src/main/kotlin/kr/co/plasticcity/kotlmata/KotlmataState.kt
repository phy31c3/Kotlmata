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
		val event: Event
		val exit: Exit
		
		interface Entry
		{
			infix fun action(action: () -> Any?)
			infix fun <T : Any> via(signal: KClass<T>): Action<T, Any?>
			infix fun <T : Any> via(signal: T): Action<T, Any?>
		}
		
		interface Event
		{
			infix fun <T : Any> input(signal: KClass<T>): Action<T, Unit>
			infix fun <T : Any> input(signal: T): Action<T, Unit>
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
	private companion object
	{
		private val none = {}
	}
	
	private var entry: () -> Any? = none
	private var entryMap: MutableMap<Any, (Any) -> Any?>? = null
	private var eventMap: MutableMap<Any, (Any) -> Unit>? = null
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
		
		private val entryMap: MutableMap<Any, (Any) -> Any?>
			get()
			{
				if (this@KotlmataStateImpl.entryMap == null)
				{
					this@KotlmataStateImpl.entryMap = HashMap()
				}
				return this@KotlmataStateImpl.entryMap!!
			}
		
		private val eventMap: MutableMap<Any, (Any) -> Unit>
			get()
			{
				if (this@KotlmataStateImpl.eventMap == null)
				{
					this@KotlmataStateImpl.eventMap = HashMap()
				}
				return this@KotlmataStateImpl.eventMap!!
			}
		
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
		override val event: KotlmataState.DisposableSetter.Event = object : KotlmataState.DisposableSetter.Event
		{
			override fun <T : Any> input(signal: KClass<T>): KotlmataState.DisposableSetter.Action<T, Unit>
			{
				return object : KotlmataState.DisposableSetter.Action<T, Unit>
				{
					override fun action(action: (T) -> Unit)
					{
						ifValid { eventMap[signal] = action as (Any) -> Unit }
					}
					
					override fun action(action: () -> Unit)
					{
						ifValid { eventMap[signal] = { _ -> action() } }
					}
				}
			}
			
			override fun <T : Any> input(signal: T): KotlmataState.DisposableSetter.Action<T, Unit>
			{
				return object : KotlmataState.DisposableSetter.Action<T, Unit>
				{
					override fun action(action: (T) -> Unit)
					{
						ifValid { eventMap[signal] = action as (Any) -> Unit }
					}
					
					override fun action(action: () -> Unit)
					{
						ifValid { eventMap[signal] = { _ -> action() } }
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
		
		private inline fun ifValid(block: () -> Unit)
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