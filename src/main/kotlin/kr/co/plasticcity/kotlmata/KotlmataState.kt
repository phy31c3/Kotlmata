@file:Suppress("unused")

package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataMutableState.*
import kr.co.plasticcity.kotlmata.KotlmataState.*
import kotlin.reflect.KClass

interface KotlmataState<T : STATE>
{
	companion object
	{
		operator fun <T : STATE> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			block: StateTemplate<T>
		): KotlmataState<T> = KotlmataStateImpl(tag, logLevel, block = block)
		
		fun <T : STATE> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			block: StateTemplate<T>
		) = lazy {
			invoke(tag, logLevel, block)
		}
	}
	
	@KotlmataMarker
	interface Init
	{
		val entry: Entry
		val input: Input
		val exit: Exit
		val error: Error
		
		infix fun SIGNAL.or(signal: SIGNAL): StatesOrSignals
	}
	
	interface Entry
	{
		infix fun action(action: EntryAction<SIGNAL>): Catch<SIGNAL> = function(action)
		infix fun function(action: EntryFunction<SIGNAL>): Catch<SIGNAL>
		infix fun <T : SIGNAL> via(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> via(signal: T): Action<T>
		infix fun <T : SIGNAL> via(predicate: (T) -> Boolean): Action<T>
		infix fun via(signals: StatesOrSignals): Action<SIGNAL>
		
		interface Action<T : SIGNAL>
		{
			infix fun action(action: EntryAction<T>): Catch<T> = function(action)
			infix fun function(action: EntryFunction<T>): Catch<T>
		}
		
		interface Catch<T : SIGNAL>
		{
			infix fun catch(error: EntryErrorAction<T>) = intercept(error)
			infix fun intercept(error: EntryErrorFunction<T>)
		}
	}
	
	interface Input
	{
		infix fun action(action: InputAction<SIGNAL>): Catch<SIGNAL> = function(action)
		infix fun function(action: InputFunction<SIGNAL>): Catch<SIGNAL>
		infix fun <T : SIGNAL> signal(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> signal(signal: T): Action<T>
		infix fun <T : SIGNAL> signal(predicate: (T) -> Boolean): Action<T>
		infix fun signal(signals: StatesOrSignals): Action<SIGNAL>
		
		interface Action<T : SIGNAL>
		{
			infix fun action(action: InputAction<T>): Catch<T> = function(action)
			infix fun function(action: InputFunction<T>): Catch<T>
		}
		
		interface Catch<T : SIGNAL>
		{
			infix fun catch(error: InputErrorAction<T>) = intercept(error)
			infix fun intercept(error: InputErrorFunction<T>)
		}
	}
	
	interface Exit
	{
		infix fun action(action: ExitAction<SIGNAL>): Catch<SIGNAL>
		infix fun <T : SIGNAL> via(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> via(signal: T): Action<T>
		infix fun <T : SIGNAL> via(predicate: (T) -> Boolean): Action<T>
		infix fun via(signals: StatesOrSignals): Action<SIGNAL>
		
		interface Action<T : SIGNAL>
		{
			infix fun action(action: ExitAction<T>): Catch<T>
		}
		
		interface Catch<T : SIGNAL>
		{
			infix fun catch(error: ExitErrorAction<T>)
		}
	}
	
	interface Error
	{
		infix fun action(error: StateError)
	}
	
	val tag: T
	
	fun entry(signal: SIGNAL): Any?
	fun <T : SIGNAL> entry(signal: T, type: KClass<in T>): Any?
	
	fun input(signal: SIGNAL, payload: Any? = null): Any?
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null): Any?
	
	fun exit(signal: SIGNAL)
	fun <T : SIGNAL> exit(signal: T, type: KClass<in T>)
}

interface KotlmataMutableState<T : STATE> : KotlmataState<T>
{
	companion object
	{
		operator fun <T : STATE> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			block: StateTemplate<T>
		): KotlmataMutableState<T> = KotlmataStateImpl(tag, logLevel, block = block)
		
		fun <T : STATE> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			block: StateTemplate<T>
		) = lazy {
			invoke(tag, logLevel, block)
		}
		
		internal fun <T : STATE> create(
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
			infix fun via(keyword: all)
		}
		
		interface Signal
		{
			infix fun <T : SIGNAL> signal(signal: KClass<T>)
			infix fun <T : SIGNAL> signal(signal: T)
			infix fun signal(keyword: all)
		}
	}
	
	operator fun invoke(block: Modifier.(state: T) -> Unit) = modify(block)
	
	infix fun modify(block: Modifier.(state: T) -> Unit)
}

