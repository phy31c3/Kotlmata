package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataState
{
	companion object
	{
		operator fun invoke(
				name: String? = null,
				block: Initializer.() -> Unit
		): KotlmataState = KotlmataStateImpl(name, block)
	}
	
	interface Initializer
	{
		val entry: Entry
		val input: Input
		val exit: Exit
	}
	
	interface Entry
	{
		infix fun action(action: () -> Any?) = this.action { _ -> action() }
		infix fun action(action: (signal: Any) -> Any?)
		infix fun <T : Any> via(signal: KClass<T>): action<T, Any?>
		infix fun <T : Any> via(signal: T): action<T, Any?>
	}
	
	interface Input
	{
		infix fun action(action: () -> Unit) = this.action { _ -> action() }
		infix fun action(action: (signal: Any) -> Unit)
		infix fun <T : Any> signal(signal: KClass<T>): action<T, Unit>
		infix fun <T : Any> signal(signal: T): action<T, Unit>
	}
	
	interface Exit
	{
		infix fun action(action: () -> Unit)
	}
	
	interface action<T, U>
	{
		infix fun action(action: () -> U) = this.action { _ -> action() }
		infix fun action(action: (signal: T) -> U)
	}
	
	val key: Any
	
	/**
	 * @param block If 'entry action' returns a next signal, the block is runned.
	 */
	fun entry(signal: Any, block: (signal: Any) -> Unit)
	
	/**
	 * @param block If 'entry action' returns a next signal, the block is runned.
	 */
	fun <T : Any> entry(signal: Any, type: KClass<in T>, block: (signal: Any) -> Unit)
	
	fun input(signal: Any)
	
	fun <T : Any> input(signal: T, type: KClass<in T>)
	
	fun exit()
}

interface KotlmataMutableState : KotlmataState
{
	companion object
	{
		operator fun invoke(
				name: String? = null,
				block: (KotlmataState.Initializer.() -> Unit)? = null
		): KotlmataMutableState = KotlmataStateImpl(name, block)
	}
	
	interface Modifier : KotlmataState.Initializer
	{
		val delete: Delete
	}
	
