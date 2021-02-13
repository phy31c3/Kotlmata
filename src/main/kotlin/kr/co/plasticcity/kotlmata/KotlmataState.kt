package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataMutableState.*
import kr.co.plasticcity.kotlmata.KotlmataState.*
import kr.co.plasticcity.kotlmata.Log.normal
import kotlin.reflect.KClass

interface KotlmataState<T : STATE>
{
	@KotlmataMarker
	interface Init : SignalsDefinable
	{
		val entry: Entry
		val input: Input
		val exit: Exit
		val on: On
	}
	
	interface Entry
	{
		infix fun action(action: EntryAction<SIGNAL>): Catch<SIGNAL> = function(action)
		infix fun function(function: EntryFunction<SIGNAL>): Catch<SIGNAL>
		infix fun <T : SIGNAL> via(signal: T): Action<T>
		infix fun <T : SIGNAL> via(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> via(signals: Signals<T>): Action<T>
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
		infix fun <T : SIGNAL> signal(signal: T): Action<T>
		infix fun <T : SIGNAL> signal(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> signal(signals: Signals<T>): Action<T>
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
		infix fun <T : SIGNAL> via(signal: T): Action<T>
		infix fun <T : SIGNAL> via(signal: KClass<T>): Action<T>
		infix fun <T : SIGNAL> via(signals: Signals<T>): Action<T>
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
	
	interface On
	{
		infix fun clear(callback: StateSimpleCallback): Catch
		infix fun error(fallback: StateFallback)
		
		interface Catch : Finally
		{
			infix fun catch(catch: StateSimpleFallback): Finally
		}
		
		interface Finally
		{
			infix fun finally(finally: StateSimpleCallback)
		}
	}
	
	val tag: T
	
	fun <S : T, T : SIGNAL> entry(from: STATE, signal: S, type: KClass<T>, transitionCount: Long, payload: Any? = null): Any?
	fun <S : T, T : SIGNAL> input(signal: S, type: KClass<T>, transitionCount: Long, payload: Any? = null): Any?
	fun <S : T, T : SIGNAL> exit(signal: S, type: KClass<T>, transitionCount: Long, payload: Any? = null, to: STATE)
	fun clear()
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
	
	interface Update : Init
	{
		val delete: Delete
	}
	
	interface Delete
	{
		infix fun action(entry: Entry): Via
		infix fun action(input: Input): Signal
		infix fun action(exit: Exit): Via
		infix fun action(all: all)
		
		interface Via
		{
			infix fun <T : SIGNAL> via(signal: KClass<T>)
			infix fun <T : SIGNAL> via(signal: T)
			infix fun <T : SIGNAL> via(predicate: (T) -> Boolean)
			infix fun via(all: all)
		}
		
		interface Signal
		{
			infix fun <T : SIGNAL> signal(signal: KClass<T>)
			infix fun <T : SIGNAL> signal(signal: T)
			infix fun <T : SIGNAL> signal(predicate: (T) -> Boolean)
			infix fun signal(all: all)
		}
	}
	
	operator fun invoke(block: Update.(state: T) -> Unit) = update(block)
	infix fun update(block: Update.(state: T) -> Unit)
}

private class EntryDef(
	val function: EntryFunction<SIGNAL>,
	val intercept: EntryErrorFunction<SIGNAL>? = null,
	val finally: EntryAction<SIGNAL>? = null
)

private class InputDef(
	val function: InputFunction<SIGNAL>,
	val intercept: InputErrorFunction<SIGNAL>? = null,
	val finally: InputAction<SIGNAL>? = null
)

private class ExitDef(
	val action: ExitAction<SIGNAL>,
	val catch: ExitErrorAction<SIGNAL>? = null,
	val finally: ExitAction<SIGNAL>? = null
)

private class SimpleCallbackDef(
	val callback: StateSimpleCallback,
	val catch: StateSimpleFallback? = null,
	val finally: StateSimpleCallback? = null
)

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
	private var onClear: SimpleCallbackDef? = null
	private var onError: StateFallback? = null
	
	private var entryTester: Tester? = null
	private var inputTester: Tester? = null
	private var exitTester: Tester? = null
	
	init
	{
		UpdateImpl().use {
			it.block(tag)
		}
	}
	
	override fun <S : T, T : SIGNAL> entry(from: STATE, signal: S, type: KClass<T>, transitionCount: Long, payload: Any?): Any?
	{
		val entryDef = entryMap?.let { entryMap ->
			entryMap[signal]?.also {
				logLevel.normal(prefix, tag, signal) { STATE_RUN_ENTRY_VIA }
			} ?: entryTester?.test(signal)?.let { predicate ->
				logLevel.normal(prefix, tag) { STATE_RUN_ENTRY_PREDICATE }
				entryMap[predicate]
			} ?: entryMap[type]?.also {
				logLevel.normal(prefix, tag, type) { STATE_RUN_ENTRY_VIA }
			}
		} ?: entry?.also {
			logLevel.normal(prefix, tag) { STATE_RUN_ENTRY }
		} ?: null.also {
			logLevel.normal(prefix, tag) { STATE_NO_ENTRY }
		}
		
		return entryDef?.run {
			try
			{
				EntryFunctionReceiver(from, transitionCount, payload).function(signal)
			}
			catch (e: Throwable)
			{
				intercept?.let { intercept ->
					EntryErrorFunctionReceiver(from, transitionCount, payload, e).intercept(signal) ?: Unit
				} ?: onError?.let { onError ->
					ErrorActionReceiver(e).onError(signal)
				} ?: throw e
			}
			finally
			{
				finally?.let { finally ->
					EntryActionReceiver(from, transitionCount, payload).finally(signal)
				}
			}
		}
	}
	
	override fun <S : T, T : SIGNAL> input(signal: S, type: KClass<T>, transitionCount: Long, payload: Any?): Any?
	{
		val inputDef = inputMap?.let { inputMap ->
			inputMap[signal]?.also {
				logLevel.normal(prefix, tag, signal) { STATE_RUN_INPUT_SIGNAL }
			} ?: inputTester?.test(signal)?.let { predicate ->
				logLevel.normal(prefix, tag) { STATE_RUN_INPUT_PREDICATE }
				inputMap[predicate]
			} ?: inputMap[type]?.also {
				logLevel.normal(prefix, tag, type) { STATE_RUN_INPUT_SIGNAL }
			}
		} ?: input?.also {
			logLevel.normal(prefix, tag) { STATE_RUN_INPUT }
		} ?: null.also {
			if (tag !== Initial_state_for_KotlmataDaemon)
			{
				logLevel.normal(prefix, tag) { STATE_NO_INPUT }
			}
		}
		
		return inputDef?.run {
			try
			{
				InputFunctionReceiver(transitionCount, payload).function(signal)
			}
			catch (e: Throwable)
			{
				intercept?.let { intercept ->
					InputErrorFunctionReceiver(transitionCount, payload, e).intercept(signal) ?: Unit
				} ?: onError?.let { onError ->
					ErrorActionReceiver(e).onError(signal)
				} ?: throw e
			}
			finally
			{
				finally?.let { finally ->
					InputActionReceiver(transitionCount, payload).finally(signal)
				}
			}
		}
	}
	
	override fun <S : T, T : SIGNAL> exit(signal: S, type: KClass<T>, transitionCount: Long, payload: Any?, to: STATE)
	{
		val exitDef = exitMap?.let { exitMap ->
			exitMap[signal]?.also {
				logLevel.normal(prefix, tag, signal) { STATE_RUN_EXIT_VIA }
			} ?: exitTester?.test(signal)?.let { predicate ->
				logLevel.normal(prefix, tag) { STATE_RUN_EXIT_PREDICATE }
				exitMap[predicate]
			} ?: exitMap[type]?.also {
				logLevel.normal(prefix, tag, type) { STATE_RUN_EXIT_VIA }
			}
		} ?: exit?.also {
			logLevel.normal(prefix, tag) { STATE_RUN_EXIT }
		} ?: null.also {
			if (tag !== Initial_state_for_KotlmataDaemon)
			{
				logLevel.normal(prefix, tag) { STATE_NO_EXIT }
			}
		}
		
		exitDef?.apply {
			try
			{
				ExitActionReceiver(to, transitionCount, payload).action(signal)
			}
			catch (e: Throwable)
			{
				catch?.let { catch ->
					ExitErrorActionReceiver(to, transitionCount, payload, e).catch(signal)
				} ?: onError?.let { onError ->
					ErrorActionReceiver(e).onError(signal)
				} ?: throw e
			}
			finally
			{
				finally?.let { finally ->
					ExitActionReceiver(to, transitionCount, payload).finally(signal)
				}
			}
		}
	}
	
	override fun clear()
	{
		onClear?.apply {
			logLevel.normal(prefix, tag) { STATE_ON_CLEAR }
			try
			{
				ActionReceiver.callback()
			}
			catch (e: Throwable)
			{
				catch?.let { catch ->
					ErrorActionReceiver(e).catch()
				} ?: onError?.let { onError ->
					ErrorActionReceiver(e).onError(null)
				} ?: throw e
			}
			finally
			{
				finally?.let { finally ->
					ActionReceiver.finally()
				}
			}
		}
	}
	
	override fun update(block: Update.(T) -> Unit)
	{
		UpdateImpl().use {
			it.block(tag)
		}
	}
	
	override fun toString(): String
	{
		return "KotlmataState[$tag]{${hashCode().toString(16)}}"
	}
	
	private inner class UpdateImpl : Update, SignalsDefinable by SignalsDefinableImpl, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_OBJECT } })
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
		
		private val entryTester: Tester
			get() = this@KotlmataStateImpl.entryTester ?: Tester().also {
				this@KotlmataStateImpl.entryTester = it
			}
		
		private val inputTester: Tester
			get() = this@KotlmataStateImpl.inputTester ?: Tester().also {
				this@KotlmataStateImpl.inputTester = it
			}
		
		private val exitTester: Tester
			get() = this@KotlmataStateImpl.exitTester ?: Tester().also {
				this@KotlmataStateImpl.exitTester = it
			}
		
		/*###################################################################################################################################
		 * Entry
		 *###################################################################################################################################*/
		@Suppress("UNCHECKED_CAST")
		override val entry = object : Entry
		{
			override fun function(function: EntryFunction<SIGNAL>): Entry.Catch<SIGNAL>
			{
				this@UpdateImpl shouldNot expired
				this@KotlmataStateImpl.entry = EntryDef(function)
				return object : Entry.Catch<SIGNAL>
				{
					override fun intercept(intercept: EntryErrorFunction<SIGNAL>): Entry.Finally<SIGNAL>
					{
						this@UpdateImpl shouldNot expired
						this@KotlmataStateImpl.entry = EntryDef(function, intercept)
						return object : Entry.Finally<SIGNAL>
						{
							override fun finally(finally: EntryAction<SIGNAL>)
							{
								this@UpdateImpl shouldNot expired
								this@KotlmataStateImpl.entry = EntryDef(function, intercept, finally)
							}
						}
					}
					
					override fun finally(finally: EntryAction<SIGNAL>)
					{
						this@UpdateImpl shouldNot expired
						this@KotlmataStateImpl.entry = EntryDef(function, null, finally)
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: T) = object : Entry.Action<T>
			{
				override fun function(function: EntryFunction<T>): Entry.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					entryMap[signal] = EntryDef(function as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(intercept: EntryErrorFunction<T>): Entry.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							entryMap[signal] = EntryDef(function, intercept as EntryErrorFunction<SIGNAL>)
							return object : Entry.Finally<T>
							{
								override fun finally(finally: EntryAction<T>)
								{
									this@UpdateImpl shouldNot expired
									entryMap[signal] = EntryDef(function, intercept, finally as EntryAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: EntryAction<T>)
						{
							this@UpdateImpl shouldNot expired
							entryMap[signal] = EntryDef(function, null, finally as EntryAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: KClass<T>) = object : Entry.Action<T>
			{
				override fun function(function: EntryFunction<T>): Entry.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					entryMap[signal] = EntryDef(function as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(intercept: EntryErrorFunction<T>): Entry.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							entryMap[signal] = EntryDef(function, intercept as EntryErrorFunction<SIGNAL>)
							return object : Entry.Finally<T>
							{
								override fun finally(finally: EntryAction<T>)
								{
									this@UpdateImpl shouldNot expired
									entryMap[signal] = EntryDef(function, intercept, finally as EntryAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: EntryAction<T>)
						{
							this@UpdateImpl shouldNot expired
							entryMap[signal] = EntryDef(function, null, finally as EntryAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signals: Signals<T>) = object : Entry.Action<T>
			{
				override fun function(function: EntryFunction<T>): Entry.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					function as EntryFunction<SIGNAL>
					signals.forEach {
						entryMap[it] = EntryDef(function)
					}
					return object : Entry.Catch<T>
					{
						override fun intercept(intercept: EntryErrorFunction<T>): Entry.Finally<T>
						{
							this@UpdateImpl shouldNot expired
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
							this@UpdateImpl shouldNot expired
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
					this@UpdateImpl shouldNot expired
					entryTester += predicate
					entryMap[predicate] = EntryDef(function as EntryFunction<SIGNAL>)
					return object : Entry.Catch<T>
					{
						override fun intercept(intercept: EntryErrorFunction<T>): Entry.Finally<T>
						{
							this@UpdateImpl shouldNot expired
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
							this@UpdateImpl shouldNot expired
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
				this@UpdateImpl shouldNot expired
				this@KotlmataStateImpl.input = InputDef(function)
				return object : Input.Catch<SIGNAL>
				{
					override fun intercept(intercept: InputErrorFunction<SIGNAL>): Input.Finally<SIGNAL>
					{
						this@UpdateImpl shouldNot expired
						this@KotlmataStateImpl.input = InputDef(function, intercept)
						return object : Input.Finally<SIGNAL>
						{
							override fun finally(finally: InputAction<SIGNAL>)
							{
								this@UpdateImpl shouldNot expired
								this@KotlmataStateImpl.input = InputDef(function, intercept, finally)
							}
						}
					}
					
					override fun finally(finally: InputAction<SIGNAL>)
					{
						this@UpdateImpl shouldNot expired
						this@KotlmataStateImpl.input = InputDef(function, null, finally)
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signal: T) = object : Input.Action<T>
			{
				override fun function(function: InputFunction<T>): Input.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					inputMap[signal] = InputDef(function as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(intercept: InputErrorFunction<T>): Input.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							inputMap[signal] = InputDef(function, intercept as InputErrorFunction<SIGNAL>)
							return object : Input.Finally<T>
							{
								override fun finally(finally: InputAction<T>)
								{
									this@UpdateImpl shouldNot expired
									inputMap[signal] = InputDef(function, intercept, finally as InputAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: InputAction<T>)
						{
							this@UpdateImpl shouldNot expired
							inputMap[signal] = InputDef(function, null, finally as InputAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signal: KClass<T>) = object : Input.Action<T>
			{
				override fun function(function: InputFunction<T>): Input.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					inputMap[signal] = InputDef(function as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(intercept: InputErrorFunction<T>): Input.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							inputMap[signal] = InputDef(function, intercept as InputErrorFunction<SIGNAL>)
							return object : Input.Finally<T>
							{
								override fun finally(finally: InputAction<T>)
								{
									this@UpdateImpl shouldNot expired
									inputMap[signal] = InputDef(function, intercept, finally as InputAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: InputAction<T>)
						{
							this@UpdateImpl shouldNot expired
							inputMap[signal] = InputDef(function, null, finally as InputAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> signal(signals: Signals<T>) = object : Input.Action<T>
			{
				override fun function(function: InputFunction<T>): Input.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					function as InputFunction<SIGNAL>
					signals.forEach {
						inputMap[it] = InputDef(function)
					}
					return object : Input.Catch<T>
					{
						override fun intercept(intercept: InputErrorFunction<T>): Input.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							intercept as InputErrorFunction<SIGNAL>
							signals.forEach {
								inputMap[it] = InputDef(function, intercept)
							}
							return object : Input.Finally<T>
							{
								override fun finally(finally: InputAction<T>)
								{
									this@UpdateImpl shouldNot expired
									signals.forEach {
										inputMap[it] = InputDef(function, intercept, finally as InputAction<SIGNAL>)
									}
								}
							}
						}
						
						override fun finally(finally: InputAction<T>)
						{
							this@UpdateImpl shouldNot expired
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
					this@UpdateImpl shouldNot expired
					inputTester += predicate
					inputMap[predicate] = InputDef(function as InputFunction<SIGNAL>)
					return object : Input.Catch<T>
					{
						override fun intercept(intercept: InputErrorFunction<T>): Input.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							inputMap[predicate] = InputDef(function, intercept as InputErrorFunction<SIGNAL>)
							return object : Input.Finally<T>
							{
								override fun finally(finally: InputAction<T>)
								{
									this@UpdateImpl shouldNot expired
									inputMap[predicate] = InputDef(function, intercept, finally as InputAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: InputAction<T>)
						{
							this@UpdateImpl shouldNot expired
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
				this@UpdateImpl shouldNot expired
				this@KotlmataStateImpl.exit = ExitDef(action)
				return object : Exit.Catch<SIGNAL>
				{
					override fun catch(catch: ExitErrorAction<SIGNAL>): Exit.Finally<SIGNAL>
					{
						this@UpdateImpl shouldNot expired
						this@KotlmataStateImpl.exit = ExitDef(action, catch)
						return object : Exit.Finally<SIGNAL>
						{
							override fun finally(finally: ExitAction<SIGNAL>)
							{
								this@UpdateImpl shouldNot expired
								this@KotlmataStateImpl.exit = ExitDef(action, catch, finally)
							}
						}
					}
					
					override fun finally(finally: ExitAction<SIGNAL>)
					{
						this@UpdateImpl shouldNot expired
						this@KotlmataStateImpl.exit = ExitDef(action, null, finally)
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: T) = object : Exit.Action<T>
			{
				override fun action(action: ExitAction<T>): Exit.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					exitMap[signal] = ExitDef(action as ExitAction<SIGNAL>)
					return object : Exit.Catch<T>
					{
						override fun catch(catch: ExitErrorAction<T>): Exit.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							exitMap[signal] = ExitDef(action, catch as ExitErrorAction<SIGNAL>)
							return object : Exit.Finally<T>
							{
								override fun finally(finally: ExitAction<T>)
								{
									this@UpdateImpl shouldNot expired
									exitMap[signal] = ExitDef(action, catch, finally as ExitAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: ExitAction<T>)
						{
							this@UpdateImpl shouldNot expired
							exitMap[signal] = ExitDef(action, null, finally as ExitAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signal: KClass<T>) = object : Exit.Action<T>
			{
				override fun action(action: ExitAction<T>): Exit.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					exitMap[signal] = ExitDef(action as ExitAction<SIGNAL>)
					return object : Exit.Catch<T>
					{
						override fun catch(catch: ExitErrorAction<T>): Exit.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							exitMap[signal] = ExitDef(action, catch as ExitErrorAction<SIGNAL>)
							return object : Exit.Finally<T>
							{
								override fun finally(finally: ExitAction<T>)
								{
									this@UpdateImpl shouldNot expired
									exitMap[signal] = ExitDef(action, catch, finally as ExitAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: ExitAction<T>)
						{
							this@UpdateImpl shouldNot expired
							exitMap[signal] = ExitDef(action, null, finally as ExitAction<SIGNAL>)
						}
					}
				}
			}
			
			override fun <T : SIGNAL> via(signals: Signals<T>) = object : Exit.Action<T>
			{
				override fun action(action: ExitAction<T>): Exit.Catch<T>
				{
					this@UpdateImpl shouldNot expired
					action as ExitAction<SIGNAL>
					signals.forEach {
						exitMap[it] = ExitDef(action)
					}
					return object : Exit.Catch<T>
					{
						override fun catch(catch: ExitErrorAction<T>): Exit.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							catch as ExitErrorAction<SIGNAL>
							signals.forEach {
								exitMap[it] = ExitDef(action, catch)
							}
							return object : Exit.Finally<T>
							{
								override fun finally(finally: ExitAction<T>)
								{
									this@UpdateImpl shouldNot expired
									signals.forEach {
										exitMap[it] = ExitDef(action, catch, finally as ExitAction<SIGNAL>)
									}
								}
							}
						}
						
						override fun finally(finally: ExitAction<T>)
						{
							this@UpdateImpl shouldNot expired
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
					this@UpdateImpl shouldNot expired
					exitTester += predicate
					exitMap[predicate] = ExitDef(action as ExitAction<SIGNAL>)
					return object : Exit.Catch<T>
					{
						override fun catch(catch: ExitErrorAction<T>): Exit.Finally<T>
						{
							this@UpdateImpl shouldNot expired
							exitMap[predicate] = ExitDef(action, catch as ExitErrorAction<SIGNAL>)
							return object : Exit.Finally<T>
							{
								override fun finally(finally: ExitAction<T>)
								{
									this@UpdateImpl shouldNot expired
									exitMap[predicate] = ExitDef(action, catch, finally as ExitAction<SIGNAL>)
								}
							}
						}
						
						override fun finally(finally: ExitAction<T>)
						{
							this@UpdateImpl shouldNot expired
							exitMap[predicate] = ExitDef(action, null, finally as ExitAction<SIGNAL>)
						}
					}
				}
			}
		}
		
		override val on = object : On
		{
			override fun clear(callback: StateSimpleCallback): On.Catch
			{
				this@UpdateImpl shouldNot expired
				onClear = SimpleCallbackDef(callback, null, null)
				return object : On.Catch
				{
					override fun catch(catch: StateSimpleFallback): On.Finally
					{
						this@UpdateImpl shouldNot expired
						onClear = SimpleCallbackDef(callback, catch, null)
						return object : On.Finally
						{
							override fun finally(finally: StateSimpleCallback)
							{
								this@UpdateImpl shouldNot expired
								onClear = SimpleCallbackDef(callback, catch, finally)
							}
						}
					}
					
					override fun finally(finally: StateSimpleCallback)
					{
						this@UpdateImpl shouldNot expired
						onClear = SimpleCallbackDef(callback, null, finally)
					}
				}
			}
			
			override fun error(fallback: StateFallback)
			{
				this@UpdateImpl shouldNot expired
				this@KotlmataStateImpl.onError = fallback
			}
		}
		
		override val delete = object : Delete
		{
			override fun action(entry: Entry) = object : Delete.Via
			{
				val stash = this@KotlmataStateImpl.entry
				
				init
				{
					this@UpdateImpl not expired then {
						this@KotlmataStateImpl.entry = null
					}
				}
				
				override fun <T : SIGNAL> via(signal: KClass<T>)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.entry = stash
					entryMap.remove(signal)
				}
				
				override fun <T : SIGNAL> via(signal: T)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.entry = stash
					entryMap.remove(signal)
				}
				
				override fun <T : SIGNAL> via(predicate: (T) -> Boolean)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.entry = stash
					entryMap.remove(predicate)
					entryTester.remove(predicate)
				}
				
				override fun via(all: all)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.entry = stash
					this@KotlmataStateImpl.entryMap = null
					this@KotlmataStateImpl.entryTester = null
				}
			}
			
			override fun action(input: Input) = object : Delete.Signal
			{
				val stash = this@KotlmataStateImpl.input
				
				init
				{
					this@UpdateImpl not expired then {
						this@KotlmataStateImpl.input = null
					}
				}
				
				override fun <T : SIGNAL> signal(signal: KClass<T>)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.input = stash
					inputMap.remove(signal)
				}
				
				override fun <T : SIGNAL> signal(signal: T)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.input = stash
					inputMap.remove(signal)
				}
				
				override fun <T : SIGNAL> signal(predicate: (T) -> Boolean)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.input = stash
					inputMap.remove(predicate)
					inputTester.remove(predicate)
				}
				
				override fun signal(all: all)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.input = stash
					this@KotlmataStateImpl.inputMap = null
					this@KotlmataStateImpl.inputTester = null
				}
			}
			
			override fun action(exit: Exit) = object : Delete.Via
			{
				val stash = this@KotlmataStateImpl.exit
				
				init
				{
					this@UpdateImpl not expired then {
						this@KotlmataStateImpl.exit = null
					}
				}
				
				override fun <T : SIGNAL> via(signal: KClass<T>)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					exitMap.remove(signal)
				}
				
				override fun <T : SIGNAL> via(signal: T)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					exitMap.remove(signal)
				}
				
				override fun <T : SIGNAL> via(predicate: (T) -> Boolean)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					exitMap.remove(predicate)
					exitTester.remove(predicate)
				}
				
				override fun via(all: all)
				{
					this@UpdateImpl shouldNot expired
					this@KotlmataStateImpl.exit = stash
					this@KotlmataStateImpl.exitMap = null
					this@KotlmataStateImpl.exitTester = null
				}
			}
			
			override fun action(all: all)
			{
				this@UpdateImpl shouldNot expired
				this@KotlmataStateImpl.entry = null
				this@KotlmataStateImpl.input = null
				this@KotlmataStateImpl.exit = null
				this@KotlmataStateImpl.entryMap = null
				this@KotlmataStateImpl.inputMap = null
				this@KotlmataStateImpl.exitMap = null
				this@KotlmataStateImpl.entryTester = null
				this@KotlmataStateImpl.inputTester = null
				this@KotlmataStateImpl.exitTester = null
			}
		}
	}
}