private class EntryDef(val action: EntryFunction<SIGNAL>, val catch: EntryErrorFunction<SIGNAL>? = null)
private class InputDef(val action: InputFunction<SIGNAL>, val catch: InputErrorFunction<SIGNAL>? = null)
private class ExitDef(val action: ExitAction<SIGNAL>, val catch: ExitErrorAction<SIGNAL>? = null)

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
	private var error: StateError? = null
	
	private var entryPredicates = Predicates()
	private var inputPredicates = Predicates()
	private var exitPredicates = Predicates()
	
	init
	{
		ModifierImpl(block)
		if (tag !== CREATED)
		{
			logLevel.normal(prefix, tag) { STATE_CREATED }
		}
	}
	
	private fun EntryDef.run(signal: SIGNAL): Any? = try
	{
		Function.action(signal)
	}
	catch (e: Throwable)
	{
		catch?.let {
			ErrorFunction(e).it(signal) ?: Unit
		} ?: error?.let {
			ErrorAction(e).it(signal)
		} ?: throw e
	}
	
	private fun InputDef.run(signal: SIGNAL, payload: Any?): Any? = try
	{
		PayloadFunction(payload).action(signal)
	}
	catch (e: Throwable)
	{
		catch?.let {
			ErrorPayloadFunction(e, payload).it(signal) ?: Unit
		} ?: error?.let {
			ErrorAction(e).it(signal)
		} ?: throw e
	}
	
	private fun ExitDef.run(signal: SIGNAL) = try
	{
		Action.action(signal)
	}
	catch (e: Throwable)
	{
		catch?.let {
			ErrorAction(e).it(signal)
		} ?: error?.let {
			ErrorAction(e).it(signal)
		} ?: throw e
	}
	
	override fun entry(signal: SIGNAL): Any?
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
				else -> null
			}
		} ?: entry?.also {
			logLevel.normal(prefix, tag, signal) { STATE_RUN_ENTRY_DEFAULT }
		} ?: null.also {
			logLevel.normal(prefix, tag, signal) { STATE_NO_ENTRY }
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
		
		return entryDef?.run(signal)
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
				else -> null
			}
		} ?: input?.also {
			logLevel.normal(prefix, tag, signal) { STATE_RUN_INPUT_DEFAULT }
		} ?: null.also {
			if (tag !== CREATED) logLevel.normal(prefix, tag, signal) { STATE_NO_INPUT }
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
			if (tag !== CREATED) logLevel.normal(prefix, tag, signal, "${type.simpleName}::class") { STATE_NO_INPUT_TYPED }
		}
		
		return inputDef?.run(signal, payload)
	}
	
	override fun exit(signal: SIGNAL)
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
				else -> null
			}
		} ?: exit?.also {
			logLevel.normal(prefix, tag, signal) { STATE_RUN_EXIT_DEFAULT }
		} ?: null.also {
			if (tag !== CREATED) logLevel.normal(prefix, tag, signal) { STATE_NO_EXIT }
		}
		
		exitDef?.run(signal)
	}
	
	override fun <T : SIGNAL> exit(signal: T, type: KClass<in T>)
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
			if (tag !== CREATED) logLevel.normal(prefix, tag, signal, "${type.simpleName}::class") { STATE_NO_EXIT_TYPED }
		}
		
		exitDef?.run(signal)
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
		
		/*###################################################################################################################################
		 * Entry
		 *###################################################################################################################################*/
		@Suppress("UNCHECKED_CAST")
		override val entry = object : Entry
		{
			override fun function(action: EntryFunction<SIGNAL>): Entry.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.entry = EntryDef(action)
				return object : Entry.Catch<SIGNAL>
				{
					override fun intercept(error: EntryErrorFunction<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.entry = EntryDef(action, error)
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: KClass<T>) = object : Entry.Action<T>
			{
				override fun function(action: EntryFunction<T>): Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					entryMap[signal] = EntryDef(action as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(error: EntryErrorFunction<T>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(action, error as EntryErrorFunction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: T) = object : Entry.Action<T>
			{
				override fun function(action: EntryFunction<T>): Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					entryMap[signal] = EntryDef(action as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(error: EntryErrorFunction<T>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[signal] = EntryDef(action, error as EntryErrorFunction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(predicate: (T) -> Boolean) = object : Entry.Action<T>
			{
				override fun function(action: EntryFunction<T>): Entry.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					entryPredicates.store(predicate)
					entryMap[predicate] = EntryDef(action as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(error: EntryErrorFunction<T>)
						{
							this@ModifierImpl shouldNot expired
							entryMap[predicate] = EntryDef(action, error as EntryErrorFunction<SIGNAL>)
						}
					}
				}
			}
			
			override fun via(signals: StatesOrSignals) = object : Entry.Action<SIGNAL>
			{
				override fun function(action: EntryFunction<SIGNAL>): Entry.Catch<SIGNAL>
				{
					this@ModifierImpl shouldNot expired
					signals.forEach {
						entryMap[it] = EntryDef(action)
					}
					return object : Entry.Catch<SIGNAL>
					{
						override fun intercept(error: EntryErrorFunction<SIGNAL>)
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
		override val input = object : Input
		{
			override fun function(action: InputFunction<SIGNAL>): Input.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.input = InputDef(action)
				return object : Input.Catch<SIGNAL>
				{
					override fun intercept(error: InputErrorFunction<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.input = InputDef(action, error)
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signal: KClass<T>) = object : Input.Action<T>
			{
				override fun function(action: InputFunction<T>): Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					inputMap[signal] = InputDef(action as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(error: InputErrorFunction<T>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action, error as InputErrorFunction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signal: T) = object : Input.Action<T>
			{
				override fun function(action: InputFunction<T>): Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					inputMap[signal] = InputDef(action as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(error: InputErrorFunction<T>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[signal] = InputDef(action, error as InputErrorFunction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> signal(predicate: (T) -> Boolean) = object : Input.Action<T>
			{
				override fun function(action: InputFunction<T>): Input.Catch<T>
				{
					this@ModifierImpl shouldNot expired
					inputPredicates.store(predicate)
					inputMap[predicate] = InputDef(action as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(error: InputErrorFunction<T>)
						{
							this@ModifierImpl shouldNot expired
							inputMap[predicate] = InputDef(action, error as InputErrorFunction<SIGNAL>)
						}
					}
				}
			}
			
			override fun signal(signals: StatesOrSignals) = object : Input.Action<SIGNAL>
			{
				override fun function(action: InputFunction<SIGNAL>): Input.Catch<SIGNAL>
				{
					this@ModifierImpl shouldNot expired
					signals.forEach {
						inputMap[it] = InputDef(action)
					}
					return object : Input.Catch<SIGNAL>
					{
						override fun intercept(error: InputErrorFunction<SIGNAL>)
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
		@Suppress("UNCHECKED_CAST")
		override val exit = object : Exit
		{
			override fun action(action: ExitAction<SIGNAL>): Exit.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.exit = ExitDef(action)
				return object : Exit.Catch<SIGNAL>
				{
					override fun catch(error: ExitErrorAction<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						this@KotlmataStateImpl.exit = ExitDef(action, error)
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
						override fun catch(error: ExitErrorAction<T>)
						{
							this@ModifierImpl shouldNot expired
							exitMap[signal] = ExitDef(action, error as ExitErrorAction<SIGNAL>)
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
						override fun catch(error: ExitErrorAction<T>)
						{
							this@ModifierImpl shouldNot expired
							exitMap[signal] = ExitDef(action, error as ExitErrorAction<SIGNAL>)
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
						override fun catch(error: ExitErrorAction<T>)
						{
							this@ModifierImpl shouldNot expired
							exitMap[predicate] = ExitDef(action, error as ExitErrorAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun via(signals: StatesOrSignals) = object : Exit.Action<SIGNAL>
			{
				override fun action(action: ExitAction<SIGNAL>): Exit.Catch<SIGNAL>
				{
					this@ModifierImpl shouldNot expired
					signals.forEach {
						exitMap[it] = ExitDef(action)
					}
					return object : Exit.Catch<SIGNAL>
					{
						override fun catch(error: ExitErrorAction<SIGNAL>)
						{
							this@ModifierImpl shouldNot expired
							signals.forEach {
								exitMap[it] = ExitDef(action, error)
							}
						}
					}
				}
			}
		}
		
		override val error = object : Error
		{
			override fun action(error: StateError)
			{
				this@ModifierImpl shouldNot expired
				this@KotlmataStateImpl.error = error
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
				
				override fun via(keyword: all)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.entry = stash
					this@KotlmataStateImpl.entryMap = null
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
				
				override fun signal(keyword: all)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.input = stash
					this@KotlmataStateImpl.inputMap = null
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
				
				override fun via(keyword: all)
				{
					this@ModifierImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					this@KotlmataStateImpl.exitMap = null
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
			}
		}
		
		override fun SIGNAL.or(signal: SIGNAL): StatesOrSignals = object : StatesOrSignals, MutableList<SIGNAL> by mutableListOf(this, signal)
		{
			override fun or(stateOrSignal: STATE_OR_SIGNAL): StatesOrSignals
			{
				this@ModifierImpl shouldNot expired
				add(signal)
				return this
			}
		}
		
		init
		{
			block(tag)
			expire()
		}
	}
}
