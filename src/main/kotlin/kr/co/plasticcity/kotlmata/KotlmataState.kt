package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataState<T : STATE>
{
	companion object
	{
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG,
				block: Initializer.(state: String) -> Unit
		): KotlmataState<String> = KotlmataStateImpl(name, logLevel, block = block)
	}
	
	@KotlmataMarker
	interface Initializer
	{
		val entry: Entry
		val input: Input
		val exit: Exit
		
		infix fun SIGNAL.or(signal: SIGNAL): Signals
		
		interface Signals : MutableList<SIGNAL>
		{
			infix fun or(signal: SIGNAL): Signals
		}
	}
	
	interface Entry
	{
		infix fun <R> action(action: KotlmataAction2<SIGNAL, R>)
		infix fun <T : SIGNAL> via(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> via(signal: T): Action<T>
		infix fun via(signals: Initializer.Signals): Action<SIGNAL>
		
		interface Action<T : SIGNAL>
		{
			infix fun <R> action(action: KotlmataAction2<T, R>)
		}
	}
	
	interface Input
	{
		infix fun action(action: KotlmataAction2<SIGNAL, Unit>)
		infix fun <T : SIGNAL> signal(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> signal(signal: T): Action<T>
		infix fun signal(signals: Initializer.Signals): Action<SIGNAL>
		
		interface Action<T : SIGNAL>
		{
			infix fun action(action: KotlmataAction2<T, Unit>)
		}
	}
	
	interface Exit
	{
		infix fun action(action: KotlmataAction2<SIGNAL, Unit>)
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
				logLevel: Int = NO_LOG,
				block: (KotlmataState.Initializer.(state: String) -> Unit)? = null
		): KotlmataMutableState<String> = KotlmataStateImpl(name, logLevel, block = block)
		
		internal fun <T : STATE> create(
				key: T,
				logLevel: Int,
				prefix: String,
				block: (KotlmataState.Initializer.(state: T) -> Unit)
		): KotlmataMutableState<T> = KotlmataStateImpl(key, logLevel, prefix, block)
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
	
	operator fun invoke(block: Modifier.(state: T) -> Unit) = modify(block)
	
	infix fun modify(block: Modifier.(state: T) -> Unit)
}

private class KotlmataStateImpl<T : STATE>(
		override val key: T,
		val logLevel: Int = NO_LOG,
		val prefix: String = "State[$key]:",
		block: (KotlmataState.Initializer.(T) -> Unit)? = null
) : KotlmataMutableState<T>
{
	private var entry: (KotlmataAction2<SIGNAL, Any?>)? = null
	private var input: (KotlmataAction2<SIGNAL, Unit>)? = null
	private var exit: (KotlmataAction2<SIGNAL, Unit>)? = null
	private var entryMap: MutableMap<SIGNAL, KotlmataAction2<SIGNAL, Any?>>? = null
	private var inputMap: MutableMap<SIGNAL, KotlmataAction2<SIGNAL, Unit>>? = null
	
	init
	{
		block?.let {
			ModifierImpl(it)
		}
		if (key != initial)
		{
			logLevel.simple(prefix, key) { STATE_CREATED }
		}
	}
	
	override fun entry(signal: SIGNAL, block: (signal: SIGNAL) -> Unit)
	{
		val action: (KotlmataAction2<SIGNAL, Any?>)? = entryMap?.let {
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
			Kotlmata.Marker.it(signal)?.let { ret ->
				if (ret !is Unit/* ret is SIGNAL */) block(ret)
			}
		} ?: logLevel.normal(prefix, key, signal) { STATE_ENTRY_NONE }
	}
	
	override fun <T : SIGNAL> entry(signal: T, type: KClass<in T>, block: (signal: SIGNAL) -> Unit)
	{
		val action: (KotlmataAction2<SIGNAL, Any?>)? = entryMap?.let {
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
			Kotlmata.Marker.it(signal)?.let { ret ->
				if (ret !is Unit/* ret is SIGNAL */) block(ret)
			}
		} ?: logLevel.normal(prefix, key, signal) { STATE_ENTRY_NONE }
	}
	
	override fun input(signal: SIGNAL)
	{
		val action: (KotlmataAction2<SIGNAL, Unit>)? = inputMap?.let {
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
			Kotlmata.Marker.it(signal)
		} ?: logLevel.normal(prefix, key, signal) { STATE_INPUT_NONE }
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	{
		val action: (KotlmataAction2<SIGNAL, Unit>)? = inputMap?.let {
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
			Kotlmata.Marker.it(signal)
		} ?: logLevel.normal(prefix, key, signal) { STATE_INPUT_NONE }
	}
	
	override fun exit(signal: SIGNAL)
	{
		exit?.also {
			logLevel.normal(prefix, key, signal) { STATE_EXIT }
			Kotlmata.Marker.it(signal)
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
		private val entryMap: MutableMap<SIGNAL, KotlmataAction2<SIGNAL, Any?>>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<SIGNAL, KotlmataAction2<SIGNAL, Any?>>().also {
				this@KotlmataStateImpl.entryMap = it
			}
		
		private val inputMap: MutableMap<SIGNAL, KotlmataAction2<SIGNAL, Unit>>
			get() = this@KotlmataStateImpl.inputMap ?: HashMap<SIGNAL, KotlmataAction2<SIGNAL, Unit>>().also {
				this@KotlmataStateImpl.inputMap = it
			}
		
		@Suppress("UNCHECKED_CAST")
		override val entry = object : KotlmataState.Entry
		{
			override fun <R> action(action: KotlmataAction2<SIGNAL, R>)
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.entry = action
			}
			
			override fun <T : SIGNAL> via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
			{
				override fun <R> action(action: KotlmataAction2<T, R>)
				{
					this@ModifierImpl shouldNot expired
					entryMap[signal] = action as KotlmataAction2<SIGNAL, Any?>
				}
			}
			
			override fun <T : SIGNAL> via(signal: T) = object : KotlmataState.Entry.Action<T>
			{
				override fun <R> action(action: KotlmataAction2<T, R>)
				{
					this@ModifierImpl shouldNot expired
					entryMap[signal] = action as KotlmataAction2<SIGNAL, Any?>
				}
			}
			
			override fun via(signals: KotlmataState.Initializer.Signals) = object : KotlmataState.Entry.Action<SIGNAL>
			{
				override fun <R> action(action: KotlmataAction2<SIGNAL, R>)
				{
					this@ModifierImpl shouldNot expired
					signals.forEach {
						entryMap[it] = action
					}
				}
			}
		}
		
		@Suppress("UNCHECKED_CAST")
		override val input = object : KotlmataState.Input
		{
			override fun action(action: KotlmataAction2<SIGNAL, Unit>)
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.input = action
			}
			
			override fun <T : SIGNAL> signal(signal: KClass<T>) = object : KotlmataState.Input.Action<T>
			{
				override fun action(action: KotlmataAction2<T, Unit>)
				{
					this@ModifierImpl shouldNot expired
					inputMap[signal] = action as KotlmataAction2<SIGNAL, Unit>
				}
			}
			
			override fun <T : SIGNAL> signal(signal: T) = object : KotlmataState.Input.Action<T>
			{
				override fun action(action: KotlmataAction2<T, Unit>)
				{
					this@ModifierImpl shouldNot expired
					inputMap[signal] = action as KotlmataAction2<SIGNAL, Unit>
				}
			}
			
			override fun signal(signals: KotlmataState.Initializer.Signals) = object : KotlmataState.Input.Action<SIGNAL>
			{
				override fun action(action: KotlmataAction2<SIGNAL, Unit>)
				{
					this@ModifierImpl shouldNot expired
					signals.forEach {
						inputMap[it] = action
					}
				}
			}
		}
		
		override val exit = object : KotlmataState.Exit
		{
			override fun action(action: KotlmataAction2<SIGNAL, Unit>)
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.exit = action
			}
		}
		
		override val delete = object : KotlmataMutableState.Delete
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
		
		override fun SIGNAL.or(signal: SIGNAL): KotlmataState.Initializer.Signals = object : KotlmataState.Initializer.Signals, MutableList<SIGNAL> by mutableListOf(this, signal)
		{
			override fun or(signal: SIGNAL): KotlmataState.Initializer.Signals
			{
				add(signal)
				return this
			}
		}
		
		init
		{
			block(key)
			expire()
		}
	}
}