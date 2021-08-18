package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataDaemon.Base
import kr.co.plasticcity.kotlmata.KotlmataDaemon.Companion.By
import kr.co.plasticcity.kotlmata.KotlmataDaemon.Companion.Extends
import kr.co.plasticcity.kotlmata.KotlmataDaemon.Init
import kr.co.plasticcity.kotlmata.KotlmataDaemonImpl.Request.*
import kr.co.plasticcity.kotlmata.KotlmataDaemonImpl.Request.Terminate.Type.*
import kr.co.plasticcity.kotlmata.Log.detail
import kr.co.plasticcity.kotlmata.Log.normal
import kr.co.plasticcity.kotlmata.Log.simple
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.reflect.KClass

interface KotlmataDaemon
{
	companion object
	{
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false,
			block: DaemonDefine
		): KotlmataDaemon = KotlmataDaemonImpl(name, logLevel, threadName, isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false
		) = object : Extends<KotlmataDaemon>
		{
			override fun extends(template: DaemonTemplate) = object : By<KotlmataDaemon>
			{
				override fun by(define: DaemonDefine) = invoke(name, logLevel, threadName, isDaemon) { daemon ->
					template(daemon)
					define(daemon)
				}
			}
			
			override fun extends(templates: DaemonTemplates) = object : By<KotlmataDaemon>
			{
				override fun by(define: DaemonDefine) = invoke(name, logLevel, threadName, isDaemon) { daemon ->
					templates.forEach { template ->
						template(daemon)
					}
					define(daemon)
				}
			}
			
			override fun by(define: DaemonDefine) = invoke(name, logLevel, threadName, isDaemon, define)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false,
			block: DaemonDefine
		) = lazy {
			invoke(name, logLevel, threadName, isDaemon, block)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false
		) = object : Extends<Lazy<KotlmataDaemon>>
		{
			override fun extends(template: DaemonTemplate) = object : By<Lazy<KotlmataDaemon>>
			{
				override fun by(define: DaemonDefine) = lazy {
					invoke(name, logLevel, threadName, isDaemon) extends template by define
				}
			}
			
			override fun extends(templates: DaemonTemplates) = object : By<Lazy<KotlmataDaemon>>
			{
				override fun by(define: DaemonDefine) = lazy {
					invoke(name, logLevel, threadName, isDaemon) extends templates by define
				}
			}
			
			override fun by(define: DaemonDefine) = lazy {
				invoke(name, logLevel, threadName, isDaemon, define)
			}
		}
		
		interface Extends<R> : By<R>
		{
			infix fun extends(template: DaemonTemplate): By<R>
			infix fun extends(templates: DaemonTemplates): By<R>
		}
		
		interface By<R>
		{
			infix fun by(define: DaemonDefine): R
		}
	}
	
	interface Base : KotlmataMachine.Base
	{
		override val on: On
		
		interface On : KotlmataMachine.Base.On
		{
			infix fun create(callback: DaemonSimpleCallback): Catch<DaemonSimpleCallback, DaemonSimpleFallback>
			infix fun start(callback: DaemonCallback): Catch<DaemonCallback, DaemonFallback>
			infix fun pause(callback: DaemonCallback): Catch<DaemonCallback, DaemonFallback>
			infix fun stop(callback: DaemonCallback): Catch<DaemonCallback, DaemonFallback>
			infix fun resume(callback: DaemonCallback): Catch<DaemonCallback, DaemonFallback>
			infix fun finish(callback: DaemonCallback): Catch<DaemonCallback, DaemonFallback>
			infix fun destroy(callback: DaemonSimpleCallback): Catch<DaemonSimpleCallback, DaemonSimpleFallback>
			infix fun fatal(block: MachineFallback)
		}
		
		interface Catch<in C, in F> : Finally<C>
		{
			infix fun catch(fallback: F): Finally<C>
		}
		
		interface Finally<in C>
		{
			infix fun finally(finally: C)
		}
	}
	
	interface Init : Base, KotlmataMachine.Init
	
	val name: String
	var isTerminated: Boolean
	
	fun run(payload: Any? = null)
	fun pause(payload: Any? = null)
	fun stop(payload: Any? = null)
	fun terminate(payload: Any? = null)
	
	/**
	 * @param priority Smaller means higher. Priority must be (priority >= 0). Default value is 0.
	 */
	fun input(signal: SIGNAL, payload: Any? = null, priority: Int = 0)
	
	/**
	 * @param priority Smaller means higher. Priority must be (priority >= 0). Default value is 0.
	 */
	fun <S : T, T : SIGNAL> input(signal: S, type: KClass<T>, payload: Any? = null, priority: Int = 0)
	
	@Deprecated("KClass<*> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<*>, payload: Any? = null, priority: Int = 0)
}

