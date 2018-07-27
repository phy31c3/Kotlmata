package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataState
{
	companion object
	{
		infix fun new(name: String): KotlmataState = KotlmataStateImpl(name)
		infix fun new(block: Setter.() -> Unit): KotlmataState = KotlmataStateImpl(block)
	}
	
	operator fun invoke(block: Setter.() -> Unit): KotlmataState
	
	infix fun set(block: Setter.() -> Unit): KotlmataState
	
	interface Setter
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

private class KotlmataStateImpl(key: Any? = null, block: (KotlmataState.Setter.() -> Unit)? = null) : KotlmataState
{
	private companion object
	{
		private val none = {}
	}
	
	private val key: Any = key ?: this
	private var entry: () -> Any? = none
	private var entryMap: MutableMap<Any, (Any) -> Any?>? = null
	private var eventMap: MutableMap<Any, (Any) -> Unit>? = null
	private var exit: () -> Unit = none
	
	init
	{
		block?.let {
			set(it)
		}
	}
	
	override fun invoke(block: KotlmataState.Setter.() -> Unit): KotlmataState = set(block)
	
	override fun set(block: KotlmataState.Setter.() -> Unit): KotlmataState
	{
		SetterImpl(block)
		return this
	}
	
	private inner class SetterImpl internal constructor(block: KotlmataState.Setter.() -> Unit)
		: KotlmataState.Setter
	{
		@Volatile
		private var expired: Boolean = false
		
		private val entryMap: MutableMap<Any, (Any) -> Any?>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<Any, (Any) -> Any?>().let {
				this@KotlmataStateImpl.entryMap = it
				it
			}
		
		private val eventMap: MutableMap<Any, (Any) -> Unit>
			get() = this@KotlmataStateImpl.eventMap ?: HashMap<Any, (Any) -> Unit>().let {
				this@KotlmataStateImpl.eventMap = it
				it
			}
		
		@Suppress("UNCHECKED_CAST")
		override val entry: KotlmataState.Setter.Entry = object : KotlmataState.Setter.Entry
		{
			override fun action(action: () -> Any?)
			{
				expired should { return }
				this@KotlmataStateImpl.entry = action
			}
			
			override fun <T : Any> via(signal: KClass<T>): KotlmataState.Setter.Action<T, Any?>
			{
				return object : KotlmataState.Setter.Action<T, Any?>
				{
					override fun action(action: (T) -> Any?)
					{
						expired should { return }
						entryMap[signal] = action as (Any) -> Any?
					}
					
					override fun action(action: () -> Any?)
					{
						expired should { return }
						entryMap[signal] = { _ -> action() }
					}
				}
			}
			
			override fun <T : Any> via(signal: T): KotlmataState.Setter.Action<T, Any?>
			{
				return object : KotlmataState.Setter.Action<T, Any?>
				{
					override fun action(action: (T) -> Any?)
					{
						expired should { return }
						entryMap[signal] = action as (Any) -> Any?
					}
					
					override fun action(action: () -> Any?)
					{
						expired should { return }
						entryMap[signal] = { _ -> action() }
					}
				}
			}
		}
		
		@Suppress("UNCHECKED_CAST")
		override val event: KotlmataState.Setter.Event = object : KotlmataState.Setter.Event
		{
			override fun <T : Any> input(signal: KClass<T>): KotlmataState.Setter.Action<T, Unit>
			{
				return object : KotlmataState.Setter.Action<T, Unit>
				{
					override fun action(action: (T) -> Unit)
					{
						expired should { return }
						eventMap[signal] = action as (Any) -> Unit
					}
					
					override fun action(action: () -> Unit)
					{
						expired should { return }
						eventMap[signal] = { _ -> action() }
					}
				}
			}
			
			override fun <T : Any> input(signal: T): KotlmataState.Setter.Action<T, Unit>
			{
				return object : KotlmataState.Setter.Action<T, Unit>
				{
					override fun action(action: (T) -> Unit)
					{
						expired should { return }
						eventMap[signal] = action as (Any) -> Unit
					}
					
					override fun action(action: () -> Unit)
					{
						expired should { return }
						eventMap[signal] = { _ -> action() }
					}
				}
			}
		}
		
		override val exit: KotlmataState.Setter.Exit = object : KotlmataState.Setter.Exit
		{
			override fun action(action: () -> Unit)
			{
				expired should { return }
				this@KotlmataStateImpl.exit = action
			}
		}
		
		init
		{
			block()
			expired = true
		}
		
		private inline infix fun Boolean.should(block: () -> Unit)
		{
			if (expired)
			{
				Logger.e(key) { INVALID_STATE_SETTER }
				block()
			}
		}
	}
}