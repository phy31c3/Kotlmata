package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataState<T : STATE>
{
	companion object
	{
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG,
				block: Init.(state: String) -> Unit
		): KotlmataState<String> = KotlmataStateImpl(name, logLevel, block = block)
		
		@Suppress("unused")
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG,
				block: Init.(state: String) -> Unit
		) = lazy {
			invoke(name, logLevel, block)
		}
	}
	
	@KotlmataMarker
	interface Init
	{
		val entry: Entry
		val input: Input
		val exit: Exit
		val error: Error
		
		infix fun SIGNAL.or(signal: SIGNAL): Signals
		
		interface Signals : MutableList<SIGNAL>
		{
			infix fun or(signal: SIGNAL): Signals
		}
	}
	
	interface Entry
	{
		infix fun <R> action(action: KotlmataActionR<R>): Catch<SIGNAL>
		infix fun <T : SIGNAL> via(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> via(signal: T): Action<T>
		infix fun via(signals: Init.Signals): Action<SIGNAL>
		
		interface Action<T : SIGNAL>
		{
			infix fun <R> action(action: KotlmataAction1R<T, R>): Catch<T>
		}
		
		interface Catch<T : SIGNAL>
		{
			infix fun <R> catch(error: KotlmataErrorR<R>)
			infix fun <R> catch(error: KotlmataError1R<T, R>)
		}
	}
	
	interface Input
	{
		infix fun <R> action(action: KotlmataActionR<R>): Catch<SIGNAL>
		infix fun <T : SIGNAL> signal(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> signal(signal: T): Action<T>
		infix fun signal(signals: Init.Signals): Action<SIGNAL>
		
		interface Action<T : SIGNAL>
		{
			infix fun <R> action(action: KotlmataAction1R<T, R>): Catch<T>
			infix fun <R> actionWithPayload(action: KotlmataAction2R<T, R>): Catch<T>
		}
		
		interface Catch<T : SIGNAL>
		{
			infix fun <R> catch(error: KotlmataErrorR<R>)
			infix fun <R> catch(error: KotlmataError1R<T, R>)
		}
	}
	
	interface Exit
	{
		infix fun action(action: KotlmataAction): Catch
		
		interface Catch
		{
			infix fun catch(error: KotlmataError)
			infix fun catch(error: KotlmataError1<SIGNAL>)
		}
	}
	
	interface Error
	{
		infix fun action(error: KotlmataError)
		infix fun action(error: KotlmataError1<SIGNAL>)
	}
	
	val key: T
	
	fun entry(signal: SIGNAL): Any?
	fun <T : SIGNAL> entry(signal: T, type: KClass<in T>): Any?
	
	fun input(signal: SIGNAL, payload: Any? = null): Any?
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null): Any?
	
	fun exit(signal: SIGNAL)
}

interface KotlmataMutableState<T : STATE> : KotlmataState<T>
{
	companion object
	{
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG,
				block: StateTemplate<String>? = null
		): KotlmataMutableState<String> = KotlmataStateImpl(name, logLevel, block = block)
		
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG,
				block: StateTemplate<String>? = null
		) = lazy {
			invoke(name, logLevel, block)
		}
		
		internal fun <T : STATE> create(
				key: T,
				logLevel: Int,
				prefix: String,
				block: StateTemplate<T>
		): KotlmataMutableState<T> = KotlmataStateImpl(key, logLevel, prefix, block)
	}
	
	@KotlmataMarker
	interface Modifier : KotlmataState.Init
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

private class EntryDef(val action: KotlmataActionR<Any?>, val error: KotlmataError1R<SIGNAL, Any?>? = null)
private class InputDef(val action: KotlmataAction2R<SIGNAL, Any?>, val error: KotlmataError1R<SIGNAL, Any?>? = null)
private class ExitDef(val action: KotlmataAction, val error: KotlmataError1<SIGNAL>? = null)

