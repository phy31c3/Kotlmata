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
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG,
				threadName: String? = null,
				isDaemon: Boolean = false,
				block: Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		): KotlmataDaemon<String> = KotlmataDaemonImpl(name, logLevel, threadName, isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG,
				threadName: String? = null,
				isDaemon: Boolean = false,
				block: Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		) = lazy {
			invoke(name, logLevel, threadName, isDaemon, block)
		}
		
		internal fun <T : DAEMON> create(
				key: T,
				block: Initializer.(daemon: T) -> KotlmataMachine.Initializer.End
		): KotlmataDaemon<T> = KotlmataDaemonImpl(key, block = block)
	}
	
	@KotlmataMarker
	interface Initializer : KotlmataMachine.Initializer
	{
		override val on: On
		
		interface On : KotlmataMachine.Initializer.On
		{
			infix fun start(block: KotlmataCallback): Catch
			infix fun pause(block: KotlmataCallback): Catch
			infix fun stop(block: KotlmataCallback): Catch
			infix fun resume(block: KotlmataCallback): Catch
			infix fun terminate(block: KotlmataCallback): Catch
		}
		
		interface Catch
		{
			infix fun catch(error: KotlmataFallback)
			infix fun catch(error: KotlmataFallback1)
		}
	}
	
	val key: T
	
	fun run(payload: Any? = null)
	fun pause(payload: Any? = null)
	fun stop(payload: Any? = null)
	fun terminate(payload: Any? = null)
	
	/**
	 * @param priority Smaller means higher. Priority must be (priority >= 0). Default value is 0.
	 */
	fun input(signal: SIGNAL, priority: Int = 0)
	
	/**
	 * @param priority Smaller means higher. Priority must be (priority >= 0). Default value is 0.
	 */
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, priority: Int = 0)
}

