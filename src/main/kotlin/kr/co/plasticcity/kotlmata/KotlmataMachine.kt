@file:Suppress("unused")

package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataMachine<T : MACHINE>
{
	companion object
	{
		operator fun <T : MACHINE> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			block: MachineTemplate<T>
		): KotlmataMachine<T> = KotlmataMachineImpl(tag, logLevel, block = block)
		
		operator fun <T : MACHINE> invoke(
			tag: T,
			logLevel: Int = NO_LOG
		) = object : ExtendsInvoke<T>
		{
			override fun extends(block: MachineTemplate<T>) = invoke(tag, logLevel, block)
		}
		
		fun <T : MACHINE> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			block: MachineTemplate<T>
		) = lazy {
			invoke(tag, logLevel, block)
		}
		
		fun <T : MACHINE> lazy(
			tag: T,
			logLevel: Int = NO_LOG
		) = object : ExtendsLazy<T>
		{
			override fun extends(block: MachineTemplate<T>) = lazy { invoke(tag, logLevel, block) }
		}
		
		interface ExtendsInvoke<T : MACHINE>
		{
			infix fun extends(block: MachineTemplate<T>): KotlmataMachine<T>
		}
		
		interface ExtendsLazy<T : MACHINE>
		{
			infix fun extends(block: MachineTemplate<T>): Lazy<KotlmataMachine<T>>
		}
	}
	
	@KotlmataMarker
	interface Init : StateDefine, RuleDefine
	{
		val on: On
		val start: Start
		
		interface On
		{
			infix fun error(block: MachineError)
			infix fun transition(block: TransitionCallback)
		}
		
		interface Start
		{
			infix fun at(state: STATE): End
		}
		
		class End internal constructor()
	}
	
	interface StateDefine
	{
		operator fun <S : STATE> S.invoke(block: StateTemplate<S>)
		infix fun <S : STATE> S.extends(block: StateTemplate<S>) = invoke(block)
		
		infix fun <S : STATE> S.action(action: EntryAction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL> = function(action)
		infix fun <S : STATE> S.function(action: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>): KotlmataState.Entry.Action<T>
		infix fun <S : STATE, T : SIGNAL> S.via(signal: T): KotlmataState.Entry.Action<T>
		infix fun <S : STATE> S.via(signals: KotlmataState.Init.Signals): KotlmataState.Entry.Action<SIGNAL>
	}
	
	interface RuleDefine
	{
		/* Basic rule interface */
		
		infix fun STATE.x(signal: SIGNAL): RuleLeft
		infix fun STATE.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun STATE.x(keyword: any): RuleLeft
		
		infix fun any.x(signal: SIGNAL): RuleLeft
		infix fun any.x(signal: KClass<out SIGNAL>): RuleLeft
		infix fun any.x(keyword: any): RuleLeft
		
		/* For 'AnyXX' interface */
		
		interface AnyOf : List<STATE_OR_SIGNAL>
		interface AnyExcept : List<STATE_OR_SIGNAL>
		
		fun any.of(vararg args: STATE_OR_SIGNAL): AnyOf
		fun any.except(vararg args: STATE_OR_SIGNAL): AnyExcept
		
		/* any.xxx(...) x "signal" %= "to" */
		
		infix fun AnyOf.x(signal: SIGNAL): RuleAssignable
		infix fun AnyOf.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun AnyOf.x(keyword: any): RuleAssignable
		infix fun AnyExcept.x(signal: SIGNAL): RuleAssignable
		infix fun AnyExcept.x(signal: KClass<out SIGNAL>): RuleAssignable
		infix fun AnyExcept.x(keyword: any): RuleAssignable
		
		/* "from" x any.xxx(...) %= "to" */
		
		infix fun STATE.x(anyOf: AnyOf): RuleAssignable
		infix fun any.x(anyOf: AnyOf): RuleAssignable
		infix fun STATE.x(anyExcept: AnyExcept): RuleAssignable
		infix fun any.x(anyExcept: AnyExcept): RuleAssignable
		
		/* any.xxx(...) x any.xxx(...) %= "to" */
		
		infix fun AnyOf.x(anyOf: AnyOf): RuleAssignable
		infix fun AnyOf.x(anyExcept: AnyExcept): RuleAssignable
		infix fun AnyExcept.x(anyOf: AnyOf): RuleAssignable
		infix fun AnyExcept.x(anyExcept: AnyExcept): RuleAssignable
		
		/* For Signals interface */
		infix fun SIGNAL.or(signal: SIGNAL): Signals
		
		interface Signals : MutableList<SIGNAL>
		{
			infix fun or(signal: SIGNAL): Signals
		}
		
		infix fun STATE.x(signals: Signals): RuleAssignable
		infix fun any.x(signals: Signals): RuleAssignable
		infix fun AnyOf.x(signals: Signals): RuleAssignable
		infix fun AnyExcept.x(signals: Signals): RuleAssignable
		
		/**
		 * For chaining transition rule
		 *
		 * `chain from "state1" to "state2" to "state3" to ... via "signal"`
		 */
		val chain: Chain
		
		interface Chain
		{
			infix fun from(state: STATE): To
		}
		
		interface To
		{
			infix fun to(state: STATE): Via
		}
		
		interface Via : To
		{
			infix fun via(signal: SIGNAL)
			infix fun via(signal: KClass<out SIGNAL>)
			infix fun via(keyword: any)
		}
	}
	
	interface RuleAssignable
	{
		operator fun remAssign(state: STATE)
		operator fun remAssign(keyword: self) = remAssign(state = keyword)
	}
	
	interface RuleLeft : RuleAssignable
	{
		val state: STATE
		val signal: SIGNAL
	}
	
	val tag: T
	
	fun input(signal: SIGNAL, payload: Any? = null)
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null)
	
	/**
	 * @param block Called if the state is switched and the next state's entry function returns a signal.
	 */
	fun input(signal: SIGNAL, payload: Any? = null, block: (FunctionDSL.Sync) -> Unit)
	
	/**
	 * @param block Called if the state is switched and the next state's entry function returns a signal.
	 */
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null, block: (FunctionDSL.Sync) -> Unit)
	
	@Deprecated("KClass<T> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<out Any>, payload: Any? = null)
	
	@Deprecated("KClass<T> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<out Any>, payload: Any? = null, block: (FunctionDSL.Sync) -> Unit)
}

interface KotlmataMutableMachine<T : MACHINE> : KotlmataMachine<T>
{
	companion object
	{
		operator fun <T : MACHINE> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			block: MachineTemplate<T>
		): KotlmataMutableMachine<T> = KotlmataMachineImpl(tag, logLevel, block = block)
		
		operator fun <T : MACHINE> invoke(
			tag: T,
			logLevel: Int = NO_LOG
		) = object : ExtendsInvoke<T>
		{
			override fun extends(block: MachineTemplate<T>) = invoke(tag, logLevel, block)
		}
		
		fun <T : MACHINE> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			block: MachineTemplate<T>
		) = lazy {
			invoke(tag, logLevel, block)
		}
		
		fun <T : MACHINE> lazy(
			tag: T,
			logLevel: Int = NO_LOG
		) = object : ExtendsLazy<T>
		{
			override fun extends(block: MachineTemplate<T>) = lazy { invoke(tag, logLevel, block) }
		}
		
		interface ExtendsInvoke<T : MACHINE>
		{
			infix fun extends(block: MachineTemplate<T>): KotlmataMutableMachine<T>
		}
		
		interface ExtendsLazy<T : MACHINE>
		{
			infix fun extends(block: MachineTemplate<T>): Lazy<KotlmataMutableMachine<T>>
		}
		
		internal fun <T : MACHINE> create(
			tag: T,
			logLevel: Int,
			prefix: String,
			block: MachineTemplate<T>
		): KotlmataMutableMachine<T> = KotlmataMachineImpl(tag, logLevel, prefix, block)
	}
	
	@KotlmataMarker
	interface Modifier : KotlmataMachine.StateDefine, KotlmataMachine.RuleDefine
	{
		val current: STATE
		val has: Has
		val insert: Insert
		val replace: Replace
		val update: Update
		val delete: Delete
		
		interface Has
		{
			infix fun state(state: STATE): Then
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): Then
			
			interface Then
			{
				infix fun then(block: () -> Unit): Or
			}
			
			interface Or
			{
				infix fun or(block: () -> Unit)
			}
		}
		
		interface Insert
		{
			infix fun <T : STATE> state(state: T): By<T>
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): RemAssign
			infix fun or(keyword: Replace): State
			infix fun or(keyword: Update): Rule
			
			interface State
			{
				infix fun <T : STATE> state(state: T): By<T>
			}
			
			interface Rule
			{
				infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): RemAssign
			}
			
			interface By<T : STATE>
			{
				infix fun by(block: StateTemplate<T>)
			}
			
			interface RemAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Replace
		{
			infix fun <T : STATE> state(state: T): By<T>
			
			interface By<T : STATE>
			{
				infix fun by(block: StateTemplate<T>)
			}
		}
		
		interface Update
		{
			infix fun <T : STATE> state(state: T): By<T>
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): RemAssign
			
			interface By<T : STATE>
			{
				infix fun by(block: KotlmataMutableState.Modifier.(state: T) -> Unit): Or<T>
			}
			
			interface Or<T : STATE>
			{
				infix fun or(block: StateTemplate<T>)
			}
			
			interface RemAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Delete
		{
			infix fun state(state: STATE)
			infix fun state(keyword: all)
			
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft)
			infix fun rule(keyword: of): State
			infix fun rule(keyword: all)
			
			interface State
			{
				infix fun state(state: STATE)
			}
		}
	}
	
	operator fun invoke(block: Modifier.(machine: T) -> Unit) = modify(block)
	
	infix fun modify(block: Modifier.(machine: T) -> Unit)
}

