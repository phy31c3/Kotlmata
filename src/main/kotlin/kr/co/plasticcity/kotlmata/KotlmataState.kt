package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataMutableState.*
import kr.co.plasticcity.kotlmata.KotlmataState.*
import kr.co.plasticcity.kotlmata.Log.normal
import kotlin.reflect.KClass

interface KotlmataState<T : STATE>
{
	@KotlmataMarker
	interface Init : StatesOrSignalsDefinable
	{
		val entry: Entry
		val input: Input
		val exit: Exit
		val on: On
		
		interface On
		{
			infix fun error(block: StateErrorCallback)
		}
	}
	
	interface Entry
	{
		infix fun action(action: EntryAction<SIGNAL>): Catch<SIGNAL> = function(action)
		infix fun function(function: EntryFunction<SIGNAL>): Catch<SIGNAL>
		infix fun <T : SIGNAL> via(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> via(signal: T): Action<T>
		infix fun <T : SIGNAL> via(signals: StatesOrSignals<T>): Action<T>
		infix fun <T : SIGNAL> via(predicate: (T) -> Boolean): Action<T>
		infix fun <T> via(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = via { t: T -> range.contains(t) }
		
		interface Action<T : SIGNAL>
		{
			infix fun action(action: EntryAction<T>): Catch<T> = function(action)
			infix fun function(function: EntryFunction<T>): Catch<T>
		}
		
		interface Catch<T : SIGNAL> : Finally<T>
		{
			infix fun catch(catch: EntryErrorAction<T>) = intercept(catch)
			infix fun intercept(intercept: EntryErrorFunction<T>): Finally<T>
		}
		
		interface Finally<T : SIGNAL>
		{
			infix fun finally(finally: EntryAction<T>)
		}
	}
	
	interface Input
	{
		infix fun action(action: InputAction<SIGNAL>): Catch<SIGNAL> = function(action)
		infix fun function(function: InputFunction<SIGNAL>): Catch<SIGNAL>
		infix fun <T : SIGNAL> signal(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> signal(signal: T): Action<T>
		infix fun <T : SIGNAL> signal(signals: StatesOrSignals<T>): Action<T>
		infix fun <T : SIGNAL> signal(predicate: (T) -> Boolean): Action<T>
		infix fun <T> signal(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = signal { t: T -> range.contains(t) }
		
		interface Action<T : SIGNAL>
		{
			infix fun action(action: InputAction<T>): Catch<T> = function(action)
			infix fun function(function: InputFunction<T>): Catch<T>
		}
		
		interface Catch<T : SIGNAL> : Finally<T>
		{
			infix fun catch(catch: InputErrorAction<T>) = intercept(catch)
			infix fun intercept(intercept: InputErrorFunction<T>): Finally<T>
		}
		
		interface Finally<T : SIGNAL>
		{
			infix fun finally(finally: InputAction<T>)
		}
	}
	
	interface Exit
	{
		infix fun action(action: ExitAction<SIGNAL>): Catch<SIGNAL>
		infix fun <T : SIGNAL> via(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> via(signal: T): Action<T>
		infix fun <T : SIGNAL> via(signals: StatesOrSignals<T>): Action<T>
		infix fun <T : SIGNAL> via(predicate: (T) -> Boolean): Action<T>
		infix fun <T> via(range: ClosedRange<T>) where T : SIGNAL, T : Comparable<T> = via { t: T -> range.contains(t) }
		
		interface Action<T : SIGNAL>
		{
			infix fun action(action: ExitAction<T>): Catch<T>
		}
		
		interface Catch<T : SIGNAL> : Finally<T>
		{
			infix fun catch(catch: ExitErrorAction<T>): Finally<T>
		}
		
		interface Finally<T : SIGNAL>
		{
			infix fun finally(finally: ExitAction<T>)
		}
	}
	
	val tag: T
	
	fun entry(from: STATE, signal: SIGNAL): Any?
	fun <T : SIGNAL> entry(from: STATE, signal: T, type: KClass<in T>): Any?
	
	fun input(signal: SIGNAL, payload: Any? = null): Any?
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null): Any?
	
	fun exit(signal: SIGNAL, to: STATE)
	fun <T : SIGNAL> exit(signal: T, type: KClass<in T>, to: STATE)
}

interface KotlmataMutableState<T : STATE> : KotlmataState<T>
{
	companion object
	{
		internal operator fun <T : STATE> invoke(
			tag: T,
			logLevel: Int,
			prefix: String,
			block: StateTemplate<T>
		): KotlmataMutableState<T> = KotlmataStateImpl(tag, logLevel, prefix, block)
	}
	
	@KotlmataMarker
	interface Modifier : Init
	{
		val delete: Delete
	}
	
	interface Delete
	{
		infix fun action(keyword: Entry): Via
		infix fun action(keyword: Input): Signal
		infix fun action(keyword: Exit): Via
		infix fun action(keyword: all)
		
		interface Via
		{
			infix fun <T : SIGNAL> via(signal: KClass<T>)
			infix fun <T : SIGNAL> via(signal: T)
			infix fun <T : SIGNAL> via(predicate: (T) -> Boolean)
			infix fun via(keyword: all)
		}
		
		interface Signal
		{
			infix fun <T : SIGNAL> signal(signal: KClass<T>)
			infix fun <T : SIGNAL> signal(signal: T)
			infix fun <T : SIGNAL> signal(predicate: (T) -> Boolean)
			infix fun signal(keyword: all)
		}
	}
	
	operator fun invoke(block: Modifier.(state: T) -> Unit) = modify(block)
	infix fun modify(block: Modifier.(state: T) -> Unit)
}

private class EntryDef(val function: EntryFunction<SIGNAL>, val intercept: EntryErrorFunction<SIGNAL>? = null, val finally: EntryAction<SIGNAL>? = null)
private class InputDef(val function: InputFunction<SIGNAL>, val intercept: InputErrorFunction<SIGNAL>? = null, val finally: InputAction<SIGNAL>? = null)
private class ExitDef(val action: ExitAction<SIGNAL>, val catch: ExitErrorAction<SIGNAL>? = null, val finally: ExitAction<SIGNAL>? = null)

private class KotlmataStateImpl<T : STATE>(
	override val tag: T,
	val logLevel: Int = NO_LOG,
	val prefix: String = "State[$tag]:",
	block: (StateTemplate<T>)
) : KotlmataMutableState<T>
{
	private var entry: EntryDef? = null
	private var input: InputDef? = null
	private var exit: ExitDef? = null
	private var entryMap: MutableMap<SIGNAL, EntryDef>? = null
	private var inputMap: MutableMap<SIGNAL, InputDef>? = null
	private var exitMap: MutableMap<SIGNAL, ExitDef>? = null
	private var onError: StateErrorCallback? = null
	
	private var entryPredicates: Predicates? = null
	private var inputPredicates: Predicates? = null
	private var exitPredicates: Predicates? = null
	
	init
	{
		ModifierImpl(block)
		if (tag !== `Initial state for KotlmataDaemon`)
		{
			logLevel.normal(prefix, tag) { STATE_CREATED }
		}
	}
	
	private fun EntryDef.run(from: STATE, signal: SIGNAL): Any? = try
	{
		EntryFunctionReceiver(from).function(signal)
	}
	catch (e: Throwable)
	{
		intercept?.let { intercept ->
			EntryErrorFunctionReceiver(from, e).intercept(signal) ?: Unit
		} ?: onError?.let { onError ->
			ErrorActionReceiver(e).onError(signal)
		} ?: throw e
	}
	finally
	{
		finally?.let { finally ->
			EntryActionReceiver(from).finally(signal)
		}
	}
	
	private fun InputDef.run(signal: SIGNAL, payload: Any?): Any? = try
	{
		PayloadFunctionReceiver(payload).function(signal)
	}
	catch (e: Throwable)
	{
		intercept?.let { intercept ->
			PayloadErrorFunctionReceiver(payload, e).intercept(signal) ?: Unit
		} ?: onError?.let { onError ->
			ErrorActionReceiver(e).onError(signal)
		} ?: throw e
	}
	finally
	{
		finally?.let { finally ->
			PayloadActionReceiver(payload).finally(signal)
		}
	}
	
	private fun ExitDef.run(signal: SIGNAL, to: STATE) = try
	{
		ExitActionReceiver(to).action(signal)
	}
	catch (e: Throwable)
	{
		catch?.let { catch ->
			ExitErrorActionReceiver(to, e).catch(signal)
		} ?: onError?.let { onError ->
			ErrorActionReceiver(e).onError(signal)
		} ?: throw e
	}
	finally
	{
		finally?.let { finally ->
			ExitActionReceiver(to).finally(signal)
		}
	}
	
	override fun entry(from: STATE, signal: SIGNAL): Any?
	{
		val entryDef = entryMap?.let {
			when
			{
				signal in it ->
				{
					logLevel.normal(prefix, tag, signal) { STATE_RUN_ENTRY_OBJECT }
					it[signal]
				}
				signal::class in it ->
				{
					logLevel.normal(prefix, tag, "${signal::class.simpleName}::class", signal) { STATE_RUN_ENTRY_CLASS }
					it[signal::class]
				}
				else -> entryPredicates?.test(signal)?.let { predicate ->
					logLevel.normal(prefix, tag, signal) { STATE_RUN_ENTRY_PREDICATE }
					it[predicate]
				}
			}
		} ?: entry?.also {
			logLevel.normal(prefix, tag, signal) { STATE_RUN_ENTRY_DEFAULT }
		} ?: null.also {
			logLevel.normal(prefix, tag, signal) { STATE_NO_ENTRY }
		}
		
		return entryDef?.run(from, signal)
	}
	
	override fun <T : SIGNAL> entry(from: STATE, signal: T, type: KClass<in T>): Any?
	{
		val entryDef = entryMap?.let {
			when (type)
			{
				in it ->
				{
					logLevel.normal(prefix, tag, "${type.simpleName}::class", signal, "${type.simpleName}::class") { STATE_RUN_ENTRY_CLASS_TYPED }
					it[type]
				}
				else -> null
			}
		} ?: entry?.also {
			logLevel.normal(prefix, tag, signal, "${type.simpleName}::class") { STATE_RUN_ENTRY_DEFAULT_TYPED }
		} ?: null.also {
			logLevel.normal(prefix, tag, signal, "${type.simpleName}::class") { STATE_NO_ENTRY_TYPED }
		}
		
		return entryDef?.run(from, signal)
	}
	
	override fun input(signal: SIGNAL, payload: Any?): Any?
	{
		val inputDef = inputMap?.let {
			when
			{
				signal in it ->
				{
					logLevel.normal(prefix, tag, signal) { STATE_RUN_INPUT_OBJECT }
					it[signal]
				}
				signal::class in it ->
				{
					logLevel.normal(prefix, tag, "${signal::class.simpleName}::class", signal) { STATE_RUN_INPUT_CLASS }
					it[signal::class]
				}
				else -> inputPredicates?.test(signal)?.let { predicate ->
					logLevel.normal(prefix, tag, signal) { STATE_RUN_INPUT_PREDICATE }
					it[predicate]
				}
			}
		} ?: input?.also {
			logLevel.normal(prefix, tag, signal) { STATE_RUN_INPUT_DEFAULT }
		} ?: null.also {
			if (tag !== `Initial state for KotlmataDaemon`) logLevel.normal(prefix, tag, signal) { STATE_NO_INPUT }
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
					logLevel.normal(prefix, tag, "${type.simpleName}::class", signal, "${type.simpleName}::class") { STATE_RUN_INPUT_CLASS_TYPED }
					it[type]
				}
				else -> null
			}
		} ?: input?.also {
			logLevel.normal(prefix, tag, signal, "${type.simpleName}::class") { STATE_RUN_INPUT_DEFAULT_TYPED }
		} ?: null.also {
			if (tag !== `Initial state for KotlmataDaemon`) logLevel.normal(prefix, tag, signal, "${type.simpleName}::class") { STATE_NO_INPUT_TYPED }
		}
		
		return inputDef?.run(signal, payload)
	}
	
	override fun exit(signal: SIGNAL, to: STATE)
	{
		val exitDef = exitMap?.let {
			when
			{
				signal in it ->
				{
					logLevel.normal(prefix, tag, signal) { STATE_RUN_EXIT_OBJECT }
					it[signal]
				}
				signal::class in it ->
				{
					logLevel.normal(prefix, tag, "${signal::class.simpleName}::class", signal) { STATE_RUN_EXIT_CLASS }
					it[signal::class]
				}
				else -> exitPredicates?.test(signal)?.let { predicate ->
					logLevel.normal(prefix, tag, signal) { STATE_RUN_EXIT_PREDICATE }
					it[predicate]
				}
			}
		} ?: exit?.also {
			logLevel.normal(prefix, tag, signal) { STATE_RUN_EXIT_DEFAULT }
		} ?: null.also {
			if (tag !== `Initial state for KotlmataDaemon`) logLevel.normal(prefix, tag, signal) { STATE_NO_EXIT }
		}
		
		exitDef?.run(signal, to)
	}
	
	override fun <T : SIGNAL> exit(signal: T, type: KClass<in T>, to: STATE)
	{
		val exitDef = exitMap?.let {
			when (type)
			{
				in it ->
				{
					logLevel.normal(prefix, tag, "${type.simpleName}::class", signal, "${type.simpleName}::class") { STATE_RUN_EXIT_CLASS_TYPED }
					it[type]
				}
				else -> null
			}
		} ?: exit?.also {
			logLevel.normal(prefix, tag, signal, "${type.simpleName}::class") { STATE_RUN_EXIT_DEFAULT_TYPED }
		} ?: null.also {
			if (tag !== `Initial state for KotlmataDaemon`) logLevel.normal(prefix, tag, signal, "${type.simpleName}::class") { STATE_NO_EXIT_TYPED }
		}
		
		exitDef?.run(signal, to)
	}
	
	override fun modify(block: Modifier.(T) -> Unit)
	{
		ModifierImpl(block)
		logLevel.normal(prefix, tag) { STATE_UPDATED }
	}
	
	override fun toString(): String
	{
		return "KotlmataState[$tag]{${hashCode().toString(16)}}"
	}
	
	private inner class ModifierImpl(
		block: Modifier.(T) -> Unit
	) : Modifier, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		private val entryMap: MutableMap<SIGNAL, EntryDef>
			get() = this@KotlmataStateImpl.entryMap ?: HashMap<SIGNAL, EntryDef>().also {
				this@KotlmataStateImpl.entryMap = it
			}
		
		private val inputMap: MutableMap<SIGNAL, InputDef>
			get() = this@KotlmataStateImpl.inputMap ?: HashMap<SIGNAL, InputDef>().also {
				this@KotlmataStateImpl.inputMap = it
			}
		
		private val exitMap: MutableMap<SIGNAL, ExitDef>
			get() = this@KotlmataStateImpl.exitMap ?: HashMap<SIGNAL, ExitDef>().also {
				this@KotlmataStateImpl.exitMap = it
			}
		
		private val entryPredicates: Predicates
			get() = this@KotlmataStateImpl.entryPredicates ?: Predicates().also {
				this@KotlmataStateImpl.entryPredicates = it
			}
		
		private val inputPredicates: Predicates
			get() = this@KotlmataStateImpl.inputPredicates ?: Predicates().also {
				this@KotlmataStateImpl.inputPredicates = it
			}
		
		private val exitPredicates: Predicates
			get() = this@KotlmataStateImpl.exitPredicates ?: Predicates().also {
				this@KotlmataStateImpl.exitPredicates = it
			}
		
		/*###################################################################################################################################
		 * Entry
		 *###################################################################################################################################*/
		@Suppress("UNCHECKED_CAST")
		override val entry = object : Entry
		{
			override fun function(function: EntryFunction<SIGNAL>): Entry.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.entry = EntryDef(function)
				return object : Entry.Catch<SIGNAL>
				{
					override fun intercept(intercept: EntryErrorFunction<SIGNAL>): Entry.Finally<SIGNAL>
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.entry = EntryDef(function, intercept)
						return object : Entry.Finally<SIGNAL>
						{
							override fun finally(finally: EntryAction<SIGNAL>)
							{
								this@ModifierImpl shouldNot expired
								this@KotlmataStateImpl.entry = EntryDef(function, intercept, finally)
							}
						}
					}
					
					override fun finally(finally: EntryAction<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.entry = EntryDef(function, null, finally)
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: KClass<T>) = object : Entry.Action<T>
			{
				override fun function(function: EntryFunction<T>): Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					entryMap[signal] = EntryDef(function as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(intercept: EntryErrorFunction<T>): Entry.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(function, intercept as EntryErrorFunction<SIGNAL>)
							return object : Entry.Finally<T>
							{
								override fun finally(finally: EntryAction<T>)
								{
									this@ModifierImpl shouldNot expired
									entryMap[signal] = EntryDef(function, intercept, finally as EntryAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: EntryAction<T>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(function, null, finally as EntryAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: T) = object : Entry.Action<T>
			{
				override fun function(function: EntryFunction<T>): Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					entryMap[signal] = EntryDef(function as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(intercept: EntryErrorFunction<T>): Entry.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(function, intercept as EntryErrorFunction<SIGNAL>)
							return object : Entry.Finally<T>
							{
								override fun finally(finally: EntryAction<T>)
								{
									this@ModifierImpl shouldNot expired
									entryMap[signal] = EntryDef(function, intercept, finally as EntryAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: EntryAction<T>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(function, null, finally as EntryAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signals: StatesOrSignals<T>) = object : Entry.Action<T>
			{
				override fun function(function: EntryFunction<T>): Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					function as EntryFunction<SIGNAL>
					signals.forEach {
						entryMap[it] = EntryDef(function)
					}
					return object : Entry.Catch<T>
					{
						override fun intercept(intercept: EntryErrorFunction<T>): Entry.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							intercept as EntryErrorFunction<SIGNAL>
							signals.forEach {
								entryMap[it] = EntryDef(function, intercept)
							}
							return object : Entry.Finally<T>
							{
								override fun finally(finally: EntryAction<T>)
								{
									signals.forEach {
										entryMap[it] = EntryDef(function, intercept, finally as EntryAction<SIGNAL>)
									}
								}
							}
						}
						
						override fun finally(finally: EntryAction<T>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								entryMap[it] = EntryDef(function, null, finally as EntryAction<SIGNAL>)
							}
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(predicate: (T) -> Boolean) = object : Entry.Action<T>
			{
				override fun function(function: EntryFunction<T>): Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					entryPredicates.store(predicate)
					entryMap[predicate] = EntryDef(function as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(intercept: EntryErrorFunction<T>): Entry.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							entryMap[predicate] = EntryDef(function, intercept as EntryErrorFunction<SIGNAL>)
							return object : Entry.Finally<T>
							{
								override fun finally(finally: EntryAction<T>)
								{
									entryMap[predicate] = EntryDef(function, intercept, finally as EntryAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: EntryAction<T>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[predicate] = EntryDef(function, null, finally as EntryAction<SIGNAL>)
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Input
		 *###################################################################################################################################*/
		@Suppress("UNCHECKED_CAST")
		override val input = object : Input
		{
			override fun function(function: InputFunction<SIGNAL>): Input.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.input = InputDef(function)
				return object : Input.Catch<SIGNAL>
				{
					override fun intercept(intercept: InputErrorFunction<SIGNAL>): Input.Finally<SIGNAL>
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.input = InputDef(function, intercept)
						return object : Input.Finally<SIGNAL>
						{
							override fun finally(finally: InputAction<SIGNAL>)
							{
								this@ModifierImpl shouldNot expired
								this@KotlmataStateImpl.input = InputDef(function, intercept, finally)
							}
						}
					}
					
					override fun finally(finally: InputAction<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.input = InputDef(function, null, finally)
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signal: KClass<T>) = object : Input.Action<T>
			{
				override fun function(function: InputFunction<T>): Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					inputMap[signal] = InputDef(function as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(intercept: InputErrorFunction<T>): Input.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(function, intercept as InputErrorFunction<SIGNAL>)
							return object : Input.Finally<T>
							{
								override fun finally(finally: InputAction<T>)
								{
									this@ModifierImpl shouldNot expired
									inputMap[signal] = InputDef(function, intercept, finally as InputAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: InputAction<T>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(function, null, finally as InputAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signal: T) = object : Input.Action<T>
			{
				override fun function(function: InputFunction<T>): Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					inputMap[signal] = InputDef(function as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(intercept: InputErrorFunction<T>): Input.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(function, intercept as InputErrorFunction<SIGNAL>)
							return object : Input.Finally<T>
							{
								override fun finally(finally: InputAction<T>)
								{
									this@ModifierImpl shouldNot expired
									inputMap[signal] = InputDef(function, intercept, finally as InputAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: InputAction<T>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(function, null, finally as InputAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signals: StatesOrSignals<T>) = object : Input.Action<T>
			{
				override fun function(function: InputFunction<T>): Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					function as InputFunction<SIGNAL>
					signals.forEach {
						inputMap[it] = InputDef(function)
					}
					return object : Input.Catch<T>
					{
						override fun intercept(intercept: InputErrorFunction<T>): Input.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							intercept as InputErrorFunction<SIGNAL>
							signals.forEach {
								inputMap[it] = InputDef(function, intercept)
							}
							return object : Input.Finally<T>
							{
								override fun finally(finally: InputAction<T>)
								{
									this@ModifierImpl shouldNot expired
									signals.forEach {
										inputMap[it] = InputDef(function, intercept, finally as InputAction<SIGNAL>)
									}
								}
							}
						}
						
						override fun finally(finally: InputAction<T>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								inputMap[it] = InputDef(function, null, finally as InputAction<SIGNAL>)
							}
						}
					}
				}
			}
			
			override fun <T : SIGNAL> signal(predicate: (T) -> Boolean) = object : Input.Action<T>
			{
				override fun function(function: InputFunction<T>): Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					inputPredicates.store(predicate)
					inputMap[predicate] = InputDef(function as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(intercept: InputErrorFunction<T>): Input.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							inputMap[predicate] = InputDef(function, intercept as InputErrorFunction<SIGNAL>)
							return object : Input.Finally<T>
							{
								override fun finally(finally: InputAction<T>)
								{
									this@ModifierImpl shouldNot expired
									inputMap[predicate] = InputDef(function, intercept, finally as InputAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: InputAction<T>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[predicate] = InputDef(function, null, finally as InputAction<SIGNAL>)
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Exit
		 *###################################################################################################################################*/
		@Suppress("UNCHECKED_CAST")
		override val exit = object : Exit
		{
			override fun action(action: ExitAction<SIGNAL>): Exit.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.exit = ExitDef(action)
				return object : Exit.Catch<SIGNAL>
				{
					override fun catch(catch: ExitErrorAction<SIGNAL>): Exit.Finally<SIGNAL>
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.exit = ExitDef(action, catch)
						return object : Exit.Finally<SIGNAL>
						{
							override fun finally(finally: ExitAction<SIGNAL>)
							{
								this@ModifierImpl shouldNot expired
								this@KotlmataStateImpl.exit = ExitDef(action, catch, finally)
							}
						}
					}
					
					override fun finally(finally: ExitAction<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.exit = ExitDef(action, null, finally)
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: KClass<T>) = object : Exit.Action<T>
			{
				override fun action(action: ExitAction<T>): Exit.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					exitMap[signal] = ExitDef(action as ExitAction<SIGNAL>)
					return object : Exit.Catch<T>
					{
						override fun catch(catch: ExitErrorAction<T>): Exit.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							exitMap[signal] = ExitDef(action, catch as ExitErrorAction<SIGNAL>)
							return object : Exit.Finally<T>
							{
								override fun finally(finally: ExitAction<T>)
								{
									this@ModifierImpl shouldNot expired
									exitMap[signal] = ExitDef(action, catch, finally as ExitAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: ExitAction<T>)
						{
							this@ModifierImpl shouldNot expired
							exitMap[signal] = ExitDef(action, null, finally as ExitAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: T) = object : Exit.Action<T>
			{
				override fun action(action: ExitAction<T>): Exit.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					exitMap[signal] = ExitDef(action as ExitAction<SIGNAL>)
					return object : Exit.Catch<T>
					{
						override fun catch(catch: ExitErrorAction<T>): Exit.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							exitMap[signal] = ExitDef(action, catch as ExitErrorAction<SIGNAL>)
							return object : Exit.Finally<T>
							{
								override fun finally(finally: ExitAction<T>)
								{
									this@ModifierImpl shouldNot expired
									exitMap[signal] = ExitDef(action, catch, finally as ExitAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: ExitAction<T>)
						{
							this@ModifierImpl shouldNot expired
							exitMap[signal] = ExitDef(action, null, finally as ExitAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signals: StatesOrSignals<T>) = object : Exit.Action<T>
			{
				override fun action(action: ExitAction<T>): Exit.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					action as ExitAction<SIGNAL>
					signals.forEach {
						exitMap[it] = ExitDef(action)
					}
					return object : Exit.Catch<T>
					{
						override fun catch(catch: ExitErrorAction<T>): Exit.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							catch as ExitErrorAction<SIGNAL>
							signals.forEach {
								exitMap[it] = ExitDef(action, catch)
							}
							return object : Exit.Finally<T>
							{
								override fun finally(finally: ExitAction<T>)
								{
									this@ModifierImpl shouldNot expired
									signals.forEach {
										exitMap[it] = ExitDef(action, catch, finally as ExitAction<SIGNAL>)
									}
								}
							}
						}
						
						override fun finally(finally: ExitAction<T>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								exitMap[it] = ExitDef(action, null, finally as ExitAction<SIGNAL>)
							}
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(predicate: (T) -> Boolean) = object : Exit.Action<T>
			{
				override fun action(action: ExitAction<T>): Exit.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					exitPredicates.store(predicate)
					exitMap[predicate] = ExitDef(action as ExitAction<SIGNAL>)
					return object : Exit.Catch<T>
					{
						override fun catch(catch: ExitErrorAction<T>): Exit.Finally<T>
						{
							this@ModifierImpl shouldNot expired
							exitMap[predicate] = ExitDef(action, catch as ExitErrorAction<SIGNAL>)
							return object : Exit.Finally<T>
							{
								override fun finally(finally: ExitAction<T>)
								{
									this@ModifierImpl shouldNot expired
									exitMap[predicate] = ExitDef(action, catch, finally as ExitAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: ExitAction<T>)
						{
							this@ModifierImpl shouldNot expired
							exitMap[predicate] = ExitDef(action, null, finally as ExitAction<SIGNAL>)
						}
					}
				}
			}
		}
		
		override val on = object : Init.On
		{
			override fun error(block: StateErrorCallback)
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.onError = block
			}
		}
		
		override val delete = object : Delete
		{
			override fun action(keyword: Entry) = object : Delete.Via
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
				
				override fun <T : SIGNAL> via(predicate: (T) -> Boolean)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.entry = stash
					entryMap.remove(predicate)
					entryPredicates.remove(predicate)
				}
				
				override fun via(keyword: all)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.entry = stash
					this@KotlmataStateImpl.entryMap = null
					this@KotlmataStateImpl.entryPredicates = null
				}
			}
			
			override fun action(keyword: Input) = object : Delete.Signal
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
				
				override fun <T : SIGNAL> signal(predicate: (T) -> Boolean)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.input = stash
					inputMap.remove(predicate)
					inputPredicates.remove(predicate)
				}
				
				override fun signal(keyword: all)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.input = stash
					this@KotlmataStateImpl.inputMap = null
					this@KotlmataStateImpl.inputPredicates = null
				}
			}
			
			override fun action(keyword: Exit) = object : Delete.Via
			{
				val stash = this@KotlmataStateImpl.exit
				
				init
				{
					this@ModifierImpl not expired then {
						this@KotlmataStateImpl.exit = null
					}
				}
				
				override fun <T : SIGNAL> via(signal: KClass<T>)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					exitMap.remove(signal)
				}
				
				override fun <T : SIGNAL> via(signal: T)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					exitMap.remove(signal)
				}
				
				override fun <T : SIGNAL> via(predicate: (T) -> Boolean)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					exitMap.remove(predicate)
					exitPredicates.remove(predicate)
				}
				
				override fun via(keyword: all)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					this@KotlmataStateImpl.exitMap = null
					this@KotlmataStateImpl.exitPredicates = null
				}
			}
			
			override fun action(keyword: all)
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.entry = null
				this@KotlmataStateImpl.input = null
				this@KotlmataStateImpl.exit = null
				this@KotlmataStateImpl.entryMap = null
				this@KotlmataStateImpl.inputMap = null
				this@KotlmataStateImpl.exitMap = null
				this@KotlmataStateImpl.entryPredicates = null
				this@KotlmataStateImpl.inputPredicates = null
				this@KotlmataStateImpl.exitPredicates = null
			}
		}
		
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> T1.or(stateOrSignal: T2) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> T1.or(stateOrSignal: KClass<T2>) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> KClass<T1>.or(stateOrSignal: T2) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> KClass<T1>.or(stateOrSignal: KClass<T2>) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> StatesOrSignals<T1>.or(stateOrSignal: T2) = or(this, stateOrSignal)
		override fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> StatesOrSignals<T1>.or(stateOrSignal: KClass<T2>) = or(this, stateOrSignal)
		
		init
		{
			block(tag)
			expire()
		}
	}
}
