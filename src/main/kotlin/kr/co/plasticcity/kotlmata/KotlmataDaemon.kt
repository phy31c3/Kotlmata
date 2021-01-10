package kr.co.plasticcity.kotlmata

import kr.co.plasticcity.kotlmata.KotlmataDaemon.Base
import kr.co.plasticcity.kotlmata.KotlmataDaemon.Init
import kr.co.plasticcity.kotlmata.KotlmataDaemonImpl.Request.*
import kr.co.plasticcity.kotlmata.KotlmataMachine.Companion.By
import kr.co.plasticcity.kotlmata.KotlmataMachine.Companion.Extends
import kr.co.plasticcity.kotlmata.Log.detail
import kr.co.plasticcity.kotlmata.Log.normal
import kr.co.plasticcity.kotlmata.Log.simple
import java.util.*
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
			block: DaemonTemplate
		): KotlmataDaemon = KotlmataDaemonImpl(name, logLevel, threadName, isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false
		) = object : Extends<DaemonBase, DaemonTemplate, KotlmataDaemon>
		{
			override fun extends(base: DaemonBase) = object : By<DaemonTemplate, KotlmataDaemon>
			{
				override fun by(template: DaemonTemplate) = invoke(name, logLevel, threadName, isDaemon) { daemon ->
					base(daemon)
					template(daemon)
				}
			}
			
			override fun by(template: DaemonTemplate) = invoke(name, logLevel, threadName, isDaemon, template)
		}
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun lazy(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false,
			block: DaemonTemplate
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
		) = object : Extends<DaemonBase, DaemonTemplate, Lazy<KotlmataDaemon>>
		{
			override fun extends(base: DaemonBase) = object : By<DaemonTemplate, Lazy<KotlmataDaemon>>
			{
				override fun by(template: DaemonTemplate) = lazy {
					invoke(name, logLevel, threadName, isDaemon) extends base by template
				}
			}
			
			override fun by(template: DaemonTemplate) = lazy {
				invoke(name, logLevel, threadName, isDaemon, template)
			}
		}
	}
	
	interface Base : KotlmataMachine.Base
	{
		override val on: On
		
		interface On : KotlmataMachine.Base.On
		{
			infix fun create(callback: DaemonCallback): Catch
			infix fun start(callback: DaemonCallback): Catch
			infix fun pause(callback: DaemonCallback): Catch
			infix fun stop(callback: DaemonCallback): Catch
			infix fun resume(callback: DaemonCallback): Catch
			infix fun finish(callback: DaemonCallback): Catch
			infix fun destroy(callback: DaemonCallback): Catch
			infix fun fatal(block: MachineErrorCallback)
		}
		
		interface Catch : Finally
		{
			infix fun catch(fallback: DaemonFallback): Finally
		}
		
		interface Finally
		{
			infix fun finally(finally: DaemonCallback)
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
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any? = null, priority: Int = 0)
	
	@Deprecated("KClass<T> type cannot be used as input.", level = DeprecationLevel.ERROR)
	fun input(signal: KClass<out Any>, payload: Any? = null, priority: Int = 0)
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
			block: DaemonTemplate
		): KotlmataMutableDaemon = KotlmataDaemonImpl(name, logLevel, threadName, isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		operator fun invoke(
			name: String,
			logLevel: Int = NO_LOG,
			threadName: String = "thread-KotlmataDaemon[$name]",
			isDaemon: Boolean = false
		) = object : Extends<DaemonBase, DaemonTemplate, KotlmataMutableDaemon>
		{
			override fun extends(base: DaemonBase) = object : By<DaemonTemplate, KotlmataMutableDaemon>
			{
				override fun by(template: DaemonTemplate) = invoke(name, logLevel, threadName, isDaemon) { daemon ->
					base(daemon)
					template(daemon)
				}
			}
			
			override fun by(template: DaemonTemplate) = invoke(name, logLevel, threadName, isDaemon, template)
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
			block: DaemonTemplate
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
		) = object : Extends<DaemonBase, DaemonTemplate, Lazy<KotlmataMutableDaemon>>
		{
			override fun extends(base: DaemonBase) = object : By<DaemonTemplate, Lazy<KotlmataMutableDaemon>>
			{
				override fun by(template: DaemonTemplate) = lazy {
					invoke(name, logLevel, threadName, isDaemon) extends base by template
				}
			}
			
			override fun by(template: DaemonTemplate) = lazy {
				invoke(name, logLevel, threadName, isDaemon, template)
			}
		}
	}
	
	operator fun invoke(block: KotlmataMutableMachine.Update.() -> Unit) = update(block)
	infix fun update(block: KotlmataMutableMachine.Update.() -> Unit)
}

private class LifecycleDef(val callback: DaemonCallback, val fallback: DaemonFallback? = null, val finally: DaemonCallback? = null)