	interface Delete
	{
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

private class KotlmataStateImpl(
		key: Any? = null,
		block: (KotlmataState.Initializer.() -> Unit)? = null
) : KotlmataMutableState
{
	override val key: Any = key ?: this
	private var entry: ((SIGNAL) -> SIGNAL?)? = null
	private var input: ((SIGNAL) -> Unit)? = null
	private var exit: (() -> Unit)? = null
	private var entryMap: MutableMap<SIGNAL, (SIGNAL) -> SIGNAL?>? = null
	private var inputMap: MutableMap<SIGNAL, (SIGNAL) -> Unit>? = null
	
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
	
	override fun entry(signal: Any, block: (signal: Any) -> Unit)
	{
		val next = entryMap?.let {
			when
			{
				it.containsKey(signal) -> it[signal]?.invoke(signal)
				it.containsKey(signal::class) -> it[signal::class]?.invoke(signal)
				else -> null
			}
		} ?: entry?.invoke(signal)
		next?.apply(block)
	}
	
	override fun <T : Any> entry(signal: Any, type: KClass<in T>, block: (signal: Any) -> Unit)
	{
		val next = entryMap?.let {
			when
			{
				it.containsKey(signal::class) -> it[signal::class]?.invoke(signal)
				else -> null
			}
		} ?: entry?.invoke(signal)
		next?.apply(block)
	}
	
	override fun input(signal: Any)
	{
		inputMap?.let {
			when
			{
				it.containsKey(signal) -> it[signal]?.invoke(signal)
				it.containsKey(signal::class) -> it[signal::class]?.invoke(signal)
				else -> null
			}
		} ?: input?.invoke(signal)
	}
	
	override fun <T : Any> input(signal: T, type: KClass<in T>)
	{
		inputMap?.let {
			when
			{
				it.containsKey(type) -> it[type]?.invoke(signal)
				else -> null
			}
		} ?: input?.invoke(signal)
	}
	
	override fun exit()
	{
		exit?.invoke()
	}
	
	private inner class ModifierImpl internal constructor(
			block: KotlmataMutableState.Modifier.() -> Unit
	) : KotlmataMutableState.Modifier
	{
		@Volatile
		private var expired: Boolean = false
		
		private val entryMap: MutableMap<SIGNAL, (SIGNAL) -> SIGNAL?>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<SIGNAL, (SIGNAL) -> SIGNAL?>().apply {
				this@KotlmataStateImpl.entryMap = this
			}
		
		private val inputMap: MutableMap<SIGNAL, (SIGNAL) -> Unit>
			get() = this@KotlmataStateImpl.inputMap ?: HashMap<SIGNAL, (SIGNAL) -> Unit>().apply {
				this@KotlmataStateImpl.inputMap = this
			}
		
		@Suppress("UNCHECKED_CAST")
		override val entry: KotlmataState.Entry = object : KotlmataState.Entry
		{
			override fun action(action: (signal: Any) -> Any?)
			{
				expired should { return }
				this@KotlmataStateImpl.entry = action
			}
			
			override fun <T : Any> via(signal: KClass<T>): KotlmataState.action<T, Any?> = object : KotlmataState.action<T, Any?>
			{
				override fun action(action: (signal: T) -> Any?)
				{
					expired should { return }
					entryMap[signal] = action as (Any) -> Any?
				}
			}
			
			override fun <T : Any> via(signal: T): KotlmataState.action<T, Any?> = object : KotlmataState.action<T, Any?>
			{
				override fun action(action: (signal: T) -> Any?)
				{
					expired should { return }
					entryMap[signal] = action as (Any) -> Any?
				}
			}
		}
		
		@Suppress("UNCHECKED_CAST")
		override val input: KotlmataState.Input = object : KotlmataState.Input
		{
			override fun action(action: (signal: Any) -> Unit)
			{
				expired should { return }
				this@KotlmataStateImpl.input = action
			}
			
			override fun <T : Any> signal(signal: KClass<T>): KotlmataState.action<T, Unit> = object : KotlmataState.action<T, Unit>
			{
				override fun action(action: (signal: T) -> Unit)
				{
					expired should { return }
					inputMap[signal] = action as (Any) -> Unit
				}
			}
			
			override fun <T : Any> signal(signal: T): KotlmataState.action<T, Unit> = object : KotlmataState.action<T, Unit>
			{
				override fun action(action: (signal: T) -> Unit)
				{
					expired should { return }
					inputMap[signal] = action as (Any) -> Unit
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
				val stash = this@KotlmataStateImpl.entry
				expired not {
					this@KotlmataStateImpl.entry = null
				}
				return object : KotlmataMutableState.Delete.Entry
				{
					override fun <T : Any> via(signal: KClass<T>)
					{
						expired should { return }
						this@KotlmataStateImpl.entry = stash
						entryMap.remove(signal)
					}
					
					override fun <T : Any> via(signal: T)
					{
						expired should { return }
						this@KotlmataStateImpl.entry = stash
						entryMap.remove(signal)
					}
					
					override fun via(keyword: all)
					{
						expired should { return }
						this@KotlmataStateImpl.entry = stash
						this@KotlmataStateImpl.entryMap = null
					}
				}
			}
			
			override fun action(keyword: KotlmataState.Input): KotlmataMutableState.Delete.Input
			{
				val stash = this@KotlmataStateImpl.input
				expired not {
					this@KotlmataStateImpl.input = null
				}
				return object : KotlmataMutableState.Delete.Input
				{
					override fun <T : Any> signal(signal: KClass<T>)
					{
						expired should { return }
						this@KotlmataStateImpl.input = stash
						inputMap.remove(signal)
					}
					
					override fun <T : Any> signal(signal: T)
					{
						expired should { return }
						this@KotlmataStateImpl.input = stash
						inputMap.remove(signal)
					}
					
					override fun signal(keyword: all)
					{
						expired should { return }
						this@KotlmataStateImpl.input = stash
						this@KotlmataStateImpl.inputMap = null
					}
				}
			}
			
			override fun action(keyword: KotlmataState.Exit)
			{
				expired should { return }
				this@KotlmataStateImpl.exit = null
			}
			
			override fun action(keyword: all)
			{
				expired should { return }
				this@KotlmataStateImpl.entry = null
				this@KotlmataStateImpl.input = null
				this@KotlmataStateImpl.exit = null
				this@KotlmataStateImpl.entryMap = null
				this@KotlmataStateImpl.inputMap = null
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
		
		private inline infix fun Boolean.not(block: () -> Unit)
		{
			if (!expired)
			{
				block()
			}
		}
	}
}