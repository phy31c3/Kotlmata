package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataState
{
	companion object
	{
		operator fun invoke(name: String? = null, block: Initializer.() -> Unit): KotlmataState = KotlmataStateImpl(name, block)
	}
	
	interface Initializer
	{
		val entry: Entry
		val input: Input
		val exit: Exit
	}
	
	interface Entry
	{
		infix fun action(action: () -> Any?)
		infix fun action(action: (Any) -> Any?)
		infix fun <T : Any> via(signal: KClass<T>): Action<T, Any?>
		infix fun <T : Any> via(signal: T): Action<T, Any?>
	}
	
	interface Input
	{
		infix fun action(action: (Any) -> Unit)
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

interface KotlmataMutableState : KotlmataState
{
	companion object
	{
		operator fun invoke(name: String? = null, block: (KotlmataState.Initializer.() -> Unit)? = null): KotlmataMutableState = KotlmataStateImpl(name, block)
	}
	
	interface Modifier : KotlmataState.Initializer
	{
		val delete: Delete
	}
	
	interface Delete
	{
		object all
		
		infix fun action(keyword: KotlmataState.Entry): Entry
		infix fun action(keyword: KotlmataState.Input): Input
		infix fun action(keyword: KotlmataState.Exit)
		infix fun action(keyword: all)
		
		interface Entry
		{
			infix fun <T : Any> via(signal: KClass<T>)
			infix fun <T : Any> via(signal: T)
			infix fun via(keyword: all)
		}
		
		interface Input
		{
			infix fun <T : Any> signal(signal: KClass<T>)
			infix fun <T : Any> signal(signal: T)
			infix fun signal(keyword: all)
		}
	}
	
	operator fun invoke(block: Modifier.() -> Unit)
	
	infix fun modify(block: Modifier.() -> Unit)
}

private object default

private val none = {}

internal class KotlmataStateImpl(key: Any? = null, block: (KotlmataState.Initializer.() -> Unit)? = null) : KotlmataMutableState
{
	private val key: Any = key ?: this
	private var entryMap: MutableMap<Any, (Any) -> Any?>? = null
	private var inputMap: MutableMap<Any, (Any) -> Unit>? = null
	private var exit: () -> Unit = none
	
	init
	{
		block?.let {
			modify(it)
		}
	}
	
	override fun invoke(block: KotlmataMutableState.Modifier.() -> Unit)
	{
		modify(block)
	}
	
	override fun modify(block: KotlmataMutableState.Modifier.() -> Unit)
	{
		ModifierImpl(block)
	}
	
	private inner class ModifierImpl internal constructor(block: KotlmataMutableState.Modifier.() -> Unit)
		: KotlmataMutableState.Modifier
	{
		@Volatile
		private var expired: Boolean = false
		
		private val entryMap: MutableMap<Any, (Any) -> Any?>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<Any, (Any) -> Any?>().let {
				this@KotlmataStateImpl.entryMap = it
				it
			}
		
		private val inputMap: MutableMap<Any, (Any) -> Unit>
			get() = this@KotlmataStateImpl.inputMap ?: HashMap<Any, (Any) -> Unit>().let {
				this@KotlmataStateImpl.inputMap = it
				it
			}
		
		@Suppress("UNCHECKED_CAST")
		override val entry: KotlmataState.Entry = object : KotlmataState.Entry
		{
			override fun action(action: () -> Any?)
			{
				expired should { return }
				entryMap[default] = { _ -> action() }
			}
			
			override fun action(action: (Any) -> Any?)
			{
				expired should { return }
				entryMap[default] = action
			}
			
			override fun <T : Any> via(signal: KClass<T>): KotlmataState.Action<T, Any?>
			{
				return object : KotlmataState.Action<T, Any?>
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
			
			override fun <T : Any> via(signal: T): KotlmataState.Action<T, Any?>
			{
				return object : KotlmataState.Action<T, Any?>
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
		override val input: KotlmataState.Input = object : KotlmataState.Input
		{
			override fun action(action: (Any) -> Unit)
			{
				TODO("not implemented")
			}
			
			override fun <T : Any> signal(signal: KClass<T>): KotlmataState.Action<T, Unit>
			{
				return object : KotlmataState.Action<T, Unit>
				{
					override fun action(action: (T) -> Unit)
					{
						expired should { return }
						inputMap[signal] = action as (Any) -> Unit
					}
					
					override fun action(action: () -> Unit)
					{
						expired should { return }
						inputMap[signal] = { _ -> action() }
					}
				}
			}
			
			override fun <T : Any> signal(signal: T): KotlmataState.Action<T, Unit>
			{
				return object : KotlmataState.Action<T, Unit>
				{
					override fun action(action: (T) -> Unit)
					{
						expired should { return }
						inputMap[signal] = action as (Any) -> Unit
					}
					
					override fun action(action: () -> Unit)
					{
						expired should { return }
						inputMap[signal] = { _ -> action() }
					}
				}
			}
		}
		
		override val exit: KotlmataState.Exit = object : KotlmataState.Exit
		{
			override fun action(action: () -> Unit)
			{
				expired should { return }
				this@KotlmataStateImpl.exit = action
			}
		}
		
		override val delete: KotlmataMutableState.Delete = object : KotlmataMutableState.Delete
		{
			override fun action(keyword: KotlmataState.Entry): KotlmataMutableState.Delete.Entry
			{
				TODO("not implemented")
			}
			
			override fun action(keyword: KotlmataState.Input): KotlmataMutableState.Delete.Input
			{
				TODO("not implemented")
			}
			
			override fun action(keyword: KotlmataState.Exit)
			{
				TODO("not implemented")
			}
			
			override fun action(keyword: KotlmataMutableState.Delete.all)
			{
				expired should { return }
				this@KotlmataStateImpl.entryMap = null
				this@KotlmataStateImpl.inputMap = null
				this@KotlmataStateImpl.exit = none
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