interface KotlmataMutableDaemon<T : DAEMON> : KotlmataDaemon<T>
{
	companion object
	{
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun invoke(
				name: String,
				logLevel: Int = NO_LOG,
				threadName: String? = null,
				isDaemon: Boolean = false,
				block: KotlmataDaemon.Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon<String> = KotlmataDaemonImpl(name, logLevel, threadName, isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG,
				threadName: String? = null,
				isDaemon: Boolean = false,
				block: KotlmataDaemon.Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		) = lazy {
			invoke(name, logLevel, threadName, isDaemon, block)
		}
		
		internal fun <T : DAEMON> create(
				key: T,
				logLevel: Int,
				block: KotlmataDaemon.Initializer.(daemon: T) -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon<T> = KotlmataDaemonImpl(key, logLevel, block = block)
	}
	
	operator fun invoke(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit) = modify(block)
	
	infix fun modify(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
}

private class LifecycleDef(val callback: KotlmataCallback? = null, val fallback: KotlmataFallback1? = null)

private class KotlmataDaemonImpl<T : DAEMON>(
		override val key: T,
		val logLevel: Int = NO_LOG,
		threadName: String? = null,
		isDaemon: Boolean = false,
		block: KotlmataDaemon.Initializer.(T) -> KotlmataMachine.Initializer.End
) : KotlmataMutableDaemon<T>
{
	private val core: KotlmataMachine<String>
	
	private var onStart: LifecycleDef = LifecycleDef()
	private var onPause: LifecycleDef = LifecycleDef()
	private var onStop: LifecycleDef = LifecycleDef()
	private var onResume: LifecycleDef = LifecycleDef()
	private var onTerminate: LifecycleDef = LifecycleDef()
	private var onError: KotlmataError? = null
	
	@Volatile
	private var queue: PriorityBlockingQueue<Request>? = PriorityBlockingQueue()
	
	private fun LifecycleDef.call(payload: Any?)
	{
		try
		{
			callback?.invoke(DSL, payload)
		}
		catch (e: Throwable)
		{
			fallback?.also {
				DSL.it(e, payload)
			} ?: onError?.also {
				DSL.it(e)
			} ?: throw e
		}
	}
	
	init
	{
		lateinit var machine: KotlmataMutableMachine<T>
		
		val modifyMachine: KotlmataAction1<Request.Modify> = { modifyR ->
			machine modify modifyR.block
		}
		
		val postSync: (KotlmataDSL.SyncInput) -> Unit = {
			val syncR = Request.Sync(it.signal, it.type)
			logLevel.detail(key, syncR) { DAEMON_PUT_REQUEST }
			queue!!.offer(syncR)
		}
		
		val ignore: (SIGNAL, STATE) -> Unit = { signal, state ->
			if (signal is Request)
			{
				logLevel.normal(key, state, signal) { DAEMON_IGNORE_REQUEST }
			}
		}
		
		val terminate: KotlmataAction1<Request.Terminate> = { terminateR ->
			onTerminate.call(terminateR.payload)
			logLevel.simple(key) { DAEMON_TERMINATE }
		}
		
		core = KotlmataMachine.create("$key@core") {
			"Initial" { state ->
				val start: KotlmataAction1<Request.Control> = { controlR ->
					logLevel.simple(key) { DAEMON_START }
					onStart.call(controlR.payload)
					machine.input(controlR.payload/* as? SIGNAL */ ?: "start", postSync)
				}
				
				input signal Request.Run::class action start
				input signal Request.Pause::class action start
				input signal Request.Stop::class action start
				input signal Request.Terminate::class action {}
				input signal Request.Modify::class action modifyMachine
				input action { signal -> ignore(signal, state) }
			}
			
			"Run" { state ->
				input signal Request.Pause::class action {}
				input signal Request.Stop::class action {}
				input signal Request.Terminate::class action terminate
				input signal Request.Modify::class action modifyMachine
				input signal Request.Sync::class action { syncR ->
					syncR.type?.also {
						machine.input(syncR.signal, it, postSync)
					} ?: machine.input(syncR.signal, postSync)
				}
				input signal Request.Signal::class action { signalR ->
					machine.input(signalR.signal, postSync)
				}
				input signal Request.TypedSignal::class action { typedR ->
					machine.input(typedR.signal, typedR.type, postSync)
				}
				input action { request -> ignore(request, state) }
			}
			
			"Pause" { state ->
				var sync: Request.Sync? = null
				val stash: MutableList<Request> = ArrayList()
				val keep: KotlmataAction1<Request> = { request ->
					logLevel.normal(key, request) { DAEMON_KEEP_REQUEST }
					stash += request
				}
				
				entry via Request.Pause::class action { pauseR ->
					logLevel.simple(key) { DAEMON_PAUSE }
					onPause.call(pauseR.payload)
				}
				
				input signal Request.Run::class action { runR ->
					sync?.let { syncR -> queue!!.offer(syncR) }
					queue!! += stash
					logLevel.simple(key) { DAEMON_RESUME }
					onResume.call(runR.payload)
				}
				input signal Request.Stop::class action {
					sync?.let { syncR -> queue!!.offer(syncR) }
				}
				input signal Request.Terminate::class action terminate
				input signal Request.Modify::class action modifyMachine
				input signal Request.Sync::class action { syncR ->
					logLevel.normal(key, syncR) { DAEMON_STORE_REQUEST }
					sync = syncR
				}
				input signal Request.Signal::class action keep
				input signal Request.TypedSignal::class action keep
				input action { signal -> ignore(signal, state) }
				
				exit action {
					sync = null
					stash.clear()
				}
			}
			
			"Stop" { state ->
				var sync: Request.Sync? = null
				
				val cleanup: KotlmataAction1<Request> = { currentR ->
					queue!!.removeIf { queueR ->
						(queueR.isSignal && queueR.olderThan(currentR)).also {
							if (it)
							{
								logLevel.detail(key, queueR) { DAEMON_REMOVE_REQUEST }
							}
						}
					}
					sync?.let { syncR -> queue!!.offer(syncR) }
				}
				
				entry via Request.Stop::class action { stopR ->
					logLevel.simple(key) { DAEMON_STOP }
					onStop.call(stopR.payload)
				}
				
				input signal Request.Run::class action { runR ->
					cleanup(runR)
					logLevel.simple(key) { DAEMON_RESUME }
					onResume.call(runR.payload)
				}
				input signal Request.Pause::class action cleanup
				input signal Request.Terminate::class action terminate
				input signal Request.Modify::class action modifyMachine
				input signal Request.Sync::class action { syncR ->
					logLevel.normal(key, syncR) { DAEMON_STORE_REQUEST }
					sync = syncR
				}
				input action { signal -> ignore(signal, state) }
				
				exit action {
					sync = null
				}
			}
			
			"Terminate" {
				entry via Request.Terminate::class action {
					Thread.currentThread().interrupt()
				}
			}
			
			"Initial" x Request.Run::class %= "Run"
			"Initial" x Request.Pause::class %= "Pause"
			"Initial" x Request.Stop::class %= "Stop"
			
			"Run" x Request.Pause::class %= "Pause"
			"Run" x Request.Stop::class %= "Stop"
			
			"Pause" x Request.Run::class %= "Run"
			"Pause" x Request.Stop::class %= "Stop"
			
			"Stop" x Request.Run::class %= "Run"
			"Stop" x Request.Pause::class %= "Pause"
			
			any.except("Terminate") x Request.Terminate::class %= "Terminate"
			
			start at "Initial"
		}
		
		thread(name = threadName ?: "$key", isDaemon = isDaemon, start = true) {
			logLevel.normal(key, threadName, isDaemon) { DAEMON_START_THREAD }
			logLevel.normal(key) { DAEMON_START_INIT }
			machine = KotlmataMutableMachine.create(key, logLevel, "Daemon[$key]:$tab") {
				Initial {}
				val initializer = InitializerImpl(block, this)
				Initial x any %= initializer.startAt
				start at Initial
			}
			logLevel.normal(key) { DAEMON_END_INIT }
			try
			{
				while (true)
				{
					val request = queue!!.take()
					logLevel.normal(key, queue!!.size, request) { DAEMON_START_REQUEST }
					measureTimeMillis {
						core.input(request)
					}.also { time ->
						logLevel.normal(key, time, request) { DAEMON_END_REQUEST }
					}
				}
			}
			catch (e: InterruptedException)
			{
				core.input(Request.Terminate(null))
				queue!!.clear()
				queue = null
			}
			logLevel.normal(key, threadName, isDaemon) { DAEMON_TERMINATE_THREAD }
		}
	}
	
	override fun run(payload: Any?)
	{
		val runR = Request.Run(payload)
		logLevel.detail(key, runR) { DAEMON_PUT_REQUEST }
		queue?.offer(runR)
	}
	
	override fun pause(payload: Any?)
	{
		val pauseR = Request.Pause(payload)
		logLevel.detail(key, pauseR) { DAEMON_PUT_REQUEST }
		queue?.offer(pauseR)
	}
	
	override fun stop(payload: Any?)
	{
		val stopR = Request.Stop(payload)
		logLevel.detail(key, stopR) { DAEMON_PUT_REQUEST }
		queue?.offer(stopR)
	}
	
	override fun terminate(payload: Any?)
	{
		val terminateR = Request.Terminate(payload)
		logLevel.detail(key, terminateR) { DAEMON_PUT_REQUEST }
		queue?.offer(terminateR)
	}
	
	override fun input(signal: SIGNAL, priority: Int)
	{
		val signalR = Request.Signal(signal, priority)
		logLevel.detail(key, signalR) { DAEMON_PUT_REQUEST }
		queue?.offer(signalR)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, priority: Int)
	{
		val typedR = Request.TypedSignal(signal, type as KClass<SIGNAL>, priority)
		logLevel.detail(key, typedR) { DAEMON_PUT_REQUEST }
		queue?.offer(typedR)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun modify(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
	{
		val modifyR = Request.Modify(block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit)
		logLevel.detail(key, modifyR) { DAEMON_PUT_REQUEST }
		queue?.offer(modifyR)
	}
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
	
	private inner class InitializerImpl internal constructor(
			block: KotlmataDaemon.Initializer.(T) -> KotlmataMachine.Initializer.End,
			initializer: KotlmataMachine.Initializer
	) : KotlmataDaemon.Initializer, KotlmataMachine.Initializer by initializer, Expirable({ Log.e("Daemon[$key]:") { EXPIRED_MODIFIER } })
	{
		lateinit var startAt: STATE
		
		override val on = object : KotlmataDaemon.Initializer.On
		{
			override fun start(block: KotlmataCallback): KotlmataDaemon.Initializer.Catch
			{
				this@InitializerImpl shouldNot expired
				onStart = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Initializer.Catch
				{
					override fun catch(error: KotlmataFallback)
					{
						onStart = LifecycleDef(callback = block, fallback = { throwable, _ -> error(throwable) })
					}
					
					override fun catch(error: KotlmataFallback1)
					{
						onStart = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun pause(block: KotlmataCallback): KotlmataDaemon.Initializer.Catch
			{
				this@InitializerImpl shouldNot expired
				onPause = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Initializer.Catch
				{
					override fun catch(error: KotlmataFallback)
					{
						onPause = LifecycleDef(callback = block, fallback = { throwable, _ -> error(throwable) })
					}
					
					override fun catch(error: KotlmataFallback1)
					{
						onPause = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun stop(block: KotlmataCallback): KotlmataDaemon.Initializer.Catch
			{
				this@InitializerImpl shouldNot expired
				onStop = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Initializer.Catch
				{
					override fun catch(error: KotlmataFallback)
					{
						onStop = LifecycleDef(callback = block, fallback = { throwable, _ -> error(throwable) })
					}
					
					override fun catch(error: KotlmataFallback1)
					{
						onStop = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun resume(block: KotlmataCallback): KotlmataDaemon.Initializer.Catch
			{
				this@InitializerImpl shouldNot expired
				onResume = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Initializer.Catch
				{
					override fun catch(error: KotlmataFallback)
					{
						onResume = LifecycleDef(callback = block, fallback = { throwable, _ -> error(throwable) })
					}
					
					override fun catch(error: KotlmataFallback1)
					{
						onResume = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun terminate(block: KotlmataCallback): KotlmataDaemon.Initializer.Catch
			{
				this@InitializerImpl shouldNot expired
				onTerminate = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Initializer.Catch
				{
					override fun catch(error: KotlmataFallback)
					{
						onTerminate = LifecycleDef(callback = block, fallback = { throwable, _ -> error(throwable) })
					}
					
					override fun catch(error: KotlmataFallback1)
					{
						onTerminate = LifecycleDef(callback = block, fallback = error)
					}
				}
			}
			
			override fun error(block: KotlmataFallback)
			{
				this@InitializerImpl shouldNot expired
				onError = block
				initializer.on.error(block)
			}
		}
		
		override val start = object : KotlmataMachine.Initializer.Start
		{
			override fun at(state: STATE): KotlmataMachine.Initializer.End
			{
				this@InitializerImpl shouldNot expired
				
				/* For checking undefined initial state. */
				initializer.start at state
				
				startAt = state
				return KotlmataMachine.Initializer.End()
			}
		}
		
		init
		{
			block(key)
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
		
		class Sync(val signal: SIGNAL, val type: KClass<SIGNAL>?) : Request(SYNC, "signal: $signal, type: ${type?.let { "${it.simpleName}::class" } ?: "null"}")
		
		class Signal(val signal: SIGNAL, priority: Int) : Request(SIGNAL, priority, "signal: $signal, priority: $priority")
		class TypedSignal(val signal: SIGNAL, val type: KClass<SIGNAL>, priority: Int) : Request(SIGNAL, priority, "signal: $signal, type: ${type.simpleName}::class, priority: $priority")
		
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