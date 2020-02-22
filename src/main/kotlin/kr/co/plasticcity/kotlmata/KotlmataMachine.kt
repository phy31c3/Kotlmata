package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface KotlmataMachine<T : MACHINE>
{
	companion object
	{
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG,
				block: KotlmataMachineDef<String>
		): KotlmataMachine<String> = KotlmataMachineImpl(name, logLevel, block = block)
		
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG
		) = object : ExtendsInvoke
		{
			override fun extends(block: KotlmataMachineDef<String>) = invoke(name, logLevel, block)
		}
		
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG,
				block: KotlmataMachineDef<String>
		) = lazy {
			invoke(name, logLevel, block)
		}
		
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG
		) = object : ExtendsLazy
		{
			override fun extends(block: KotlmataMachineDef<String>) = lazy { invoke(name, logLevel, block) }
		}
		
		interface ExtendsInvoke
		{
			infix fun extends(block: KotlmataMachineDef<String>): KotlmataMachine<String>
		}
		
		interface ExtendsLazy
		{
			infix fun extends(block: KotlmataMachineDef<String>): Lazy<KotlmataMachine<String>>
		}
		
		internal fun create(
				name: String,
				block: KotlmataMachineDef<String>
		): KotlmataMachine<String> = KotlmataMachineImpl(name, block = block)
	}
	
	@KotlmataMarker
	interface Init : StateDefine, RuleDefine
	{
		val on: On
		val start: Start
		
		interface On
		{
			infix fun error(block: KotlmataError)
		}
		
		interface Start
		{
			infix fun at(state: STATE): End
		}
		
		class End internal constructor()
	}
	
	interface StateDefine
	{
		operator fun <S : STATE> S.invoke(block: KotlmataStateDef<S>)
		infix fun <S : STATE> S.extends(block: KotlmataStateDef<S>) = invoke(block)
		
		infix fun <S : STATE, R> S.action(action: KotlmataActionR<R>): KotlmataState.Entry.Catch<SIGNAL>
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
	}
	
	interface RuleLeft : RuleAssignable
	{
		val state: STATE
		val signal: SIGNAL
	}
	
	val key: T
	
	fun input(signal: SIGNAL, payload: Any? = null)
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null)
	
	/**
	 * @param block Called if the state is switched and the next state's entry function returns a signal.
	 */
	fun input(signal: SIGNAL, payload: Any? = null, block: (KotlmataDSL.Sync) -> Unit)
	
	/**
	 * @param block Called if the state is switched and the next state's entry function returns a signal.
	 */
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null, block: (KotlmataDSL.Sync) -> Unit)
	
	@Deprecated("KClass<T> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<out Any>, payload: Any? = null)
	
	@Deprecated("KClass<T> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<out Any>, payload: Any? = null, block: (KotlmataDSL.Sync) -> Unit)
}

interface KotlmataMutableMachine<T : MACHINE> : KotlmataMachine<T>
{
	companion object
	{
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG,
				block: KotlmataMachineDef<String>
		): KotlmataMutableMachine<String> = KotlmataMachineImpl(name, logLevel, block = block)
		
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG
		) = object : ExtendsInvoke
		{
			override fun extends(block: KotlmataMachineDef<String>) = invoke(name, logLevel, block)
		}
		
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG,
				block: KotlmataMachineDef<String>
		) = lazy {
			invoke(name, logLevel, block)
		}
		
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG
		) = object : ExtendsLazy
		{
			override fun extends(block: KotlmataMachineDef<String>) = lazy { invoke(name, logLevel, block) }
		}
		
		interface ExtendsInvoke
		{
			infix fun extends(block: KotlmataMachineDef<String>): KotlmataMutableMachine<String>
		}
		
		interface ExtendsLazy
		{
			infix fun extends(block: KotlmataMachineDef<String>): Lazy<KotlmataMutableMachine<String>>
		}
		
		internal fun <T : MACHINE> create(
				key: T,
				logLevel: Int,
				prefix: String,
				block: KotlmataMachineDef<T>
		): KotlmataMutableMachine<T> = KotlmataMachineImpl(key, logLevel, prefix, block)
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
			infix fun state(state: STATE): then
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): then
			
			interface then
			{
				infix fun then(block: () -> Unit): or
			}
			
			interface or
			{
				infix fun or(block: () -> Unit)
			}
		}
		
		interface Insert
		{
			infix fun <T : STATE> state(state: T): with<T>
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): remAssign
			infix fun or(keyword: Replace): state
			infix fun or(keyword: Update): rule
			
			interface state
			{
				infix fun <T : STATE> state(state: T): with<T>
			}
			
			interface rule
			{
				infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): remAssign
			}
			
			interface with<T : STATE>
			{
				infix fun with(block: KotlmataStateDef<T>)
			}
			
			interface remAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Replace
		{
			infix fun <T : STATE> state(state: T): with<T>
			
			interface with<T : STATE>
			{
				infix fun with(block: KotlmataStateDef<T>)
			}
		}
		
		interface Update
		{
			infix fun <T : STATE> state(state: T): with<T>
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft): remAssign
			
			interface with<T : STATE>
			{
				infix fun with(block: KotlmataMutableState.Modifier.(state: T) -> Unit): or<T>
			}
			
			interface or<T : STATE>
			{
				infix fun or(block: KotlmataStateDef<T>)
			}
			
			interface remAssign
			{
				operator fun remAssign(state: STATE)
			}
		}
		
		interface Delete
		{
			infix fun state(state: STATE)
			infix fun state(keyword: all)
			
			infix fun rule(ruleLeft: KotlmataMachine.RuleLeft)
			infix fun rule(keyword: of): state
			infix fun rule(keyword: all)
			
			interface state
			{
				infix fun state(state: STATE)
			}
		}
	}
	
	operator fun invoke(block: Modifier.(machine: T) -> Unit) = modify(block)
	
	infix fun modify(block: Modifier.(machine: T) -> Unit)
}