interface KotlmataMutableDaemon : KotlmataDaemon
{
	companion object
	{
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false,
			block: DaemonDefine
		): KotlmataMutableDaemon = KotlmataDaemonImpl(name, logLevel, threadName, isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false
		) = object : Extends<KotlmataMutableDaemon>
		{
			override fun extends(template: DaemonTemplate) = object : By<KotlmataMutableDaemon>
			{
				override fun by(define: DaemonDefine) = invoke(name, logLevel, threadName, isDaemon) { daemon ->
					template(daemon)
					define(daemon)
				}
			}
			
			override fun extends(templates: DaemonTemplates) = object : By<KotlmataMutableDaemon>
			{
				override fun by(define: DaemonDefine) = invoke(name, logLevel, threadName, isDaemon) { daemon ->
					templates.forEach { template ->
						template(daemon)
					}
					define(daemon)
				}
			}
			
			override fun by(define: DaemonDefine) = invoke(name, logLevel, threadName, isDaemon, define)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		@Suppress("unused")
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false,
			block: DaemonDefine
		) = lazy {
			invoke(name, logLevel, threadName, isDaemon, block)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		@Suppress("unused")
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false
		) = object : Extends<Lazy<KotlmataMutableDaemon>>
		{
			override fun extends(template: DaemonTemplate) = object : By<Lazy<KotlmataMutableDaemon>>
			{
				override fun by(define: DaemonDefine) = lazy {
					invoke(name, logLevel, threadName, isDaemon) extends template by define
				}
			}
			
			override fun extends(templates: DaemonTemplates) = object : By<Lazy<KotlmataMutableDaemon>>
			{
				override fun by(define: DaemonDefine) = lazy {
					invoke(name, logLevel, threadName, isDaemon) extends templates by define
				}
			}
			
			override fun by(define: DaemonDefine) = lazy {
				invoke(name, logLevel, threadName, isDaemon, define)
			}
		}
	}
	
	operator fun invoke(block: KotlmataMutableMachine.Update.() -> Unit) = update(block)
	infix fun update(block: KotlmataMutableMachine.Update.() -> Unit)
}

private class LifecycleCallback(
	val callback: DaemonCallback,
	val fallback: DaemonFallback? = null,
	val finally: DaemonCallback? = null
)