private class KotlmataMachineImpl<T : MACHINE>(
	override val tag: T,
	val logLevel: Int = NO_LOG,
	val prefix: String = "Machine[$tag]:",
	block: MachineTemplate<T>
) : KotlmataMutableMachine<T>
{
	private val stateMap: MutableMap<STATE, KotlmataMutableState<out STATE>> = HashMap()
	private val ruleMap: MutableMap<STATE, MutableMap<SIGNAL, STATE>> = HashMap()
	
	private var onTransition: TransitionCallback? = null
	private var onError: MachineError? = null
	
	private lateinit var current: KotlmataState<out STATE>
	
	init
	{
		logLevel.normal(prefix) { MACHINE_START_BUILD }
		ModifierImpl(init = block)
		logLevel.normal(prefix) { MACHINE_END_BUILD }
	}
	
	private inline fun <T> tryCatchReturn(block: () -> T?): T? = try
	{
		block()
	}
	catch (e: Throwable)
	{
		onError?.also { onError ->
			Error(e).onError()
		} ?: throw e
		null
	}
	
	private fun defaultInput(begin: FunctionDSL.Sync)
	{
		var next: FunctionDSL.Sync? = begin
		while (next != null) next.also {
			next = null
			if (it.type == null) input(it.signal, it.payload) { sync ->
				next = sync
			}
			else input(it.signal, it.type, it.payload) { sync ->
				next = sync
			}
		}
	}
	
	override fun input(signal: SIGNAL, payload: Any?)
	{
		defaultInput(FunctionDSL.Sync(signal, null, payload))
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?)
	{
		defaultInput(FunctionDSL.Sync(signal, type as KClass<SIGNAL>, payload))
	}
	
	override fun input(signal: SIGNAL, payload: Any?, block: (FunctionDSL.Sync) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[signal] ?: this[signal::class] ?: this[any]
		}
		
		tryCatchReturn {
			if (current.tag !== CREATED)
			{
				logLevel.normal(prefix, signal, payload, current.tag) { MACHINE_START_INPUT }
			}
			current.input(signal, payload)
		}.also {
			if (current.tag !== CREATED)
			{
				logLevel.normal(prefix, signal, payload, current.tag) { MACHINE_END_INPUT }
			}
		}.convertToSync()?.also { sync ->
			block(sync)
		} ?: ruleMap.let {
			it[current.tag]?.next() ?: it[any]?.next()
		}?.let {
			when (it)
			{
				is stay ->
				{
					null
				}
				is self ->
				{
					current
				}
				!in stateMap ->
				{
					Log.w(prefix.trimEnd(), current.tag, signal, it) { TRANSITION_FAILED }
					null
				}
				else ->
				{
					stateMap[it]
				}
			}
		}?.also { next ->
			logLevel.simple(prefix, current.tag, signal, next.tag) { MACHINE_START_TRANSITION }
			tryCatchReturn { current.exit(signal) }
			onTransition?.invoke(Transition(), current.tag, signal, next.tag)
			current = next
			tryCatchReturn { current.entry(signal) }.convertToSync()?.also(block)
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?, block: (FunctionDSL.Sync) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[type] ?: this[any]
		}
		
		tryCatchReturn {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", payload, current.tag) { MACHINE_START_TYPED_INPUT }
			current.input(signal, type, payload)
		}.also {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", payload, current.tag) { MACHINE_END_TYPED_INPUT }
		}.convertToSync()?.also { sync ->
			block(sync)
		} ?: ruleMap.let {
			it[current.tag]?.next() ?: it[any]?.next()
		}?.let {
			when (it)
			{
				is stay ->
				{
					null
				}
				is self ->
				{
					current
				}
				!in stateMap ->
				{
					Log.w(prefix.trimEnd(), current.tag, "${type.simpleName}::class", it) { TRANSITION_FAILED }
					null
				}
				else ->
				{
					stateMap[it]
				}
			}
		}?.also { next ->
			logLevel.simple(prefix, current.tag, "${type.simpleName}::class", next.tag) { MACHINE_START_TRANSITION }
			tryCatchReturn { current.exit(signal) }
			onTransition?.invoke(Transition(), current.tag, signal, next.tag)
			current = next
			tryCatchReturn { current.entry(signal, type) }.convertToSync()?.also(block)
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<out Any>, payload: Any?)
	{
		throw IllegalArgumentException("KClass<T> type cannot be used as input.")
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<out Any>, payload: Any?, block: (FunctionDSL.Sync) -> Unit)
	{
		throw IllegalArgumentException("KClass<T> type cannot be used as input.")
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.(T) -> Unit)
	{
		logLevel.normal(prefix, current.tag) { MACHINE_START_MODIFY }
		ModifierImpl(modify = block)
		logLevel.normal(prefix, current.tag) { MACHINE_END_MODIFY }
	}
	
	override fun toString(): String
	{
		return "KotlmataMachine[$tag]{${hashCode().toString(16)}}"
	}
	
	private inner class ModifierImpl(
		init: (MachineTemplate<T>)? = null,
		modify: (KotlmataMutableMachine.Modifier.(T) -> Unit)? = null
	) : KotlmataMachine.Init, KotlmataMutableMachine.Modifier, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		override val on = object : KotlmataMachine.Init.On
		{
			override fun error(block: MachineError)
			{
				this@ModifierImpl shouldNot expired
				onError = block
			}
			
			override fun transition(block: TransitionCallback)
			{
				this@ModifierImpl shouldNot expired
				onTransition = block
			}
		}
		
		override val start = object : KotlmataMachine.Init.Start
		{
			override fun at(state: STATE): KotlmataMachine.Init.End
			{
				this@ModifierImpl shouldNot expired
				
				stateMap[state]?.also {
					this@KotlmataMachineImpl.current = it
				} ?: Log.e(prefix.trimEnd(), state) { UNDEFINED_START_STATE }
				
				return KotlmataMachine.Init.End()
			}
		}
		
		override val current: STATE
			get()
			{
				this@ModifierImpl shouldNot expired
				return this@KotlmataMachineImpl.current.tag
			}
		
		override val has = object : KotlmataMutableMachine.Modifier.Has
		{
			val stop = object : KotlmataMutableMachine.Modifier.Has.Or
			{
				override fun or(block: () -> Unit)
				{
					/* do nothing */
				}
			}
			
			val or = object : KotlmataMutableMachine.Modifier.Has.Or
			{
				override fun or(block: () -> Unit)
				{
					block()
				}
			}
			
			override fun state(state: STATE) = object : KotlmataMutableMachine.Modifier.Has.Then
			{
				override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.Or
				{
					this@ModifierImpl shouldNot expired
					return if (state in stateMap)
					{
						block()
						stop
					}
					else
					{
						or
					}
				}
			}
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Has.Then
			{
				override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.Or
				{
					this@ModifierImpl shouldNot expired
					return ruleMap.let {
						it[ruleLeft.state]
					}?.let {
						it[ruleLeft.signal]
					}?.let {
						block()
						stop
					} ?: or
				}
			}
		}
		
		override val insert = object : KotlmataMutableMachine.Modifier.Insert
		{
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Insert.By<T>
			{
				override fun by(block: StateTemplate<T>)
				{
					this@ModifierImpl shouldNot expired
					if (state !in stateMap)
					{
						state.invoke(block)
					}
				}
			}
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Insert.RemAssign
			{
				override fun remAssign(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap.let {
						it[ruleLeft.state]
					}?.let {
						it[ruleLeft.signal]
					} ?: let {
						ruleLeft %= state
					}
				}
			}
			
			override fun or(keyword: KotlmataMutableMachine.Modifier.Replace) = object : KotlmataMutableMachine.Modifier.Insert.State
			{
				override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Insert.By<T>
				{
					override fun by(block: StateTemplate<T>)
					{
						this@ModifierImpl shouldNot expired
						state.invoke(block)
					}
				}
			}
			
			override fun or(keyword: KotlmataMutableMachine.Modifier.Update) = object : KotlmataMutableMachine.Modifier.Insert.Rule
			{
				override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Insert.RemAssign
				{
					override fun remAssign(state: STATE)
					{
						this@ModifierImpl shouldNot expired
						ruleLeft %= state
					}
				}
			}
		}
		
		override val replace = object : KotlmataMutableMachine.Modifier.Replace
		{
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Replace.By<T>
			{
				override fun by(block: StateTemplate<T>)
				{
					this@ModifierImpl shouldNot expired
					if (state in stateMap)
					{
						state.invoke(block)
					}
				}
			}
		}
		
		override val update = object : KotlmataMutableMachine.Modifier.Update
		{
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Update.By<T>
			{
				val stop = object : KotlmataMutableMachine.Modifier.Update.Or<T>
				{
					override fun or(block: StateTemplate<T>)
					{
						/* do nothing */
					}
				}
				
				val or = object : KotlmataMutableMachine.Modifier.Update.Or<T>
				{
					override fun or(block: StateTemplate<T>)
					{
						state.invoke(block)
					}
				}
				
				@Suppress("UNCHECKED_CAST")
				override fun by(block: KotlmataMutableState.Modifier.(T) -> Unit): KotlmataMutableMachine.Modifier.Update.Or<T>
				{
					this@ModifierImpl shouldNot expired
					return if (state in stateMap)
					{
						stateMap[state]!!.modify(block as KotlmataMutableState.Modifier.(STATE) -> Unit)
						stop
					}
					else
					{
						or
					}
				}
			}
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Update.RemAssign
			{
				override fun remAssign(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap.let {
						it[ruleLeft.state]
					}?.let {
						it[ruleLeft.signal]
					}?.let {
						ruleLeft %= state
					}
				}
			}
		}
		
		override val delete = object : KotlmataMutableMachine.Modifier.Delete
		{
			override fun state(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				stateMap -= state
			}
			
			override fun state(keyword: all)
			{
				this@ModifierImpl shouldNot expired
				stateMap.clear()
			}
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft)
			{
				this@ModifierImpl shouldNot expired
				ruleMap.let {
					it[ruleLeft.state]
				}?.let {
					it -= ruleLeft.signal
				}
			}
			
			override fun rule(keyword: of) = object : KotlmataMutableMachine.Modifier.Delete.State
			{
				override fun state(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap -= state
				}
			}
			
			override fun rule(keyword: all)
			{
				this@ModifierImpl shouldNot expired
				ruleMap.clear()
			}
		}
		
		override fun <S : STATE> S.invoke(block: StateTemplate<S>)
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState.create(this, logLevel, "$prefix$tab", block)
		}
		
		override fun <S : STATE> S.function(action: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState.create(this, logLevel, "$prefix$tab") {
				entry function action
			}
			return object : KotlmataState.Entry.Catch<SIGNAL>
			{
				override fun intercept(error: EntryErrorFunction<SIGNAL>)
				{
					this@ModifierImpl shouldNot expired
					stateMap[this@function]?.modify {
						entry function action intercept error
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(action: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signal function action
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(error: EntryErrorFunction<T>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal function action intercept error
						}
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: T) = object : KotlmataState.Entry.Action<T>
		{
			override fun function(action: EntryFunction<T>): KotlmataState.Entry.Catch<T>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signal function action
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun intercept(error: EntryErrorFunction<T>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal function action intercept error
						}
					}
				}
			}
		}
		
		override fun <S : STATE> S.via(signals: KotlmataState.Init.Signals) = object : KotlmataState.Entry.Action<SIGNAL>
		{
			override fun function(action: EntryFunction<SIGNAL>): KotlmataState.Entry.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signals function action
				}
				return object : KotlmataState.Entry.Catch<SIGNAL>
				{
					override fun intercept(error: EntryErrorFunction<SIGNAL>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signals function action intercept error
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Basic transition rules
		 *###################################################################################################################################*/
		
		override fun STATE.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun STATE.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun STATE.x(keyword: any) = ruleLeft(this, keyword)
		
		override fun any.x(signal: SIGNAL) = ruleLeft(this, signal)
		override fun any.x(signal: KClass<out SIGNAL>) = ruleLeft(this, signal)
		override fun any.x(keyword: any) = ruleLeft(this, keyword)
		
		private fun ruleLeft(from: STATE, signal: SIGNAL) = object : KotlmataMachine.RuleLeft
		{
			override val state: STATE = from
			override val signal: SIGNAL = signal
			
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				(ruleMap[from] ?: HashMap<SIGNAL, STATE>().also {
					ruleMap[from] = it
				})[signal] = state
			}
		}
		
		/*###################################################################################################################################
		 * 'AnyXX' transition rules
		 *###################################################################################################################################*/
		override fun any.of(vararg args: STATE_OR_SIGNAL): KotlmataMachine.RuleDefine.AnyOf = object : KotlmataMachine.RuleDefine.AnyOf, List<STATE_OR_SIGNAL> by listOf(*args)
		{
			/* empty */
		}
		
		override fun any.except(vararg args: STATE_OR_SIGNAL): KotlmataMachine.RuleDefine.AnyExcept = object : KotlmataMachine.RuleDefine.AnyExcept, List<STATE_OR_SIGNAL> by listOf(*args)
		{
			/* empty */
		}
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(signal: SIGNAL) = ruleAnyOfSignal(this, signal)
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(signal: KClass<out SIGNAL>) = ruleAnyOfSignal(this, signal)
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(keyword: any) = ruleAnyOfSignal(this, keyword)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(signal: SIGNAL) = ruleAnyExceptSignal(this, signal)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(signal: KClass<out SIGNAL>) = ruleAnyExceptSignal(this, signal)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(keyword: any) = ruleAnyExceptSignal(this, keyword)
		
		override fun STATE.x(anyOf: KotlmataMachine.RuleDefine.AnyOf) = ruleStateAnyOf(this, anyOf)
		
		override fun any.x(anyOf: KotlmataMachine.RuleDefine.AnyOf) = ruleStateAnyOf(this, anyOf)
		
		override fun STATE.x(anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = ruleStateAnyExcept(this, anyExcept)
		
		override fun any.x(anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = ruleStateAnyExcept(this, anyExcept)
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(anyOf: KotlmataMachine.RuleDefine.AnyOf) = ruleAnyOfAnyOf(this, anyOf)
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = ruleAnyOfAnyExcept(this, anyExcept)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(anyOf: KotlmataMachine.RuleDefine.AnyOf) = ruleAnyExceptAnyOf(this, anyOf)
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = ruleAnyExceptAnyExcept(this, anyExcept)
		
		private fun ruleAnyOfSignal(anyOf: KotlmataMachine.RuleDefine.AnyOf, signal: SIGNAL) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOf.forEach { from ->
					from x signal %= state
				}
			}
		}
		
		private fun ruleAnyExceptSignal(anyExcept: KotlmataMachine.RuleDefine.AnyExcept, signal: SIGNAL) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				any x signal %= state
				anyExcept.forEach { from ->
					ruleMap.let {
						it[from]
					}?.let {
						it[signal]
					} ?: run {
						from x signal %= stay
					}
				}
			}
		}
		
		private fun ruleStateAnyOf(from: STATE, anyOf: KotlmataMachine.RuleDefine.AnyOf) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOf.forEach { signal ->
					from x signal %= state
				}
			}
		}
		
		private fun ruleStateAnyExcept(from: STATE, anyExcept: KotlmataMachine.RuleDefine.AnyExcept) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				from x any %= state
				anyExcept.forEach { signal ->
					ruleMap.let {
						it[from]
					}?.let {
						it[signal]
					} ?: run {
						from x signal %= stay
					}
				}
			}
		}
		
		private fun ruleAnyOfAnyOf(anyOfState: KotlmataMachine.RuleDefine.AnyOf, anyOfSignal: KotlmataMachine.RuleDefine.AnyOf) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfState.forEach { from ->
					ruleStateAnyOf(from, anyOfSignal).remAssign(state)
				}
			}
		}
		
		private fun ruleAnyOfAnyExcept(anyOfState: KotlmataMachine.RuleDefine.AnyOf, anyExceptSignal: KotlmataMachine.RuleDefine.AnyExcept) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfState.forEach { from ->
					ruleStateAnyExcept(from, anyExceptSignal).remAssign(state)
				}
			}
		}
		
		private fun ruleAnyExceptAnyOf(anyExceptState: KotlmataMachine.RuleDefine.AnyExcept, anyOfSignal: KotlmataMachine.RuleDefine.AnyOf) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				anyOfSignal.forEach { signal ->
					ruleAnyExceptSignal(anyExceptState, signal).remAssign(state)
				}
			}
		}
		
		private fun ruleAnyExceptAnyExcept(anyExceptState: KotlmataMachine.RuleDefine.AnyExcept, anyExceptSignal: KotlmataMachine.RuleDefine.AnyExcept) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				ruleAnyExceptSignal(anyExceptState, any)
				ruleStateAnyExcept(any, anyExceptSignal)
			}
		}
		
		/*###################################################################################################################################
		 * Signals transition rule
		 *###################################################################################################################################*/
		override fun SIGNAL.or(signal: SIGNAL): KotlmataMachine.RuleDefine.Signals = object : KotlmataMachine.RuleDefine.Signals, MutableList<SIGNAL> by mutableListOf(this, signal)
		{
			override fun or(signal: SIGNAL): KotlmataMachine.RuleDefine.Signals
			{
				this@ModifierImpl shouldNot expired
				add(signal)
				return this
			}
		}
		
		override fun STATE.x(signals: KotlmataMachine.RuleDefine.Signals) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				signals.forEach { signal ->
					this@x x signal %= state
				}
			}
		}
		
		override fun any.x(signals: KotlmataMachine.RuleDefine.Signals) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				signals.forEach { signal ->
					this@x x signal %= state
				}
			}
		}
		
		override fun KotlmataMachine.RuleDefine.AnyOf.x(signals: KotlmataMachine.RuleDefine.Signals) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				forEach { from ->
					from x signals %= state
				}
			}
		}
		
		override fun KotlmataMachine.RuleDefine.AnyExcept.x(signals: KotlmataMachine.RuleDefine.Signals) = object : KotlmataMachine.RuleAssignable
		{
			override fun remAssign(state: STATE)
			{
				this@ModifierImpl shouldNot expired
				any x signals %= state
				forEach { from ->
					signals.forEach { signal ->
						ruleMap.let {
							it[from]
						}?.let {
							it[signal]
						} ?: run {
							from x signal %= stay
						}
					}
				}
			}
		}
		
		/*###################################################################################################################################
		 * Chaining transition rule
		 *###################################################################################################################################*/
		override val chain = object : KotlmataMachine.RuleDefine.Chain
		{
			val states: MutableList<STATE> = mutableListOf()
			
			override fun from(state: STATE): KotlmataMachine.RuleDefine.To
			{
				this@ModifierImpl shouldNot expired
				states.add(state)
				return object : KotlmataMachine.RuleDefine.To
				{
					override fun to(state: STATE): KotlmataMachine.RuleDefine.Via
					{
						this@ModifierImpl shouldNot expired
						states.add(state)
						return object : KotlmataMachine.RuleDefine.Via, KotlmataMachine.RuleDefine.To by this
						{
							override fun via(signal: SIGNAL)
							{
								this@ModifierImpl shouldNot expired
								done(signal)
							}
							
							override fun via(signal: KClass<out SIGNAL>)
							{
								this@ModifierImpl shouldNot expired
								done(signal)
							}
							
							override fun via(keyword: any)
							{
								this@ModifierImpl shouldNot expired
								done(keyword)
							}
							
							private fun done(signal: Any)
							{
								(1 until states.size).forEach { i ->
									val from = states[i - 1]
									val to = states[i]
									(ruleMap[from] ?: HashMap<SIGNAL, STATE>().also {
										ruleMap[from] = it
									})[signal] = to
								}
							}
						}
					}
				}
			}
		}
		
		init
		{
			init?.also { it(tag) } ?: modify?.also { it(tag) }
			expire()
		}
	}
}