private class KotlmataDaemonImpl(
	override val name: String,
	val logLevel: Int,
	threadName: String,
	isDaemon: Boolean = false,
	block: DaemonTemplate
) : KotlmataMutableDaemon
{
	private var onCreate: LifecycleDef? = null
	private var onStart: LifecycleDef? = null
	private var onPause: LifecycleDef? = null
	private var onStop: LifecycleDef? = null
	private var onResume: LifecycleDef? = null
	private var onFinish: LifecycleDef? = null
	private var onDestroy: LifecycleDef? = null
	private var onError: MachineErrorCallback? = null
	private var onFatal: MachineErrorCallback? = null
	
	@Volatile
	private var queue: PriorityBlockingQueue<Request>? = PriorityBlockingQueue()
	
	@Volatile
	override var isTerminated: Boolean = false
	
	private fun LifecycleDef.call(payload: Any? = null)
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
			lateinit var machine: KotlmataMutableMachine
			
			val logLevel = logLevel
			val suffix = if (logLevel >= DETAIL) tab else ""
			
			val update: InputAction<Update> = { updateR ->
				machine update updateR.block
			}
			
			val ignore: InputAction<Request> = {
				logLevel.detail(name) { DAEMON_IGNORE_REQUEST }
			}
			
			val postSync: (FunctionDSL.Sync) -> Unit = {
				val syncR = Sync(it.signal, it.type, it.payload)
				queue!!.offer(syncR)
			}
			
			on transition { from, signal, to ->
				logLevel.detail(name, from, to) {
					when (to as String)
					{
						"Created", "Terminated", "Destroyed" ->
							if (signal is Terminate && signal.isExplicit)
								DAEMON_LIFECYCLE_CHANGED_TAB
							else
								DAEMON_LIFECYCLE_CHANGED
						else ->
							DAEMON_LIFECYCLE_CHANGED_TAB
					}
				}
			}
			
			"Nil" {
				/* nothing */
			}
			"Created" {
				val onStart: InputAction<Control> = { controlR ->
					logLevel.simple(name, suffix, controlR.payload) { DAEMON_ON_START }
					onStart?.call(controlR.payload)
					machine.input(controlR.payload/* as? SIGNAL */ ?: `Start KotlmataDaemon`, block = postSync)
				}
				
				entry action {
					logLevel.detail(name, name) { DAEMON_START_CREATE }
					machine = KotlmataMutableMachine.create(name, logLevel, "Daemon[$name]:$suffix") {
						`Initial state for KotlmataDaemon` { /* for creating state */ }
						val init = InitImpl(block, this)
						`Initial state for KotlmataDaemon` x any %= init.startAt
						start at `Initial state for KotlmataDaemon`
					}
					logLevel.detail(name) { DAEMON_END_CREATE }
				} finally {
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
					when (previousState)
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
				val stash: MutableList<Request> = ArrayList()
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
				var sync: Sync? = null
				val cleanup: InputAction<Request> = { currentR ->
					queue!!.removeIf { queueR ->
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
					if (terminateR.isInterrupted)
					{
						Log.w(name) { DAEMON_INTERRUPTED }
					}
					when (previousState)
					{
						"Running", "Paused", "Stopped" ->
						{
							logLevel.simple(name, terminateR.payload) {
								if (terminateR.isExplicit)
									suffix + DAEMON_ON_FINISH
								else
									DAEMON_ON_FINISH
							}
							onFinish?.call(terminateR.payload)
						}
					}
				} finally { terminateR ->
					isTerminated = true
					if (terminateR.isExplicit)
					{
						Thread.currentThread().interrupt()
					}
				}
			}
			"Destroyed" {
				entry action {
					logLevel.simple(name) { DAEMON_ON_DESTROY }
					onDestroy?.call()
				} finally {
					onCreate = null
					onStart = null
					onPause = null
					onStop = null
					onResume = null
					onFinish = null
					onDestroy = null
					onError = null
					queue = null
					logLevel.simple(name, threadName, isDaemon) { DAEMON_TERMINATE_THREAD }
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
			
			any.except("Nil", "Terminated", "Destroyed") x Terminate::class %= "Terminated"
			
			"Terminated" x "destroy" %= "Destroyed"
			
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
				core.input("create")
				while (true)
				{
					queue.take().also { request ->
						logLevel.detail(name, request, queue.size) {
							start = System.currentTimeMillis()
							DAEMON_START_REQUEST
						}
						core.input(request)
						logLevel.detail(name) {
							DAEMON_END_REQUEST + " ${System.currentTimeMillis() - start}ms"
						}
					}
				}
			}
			catch (e: InterruptedException)
			{
				core.input(Terminate(null, isExplicit = false, isInterrupted = true))
			}
			catch (e: Throwable)
			{
				try
				{
					onFatal?.also { onFatal ->
						ErrorActionReceiver(e).onFatal()
					} ?: throw e
				}
				finally
				{
					core.input(Terminate(null, isExplicit = false, isInterrupted = false))
				}
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
		val terminateR = Terminate(payload)
		queue?.offer(terminateR)
	}
	
	override fun input(signal: SIGNAL, payload: Any?, priority: Int)
	{
		val inputR = Input(signal, payload, priority)
		queue?.offer(inputR)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, payload: Any?, priority: Int)
	{
		val typedR = TypedInput(signal, type as KClass<SIGNAL>, payload, priority)
		queue?.offer(typedR)
	}
	
	@Suppress("OverridingDeprecatedMember")
	override fun input(signal: KClass<out Any>, payload: Any?, priority: Int)
	{
		throw IllegalArgumentException("KClass<T> type cannot be used as input.")
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
		block: DaemonTemplate,
		init: KotlmataMachine.Init
	) : Init, KotlmataMachine.Init by init, Expirable({ Log.e("Daemon[$name]:") { EXPIRED_OBJECT } })
	{
		lateinit var startAt: STATE
		
		override val on = object : Base.On
		{
			private fun setLifecycleDef(callback: DaemonCallback, set: (LifecycleDef) -> Unit): Base.Catch
			{
				this@InitImpl shouldNot expired
				set(LifecycleDef(callback))
				return object : Base.Catch
				{
					override fun catch(fallback: DaemonFallback): Base.Finally
					{
						this@InitImpl shouldNot expired
						set(LifecycleDef(callback, fallback))
						return object : Base.Finally
						{
							override fun finally(finally: DaemonCallback)
							{
								this@InitImpl shouldNot expired
								set(LifecycleDef(callback, fallback, finally))
							}
						}
					}
					
					override fun finally(finally: DaemonCallback)
					{
						this@InitImpl shouldNot expired
						set(LifecycleDef(callback, null, finally))
					}
				}
			}
			
			private val suffix = if (logLevel >= DETAIL) tab else ""
			
			override fun create(callback: DaemonCallback): Base.Catch
			{
				logLevel.normal(name, suffix) { DAEMON_REGISTER_ON_CREATE }
				return setLifecycleDef(callback) { onCreate = it }
			}
			
			override fun start(callback: DaemonCallback): Base.Catch
			{
				logLevel.normal(name, suffix) { DAEMON_REGISTER_ON_START }
				return setLifecycleDef(callback) { onStart = it }
			}
			
			override fun pause(callback: DaemonCallback): Base.Catch
			{
				logLevel.normal(name, suffix) { DAEMON_REGISTER_ON_PAUSE }
				return setLifecycleDef(callback) { onPause = it }
			}
			
			override fun stop(callback: DaemonCallback): Base.Catch
			{
				logLevel.normal(name, suffix) { DAEMON_REGISTER_ON_STOP }
				return setLifecycleDef(callback) { onStop = it }
			}
			
			override fun resume(callback: DaemonCallback): Base.Catch
			{
				logLevel.normal(name, suffix) { DAEMON_REGISTER_ON_RESUME }
				return setLifecycleDef(callback) { onResume = it }
			}
			
			override fun finish(callback: DaemonCallback): Base.Catch
			{
				logLevel.normal(name, suffix) { DAEMON_REGISTER_ON_FINISH }
				return setLifecycleDef(callback) { onFinish = it }
			}
			
			override fun destroy(callback: DaemonCallback): Base.Catch
			{
				logLevel.normal(name, suffix) { DAEMON_REGISTER_ON_DESTROY }
				return setLifecycleDef(callback) { onDestroy = it }
			}
			
			override fun transition(callback: TransitionCallback): KotlmataMachine.Base.Catch
			{
				this@InitImpl shouldNot expired
				val transition = init.on transition callback
				return object : KotlmataMachine.Base.Catch
				{
					override fun catch(fallback: TransitionFallback): KotlmataMachine.Base.Finally
					{
						this@InitImpl shouldNot expired
						val catch = transition catch fallback
						return object : KotlmataMachine.Base.Finally
						{
							override fun finally(finally: TransitionCallback)
							{
								this@InitImpl shouldNot expired
								catch finally finally
							}
						}
					}
					
					override fun finally(finally: TransitionCallback)
					{
						this@InitImpl shouldNot expired
						transition finally finally
					}
				}
			}
			
			override fun error(block: MachineErrorCallback)
			{
				this@InitImpl shouldNot expired
				onError = block
				init.on error block
			}
			
			override fun fatal(block: MachineErrorCallback)
			{
				this@InitImpl shouldNot expired
				onFatal = block
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
			block(this@KotlmataDaemonImpl)
			expire()
		}
	}
	
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
		class Terminate(payload: Any?, val isExplicit: Boolean = true, val isInterrupted: Boolean = false) : Control(payload, "terminate")
		
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
