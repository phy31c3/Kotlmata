@file:Suppress("unused")

package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataDaemonImpl.Request.*
import kr.co.plasticcity.kotlmata.Log.detail
import kr.co.plasticcity.kotlmata.Log.normal
import kr.co.plasticcity.kotlmata.Log.simple
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
			infix fun finish(block: DaemonCallback): Catch
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
	private var onCreate: LifecycleDef? = null
	private var onStart: LifecycleDef? = null
	private var onPause: LifecycleDef? = null
	private var onStop: LifecycleDef? = null
	private var onResume: LifecycleDef? = null
	private var onFinish: LifecycleDef? = null
	private var onDestroy: LifecycleDef? = null
	private var onError: MachineErrorCallback? = null
	
	@Volatile
	private var queue: PriorityBlockingQueue<Request>? = PriorityBlockingQueue()
	
	@Volatile
	override var isTerminated: Boolean = false
	
	private fun LifecycleDef.call(payload: Any? = null)
	{
		try
		{
			callback?.also { callback ->
				PayloadActionReceiver(payload).callback()
			}
		}
		catch (e: Throwable)
		{
			fallback?.also { fallback ->
				PayloadErrorActionReceiver(e, payload).fallback()
			} ?: onError?.also { onError ->
				ErrorActionReceiver(e).onError()
			} ?: throw e
		}
	}
	
	init
	{
		val core = KotlmataMachine("$tag@core") {
			lateinit var machine: KotlmataMutableMachine<T>
			
			val logLevel = logLevel
			val suffix = if (logLevel > SIMPLE) tab else ""
			
			val modifyMachine: InputAction<Modify> = { modifyR ->
				machine modify modifyR.block
			}
			
			val ignore: (STATE, Request) -> Unit = { state, request ->
				logLevel.normal(tag, state, request) { DAEMON_IGNORE_REQUEST }
			}
			
			val postSync: (FunctionDSL.Sync) -> Unit = {
				val syncR = Sync(it.signal, it.type, it.payload)
				logLevel.detail(tag, syncR) { DAEMON_PUT_REQUEST }
				queue!!.offer(syncR)
			}
			
			"Nil" {
				input signal "create" action {
					try
					{
						logLevel.simple(tag, threadName, isDaemon) { DAEMON_START_THREAD }
						logLevel.normal(tag) { DAEMON_START_CREATE }
						machine = KotlmataMutableMachine.create(tag, logLevel, "Daemon[$tag]:$suffix") {
							CREATED { /* for creating state */ }
							val init = InitImpl(block, this)
							CREATED x any %= init.startAt
							start at CREATED
						}
						logLevel.normal(tag) { DAEMON_END_CREATE }
					}
					finally
					{
						onCreate?.call()
					}
				}
			}
			"Created" { state ->
				val start: InputAction<Control> = { controlR ->
					logLevel.simple(tag, suffix) { DAEMON_START }
					onStart?.call(controlR.payload)
					machine.input(controlR.payload/* as? SIGNAL */ ?: "start", block = postSync)
				}
				
				input signal Run::class action start
				input signal Pause::class action start
				input signal Stop::class action start
				input signal Modify::class action modifyMachine
				input signal Sync::class action { ignore(state, it) }
				input signal Input::class action { ignore(state, it) }
				input signal TypedInput::class action { ignore(state, it) }
			}
			"Running" { state ->
				input signal Run::class action { ignore(state, it) }
				input signal Terminate::class action { terminateR ->
					onFinish?.call(terminateR.payload)
				} catch { /* ignore */ }
				input signal Modify::class action modifyMachine
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
			"Paused" { state ->
				var sync: Sync? = null
				val stash: MutableList<Request> = ArrayList()
				val keep: InputAction<Request> = { request ->
					logLevel.normal(tag, request) { DAEMON_KEEP_REQUEST }
					stash += request
				}
				
				entry via Pause::class action { pauseR ->
					logLevel.simple(tag, suffix) { DAEMON_PAUSE }
					onPause?.call(pauseR.payload)
				}
				
				input signal Run::class action { runR ->
					sync?.also { syncR -> queue!!.offer(syncR) }
					queue!! += stash
					logLevel.simple(tag, suffix) { DAEMON_RESUME }
					onResume?.call(runR.payload)
				}
				input signal Pause::class action { ignore(state, it) }
				input signal Stop::class action {
					sync?.also { syncR -> queue!!.offer(syncR) }
				}
				input signal Terminate::class action { terminateR ->
					onFinish?.call(terminateR.payload)
				} catch { /* ignore */ }
				input signal Modify::class action modifyMachine
				input signal Sync::class action { syncR ->
					logLevel.normal(tag, syncR) { DAEMON_STORE_REQUEST }
					sync = syncR
				}
				input signal Input::class action keep
				input signal TypedInput::class action keep
				
				exit action {
					sync = null
					stash.clear()
				}
			}
			"Stopped" { state ->
				var sync: Sync? = null
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
				
				entry via Stop::class action { stopR ->
					logLevel.simple(tag, suffix) { DAEMON_STOP }
					onStop?.call(stopR.payload)
				}
				
				input signal Run::class action { runR ->
					cleanup(runR)
					logLevel.simple(tag, suffix) { DAEMON_RESUME }
					onResume?.call(runR.payload)
				}
				input signal Pause::class action cleanup
				input signal Stop::class action { ignore(state, it) }
				input signal Terminate::class action { terminateR ->
					onFinish?.call(terminateR.payload)
				} catch { /* ignore */ }
				input signal Modify::class action modifyMachine
				input signal Sync::class action { syncR ->
					logLevel.normal(tag, syncR) { DAEMON_STORE_REQUEST }
					sync = syncR
				}
				input signal Input::class action { ignore(state, it) }
				input signal TypedInput::class action { ignore(state, it) }
				
				exit action {
					sync = null
				}
			}
			"Terminated" {
				entry via Terminate::class action { terminateR ->
					logLevel.simple(tag, suffix) { DAEMON_TERMINATE }
					isTerminated = true
					if (terminateR.shouldInterrupt)
					{
						Thread.currentThread().interrupt()
					}
				}
			}
			"Destroyed" action {
				try
				{
					logLevel.normal(tag, suffix) { DAEMON_DESTROY }
					onDestroy?.call()
				}
				finally
				{
					onCreate = null
					onStart = null
					onPause = null
					onStop = null
					onResume = null
					onFinish = null
					onDestroy = null
					onError = null
					queue = null
					logLevel.simple(tag, threadName, isDaemon) { DAEMON_TERMINATE_THREAD }
				}
			}
			
			"Nil" x "create" %= "Created"
			
			"Created" x Run::class %= "Running"
			"Created" x Pause::class %= "Paused"
			"Created" x Stop::class %= "Stopped"
			
			"Running" x Pause::class %= "Paused"
			"Running" x Stop::class %= "Stopped"
			
			"Paused" x Run::class %= "Running"
			"Paused" x Stop::class %= "Stopped"
			
			"Stopped" x Run::class %= "Running"
			"Stopped" x Pause::class %= "Paused"
			
			any.except("Terminated", "Destroyed") x Terminate::class %= "Terminated"
			
			"Terminated" x "destroy" %= "Destroyed"
			
			start at "Nil"
		}
		
		thread(name = threadName, isDaemon = isDaemon, start = true) {
			val queue = queue!!
			val logLevel = logLevel
			val tag = tag
			
			try
			{
				core.input("create")
				while (true)
				{
					queue.take().also { request ->
						logLevel.normal(tag, queue.size, request) { DAEMON_START_REQUEST }
						measureTimeMillis {
							core.input(request)
						}.also { time ->
							logLevel.normal(tag, time, request) { DAEMON_END_REQUEST }
						}
					}
				}
			}
			catch (e: InterruptedException)
			{
				core.input(Terminate(null, false))
			}
			catch (e: Throwable)
			{
				core.input(Terminate(null, false))
				onError?.also { onError ->
					ErrorActionReceiver(e).onError()
				} ?: throw e
			}
			finally
			{
				core.input("destroy")
			}
		}
	}
	
	override fun run(payload: Any?)
	{
		val runR = Run(payload)
		logLevel.detail(tag, runR) { DAEMON_PUT_REQUEST }
		queue?.offer(runR)
	}
	
	override fun pause(payload: Any?)
	{
		val pauseR = Pause(payload)
		logLevel.detail(tag, pauseR) { DAEMON_PUT_REQUEST }
		queue?.offer(pauseR)
	}
	
	override fun stop(payload: Any?)
	{
		val stopR = Stop(payload)
		logLevel.detail(tag, stopR) { DAEMON_PUT_REQUEST }
		queue?.offer(stopR)
	}
	
	override fun terminate(payload: Any?)
	{
		val terminateR = Terminate(payload)
		logLevel.detail(tag, terminateR) { DAEMON_PUT_REQUEST }
		queue?.offer(terminateR)
	}
	
	override fun input(signal: SIGNAL, payload: Any?, priority: Int)
	{
		val inputR = Input(signal, payload, priority)
		logLevel.detail(tag, inputR) { DAEMON_PUT_REQUEST }
		queue?.offer(inputR)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?, priority: Int)
	{
		val typedR = TypedInput(signal, type as KClass<SIGNAL>, payload, priority)
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
		val modifyR = Modify(block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit)
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
			
			override fun finish(block: DaemonCallback): KotlmataDaemon.Init.Catch
			{
				this@InitImpl shouldNot expired
				onFinish = LifecycleDef(callback = block)
				return object : KotlmataDaemon.Init.Catch
				{
					override fun catch(error: DaemonFallback)
					{
						this@InitImpl shouldNot expired
						onFinish = LifecycleDef(callback = block, fallback = error)
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
			
			override fun transition(callback: TransitionCallback): KotlmataMachine.Init.Catch
			{
				this@InitImpl shouldNot expired
				init.on transition callback
				return object : KotlmataMachine.Init.Catch
				{
					override fun catch(fallback: TransitionFallback): KotlmataMachine.Init.Finally
					{
						this@InitImpl shouldNot expired
						init.on transition callback catch fallback
						return object : KotlmataMachine.Init.Finally
						{
							override fun finally(finally: TransitionCallback)
							{
								this@InitImpl shouldNot expired
								init.on transition callback catch fallback finally finally
							}
						}
					}
					
					override fun finally(finally: TransitionCallback)
					{
						this@InitImpl shouldNot expired
						init.on transition callback finally finally
					}
				}
			}
			
			override fun error(block: MachineErrorCallback)
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
		class Terminate(payload: Any?, val shouldInterrupt: Boolean = true) : Control(payload)
		
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