private class KotlmataStateImpl<T : STATE>(
		override val key: T,
		val logLevel: Int = NO_LOG,
		val prefix: String = "State[$key]:",
		block: (KotlmataState.Init.(T) -> Unit)? = null
) : KotlmataMutableState<T>
{
	private var entry: EntryDef? = null
	private var input: InputDef? = null
	private var exit: ExitDef? = null
	private var entryMap: MutableMap<SIGNAL, EntryDef>? = null
	private var inputMap: MutableMap<SIGNAL, InputDef>? = null
	private var error: KotlmataError1<SIGNAL>? = null
	
	init
	{
		block?.let {
			ModifierImpl(it)
		}
		if (key !== CONSTRUCTED)
		{
			logLevel.normal(prefix, key) { STATE_CREATED }
		}
	}
	
	private fun EntryDef.run(signal: SIGNAL): Any? = try
	{
		DSL.action(signal)
	}
	catch (e: Throwable)
	{
		error?.let {
			DSL.it(e, signal) ?: Unit
		} ?: this@KotlmataStateImpl.error?.let {
			DSL.it(e, signal)
		} ?: throw e
	}
	
	private fun InputDef.run(signal: SIGNAL, payload: Any?): Any? = try
	{
		DSL.action(signal, payload)
	}
	catch (e: Throwable)
	{
		error?.let {
			DSL.it(e, signal) ?: Unit
		} ?: this@KotlmataStateImpl.error?.let {
			DSL.it(e, signal)
		} ?: throw e
	}
	
	private fun ExitDef.run(signal: SIGNAL) = try
	{
		DSL.action(signal)
	}
	catch (e: Throwable)
	{
		error?.let {
			DSL.it(e, signal)
		} ?: this@KotlmataStateImpl.error?.let {
			DSL.it(e, signal)
		} ?: throw e
	}
	
	override fun entry(signal: SIGNAL): Any?
	{
		val entryDef = entryMap?.let {
			when
			{
				signal in it ->
				{
					logLevel.normal(prefix, key, signal) { STATE_RUN_ENTRY_OBJECT }
					it[signal]
				}
				signal::class in it ->
				{
					logLevel.normal(prefix, key, "${signal::class.simpleName}::class", signal) { STATE_RUN_ENTRY_CLASS }
					it[signal::class]
				}
				else -> null
			}
		} ?: entry?.also {
			logLevel.normal(prefix, key, signal) { STATE_RUN_ENTRY_DEFAULT }
		} ?: null.also {
			logLevel.normal(prefix, key, signal) { STATE_NO_ENTRY }
		}
		
		return entryDef?.run(signal)
	}
	
	override fun <T : SIGNAL> entry(signal: T, type: KClass<in T>): Any?
	{
		val entryDef = entryMap?.let {
			when (type)
			{
				in it ->
				{
					logLevel.normal(prefix, key, "${type.simpleName}::class", signal, "${type.simpleName}::class") { STATE_RUN_ENTRY_CLASS_TYPED }
					it[type]
				}
				else -> null
			}
		} ?: entry?.also {
			logLevel.normal(prefix, key, signal, "${type.simpleName}::class") { STATE_RUN_ENTRY_DEFAULT_TYPED }
		} ?: null.also {
			logLevel.normal(prefix, key, signal, "${type.simpleName}::class") { STATE_NO_ENTRY_TYPED }
		}
		
		return entryDef?.run(signal)
	}
	
	override fun input(signal: SIGNAL, payload: Any?): Any?
	{
		val inputDef = inputMap?.let {
			when
			{
				signal in it ->
				{
					logLevel.normal(prefix, key, signal) { STATE_RUN_INPUT_OBJECT }
					it[signal]
				}
				signal::class in it ->
				{
					logLevel.normal(prefix, key, "${signal::class.simpleName}::class", signal) { STATE_RUN_INPUT_CLASS }
					it[signal::class]
				}
				else -> null
			}
		} ?: input?.also {
			logLevel.normal(prefix, key, signal) { STATE_RUN_INPUT_DEFAULT }
		} ?: null.also {
			if (key !== CONSTRUCTED) logLevel.normal(prefix, key, signal) { STATE_NO_INPUT }
		}
		
		return inputDef?.run(signal, payload)
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?): Any?
	{
		val inputDef = inputMap?.let {
			when (type)
			{
				in it ->
				{
					logLevel.normal(prefix, key, "${type.simpleName}::class", signal, "${type.simpleName}::class") { STATE_RUN_INPUT_CLASS_TYPED }
					it[type]
				}
				else -> null
			}
		} ?: input?.also {
			logLevel.normal(prefix, key, signal, "${type.simpleName}::class") { STATE_RUN_INPUT_DEFAULT_TYPED }
		} ?: null.also {
			if (key !== CONSTRUCTED) logLevel.normal(prefix, key, signal, "${type.simpleName}::class") { STATE_NO_INPUT_TYPED }
		}
		
		return inputDef?.run(signal, payload)
	}
	
	override fun exit(signal: SIGNAL)
	{
		exit?.apply {
			logLevel.normal(prefix, key, signal) { STATE_RUN_EXIT }
			run(signal)
		} ?: if (key !== CONSTRUCTED) logLevel.normal(prefix, key, signal) { STATE_NO_EXIT }
	}
	
	override fun modify(block: KotlmataMutableState.Modifier.(T) -> Unit)
	{
		ModifierImpl(block)
		logLevel.normal(prefix, key) { STATE_UPDATED }
	}
	
	override fun toString(): String
	{
		return "KotlmataState[$key]{${hashCode().toString(16)}}"
	}
	
	private inner class ModifierImpl internal constructor(
			block: KotlmataMutableState.Modifier.(T) -> Unit
	) : KotlmataMutableState.Modifier, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		private val entryMap: MutableMap<SIGNAL, EntryDef>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<SIGNAL, EntryDef>().also {
				this@KotlmataStateImpl.entryMap = it
			}
		
		private val inputMap: MutableMap<SIGNAL, InputDef>
			get() = this@KotlmataStateImpl.inputMap ?: HashMap<SIGNAL, InputDef>().also {
				this@KotlmataStateImpl.inputMap = it
			}
		
		/*###################################################################################################################################
		 * Entry
		 *###################################################################################################################################*/
		@Suppress("UNCHECKED_CAST")
		override val entry = object : KotlmataState.Entry
		{
			override fun <R> action(action: KotlmataActionR<R>): KotlmataState.Entry.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.entry = EntryDef(action)
				return object : KotlmataState.Entry.Catch<SIGNAL>
				{
					override fun <R> catch(error: KotlmataErrorR<R>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.entry = EntryDef(action, { throwable, _ -> error(throwable) })
					}
					
					override fun <R> catch(error: KotlmataError1R<SIGNAL, R>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.entry = EntryDef(action, error)
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
			{
				override fun <R> action(action: KotlmataAction1R<T, R>): KotlmataState.Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					entryMap[signal] = EntryDef(action as KotlmataActionR<Any?>)
					return object : KotlmataState.Entry.Catch<T>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(action, { throwable, _ -> error(throwable) })
						}
						
						override fun <R> catch(error: KotlmataError1R<T, R>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(action, error as KotlmataError1R<SIGNAL, Any?>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: T) = object : KotlmataState.Entry.Action<T>
			{
				override fun <R> action(action: KotlmataAction1R<T, R>): KotlmataState.Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					entryMap[signal] = EntryDef(action as KotlmataActionR<Any?>)
					return object : KotlmataState.Entry.Catch<T>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(action, { throwable, _ -> error(throwable) })
						}
						
						override fun <R> catch(error: KotlmataError1R<T, R>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(action, error as KotlmataError1R<SIGNAL, Any?>)
						}
					}
				}
			}
			
			override fun via(signals: KotlmataState.Init.Signals) = object : KotlmataState.Entry.Action<SIGNAL>
			{
				override fun <R> action(action: KotlmataActionR<R>): KotlmataState.Entry.Catch<SIGNAL>
				{
					this@ModifierImpl shouldNot expired
					signals.forEach {
						entryMap[it] = EntryDef(action)
					}
					return object : KotlmataState.Entry.Catch<SIGNAL>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								entryMap[it] = EntryDef(action, { throwable, _ -> error(throwable) })
							}
						}
						
						override fun <R> catch(error: KotlmataError1R<SIGNAL, R>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								entryMap[it] = EntryDef(action, error)
							}
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Input
		 *###################################################################################################################################*/
		@Suppress("UNCHECKED_CAST")
		override val input = object : KotlmataState.Input
		{
			override fun <R> action(action: KotlmataActionR<R>): KotlmataState.Input.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.input = InputDef({ signal, _ -> action(signal) })
				return object : KotlmataState.Input.Catch<SIGNAL>
				{
					override fun <R> catch(error: KotlmataErrorR<R>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.input = InputDef({ signal, _ -> action(signal) }, { throwable, _ -> error(throwable) })
					}
					
					override fun <R> catch(error: KotlmataError1R<SIGNAL, R>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.input = InputDef({ signal, _ -> action(signal) }, error)
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signal: KClass<T>) = object : KotlmataState.Input.Action<T>
			{
				override fun <R> action(action: KotlmataAction1R<T, R>): KotlmataState.Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					val action2: KotlmataAction2R<SIGNAL, Any?> = (action as KotlmataActionR<R>).let {
						{ signal, _ -> it(signal) }
					}
					inputMap[signal] = InputDef(action2)
					return object : KotlmataState.Input.Catch<T>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action2, { throwable, _ -> error(throwable) })
						}
						
						override fun <R> catch(error: KotlmataError1R<T, R>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action2, error as KotlmataError1<SIGNAL>)
						}
					}
				}
				
				override fun <R> actionWithPayload(action: KotlmataAction2R<T, R>): KotlmataState.Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					inputMap[signal] = InputDef(action as KotlmataAction2R<SIGNAL, Any?>)
					return object : KotlmataState.Input.Catch<T>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action, { throwable, _ -> error(throwable) })
						}
						
						override fun <R> catch(error: KotlmataError1R<T, R>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action, error as KotlmataError1<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signal: T) = object : KotlmataState.Input.Action<T>
			{
				override fun <R> action(action: KotlmataAction1R<T, R>): KotlmataState.Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					val action2: KotlmataAction2R<SIGNAL, Any?> = (action as KotlmataActionR<R>).let {
						{ signal, _ -> it(signal) }
					}
					inputMap[signal] = InputDef(action2)
					return object : KotlmataState.Input.Catch<T>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action2, { throwable, _ -> error(throwable) })
						}
						
						override fun <R> catch(error: KotlmataError1R<T, R>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action2, error as KotlmataError1<SIGNAL>)
						}
					}
				}
				
				override fun <R> actionWithPayload(action: KotlmataAction2R<T, R>): KotlmataState.Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					inputMap[signal] = InputDef(action as KotlmataAction2R<SIGNAL, Any?>)
					return object : KotlmataState.Input.Catch<T>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action, { throwable, _ -> error(throwable) })
						}
						
						override fun <R> catch(error: KotlmataError1R<T, R>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action, error as KotlmataError1<SIGNAL>)
						}
					}
				}
			}
			
			override fun signal(signals: KotlmataState.Init.Signals) = object : KotlmataState.Input.Action<SIGNAL>
			{
				override fun <R> action(action: KotlmataActionR<R>): KotlmataState.Input.Catch<SIGNAL>
				{
					this@ModifierImpl shouldNot expired
					signals.forEach {
						inputMap[it] = InputDef({ signal, _ -> action(signal) })
					}
					return object : KotlmataState.Input.Catch<SIGNAL>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								inputMap[it] = InputDef({ signal, _ -> action(signal) }, { throwable, _ -> error(throwable) })
							}
						}
						
						override fun <R> catch(error: KotlmataError1R<SIGNAL, R>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								inputMap[it] = InputDef({ signal, _ -> action(signal) }, error)
							}
						}
					}
				}
				
				override fun <R> actionWithPayload(action: KotlmataAction2R<SIGNAL, R>): KotlmataState.Input.Catch<SIGNAL>
				{
					this@ModifierImpl shouldNot expired
					signals.forEach {
						inputMap[it] = InputDef(action)
					}
					return object : KotlmataState.Input.Catch<SIGNAL>
					{
						override fun <R> catch(error: KotlmataErrorR<R>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								inputMap[it] = InputDef(action, { throwable, _ -> error(throwable) })
							}
						}
						
						override fun <R> catch(error: KotlmataError1R<SIGNAL, R>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								inputMap[it] = InputDef(action, error)
							}
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Exit
		 *###################################################################################################################################*/
		override val exit = object : KotlmataState.Exit
		{
			override fun action(action: KotlmataAction): KotlmataState.Exit.Catch
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.exit = ExitDef(action)
				return object : KotlmataState.Exit.Catch
				{
					override fun catch(error: KotlmataError)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.exit = ExitDef(action, { throwable, _ -> error(throwable) })
					}
					
					override fun catch(error: KotlmataError1<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.exit = ExitDef(action, error)
					}
				}
			}
		}
		
		override val error = object : KotlmataState.Error
		{
			override fun action(error: KotlmataError)
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.error = { throwable, _ -> error(throwable) }
			}
			
			override fun action(error: KotlmataError1<SIGNAL>)
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.error = error
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
		
		override fun SIGNAL.or(signal: SIGNAL): KotlmataState.Init.Signals = object : KotlmataState.Init.Signals, MutableList<SIGNAL> by mutableListOf(this, signal)
		{
			override fun or(signal: SIGNAL): KotlmataState.Init.Signals
			{
				this@ModifierImpl shouldNot expired
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