private class KotlmataMachineImpl<T : MACHINE>(
		override val key: T,
		val logLevel: Int = NO_LOG,
		val prefix: String = "Machine[$key]:",
		block: KotlmataMachine.Init.(T) -> KotlmataMachine.Init.End
) : KotlmataMutableMachine<T>
{
	private val stateMap: MutableMap<STATE, KotlmataMutableState<out STATE>> = HashMap()
	private val ruleMap: MutableMap<STATE, MutableMap<SIGNAL, STATE>> = HashMap()
	
	private var onError: KotlmataError? = null
	
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
			DSL.onError(e)
		} ?: throw e
		null
	}
	
	override fun input(signal: SIGNAL, payload: Any?)
	{
		defaultInput(KotlmataDSL.Sync(signal), payload)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?)
	{
		defaultInput(KotlmataDSL.Sync(signal, type as KClass<SIGNAL>), payload)
	}
	
	private fun defaultInput(begin: KotlmataDSL.Sync, payload: Any?)
	{
		var next: KotlmataDSL.Sync? = begin
		while (next != null) next.also {
			next = null
			if (it.type == null) input(it.signal, payload) { sync ->
				next = sync
			}
			else input(it.signal, it.type, payload) { sync ->
				next = sync
			}
		}
	}
	
	override fun input(signal: SIGNAL, payload: Any?, block: (KotlmataDSL.Sync) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[signal] ?: this[signal::class] ?: this[any]
		}
		
		tryCatchReturn {
			logLevel.normal(prefix, signal, payload, current.key) { MACHINE_START_INPUT }
			current.input(signal, payload)
		}.also {
			logLevel.normal(prefix, signal, payload, current.key) { MACHINE_END_INPUT }
		}.convertToSync()?.also { sync ->
			block(sync)
		} ?: ruleMap.let {
			it[current.key]?.next() ?: it[any]?.next()
		}?.let {
			when (it)
			{
				is stay ->
				{
					null
				}
				!in stateMap ->
				{
					Log.w(prefix.trimEnd(), current.key, signal, it) { TRANSITION_FAILED }
					null
				}
				else ->
				{
					stateMap[it]
				}
			}
		}?.let { next ->
			logLevel.simple(prefix, current.key, signal, next.key) { MACHINE_START_TRANSITION }
			tryCatchReturn { current.exit(signal) }
			current = next
			tryCatchReturn { current.entry(signal) }.convertToSync()?.also(block)
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
	}
	
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?, block: (KotlmataDSL.Sync) -> Unit)
	{
		fun MutableMap<SIGNAL, STATE>.next(): STATE?
		{
			return this[type] ?: this[any]
		}
		
		tryCatchReturn {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", payload, current.key) { MACHINE_START_TYPED_INPUT }
			current.input(signal, type, payload)
		}.also {
			logLevel.normal(prefix, signal, "${type.simpleName}::class", payload, current.key) { MACHINE_END_TYPED_INPUT }
		}.convertToSync()?.also { sync ->
			block(sync)
		} ?: ruleMap.let {
			it[current.key]?.next() ?: it[any]?.next()
		}?.let {
			when (it)
			{
				is stay ->
				{
					null
				}
				!in stateMap ->
				{
					Log.w(prefix.trimEnd(), current.key, "${type.simpleName}::class", it) { TRANSITION_FAILED }
					null
				}
				else ->
				{
					stateMap[it]
				}
			}
		}?.let { next ->
			logLevel.simple(prefix, current.key, "${type.simpleName}::class", next.key) { MACHINE_START_TRANSITION }
			tryCatchReturn { current.exit(signal) }
			current = next
			tryCatchReturn { current.entry(signal) }.convertToSync()?.also(block)
			logLevel.normal(prefix) { MACHINE_END_TRANSITION }
		}
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<out Any>, payload: Any?)
	{
		throw IllegalArgumentException("KClass<T> type cannot be used as input.")
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<out Any>, payload: Any?, block: (KotlmataDSL.Sync) -> Unit)
	{
		throw IllegalArgumentException("KClass<T> type cannot be used as input.")
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.(T) -> Unit)
	{
		logLevel.normal(prefix, current.key) { MACHINE_START_MODIFY }
		ModifierImpl(modify = block)
		logLevel.normal(prefix, current.key) { MACHINE_END_MODIFY }
	}
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
	
	private inner class ModifierImpl internal constructor(
			init: (KotlmataMachine.Init.(T) -> KotlmataMachine.Init.End)? = null,
			modify: (KotlmataMutableMachine.Modifier.(T) -> Unit)? = null
	) : KotlmataMachine.Init, KotlmataMutableMachine.Modifier, Expirable({ Log.e(prefix.trimEnd()) { EXPIRED_MODIFIER } })
	{
		override val on = object : KotlmataMachine.Init.On
		{
			override fun error(block: KotlmataError)
			{
				this@ModifierImpl shouldNot expired
				onError = block
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
				return this@KotlmataMachineImpl.current.key.takeIf {
					it != Initial
				} ?: Log.w(prefix.trimEnd()) { OBTAIN_INITIAL }
			}
		
		override val has = object : KotlmataMutableMachine.Modifier.Has
		{
			val stop = object : KotlmataMutableMachine.Modifier.Has.or
			{
				override fun or(block: () -> Unit)
				{
					/* do nothing */
				}
			}
			
			val or = object : KotlmataMutableMachine.Modifier.Has.or
			{
				override fun or(block: () -> Unit)
				{
					block()
				}
			}
			
			override fun state(state: STATE) = object : KotlmataMutableMachine.Modifier.Has.then
			{
				override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.or
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
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Has.then
			{
				override fun then(block: () -> Unit): KotlmataMutableMachine.Modifier.Has.or
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
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Insert.with<T>
			{
				override fun with(block: KotlmataState.Init.(T) -> Unit)
				{
					this@ModifierImpl shouldNot expired
					if (state !in stateMap)
					{
						state.invoke(block)
					}
				}
			}
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Insert.remAssign
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
			
			override fun or(keyword: KotlmataMutableMachine.Modifier.Replace) = object : KotlmataMutableMachine.Modifier.Insert.state
			{
				override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Insert.with<T>
				{
					override fun with(block: KotlmataState.Init.(T) -> Unit)
					{
						this@ModifierImpl shouldNot expired
						state.invoke(block)
					}
				}
			}
			
			override fun or(keyword: KotlmataMutableMachine.Modifier.Update) = object : KotlmataMutableMachine.Modifier.Insert.rule
			{
				override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Insert.remAssign
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
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Replace.with<T>
			{
				override fun with(block: KotlmataState.Init.(T) -> Unit)
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
			override fun <T : STATE> state(state: T) = object : KotlmataMutableMachine.Modifier.Update.with<T>
			{
				val stop = object : KotlmataMutableMachine.Modifier.Update.or<T>
				{
					override fun or(block: KotlmataState.Init.(T) -> Unit)
					{
						/* do nothing */
					}
				}
				
				val or = object : KotlmataMutableMachine.Modifier.Update.or<T>
				{
					override fun or(block: KotlmataState.Init.(T) -> Unit)
					{
						state.invoke(block)
					}
				}
				
				@Suppress("UNCHECKED_CAST")
				override fun with(block: KotlmataMutableState.Modifier.(T) -> Unit): KotlmataMutableMachine.Modifier.Update.or<T>
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
			
			override fun rule(ruleLeft: KotlmataMachine.RuleLeft) = object : KotlmataMutableMachine.Modifier.Update.remAssign
			{
				override fun remAssign(state: STATE)
				{
					this@ModifierImpl shouldNot expired
					ruleMap.let {
						it[ruleLeft.state]
					}?.let {
						it[ruleLeft.signal]
					}.let {
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
			
			override fun rule(keyword: of) = object : KotlmataMutableMachine.Modifier.Delete.state
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
		
		override fun <S : STATE> S.invoke(block: KotlmataStateDef<S>)
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState.create(this, logLevel, "$prefix$tab", block)
		}
		
		override fun <S : STATE, R> S.action(action: KotlmataActionR<R>): KotlmataState.Entry.Catch<SIGNAL>
		{
			this@ModifierImpl shouldNot expired
			stateMap[this] = KotlmataMutableState.create(this, logLevel, "$prefix$tab") {
				entry action action
			}
			return object : KotlmataState.Entry.Catch<SIGNAL>
			{
				override fun <R> catch(error: KotlmataErrorR<R>)
				{
					this@ModifierImpl shouldNot expired
					stateMap[this@action]?.modify {
						entry action action catch error
					}
				}
				
				override fun <R> catch(error: KotlmataError1R<SIGNAL, R>)
				{
					this@ModifierImpl shouldNot expired
					stateMap[this@action]?.modify {
						entry action action catch error
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: KClass<T>) = object : KotlmataState.Entry.Action<T>
		{
			override fun <R> action(action: KotlmataAction1R<T, R>): KotlmataState.Entry.Catch<T>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signal action action
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun <R> catch(error: KotlmataErrorR<R>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal action action catch error
						}
					}
					
					override fun <R> catch(error: KotlmataError1R<T, R>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal action action catch error
						}
					}
				}
			}
		}
		
		override fun <S : STATE, T : SIGNAL> S.via(signal: T) = object : KotlmataState.Entry.Action<T>
		{
			override fun <R> action(action: KotlmataAction1R<T, R>): KotlmataState.Entry.Catch<T>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signal action action
				}
				return object : KotlmataState.Entry.Catch<T>
				{
					override fun <R> catch(error: KotlmataErrorR<R>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal action action catch error
						}
					}
					
					override fun <R> catch(error: KotlmataError1R<T, R>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signal action action catch error
						}
					}
				}
			}
		}
		
		override fun <S : STATE> S.via(signals: KotlmataState.Init.Signals) = object : KotlmataState.Entry.Action<SIGNAL>
		{
			override fun <R> action(action: KotlmataActionR<R>): KotlmataState.Entry.Catch<SIGNAL>
			{
				this@ModifierImpl shouldNot expired
				stateMap[this@via] = KotlmataMutableState.create(this@via, logLevel, "$prefix$tab") {
					entry via signals action action
				}
				return object : KotlmataState.Entry.Catch<SIGNAL>
				{
					override fun <R> catch(error: KotlmataErrorR<R>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signals action action catch error
						}
					}
					
					override fun <R> catch(error: KotlmataError1R<SIGNAL, R>)
					{
						this@ModifierImpl shouldNot expired
						stateMap[this@via]?.modify {
							entry via signals action action catch error
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
					this x signal %= state
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
			init?.let { it(key) } ?: modify?.let { it(key) }
			expire()
		}
	}
}