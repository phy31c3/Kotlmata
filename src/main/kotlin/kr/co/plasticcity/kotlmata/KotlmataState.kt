package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataState<T : STATE>
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: Initializer.(state: String) -> Unit
		): KotlmataState<String> = KotlmataStateImpl(name, block)
	}
	
	@KotlmataMarker
	interface Initializer
	{
		val entry: Entry
		val input: Input
		val exit: Exit
	}
	
	interface Entry
	{
		infix fun action(action: Kotlmata.Action.(signal: SIGNAL) -> SIGNAL?)
		infix fun <T : SIGNAL> via(signal: KClass<T>): action<T, SIGNAL?>
		infix fun <T : SIGNAL> via(signal: T): action<T, SIGNAL?>
	}
	
	interface Input
	{
		infix fun action(action: Kotlmata.Action.(signal: SIGNAL) -> Unit)
		infix fun <T : SIGNAL> signal(signal: KClass<T>): action<T, Unit>
		infix fun <T : SIGNAL> signal(signal: T): action<T, Unit>
	}
	
	interface Exit
	{
		infix fun action(action: Kotlmata.Action.(signal: SIGNAL) -> Unit)
	}
	
	interface action<T : SIGNAL, U>
	{
		infix fun action(action: Kotlmata.Action.(signal: T) -> U)
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
				block: (KotlmataState.Initializer.(state: String) -> Unit)? = null
		): KotlmataMutableState<String> = KotlmataStateImpl(name, block)
		
		internal operator fun <T : STATE> invoke(
				key: T,
				prefix: String,
				logLevel: Int,
				block: (KotlmataState.Initializer.(state: T) -> Unit)
		): KotlmataMutableState<T> = KotlmataStateImpl(key, block, prefix, logLevel)
	}
	
	@KotlmataMarker
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
	
	infix fun modify(block: Modifier.(state: T) -> Unit)
	
	operator fun invoke(block: Modifier.(state: T) -> Unit) = modify(block)
}

