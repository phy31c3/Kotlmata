@file:Suppress("unused")

package kr.co.plasticcity.kotlmata

import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

interface KotlmataDaemon<T : DAEMON>
{
	companion object
	{
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun <T : DAEMON> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			threadName: String? = null,
			isDaemon: Boolean = false,
			block: DaemonTemplate<T>
		): KotlmataDaemon<T> = KotlmataDaemonImpl(tag, logLevel, threadName ?: "thread-KotlmataDaemon[$tag]", isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun <T : DAEMON> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			threadName: String? = null,
			isDaemon: Boolean = false
		) = object : InvokeBy<T>
		{
			override fun by(block: DaemonTemplate<T>) = invoke(tag, logLevel, threadName, isDaemon, block)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun <T : DAEMON> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			threadName: String? = null,
			isDaemon: Boolean = false,
			block: DaemonTemplate<T>
		) = lazy {
			invoke(tag, logLevel, threadName, isDaemon, block)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun <T : DAEMON> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			threadName: String? = null,
			isDaemon: Boolean = false
		) = object : LazyBy<T>
		{
			override fun by(block: DaemonTemplate<T>) = lazy { invoke(tag, logLevel, threadName, isDaemon, block) }
		}
		
		interface InvokeBy<T : DAEMON>
		{
			infix fun by(block: DaemonTemplate<T>): KotlmataDaemon<T>
		}
		
		interface LazyBy<T : DAEMON>
		{
			infix fun by(block: DaemonTemplate<T>): Lazy<KotlmataDaemon<T>>
		}
	}
	
	@KotlmataMarker
	interface Init : KotlmataMachine.Init
	{
		override val on: On
		
		interface On : KotlmataMachine.Init.On
		{
			infix fun create(block: DaemonCallback): Catch
			infix fun start(block: DaemonCallback): Catch
			infix fun pause(block: DaemonCallback): Catch
			infix fun stop(block: DaemonCallback): Catch
			infix fun resume(block: DaemonCallback): Catch
			infix fun terminate(block: DaemonCallback): Catch
			infix fun destroy(block: DaemonCallback): Catch
		}
		
		interface Catch
		{
			infix fun catch(error: DaemonFallback)
		}
	}
	
	val tag: T
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
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null, priority: Int = 0)
	
	@Deprecated("KClass<T> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<out Any>, payload: Any? = null, priority: Int = 0)
}