private class KotlmataDaemonImpl(
	override val name: String,
	val logLevel: Int,
	threadName: String,
	isDaemon: Boolean = false,
	block: DaemonDefine
) : KotlmataMutableDaemon
{
	private var onCreate: LifecycleCallback? = null
	private var onStart: LifecycleCallback? = null
	private var onPause: LifecycleCallback? = null
	private var onStop: LifecycleCallback? = null
	private var onResume: LifecycleCallback? = null
	private var onFinish: LifecycleCallback? = null
	private var onDestroy: LifecycleCallback? = null
	private var onError: MachineFallback? = null
	private var onFatal: MachineFallback? = null
	
	@Volatile
	private var queue: PriorityBlockingQueue<Request>? = PriorityBlockingQueue()
	
	@Volatile
	override var isTerminated: Boolean = false
	
	private fun LifecycleCallback.call(payload: Any? = null)
	{
		try
		{
			callback.also { callback ->
				PayloadActionReceiver(payload).callback()
			}
		}
		catch (e: Throwable)
		{
			fallback?.also { fallback ->
				PayloadErrorActionReceiver(payload, e).fallback()
			} ?: onError?.also { onError ->
				ErrorActionReceiver(e).onError()
			} ?: throw e
		}
		finally
		{
			finally?.also { finally ->
				PayloadActionReceiver(payload).finally()
			}
		}
	}
	
	init
	{
		val core = KotlmataMachine("$name@core") {
			lateinit var machine: KotlmataInternalMachine
			
			val logLevel = logLevel
			val suffix = if (logLevel >= DETAIL) tab else ""
			
			val update: InputAction<Update> = { updateR ->
				machine update updateR.block
			}
			
			val ignore: InputAction<Request> = {
				logLevel.detail(name) { DAEMON_IGNORE_REQUEST }
			}
			
			val postSync: (FunctionDSL.Return) -> Unit = {
				val syncR = Sync(it.signal, it.type, it.payload)
				queue!!.offer(syncR)
			}
			
			on transition { from, signal, to ->
				logLevel.detail(name, from, to) {
					when (to as String)
					{
						"Created", "Terminated", "Destroyed" ->
							if (signal is Terminate && signal.type == EXPLICIT)
								DAEMON_LIFECYCLE_CHANGED_TAB
							else
								DAEMON_LIFECYCLE_CHANGED
						else ->
							DAEMON_LIFECYCLE_CHANGED_TAB
					}
				}
			}
			
			"Nil" {
				input signal Create action {
					logLevel.detail(name, name) { DAEMON_START_CREATE }
					machine = KotlmataMutableMachine.create(name, logLevel, "Daemon[$name]:$suffix") {
						Initial_state_for_KotlmataDaemon { /* empty */ }
						InitImpl(this).use {
							it.block(this@KotlmataDaemonImpl)
							Initial_state_for_KotlmataDaemon x any %= it.startState
						}
						start at Initial_state_for_KotlmataDaemon
					} as KotlmataInternalMachine
				} finally {
					logLevel.detail(name) { DAEMON_END }
				}
			}
			"Created" {
				val onStart: InputAction<Control> = { controlR ->
					logLevel.simple(name, suffix, controlR.payload) { DAEMON_ON_START }
					onStart?.call(controlR.payload)
					machine.input(controlR.payload/* as? SIGNAL */ ?: Start_KotlmataDaemon, block = postSync)
				}
				
				entry action {
					logLevel.simple(name) { DAEMON_ON_CREATE }
					onCreate?.call()
				}
				input signal Run::class action onStart
				input signal Pause::class action onStart
				input signal Stop::class action onStart
				input signal Update::class action update
				input signal Sync::class action ignore
				input signal Input::class action ignore
				input signal TypedInput::class action ignore
			}
			"Running" {
				entry via Run::class action { runR ->
					when (prevState)
					{
						"Paused", "Stopped" ->
						{
							logLevel.simple(name, suffix, runR.payload) { DAEMON_ON_RESUME }
							onResume?.call(runR.payload)
						}
					}
				}
				input signal Run::class action ignore
				input signal Update::class action update
				input signal Sync::class action { syncR ->
					syncR.type?.also { type ->
						machine.input(syncR.signal, type, syncR.payload, block = postSync)
					} ?: machine.input(syncR.signal, syncR.payload, block = postSync)
				}
				input signal Input::class action { inputR ->
					machine.input(inputR.signal, inputR.payload, block = postSync)
				}
				input signal TypedInput::class action { typedR ->
					machine.input(typedR.signal, typedR.type, typedR.payload, block = postSync)
				}
			}
			"Paused" {
				var sync: Sync? = null
				val stash: MutableList<Request> = mutableListOf()
				val keep: InputAction<Request> = { request ->
					stash += request
					logLevel.detail(name) { DAEMON_KEEP_REQUEST }
				}
				
				entry via Pause::class action { pauseR ->
					logLevel.simple(name, suffix, pauseR.payload) { DAEMON_ON_PAUSE }
					onPause?.call(pauseR.payload)
				}
				input signal Run::class action {
					sync?.also { syncR -> queue!!.offer(syncR) }
					queue!! += stash
				}
				input signal Pause::class action ignore
				input signal Stop::class action {
					sync?.also { syncR -> queue!!.offer(syncR) }
				}
				input signal Update::class action update
				input signal Sync::class action { syncR ->
					sync = syncR
					logLevel.detail(name) { DAEMON_KEEP_REQUEST }
				}
				input signal Input::class action keep
				input signal TypedInput::class action keep
				exit action {
					sync = null
					stash.clear()
				}
			}
			"Stopped" {
				fun PriorityBlockingQueue<Request>.deleteIf(test: (Request) -> Boolean)
				{
					val iterator = iterator()
					while (iterator.hasNext())
					{
						if (test(iterator.next()))
						{
							iterator.remove()
						}
					}
				}
				
				var sync: Sync? = null
				val cleanup: InputAction<Request> = { currentR ->
					queue!!.deleteIf { queueR ->
						queueR.isInput && queueR.olderThan(currentR)
					}
					sync?.also { syncR -> queue!!.offer(syncR) }
				}
				
				entry via Stop::class action { stopR ->
					logLevel.simple(name, suffix, stopR.payload) { DAEMON_ON_STOP }
					onStop?.call(stopR.payload)
				}
				input signal Run::class action { runR ->
					cleanup(runR)
				}
				input signal Pause::class action cleanup
				input signal Stop::class action ignore
				input signal Update::class action update
				input signal Sync::class action { syncR ->
					sync = syncR
					logLevel.detail(name) { DAEMON_KEEP_REQUEST }
				}
				input signal Input::class action ignore
				input signal TypedInput::class action ignore
				exit action {
					sync = null
				}
			}
			"Terminated" {
				entry via Terminate::class action { terminateR ->
					isTerminated = true
					if (terminateR.type == EXPLICIT)
					{
						Thread.currentThread().interrupt()
					}
					else
					{
						Log.w(name, terminateR.type) { DAEMON_UNINTENDED_TERMINATION }
					}
					prevState.ifOneOf("Running", "Paused", "Stopped") {
						try
						{
							if (terminateR.type != EXPLICIT)
							{
								logLevel.detail(name, terminateR.type) { DAEMON_TERMINATE }
							}
							machine.release()
						}
						finally
						{
							try
							{
								logLevel.simple(name, suffix, terminateR.payload) { DAEMON_ON_FINISH }
								onFinish?.call(terminateR.payload)
							}
							finally
							{
								if (terminateR.type != EXPLICIT)
								{
									logLevel.detail(name) { DAEMON_END }
								}
							}
						}
					}
				}
			}
			"Destroyed" {
				entry action {
					onCreate = null
					onStart = null
					onPause = null
					onStop = null
					onResume = null
					onFinish = null
					onError = null
					onFatal = null
					queue = null
					isTerminated = true
					if (prevState == "Terminated")
					{
						logLevel.simple(name) { DAEMON_ON_DESTROY }
						onDestroy?.call()
					}
				} finally {
					onDestroy = null
					logLevel.simple(name, threadName, isDaemon) { DAEMON_TERMINATE_THREAD }
				}
			}
			
			"Nil" x Create %= "Created"
			
			"Created" x Run::class %= "Running"
			"Created" x Pause::class %= "Paused"
			"Created" x Stop::class %= "Stopped"
			
			"Running" x Pause::class %= "Paused"
			"Running" x Stop::class %= "Stopped"
			
			"Paused" x Run::class %= "Running"
			"Paused" x Stop::class %= "Stopped"
			
			"Stopped" x Run::class %= "Running"
			"Stopped" x Pause::class %= "Paused"
			
			any.except("Nil", "Terminated", "Destroyed") x Terminate::class %= "Terminated"
			("Nil" AND "Terminated") x Destroy %= "Destroyed"
			
			start at "Nil"
		}
		
		thread(name = threadName, isDaemon = isDaemon, start = true) {
			val queue = queue!!
			val logLevel = logLevel
			val name = name
			var start = 0L
			
			try
			{
				logLevel.simple(name, threadName, isDaemon) { DAEMON_START_THREAD }
				core.input(Create)
				while (true)
				{
					queue.take().also { request ->
						try
						{
							logLevel.detail(name, request, queue.size) {
								start = System.currentTimeMillis()
								DAEMON_START_REQUEST
							}
							core.input(request)
						}
						finally
						{
							logLevel.detail(name) {
								DAEMON_END + " ${System.currentTimeMillis() - start}ms"
							}
						}
					}
				}
			}
			catch (e: InterruptedException)
			{
				core.input(Terminate(null, INTERRUPTED))
			}
			catch (e: Throwable)
			{
				Log.w(name, e) { DAEMON_FATAL_ERROR }
				if (!isTerminated)
				{
					try
					{
						onFatal?.also { onFatal ->
							ErrorActionReceiver(e).onFatal()
						} ?: throw e
					}
					finally
					{
						core.input(Terminate(null, ERROR))
					}
				}
			}
			finally
			{
				core.input(Destroy)
			}
		}
	}
	
	override fun run(payload: Any?)
	{
		val runR = Run(payload)
		queue?.offer(runR)
	}
	
	override fun pause(payload: Any?)
	{
		val pauseR = Pause(payload)
		queue?.offer(pauseR)
	}
	
	override fun stop(payload: Any?)
	{
		val stopR = Stop(payload)
		queue?.offer(stopR)
	}
	
	override fun terminate(payload: Any?)
	{
		val terminateR = Terminate(payload, EXPLICIT)
		queue?.offer(terminateR)
	}
	
	override fun input(signal: SIGNAL, payload: Any?, priority: Int)
	{
		val inputR = Input(signal, payload, priority)
		queue?.offer(inputR)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <S : T, T : SIGNAL> input(signal: S, type: KClass<T>, payload: Any?, priority: Int)
	{
		val typedR = TypedInput(signal, type as KClass<SIGNAL>, payload, priority)
		queue?.offer(typedR)
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<*>, payload: Any?, priority: Int)
	{
		throw IllegalArgumentException("KClass<*> type cannot be used as input.")
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun update(block: KotlmataMutableMachine.Update.() -> Unit)
	{
		val updateR = Update(block)
		queue?.offer(updateR)
	}
	
	override fun toString(): String
	{
		return "KotlmataDaemon[$name]{${hashCode().toString(16)}}"
	}
	
	private inner class InitImpl(
		init: KotlmataMachine.Init
	) : Init, KotlmataMachine.Init by init, Expirable({ Log.e("Daemon[$name]:") { EXPIRED_OBJECT } })
	{
		lateinit var startState: STATE
		
		override val on = object : Base.On
		{
			private fun setLifecycle(callback: DaemonCallback, set: (LifecycleCallback) -> Unit) = object : Base.Catch<DaemonCallback, DaemonFallback>
			{
				init
				{
					this@InitImpl shouldNot expired
					set(LifecycleCallback(callback))
				}
				
				override fun catch(fallback: DaemonFallback) = object : Base.Finally<DaemonCallback>
				{
					init
					{
						this@InitImpl shouldNot expired
						set(LifecycleCallback(callback, fallback))
					}
					
					override fun finally(finally: DaemonCallback)
					{
						this@InitImpl shouldNot expired
						set(LifecycleCallback(callback, fallback, finally))
					}
				}
				
				override fun finally(finally: DaemonCallback)
				{
					this@InitImpl shouldNot expired
					set(LifecycleCallback(callback, null, finally))
				}
			}
			
			private val suffix = if (logLevel >= DETAIL) tab else ""
			
			override fun create(callback: DaemonSimpleCallback): Base.Catch<DaemonSimpleCallback, DaemonSimpleFallback>
			{
				logLevel.normal(name, suffix) { DAEMON_SET_ON_CREATE }
				return setLifecycle(callback) { onCreate = it }
			}
			
			override fun start(callback: DaemonCallback): Base.Catch<DaemonCallback, DaemonFallback>
			{
				logLevel.normal(name, suffix) { DAEMON_SET_ON_START }
				return setLifecycle(callback) { onStart = it }
			}
			
			override fun pause(callback: DaemonCallback): Base.Catch<DaemonCallback, DaemonFallback>
			{
				logLevel.normal(name, suffix) { DAEMON_SET_ON_PAUSE }
				return setLifecycle(callback) { onPause = it }
			}
			
			override fun stop(callback: DaemonCallback): Base.Catch<DaemonCallback, DaemonFallback>
			{
				logLevel.normal(name, suffix) { DAEMON_SET_ON_STOP }
				return setLifecycle(callback) { onStop = it }
			}
			
			override fun resume(callback: DaemonCallback): Base.Catch<DaemonCallback, DaemonFallback>
			{
				logLevel.normal(name, suffix) { DAEMON_SET_ON_RESUME }
				return setLifecycle(callback) { onResume = it }
			}
			
			override fun finish(callback: DaemonCallback): Base.Catch<DaemonCallback, DaemonFallback>
			{
				logLevel.normal(name, suffix) { DAEMON_SET_ON_FINISH }
				return setLifecycle(callback) { onFinish = it }
			}
			
			override fun destroy(callback: DaemonSimpleCallback): Base.Catch<DaemonSimpleCallback, DaemonSimpleFallback>
			{
				logLevel.normal(name, suffix) { DAEMON_SET_ON_DESTROY }
				return setLifecycle(callback) { onDestroy = it }
			}
			
			override fun fatal(block: MachineFallback)
			{
				this@InitImpl shouldNot expired
				logLevel.normal(name, suffix) { DAEMON_SET_ON_FATAL }
				onFatal = block
			}
			
			override fun transition(callback: TransitionCallback) = object : KotlmataMachine.Base.Catch
			{
				val transition = run {
					this@InitImpl shouldNot expired
					init.on transition callback
				}
				
				override fun catch(fallback: TransitionFallback) = object : KotlmataMachine.Base.Finally
				{
					val catch = run {
						this@InitImpl shouldNot expired
						transition catch fallback
					}
					
					override fun finally(finally: TransitionCallback)
					{
						this@InitImpl shouldNot expired
						catch finally finally
					}
				}
				
				override fun finally(finally: TransitionCallback)
				{
					this@InitImpl shouldNot expired
					transition finally finally
				}
			}
			
			override fun error(block: MachineFallback)
			{
				this@InitImpl shouldNot expired
				onError = block
				init.on error block
			}
		}
		
		override val start = object : KotlmataMachine.Init.Start
		{
			override fun at(state: STATE): KotlmataMachine.Init.End
			{
				this@InitImpl shouldNot expired
				
				/* For checking undefined initial state. */
				init.start at state
				
				startState = state
				return KotlmataMachine.Init.End()
			}
		}
	}
	
	private object Create
	private object Destroy
	
	private sealed class Request(val priority: Int, val desc: String) : Comparable<Request>
	{
		constructor(basePriority: Int, subPriority: Int, desc: String) : this(
			priority = if (subPriority > 0) basePriority + subPriority else basePriority,
			desc = desc
		)
		
		open class Control(val payload: Any?, desc: String) : Request(DAEMON_CONTROL, desc)
		class Run(payload: Any?) : Control(payload, "run")
		class Pause(payload: Any?) : Control(payload, "pause")
		class Stop(payload: Any?) : Control(payload, "stop")
		class Terminate(payload: Any?, val type: Type) : Control(payload, "terminate")
		{
			enum class Type
			{
				EXPLICIT,
				INTERRUPTED,
				ERROR
			}
		}
		
		class Update(val block: KotlmataMutableMachine.Update.() -> Unit) : Request(UPDATE, "update")
		
		class Sync(val signal: SIGNAL, val type: KClass<SIGNAL>?, val payload: Any?) : Request(SYNC, "input")
		
		class Input(val signal: SIGNAL, val payload: Any?, priority: Int) : Request(INPUT, priority, "input")
		class TypedInput(val signal: SIGNAL, val type: KClass<SIGNAL>, val payload: Any?, priority: Int) : Request(INPUT, priority, "input")
		
		companion object
		{
			private const val DAEMON_CONTROL = -3
			private const val UPDATE = -2
			private const val SYNC = -1
			private const val INPUT = 0
			
			val ticket: AtomicLong = AtomicLong(0)
		}
		
		val order = ticket.getAndIncrement()
		val isInput = priority >= INPUT
		
		fun olderThan(other: Request): Boolean = order < other.order
		
		override fun compareTo(other: Request): Int
		{
			val dP = priority - other.priority
			return if (dP != 0) dP
			else (order - other.order).toInt()
		}
		
		override fun toString(): String = desc
	}
}
