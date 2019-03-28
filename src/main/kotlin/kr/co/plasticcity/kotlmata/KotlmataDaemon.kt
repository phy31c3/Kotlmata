package kr.co.plasticcity.kotlmata

import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.reflect.KClass

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
				threadName: String = "KotlmataDaemon[$name]",
				isDaemon: Boolean = false,
				block: Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		): KotlmataDaemon<String> = KotlmataDaemonImpl(name, logLevel, threadName, isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG,
				threadName: String = "KotlmataDaemon[$name]",
				isDaemon: Boolean = false,
				block: Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		) = lazy {
			invoke(name, logLevel, threadName, isDaemon, block)
		}
		
		internal fun <T : DAEMON> create(
				key: T,
				threadName: String,
				block: Initializer.(daemon: T) -> KotlmataMachine.Initializer.End
		): KotlmataDaemon<T> = KotlmataDaemonImpl(key, threadName = threadName, block = block)
	}
	
	@KotlmataMarker
	interface Initializer : KotlmataMachine.Initializer
	{
		override val on: On
		
		interface On : KotlmataMachine.Initializer.On
		{
			infix fun start(block: KotlmataCallback)
			infix fun pause(block: KotlmataCallback)
			infix fun stop(block: KotlmataCallback)
			infix fun resume(block: KotlmataCallback)
			infix fun terminate(block: KotlmataCallback)
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
				threadName: String = "KotlmataDaemon[$name]",
				isDaemon: Boolean = false,
				block: KotlmataDaemon.Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon<String> = KotlmataDaemonImpl(name, logLevel, threadName, isDaemon, block)
		
		/**
		 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
		 */
		fun lazy(
				name: String,
				logLevel: Int = NO_LOG,
				threadName: String = "KotlmataDaemon[$name]",
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

private class KotlmataDaemonImpl<T : DAEMON>(
		override val key: T,
		val logLevel: Int = NO_LOG,
		threadName: String = "KotlmataDaemon[$key]",
		isDaemon: Boolean = false,
		block: KotlmataDaemon.Initializer.(T) -> KotlmataMachine.Initializer.End
) : KotlmataMutableDaemon<T>
{
	private val core: KotlmataMachine<String>
	
	private var onStart: KotlmataCallback = {}
	private var onPause: KotlmataCallback = {}
	private var onStop: KotlmataCallback = {}
	private var onResume: KotlmataCallback = {}
	private var onTerminate: KotlmataCallback = {}
	
	private var queue: PriorityBlockingQueue<Message>? = PriorityBlockingQueue()
	
	init
	{
		lateinit var machine: KotlmataMutableMachine<T>
		
		val modifyMachine: KotlmataAction1<Message.Modify> = { modifyM ->
			machine modify modifyM.block
		}
		
		val postSync: (SIGNAL) -> Unit = { signal ->
			val syncM = Message.Sync(signal)
			logLevel.detail(key, syncM.signal, syncM.id) { DAEMON_MESSAGE_SYNC }
			queue!!.offer(syncM)
		}
		
		val ignore: (SIGNAL, STATE) -> Unit = { signal, state ->
			if (signal is Message)
			{
				logLevel.normal(key, state, signal.id) { DAEMON_IGNORE_MESSAGE }
			}
		}
		
		val terminate: KotlmataAction1<Message.Terminate> = { terminateM ->
			onTerminate(terminateM.payload)
			logLevel.simple(key) { DAEMON_TERMINATE }
		}
		
		core = KotlmataMachine.create("$key@core") {
			"Initial" { state ->
				val start: KotlmataAction1<Message.Control> = { controlM ->
					logLevel.simple(key) { DAEMON_START }
					onStart(controlM.payload)
					machine.input(controlM.payload/* as? SIGNAL */ ?: controlM, postSync)
				}
				
				input signal Message.Run::class action start
				input signal Message.Pause::class action start
				input signal Message.Stop::class action start
				input signal Message.Terminate::class action {}
				input signal Message.Modify::class action modifyMachine
				input action { signal -> ignore(signal, state) }
			}
			
			"Run" { state ->
				input signal Message.Pause::class action {}
				input signal Message.Stop::class action {}
				input signal Message.Terminate::class action terminate
				input signal Message.Modify::class action modifyMachine
				input signal Message.Sync::class action { syncM ->
					machine.input(syncM.signal, postSync)
				}
				input signal Message.Signal::class action { signalM ->
					machine.input(signalM.signal, postSync)
				}
				input signal Message.TypedSignal::class action { typedM ->
					machine.input(typedM.signal, typedM.type, postSync)
				}
				input action { message -> ignore(message, state) }
			}
			
			"Pause" { state ->
				var sync: Message.Sync? = null
				val stash: MutableList<Message> = ArrayList()
				val keep: KotlmataAction1<Message> = { message ->
					logLevel.normal(key, message.id) { DAEMON_KEEP_MESSAGE }
					stash += message
				}
				
				entry via Message.Pause::class action { pauseM ->
					logLevel.simple(key) { DAEMON_PAUSE }
					onPause(pauseM.payload)
				}
				
				input signal Message.Run::class action { runM ->
					sync?.let { syncM -> queue!!.offer(syncM) }
					queue!! += stash
					logLevel.simple(key) { DAEMON_RESUME }
					onResume(runM.payload)
				}
				input signal Message.Stop::class action {
					sync?.let { syncM -> queue!!.offer(syncM) }
				}
				input signal Message.Terminate::class action terminate
				input signal Message.Modify::class action modifyMachine
				input signal Message.Sync::class action { syncM ->
					sync = syncM
					logLevel.normal(key, syncM.id) { DAEMON_KEEP_SYNC }
				}
				input signal Message.Signal::class action keep
				input signal Message.TypedSignal::class action keep
				input action { signal -> ignore(signal, state) }
				
				exit action {
					sync = null
					stash.clear()
				}
			}
			
			"Stop" { state ->
				var sync: Message.Sync? = null
				
				val cleanup: KotlmataAction1<Message> = { currentM ->
					queue!!.removeIf { queueM ->
						(queueM.isSignal && queueM.olderThan(currentM)).apply {
							if (this)
							{
								logLevel.detail(key, queueM.id) { DAEMON_DROP_MESSAGE }
							}
						}
					}
					sync?.let { syncM -> queue!!.offer(syncM) }
				}
				
				entry via Message.Stop::class action { stopM ->
					logLevel.simple(key) { DAEMON_STOP }
					onStop(stopM.payload)
				}
				
				input signal Message.Run::class action { runM ->
					cleanup(runM)
					logLevel.simple(key) { DAEMON_RESUME }
					onResume(runM.payload)
				}
				input signal Message.Pause::class action cleanup
				input signal Message.Terminate::class action terminate
				input signal Message.Modify::class action modifyMachine
				input signal Message.Sync::class action { syncM ->
					sync = syncM
					logLevel.normal(key, syncM.id) { DAEMON_KEEP_SYNC }
				}
				input action { signal -> ignore(signal, state) }
				
				exit action {
					sync = null
				}
			}
			
			"Terminate" {
				entry via Message.Terminate::class action {
					Thread.currentThread().interrupt()
				}
			}
			
			"Initial" x Message.Run::class %= "Run"
			"Initial" x Message.Pause::class %= "Pause"
			"Initial" x Message.Stop::class %= "Stop"
			
			"Run" x Message.Pause::class %= "Pause"
			"Run" x Message.Stop::class %= "Stop"
			
			"Pause" x Message.Run::class %= "Run"
			"Pause" x Message.Stop::class %= "Stop"
			
			"Stop" x Message.Run::class %= "Run"
			"Stop" x Message.Pause::class %= "Pause"
			
			any.except("Terminate") x Message.Terminate::class %= "Terminate"
			
			start at "Initial"
		}
		
		thread(name = threadName, isDaemon = isDaemon, start = true) {
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
					val message = queue!!.take()
					logLevel.normal(key, queue!!.size, message.id) { DAEMON_START_MESSAGE }
					core.input(message)
					logLevel.normal(key, message.id) { DAEMON_END_MESSAGE }
				}
			}
			catch (e: InterruptedException)
			{
				core.input(Message.Terminate(null))
				queue!!.clear()
				queue = null
			}
			logLevel.normal(key, threadName, isDaemon) { DAEMON_TERMINATE_THREAD }
		}
	}
	
	override fun run(payload: Any?)
	{
		val runM = Message.Run(payload)
		logLevel.detail(key, runM, runM.id) { DAEMON_MESSAGE }
		queue?.offer(runM)
	}
	
	override fun pause(payload: Any?)
	{
		val pauseM = Message.Pause(payload)
		logLevel.detail(key, pauseM, pauseM.id) { DAEMON_MESSAGE }
		queue?.offer(pauseM)
	}
	
	override fun stop(payload: Any?)
	{
		val stopM = Message.Stop(payload)
		logLevel.detail(key, stopM, stopM.id) { DAEMON_MESSAGE }
		queue?.offer(stopM)
	}
	
	override fun terminate(payload: Any?)
	{
		val terminateM = Message.Terminate(payload)
		logLevel.detail(key, terminateM, terminateM.id) { DAEMON_MESSAGE }
		queue?.offer(terminateM)
	}
	
	override fun input(signal: SIGNAL, priority: Int)
	{
		val signalM = Message.Signal(signal, priority)
		logLevel.detail(key, signalM.signal, signalM.priority, signalM.id) { DAEMON_MESSAGE_SIGNAL }
		queue?.offer(signalM)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, priority: Int)
	{
		val typedM = Message.TypedSignal(signal, type as KClass<SIGNAL>, priority)
		logLevel.detail(key, typedM.signal, "${typedM.type.simpleName}::class", typedM.priority, typedM.id) { DAEMON_MESSAGE_TYPED }
		queue?.offer(typedM)
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun modify(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
	{
		val modifyM = Message.Modify(block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit)
		logLevel.detail(key, modifyM, modifyM.id) { DAEMON_MESSAGE }
		queue?.offer(modifyM)
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
			override fun start(block: KotlmataCallback)
			{
				this@InitializerImpl shouldNot expired
				onStart = block
			}
			
			override fun pause(block: KotlmataCallback)
			{
				this@InitializerImpl shouldNot expired
				onPause = block
			}
			
			override fun stop(block: KotlmataCallback)
			{
				this@InitializerImpl shouldNot expired
				onStop = block
			}
			
			override fun resume(block: KotlmataCallback)
			{
				this@InitializerImpl shouldNot expired
				onResume = block
			}
			
			override fun terminate(block: KotlmataCallback)
			{
				this@InitializerImpl shouldNot expired
				onTerminate = block
			}
			
			override fun error(block: KotlmataFallback) = initializer.on.error(block)
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
	
	private sealed class Message(val priority: Int) : Comparable<Message>
	{
		constructor(base: Int, additional: Int) : this(if (additional > 0) base + additional else base)
		
		open class Control(val payload: Any?) : Message(DAEMON_CONTROL)
		class Run(payload: Any?) : Control(payload)
		class Pause(payload: Any?) : Control(payload)
		class Stop(payload: Any?) : Control(payload)
		class Terminate(payload: Any?) : Control(payload)
		
		class Modify(val block: KotlmataMutableMachine.Modifier.(DAEMON) -> Unit) : Message(MODIFY)
		
		class Sync(val signal: SIGNAL) : Message(SYNC)
		
		class Signal(val signal: SIGNAL, priority: Int) : Message(SIGNAL, priority)
		class TypedSignal(val signal: SIGNAL, val type: KClass<SIGNAL>, priority: Int) : Message(SIGNAL, priority)
		
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
		
		val isSignal = priority >= SIGNAL
		
		fun olderThan(other: Message): Boolean = order < other.order
		
		override fun compareTo(other: Message): Int
		{
			val dP = priority - other.priority
			return if (dP != 0) dP
			else (order - other.order).toInt()
		}
		
		override fun toString(): String
		{
			return this::class.simpleName ?: super.toString()
		}
	}
}