interface KotlmataMutableDaemon<T : DAEMON> : KotlmataDaemon<T>
{
	companion object
	{
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun <T : DAEMON> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			threadName: String? = null,
			isDaemon: Boolean = false,
			block: DaemonTemplate<T>
		): KotlmataMutableDaemon<T> = KotlmataDaemonImpl(tag, logLevel, threadName ?: "thread-KotlmataDaemon[$tag]", isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun <T : DAEMON> invoke(
			tag: T,
			logLevel: Int = NO_LOG,
			threadName: String? = null,
			isDaemon: Boolean = false
		) = object : InvokeBy<T>
		{
			override fun by(block: DaemonTemplate<T>) = invoke(tag, logLevel, threadName, isDaemon, block)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		@Suppress("unused")
		fun <T : DAEMON> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			threadName: String? = null,
			isDaemon: Boolean = false,
			block: DaemonTemplate<T>
		) = lazy {
			invoke(tag, logLevel, threadName, isDaemon, block)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		@Suppress("unused")
		fun <T : DAEMON> lazy(
			tag: T,
			logLevel: Int = NO_LOG,
			threadName: String? = null,
			isDaemon: Boolean = false
		) = object : LazyBy<T>
		{
			override fun by(block: DaemonTemplate<T>) = lazy { invoke(tag, logLevel, threadName, isDaemon, block) }
		}
		
		interface InvokeBy<T : DAEMON>
		{
			infix fun by(block: DaemonTemplate<T>): KotlmataMutableDaemon<T>
		}
		
		interface LazyBy<T : DAEMON>
		{
			infix fun by(block: DaemonTemplate<T>): Lazy<KotlmataMutableDaemon<T>>
		}
	}
	
	operator fun invoke(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit) = modify(block)
	
	infix fun modify(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
}

private class LifecycleDef(val callback: DaemonCallback? = null, val fallback: DaemonFallback? = null)

private class KotlmataDaemonImpl<T : DAEMON>(
	override val tag: T,
	val logLevel: Int = NO_LOG,
	threadName: String = "thread-KotlmataDaemon[$tag]",
	isDaemon: Boolean = false,
	block: DaemonTemplate<T>
) : KotlmataMutableDaemon<T>
{
	private val core: KotlmataMachine<String>
	
	private var onCreate: LifecycleDef = LifecycleDef()
	private var onStart: LifecycleDef = LifecycleDef()
	private var onPause: LifecycleDef = LifecycleDef()
	private var onStop: LifecycleDef = LifecycleDef()
	private var onResume: LifecycleDef = LifecycleDef()
	private var onTerminate: LifecycleDef = LifecycleDef()
	private var onDestroy: LifecycleDef = LifecycleDef()
	private var onError: MachineError? = null
	
	@Volatile
	private var queue: PriorityBlockingQueue<Request>? = PriorityBlockingQueue()
	
	@Volatile
	override var isTerminated: Boolean = false
	
	private fun LifecycleDef.call(payload: Any? = null)
	{
		try
		{
			callback?.also {
				Payload(payload).it()
			}
		}
		catch (e: Throwable)
		{
			fallback?.also {
				ErrorPayload(e, payload).it()
			} ?: onError?.also {
				ErrorAction(e).it()
			} ?: throw e
		}
	}
	
	init
	{
		lateinit var machine: KotlmataMutableMachine<T>
		
		val suffix = if (logLevel > SIMPLE) tab else ""
		
		val modifyMachine: InputAction<Request.Modify> = { modifyR ->
			machine modify modifyR.block
		}
		
		val postSync: (FunctionDSL.Sync) -> Unit = {
			val syncR = Request.Sync(it.signal, it.type, it.payload)
			logLevel.detail(tag, syncR) { DAEMON_PUT_REQUEST }
			queue!!.offer(syncR)
		}
		
		val ignore: (SIGNAL, STATE) -> Unit = { signal, state ->
			if (signal is Request)
			{
				logLevel.normal(tag, state, signal) { DAEMON_IGNORE_REQUEST }
			}
		}
		
		val terminate: InputAction<Request.Terminate> = { terminateR ->
			logLevel.simple(tag, suffix) { DAEMON_TERMINATE }
			onTerminate.call(terminateR.payload)
		}
		
		core = KotlmataMachine("$tag@core") {
			"Nil" {
				input signal "create" action {
					logLevel.normal(tag) { DAEMON_START_CREATE }
					machine = KotlmataMutableMachine.create(tag, logLevel, "Daemon[$tag]:$suffix") {
						CREATED { /* for creating state */ }
						val init = InitImpl(block, this)
						CREATED x any %= init.startAt
						start at CREATED
					}
					logLevel.normal(tag) { DAEMON_END_CREATE }
					onCreate.call()
				}
			}
			"Created" { state ->
				val start: InputAction<Request.Control> = { controlR ->
					logLevel.simple(tag, suffix) { DAEMON_START }
					onStart.call(controlR.payload)
					machine.input(controlR.payload/* as? SIGNAL */ ?: "start", block = postSync)
				}
				
				input signal Request.Run::class action start
				input signal Request.Pause::class action start
				input signal Request.Stop::class action start
				input signal Request.Terminate::class action {}
				input signal Request.Modify::class action modifyMachine
				input action { signal -> ignore(signal, state) }
			}
			"Running" { state ->
				input signal Request.Pause::class action {}
				input signal Request.Stop::class action {}
				input signal Request.Terminate::class action terminate
				input signal Request.Modify::class action modifyMachine
				input signal Request.Sync::class action { syncR ->
					syncR.type?.also { type ->
						machine.input(syncR.signal, type, syncR.payload, block = postSync)
					} ?: machine.input(syncR.signal, syncR.payload, block = postSync)
				}
				input signal Request.Input::class action { inputR ->
					machine.input(inputR.signal, inputR.payload, block = postSync)
				}
				input signal Request.TypedInput::class action { typedR ->
					machine.input(typedR.signal, typedR.type, typedR.payload, block = postSync)
				}
				input action { request -> ignore(request, state) }
			}
			"Paused" { state ->
				var sync: Request.Sync? = null
				val stash: MutableList<Request> = ArrayList()
				val keep: InputAction<Request> = { request ->
					logLevel.normal(tag, request) { DAEMON_KEEP_REQUEST }
					stash += request
				}
				
				entry via Request.Pause::class action { pauseR ->
					logLevel.simple(tag, suffix) { DAEMON_PAUSE }
					onPause.call(pauseR.payload)
				}
				input signal Request.Run::class action { runR ->
					sync?.also { syncR -> queue!!.offer(syncR) }
					queue!! += stash
					logLevel.simple(tag, suffix) { DAEMON_RESUME }
					onResume.call(runR.payload)
				}
				input signal Request.Stop::class action {
					sync?.also { syncR -> queue!!.offer(syncR) }
				}
				input signal Request.Terminate::class action terminate
				input signal Request.Modify::class action modifyMachine
				input signal Request.Sync::class action { syncR ->
					logLevel.normal(tag, syncR) { DAEMON_STORE_REQUEST }
					sync = syncR
				}
				input signal Request.Input::class action keep
				input signal Request.TypedInput::class action keep
				input action { signal -> ignore(signal, state) }
				exit action {
					sync = null
					stash.clear()
				}
			}
			"Stopped" { state ->
				var sync: Request.Sync? = null
				val cleanup: InputAction<Request> = { currentR ->
					queue!!.removeIf { queueR ->
						(queueR.isSignal && queueR.olderThan(currentR)).also {
							if (it)
							{
								logLevel.detail(tag, queueR) { DAEMON_REMOVE_REQUEST }
							}
						}
					}
					sync?.also { syncR -> queue!!.offer(syncR) }
				}
				
				entry via Request.Stop::class action { stopR ->
					logLevel.simple(tag, suffix) { DAEMON_STOP }
					onStop.call(stopR.payload)
				}
				input signal Request.Run::class action { runR ->
					cleanup(runR)
					logLevel.simple(tag, suffix) { DAEMON_RESUME }
					onResume.call(runR.payload)
				}
				input signal Request.Pause::class action cleanup
				input signal Request.Terminate::class action terminate
				input signal Request.Modify::class action modifyMachine
				input signal Request.Sync::class action { syncR ->
					logLevel.normal(tag, syncR) { DAEMON_STORE_REQUEST }
					sync = syncR
				}
				input action { signal -> ignore(signal, state) }
				exit action {
					sync = null
				}
			}
			"Terminated" via Request.Terminate::class action {
				isTerminated = true
				Thread.currentThread().interrupt()
			}
			"Destroyed" action {
				logLevel.normal(tag, suffix) { DAEMON_DESTROY }
				try
				{
					onDestroy.call()
				}
				finally
				{
					queue = null
				}
			}
			
			"Nil" x "create" %= "Created"
			
			"Created" x Request.Run::class %= "Running"
			"Created" x Request.Pause::class %= "Paused"
			"Created" x Request.Stop::class %= "Stopped"
			
			"Running" x Request.Pause::class %= "Paused"
			"Running" x Request.Stop::class %= "Stopped"
			
			"Paused" x Request.Run::class %= "Running"
			"Paused" x Request.Stop::class %= "Stopped"
			
			"Stopped" x Request.Run::class %= "Running"
			"Stopped" x Request.Pause::class %= "Paused"
			
			any.except("Terminated", "Destroyed") x Request.Terminate::class %= "Terminated"
			
			"Terminated" x "destroy" %= "Destroyed"
			
			start at "Nil"
		}
		
		thread(name = threadName, isDaemon = isDaemon, start = true) {
			try
			{
				logLevel.simple(tag, threadName, isDaemon) { DAEMON_START_THREAD }
				core.input("create")
				while (true)
				{
					val request = queue!!.take()
					logLevel.normal(tag, queue!!.size, request) { DAEMON_START_REQUEST }
					measureTimeMillis {
						core.input(request)
					}.also { time ->
						logLevel.normal(tag, time, request) { DAEMON_END_REQUEST }
					}
				}
			}
			catch (e: InterruptedException)
			{
				core.input(Request.Terminate(null))
			}
			catch (e: Throwable)
			{
				onError?.also {
					ErrorAction(e).it()
				} ?: throw e
			}
			finally
			{
				core.input("destroy")
				logLevel.simple(tag, threadName, isDaemon) { DAEMON_TERMINATE_THREAD }
			}
		}
	}
	
	override fun run(payload: Any?)
	{
		val runR = Request.Run(payload)
		logLevel.detail(tag, runR) { DAEMON_PUT_REQUEST }
		queue?.offer(runR)
	}
	
	override fun pause(payload: Any?)
	{
		val pauseR = Request.Pause(payload)
		logLevel.detail(tag, pauseR) { DAEMON_PUT_REQUEST }
		queue?.offer(pauseR)
	}
	
	override fun stop(payload: Any?)
	{
		val stopR = Request.Stop(payload)
		logLevel.detail(tag, stopR) { DAEMON_PUT_REQUEST }
		queue?.offer(stopR)
	}
	
	override fun terminate(payload: Any?)
	{
		val terminateR = Request.Terminate(payload)
		logLevel.detail(tag, terminateR) { DAEMON_PUT_REQUEST }
		queue?.offer(terminateR)
	}
	
	override fun input(signal: SIGNAL, payload: Any?, priority: Int)
	{
		val inputR = Request.Input(signal, payload, priority)
		logLevel.detail(tag, inputR) { DAEMON_PUT_REQUEST }
		queue?.offer(inputR)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?, priority: Int)
	{
		val typedR = Request.TypedInput(signal, type as KClass<SIGNAL>, payload, priority)
		logLevel.detail(tag, typedR) { DAEMON_PUT_REQUEST }
		queue?.offer(typedR)
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<out Any>, payload: Any?, priority: Int)
	{
		throw IllegalArgumentException("KClass<T> type cannot be used as input.")
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun modify(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
	{
		val modifyR = Request.Modify(block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit)
		logLevel.detail(tag, modifyR) { DAEMON_PUT_REQUEST }
		queue?.offer(modifyR)
	}
	
	override fun toString(): String
	{
		return "KotlmataDaemon[$tag]{${hashCode().toString(16)}}"
	}
	
	private inner class InitImpl(
		block: DaemonTemplate<T>,
		init: KotlmataMachine.Init
	) : KotlmataDaemon.Init, KotlmataMachine.Init by init, Expirable({ Log.e("Daemon[$tag]:") { EXPIRED_MODIFIER } })
	{
		lateinit var startAt: STATE
		
		override val on = object : KotlmataDaemon.Init.On
		{
			override fun create(block: DaemonCallback): KotlmataDaemon.Init.Catch
			{
				this@InitImpl shouldNot expired
				onCreate = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Init.Catch
				{
					override fun catch(error: DaemonFallback)
					{
						this@InitImpl shouldNot expired
						onCreate = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun start(block: DaemonCallback): KotlmataDaemon.Init.Catch
			{
				this@InitImpl shouldNot expired
				onStart = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Init.Catch
				{
					override fun catch(error: DaemonFallback)
					{
						this@InitImpl shouldNot expired
						onStart = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun pause(block: DaemonCallback): KotlmataDaemon.Init.Catch
			{
				this@InitImpl shouldNot expired
				onPause = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Init.Catch
				{
					override fun catch(error: DaemonFallback)
					{
						this@InitImpl shouldNot expired
						onPause = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun stop(block: DaemonCallback): KotlmataDaemon.Init.Catch
			{
				this@InitImpl shouldNot expired
				onStop = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Init.Catch
				{
					override fun catch(error: DaemonFallback)
					{
						this@InitImpl shouldNot expired
						onStop = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun resume(block: DaemonCallback): KotlmataDaemon.Init.Catch
			{
				this@InitImpl shouldNot expired
				onResume = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Init.Catch
				{
					override fun catch(error: DaemonFallback)
					{
						this@InitImpl shouldNot expired
						onResume = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun terminate(block: DaemonCallback): KotlmataDaemon.Init.Catch
			{
				this@InitImpl shouldNot expired
				onTerminate = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Init.Catch
				{
					override fun catch(error: DaemonFallback)
					{
						this@InitImpl shouldNot expired
						onTerminate = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun destroy(block: DaemonCallback): KotlmataDaemon.Init.Catch
			{
				this@InitImpl shouldNot expired
				onDestroy = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Init.Catch
				{
					override fun catch(error: DaemonFallback)
					{
						this@InitImpl shouldNot expired
						onDestroy = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun error(block: MachineError)
			{
				this@InitImpl shouldNot expired
				onError = block
				init.on.error(block)
			}
			
			override fun transition(block: TransitionCallback)
			{
				this@InitImpl shouldNot expired
				init.on.transition(block)
			}
		}
		
		override val start = object : KotlmataMachine.Init.Start
		{
			override fun at(state: STATE): KotlmataMachine.Init.End
			{
				this@InitImpl shouldNot expired
				
				/* For checking undefined initial state. */
				init.start at state
				
				startAt = state
				return KotlmataMachine.Init.End()
			}
		}
		
		init
		{
			block(tag, this@KotlmataDaemonImpl)
			expire()
		}
	}
	
	private sealed class Request(val priority: Int, info: String? = null) : Comparable<Request>
	{
		constructor(basePriority: Int, subPriority: Int, info: String) : this(if (subPriority > 0) basePriority + subPriority else basePriority, info)
		
		open class Control(val payload: Any?) : Request(DAEMON_CONTROL, "payload: $payload")
		class Run(payload: Any?) : Control(payload)
		class Pause(payload: Any?) : Control(payload)
		class Stop(payload: Any?) : Control(payload)
		class Terminate(payload: Any?) : Control(payload)
		
		class Modify(val block: KotlmataMutableMachine.Modifier.(DAEMON) -> Unit) : Request(MODIFY)
		
		class Sync(val signal: SIGNAL, val type: KClass<SIGNAL>?, val payload: Any?) : Request(SYNC, "signal: $signal, type: ${type?.let { "${it.simpleName}::class, payload: $payload" } ?: "null"}")
		
		class Input(val signal: SIGNAL, val payload: Any?, priority: Int) : Request(SIGNAL, priority, "signal: $signal, payload: $payload, priority: $priority")
		class TypedInput(val signal: SIGNAL, val type: KClass<SIGNAL>, val payload: Any?, priority: Int) : Request(SIGNAL, priority, "signal: $signal, type: ${type.simpleName}::class, payload: $payload, priority: $priority")
		
		companion object
		{
			private const val DAEMON_CONTROL = -3
			private const val MODIFY = -2
			private const val SYNC = -1
			private const val SIGNAL = 0
			
			val ticket: AtomicLong = AtomicLong(0)
		}
		
		val order = ticket.getAndIncrement()
		val id = this.hashCode().toString(16)
		val desc = "{id: $id, request: ${this::class.simpleName?.toUpperCase()}${info?.let { ", $it" } ?: ""}}"
		
		val isSignal = priority >= SIGNAL
		
		fun olderThan(other: Request): Boolean = order < other.order
		
		override fun compareTo(other: Request): Int
		{
			val dP = priority - other.priority
			return if (dP != 0) dP
			else (order - other.order).toInt()
		}
		
		override fun toString(): String
		{
			return desc
		}
	}
}
