package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataState<T : STATE>
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: Initializer<String>.() -> Unit
		): KotlmataState<String> = KotlmataStateImpl(name, block)
	}
	
	interface KeyHolder<T : STATE>
	{
		val state: T
	}
	
	interface Initializer<T : STATE> : KeyHolder<T>
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
	
	val key: T
	
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

interface KotlmataMutableState<T : STATE> : KotlmataState<T>
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: (KotlmataState.Initializer<String>.() -> Unit)? = null
		): KotlmataMutableState<String> = KotlmataStateImpl(name, block)
		
		internal operator fun <T : STATE> invoke(
				key: T,
				prefix: String,
				logLevel: Int,
				block: (KotlmataState.Initializer<T>.() -> Unit)
		): KotlmataMutableState<T> = KotlmataStateImpl(key, block, prefix, logLevel)
	}
	
	interface Modifier<T : STATE> : KotlmataState.Initializer<T>
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
	
	infix fun modify(block: Modifier<T>.() -> Unit)
	
	operator fun invoke(block: Modifier<T>.() -> Unit) = modify(block)
}

private class KotlmataStateImpl<T : STATE>(
		override val key: T,
		block: (KotlmataState.Initializer<T>.() -> Unit)? = null,
		val prefix: String = "State[$key]:",
		val logLevel: Int = Log.logLevel
) : KotlmataMutableState<T>
{
	private var entry: ((SIGNAL) -> SIGNAL?)? = null
	private var input: ((SIGNAL) -> Unit)? = null
	private var exit: ((SIGNAL) -> Unit)? = null
	private var entryMap: MutableMap<SIGNAL, (SIGNAL) -> SIGNAL?>? = null
	private var inputMap: MutableMap<SIGNAL, (SIGNAL) -> Unit>? = null
	
	init
	{
		block?.let {
			ModifierImpl(it)
		}
		logLevel.simple(prefix, key) { STATE_CREATED }
	}
	
	override fun entry(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	{
		val action = entryMap?.let {
			when
			{
				signal in it ->
				{
					logLevel.normal(prefix, key, signal) { STATE_ENTRY_SIGNAL }
					it[signal]
				}
				signal::class in it ->
				{
					logLevel.normal(prefix, key, signal, "${signal::class.simpleName}::class") { STATE_ENTRY_TYPED }
					it[signal::class]
				}
				else -> null
			}
		} ?: entry?.also {
			logLevel.normal(prefix, key, signal) { STATE_ENTRY_DEFAULT }
		}
		
		action?.also {
			it(signal)?.let { ret ->
				if (ret !is Unit) block(ret)
			}
		} ?: logLevel.normal(prefix, key, signal) { STATE_ENTRY_NONE }
	}
	
	override fun <T : SIGNAL> entry(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	{
		val action = entryMap?.let {
			when (type)
			{
				in it ->
				{
					logLevel.normal(prefix, key, signal, "${type.simpleName}::class") { STATE_ENTRY_TYPED }
					it[type]
				}
				else -> null
			}
		} ?: entry?.also {
			logLevel.normal(prefix, key, signal) { STATE_ENTRY_DEFAULT }
		}
		
		action?.also {
			it(signal)?.let { ret ->
				if (ret !is Unit) block(ret)
			}
		} ?: logLevel.normal(prefix, key, signal) { STATE_ENTRY_NONE }
	}
	
	override fun input(signal: SIGNAL)
	{
		val action = inputMap?.let {
			when
			{
				signal in it ->
				{
					logLevel.normal(prefix, key, signal) { STATE_INPUT_SIGNAL }
					it[signal]
				}
				signal::class in it ->
				{
					logLevel.normal(prefix, key, signal, "${signal::class.simpleName}::class") { STATE_INPUT_TYPED }
					it[signal::class]
				}
				else -> null
			}
		} ?: input?.also {
			logLevel.normal(prefix, key, signal) { STATE_INPUT_DEFAULT }
		}
		
		action?.invoke(signal) ?: logLevel.normal(prefix, key, signal) { STATE_INPUT_NONE }
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	{
		val action = inputMap?.let {
			when (type)
			{
				in it ->
				{
					logLevel.normal(prefix, key, signal, "${signal::class.simpleName}::class") { STATE_INPUT_TYPED }
					it[type]
				}
				else -> null
			}
		} ?: input?.also {
			logLevel.normal(prefix, key, signal) { STATE_INPUT_DEFAULT }
		}
		
		action?.invoke(signal) ?: logLevel.normal(prefix, key, signal) { STATE_INPUT_NONE }
	}
	
	override fun exit(signal: SIGNAL)
	{
		exit?.also {
			logLevel.normal(prefix, key, signal) { STATE_EXIT }
			it(signal)
		} ?: logLevel.normal(prefix, key, signal) { STATE_EXIT_NONE }
	}
	
	override fun modify(block: KotlmataMutableState.Modifier<T>.() -> Unit)
	{
		ModifierImpl(block)
		logLevel.simple(prefix, key) { STATE_UPDATED }
	}
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
	
	private inner class ModifierImpl internal constructor(
			block: KotlmataMutableState.Modifier<T>.() -> Unit
	) : KotlmataMutableState.Modifier<T>, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		private val entryMap: MutableMap<SIGNAL, (SIGNAL) -> SIGNAL?>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<SIGNAL, (SIGNAL) -> SIGNAL?>().also {
				this@KotlmataStateImpl.entryMap = it
			}
		
		private val inputMap: MutableMap<SIGNAL, (SIGNAL) -> Unit>
			get() = this@KotlmataStateImpl.inputMap ?: HashMap<SIGNAL, (SIGNAL) -> Unit>().also {
				this@KotlmataStateImpl.inputMap = it
			}
		
		override val state: T
			get() = this@KotlmataStateImpl.key
		
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
				override fun action(keyword: KotlmataState.Entry) = object : KotlmataMutableState.Delete.Entry
				{
					val stash = this@KotlmataStateImpl.entry
					
					init
					{
						this@ModifierImpl not expired then {
							this@KotlmataStateImpl.entry = null
						}
					}
					
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
				
				override fun action(keyword: KotlmataState.Input) = object : KotlmataMutableState.Delete.Input
				{
					val stash = this@KotlmataStateImpl.input
					
					init
					{
						this@ModifierImpl not expired then {
							this@KotlmataStateImpl.input = null
						}
					}
					
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
}