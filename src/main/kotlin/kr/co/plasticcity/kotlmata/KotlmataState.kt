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
		infix fun action(action: (signal: SIGNAL) -> SIGNAL?)
		infix fun <T : SIGNAL> via(signal: KClass<T>): action<T, SIGNAL?>
		infix fun <T : SIGNAL> via(signal: T): action<T, SIGNAL?>
	}
	
	interface Input
	{
		infix fun action(action: (signal: SIGNAL) -> Unit)
		infix fun <T : SIGNAL> signal(signal: KClass<T>): action<T, Unit>
		infix fun <T : SIGNAL> signal(signal: T): action<T, Unit>
	}
	
	interface Exit
	{
		infix fun action(action: (signal: SIGNAL) -> Unit)
	}
	
	interface action<T : SIGNAL, U>
	{
		infix fun action(action: (signal: T) -> U)
	}
	
	val key: KEY
	
	/**
	 * @param block If 'entry action' returns a next signal, the block is executed.
	 */
	fun entry(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	
	/**
	 * @param block If 'entry action' returns a next signal, the block is executed.
	 */
	fun <T : SIGNAL> entry(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	
	fun input(signal: SIGNAL)
	
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	
	fun exit(signal: SIGNAL)
}

interface KotlmataMutableState : KotlmataState
{
	companion object
	{
		operator fun invoke(
				name: String? = null,
				block: (KotlmataState.Initializer.() -> Unit)? = null
		): KotlmataMutableState = KotlmataStateImpl(name, block)
		
		internal operator fun invoke(
				key: KEY,
				block: (KotlmataState.Initializer.() -> Unit)
		): KotlmataMutableState = KotlmataStateImpl(key, block)
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
			infix fun <T : SIGNAL> via(signal: KClass<T>)
			infix fun <T : SIGNAL> via(signal: T)
			infix fun via(keyword: all)
		}
		
		interface Input
		{
			infix fun <T : SIGNAL> signal(signal: KClass<T>)
			infix fun <T : SIGNAL> signal(signal: T)
			infix fun signal(keyword: all)
		}
	}
	
	operator fun invoke(block: Modifier.() -> Unit)
	
	infix fun modify(block: Modifier.() -> Unit)
}