private class KotlmataStateImpl<T : STATE>(
		override val key: T,
		block: (KotlmataState.Initializer.(T) -> Unit)? = null,
		val prefix: String = "State[$key]:",
		val logLevel: Int = Log.logLevel
) : KotlmataMutableState<T>
{
	private var entry: (Kotlmata.Action.(SIGNAL) -> SIGNAL?)? = null
	private var input: (Kotlmata.Action.(SIGNAL) -> Unit)? = null
	private var exit: (Kotlmata.Action.(SIGNAL) -> Unit)? = null
	private var entryMap: MutableMap<SIGNAL, Kotlmata.Action.(SIGNAL) -> SIGNAL?>? = null
	private var inputMap: MutableMap<SIGNAL, Kotlmata.Action.(SIGNAL) -> Unit>? = null
	
	init
	{
		block?.let {
			ModifierImpl(it)
		}
		logLevel.simple(prefix, key) { STATE_CREATED }
	}
	
	override fun entry(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	{
		val action: (Kotlmata.Action.(SIGNAL) -> SIGNAL?)? = entryMap?.let {
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
			Kotlmata.Action.it(signal)?.let { ret ->
				if (ret !is Unit) block(ret)
			}
		} ?: logLevel.normal(prefix, key, signal) { STATE_ENTRY_NONE }
	}
	
	override fun <T : SIGNAL> entry(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	{
		val action: (Kotlmata.Action.(SIGNAL) -> SIGNAL?)? = entryMap?.let {
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
			Kotlmata.Action.it(signal)?.let { ret ->
				if (ret !is Unit) block(ret)
			}
		} ?: logLevel.normal(prefix, key, signal) { STATE_ENTRY_NONE }
	}
	
	override fun input(signal: SIGNAL)
	{
		val action: (Kotlmata.Action.(SIGNAL) -> Unit)? = inputMap?.let {
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
		
		action?.also {
			Kotlmata.Action.it(signal)
		} ?: logLevel.normal(prefix, key, signal) { STATE_INPUT_NONE }
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	{
		val action: (Kotlmata.Action.(SIGNAL) -> Unit)? = inputMap?.let {
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
		
		action?.also {
			Kotlmata.Action.it(signal)
		} ?: logLevel.normal(prefix, key, signal) { STATE_INPUT_NONE }
	}
	
	override fun exit(signal: SIGNAL)
	{
		exit?.also {
			logLevel.normal(prefix, key, signal) { STATE_EXIT }
			Kotlmata.Action.it(signal)
		} ?: logLevel.normal(prefix, key, signal) { STATE_EXIT_NONE }
	}
	
	override fun modify(block: KotlmataMutableState.Modifier.(T) -> Unit)
	{
		ModifierImpl(block)
		logLevel.simple(prefix, key) { STATE_UPDATED }
	}
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
	
	private inner class ModifierImpl internal constructor(
			block: KotlmataMutableState.Modifier.(T) -> Unit
	) : KotlmataMutableState.Modifier, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		private val entryMap: MutableMap<SIGNAL, Kotlmata.Action.(SIGNAL) -> SIGNAL?>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<SIGNAL, Kotlmata.Action.(SIGNAL) -> SIGNAL?>().also {
				this@KotlmataStateImpl.entryMap = it
			}
		
		private val inputMap: MutableMap<SIGNAL, Kotlmata.Action.(SIGNAL) -> Unit>
			get() = this@KotlmataStateImpl.inputMap ?: HashMap<SIGNAL, Kotlmata.Action.(SIGNAL) -> Unit>().also {
				this@KotlmataStateImpl.inputMap = it
			}
		
		@Suppress("UNCHECKED_CAST")
		override val entry by lazy {
			object : KotlmataState.Entry
			{
				override fun action(action: Kotlmata.Action.(signal: SIGNAL) -> SIGNAL?)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.entry = action
				}
				
				override fun <T : SIGNAL> via(signal: KClass<T>) = object : KotlmataState.action<T, SIGNAL?>
				{
					override fun action(action: Kotlmata.Action.(signal: T) -> SIGNAL?)
					{
						this@ModifierImpl shouldNot expired
						entryMap[signal] = action as Kotlmata.Action.(SIGNAL) -> SIGNAL?
					}
				}
				
				override fun <T : SIGNAL> via(signal: T) = object : KotlmataState.action<T, SIGNAL?>
				{
					override fun action(action: Kotlmata.Action.(signal: T) -> SIGNAL?)
					{
						this@ModifierImpl shouldNot expired
						entryMap[signal] = action as Kotlmata.Action.(SIGNAL) -> SIGNAL?
					}
				}
			}
		}
		
		@Suppress("UNCHECKED_CAST")
		override val input by lazy {
			object : KotlmataState.Input
			{
				override fun action(action: Kotlmata.Action.(signal: SIGNAL) -> Unit)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.input = action
				}
				
				override fun <T : SIGNAL> signal(signal: KClass<T>) = object : KotlmataState.action<T, Unit>
				{
					override fun action(action: Kotlmata.Action.(signal: T) -> Unit)
					{
						this@ModifierImpl shouldNot expired
						inputMap[signal] = action as Kotlmata.Action.(SIGNAL) -> Unit
					}
				}
				
				override fun <T : SIGNAL> signal(signal: T) = object : KotlmataState.action<T, Unit>
				{
					override fun action(action: Kotlmata.Action.(signal: T) -> Unit)
					{
						this@ModifierImpl shouldNot expired
						inputMap[signal] = action as Kotlmata.Action.(SIGNAL) -> Unit
					}
				}
			}
		}
		
		override val exit by lazy {
			object : KotlmataState.Exit
			{
				override fun action(action: Kotlmata.Action.(signal: SIGNAL) -> Unit)
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
			block(key)
			expire()
		}
	}
}