private class KotlmataStateImpl(
		key: KEY? = null,
		block: (KotlmataState.Initializer.() -> Unit)? = null
) : KotlmataMutableState
{
	override val key: KEY = key ?: this
	
	private var entry: ((SIGNAL) -> SIGNAL?)? = null
	private var input: ((SIGNAL) -> Unit)? = null
	private var exit: ((SIGNAL) -> Unit)? = null
	private var entryMap: MutableMap<SIGNAL, (SIGNAL) -> SIGNAL?>? = null
	private var inputMap: MutableMap<SIGNAL, (SIGNAL) -> Unit>? = null
	
	init
	{
		block?.also {
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
	
	override fun entry(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	{
		val next = entryMap?.let {
			when
			{
				it.containsKey(signal) -> it[signal]?.invoke(signal)
				it.containsKey(signal::class) -> it[signal::class]?.invoke(signal)
				else -> null
			}
		} ?: entry?.invoke(signal)
		next?.also {
			if (it !is Unit) block(it)
		}
	}
	
	override fun <T : SIGNAL> entry(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	{
		val next = entryMap?.let {
			when
			{
				it.containsKey(signal::class) -> it[signal::class]?.invoke(signal)
				else -> null
			}
		} ?: entry?.invoke(signal)
		next?.also {
			if (it !is Unit) block(it)
		}
	}
	
	override fun input(signal: SIGNAL)
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
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	{
		inputMap?.let {
			when
			{
				it.containsKey(type) -> it[type]?.invoke(signal)
				else -> null
			}
		} ?: input?.invoke(signal)
	}
	
	override fun exit(signal: SIGNAL)
	{
		exit?.invoke(signal)
	}
	
	private inner class ModifierImpl internal constructor(
			block: KotlmataMutableState.Modifier.() -> Unit
	) : KotlmataMutableState.Modifier, Expirable({ Log.e(key) { EXPIRED_STATE_MODIFIER } })
	{
		private val entryMap: MutableMap<SIGNAL, (SIGNAL) -> SIGNAL?>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<SIGNAL, (SIGNAL) -> SIGNAL?>().apply {
				this@KotlmataStateImpl.entryMap = this
			}
		
		private val inputMap: MutableMap<SIGNAL, (SIGNAL) -> Unit>
			get() = this@KotlmataStateImpl.inputMap ?: HashMap<SIGNAL, (SIGNAL) -> Unit>().apply {
				this@KotlmataStateImpl.inputMap = this
			}
		
		@Suppress("UNCHECKED_CAST")
		override val entry by lazy {
			object : KotlmataState.Entry
			{
				override fun action(action: (signal: SIGNAL) -> SIGNAL?)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.entry = action
				}
				
				override fun <T : SIGNAL> via(signal: KClass<T>) = object : KotlmataState.action<T, SIGNAL?>
				{
					override fun action(action: (signal: T) -> SIGNAL?)
					{
						this@ModifierImpl shouldNot expired
						entryMap[signal] = action as (SIGNAL) -> SIGNAL?
					}
				}
				
				override fun <T : SIGNAL> via(signal: T) = object : KotlmataState.action<T, SIGNAL?>
				{
					override fun action(action: (signal: T) -> SIGNAL?)
					{
						this@ModifierImpl shouldNot expired
						entryMap[signal] = action as (SIGNAL) -> SIGNAL?
					}
				}
			}
		}
		
		@Suppress("UNCHECKED_CAST")
		override val input by lazy {
			object : KotlmataState.Input
			{
				override fun action(action: (signal: SIGNAL) -> Unit)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.input = action
				}
				
				override fun <T : SIGNAL> signal(signal: KClass<T>) = object : KotlmataState.action<T, Unit>
				{
					override fun action(action: (signal: T) -> Unit)
					{
						this@ModifierImpl shouldNot expired
						inputMap[signal] = action as (SIGNAL) -> Unit
					}
				}
				
				override fun <T : SIGNAL> signal(signal: T) = object : KotlmataState.action<T, Unit>
				{
					override fun action(action: (signal: T) -> Unit)
					{
						this@ModifierImpl shouldNot expired
						inputMap[signal] = action as (SIGNAL) -> Unit
					}
				}
			}
		}
		
		override val exit by lazy {
			object : KotlmataState.Exit
			{
				override fun action(action: (signal: SIGNAL) -> Unit)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.exit = action
				}
			}
		}
		
		override val delete by lazy {
			object : KotlmataMutableState.Delete
			{
				override fun action(keyword: KotlmataState.Entry): KotlmataMutableState.Delete.Entry
				{
					val stash = this@KotlmataStateImpl.entry
					this@ModifierImpl not expired then {
						this@KotlmataStateImpl.entry = null
					}
					return object : KotlmataMutableState.Delete.Entry
					{
						override fun <T : SIGNAL> via(signal: KClass<T>)
						{
							this@ModifierImpl shouldNot expired
							this@KotlmataStateImpl.entry = stash
							entryMap.remove(signal)
						}
						
						override fun <T : SIGNAL> via(signal: T)
						{
							this@ModifierImpl shouldNot expired
							this@KotlmataStateImpl.entry = stash
							entryMap.remove(signal)
						}
						
						override fun via(keyword: all)
						{
							this@ModifierImpl shouldNot expired
							this@KotlmataStateImpl.entry = stash
							this@KotlmataStateImpl.entryMap = null
						}
					}
				}
				
				override fun action(keyword: KotlmataState.Input): KotlmataMutableState.Delete.Input
				{
					val stash = this@KotlmataStateImpl.input
					this@ModifierImpl not expired then {
						this@KotlmataStateImpl.input = null
					}
					return object : KotlmataMutableState.Delete.Input
					{
						override fun <T : SIGNAL> signal(signal: KClass<T>)
						{
							this@ModifierImpl shouldNot expired
							this@KotlmataStateImpl.input = stash
							inputMap.remove(signal)
						}
						
						override fun <T : SIGNAL> signal(signal: T)
						{
							this@ModifierImpl shouldNot expired
							this@KotlmataStateImpl.input = stash
							inputMap.remove(signal)
						}
						
						override fun signal(keyword: all)
						{
							this@ModifierImpl shouldNot expired
							this@KotlmataStateImpl.input = stash
							this@KotlmataStateImpl.inputMap = null
						}
					}
				}
				
				override fun action(keyword: KotlmataState.Exit)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.exit = null
				}
				
				override fun action(keyword: all)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.entry = null
					this@KotlmataStateImpl.input = null
					this@KotlmataStateImpl.exit = null
					this@KotlmataStateImpl.entryMap = null
					this@KotlmataStateImpl.inputMap = null
				}
			}
		}
		
		init
		{
			block()
			expire()
		}
	}
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
}