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
		operator fun invoke(
				name: String,
				block: Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		): KotlmataDaemon<String> = KotlmataDaemonImpl(name, block)
		
		internal operator fun invoke(
				name: String,
				threadName: String,
				block: Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		): KotlmataDaemon<String> = KotlmataDaemonImpl(name, block, threadName)
	}
	
	@KotlmataMarker
	interface Initializer : KotlmataMachine.Initializer
	{
		override val on: On
		
		interface On : KotlmataMachine.Initializer.On
		{
			infix fun start(block: () -> Unit)
			infix fun pause(block: () -> Unit)
			infix fun stop(block: () -> Unit)
			infix fun resume(block: () -> Unit)
			infix fun terminate(block: () -> Unit)
		}
	}
	
	val key: T
	
	fun run()
	fun pause()
	fun stop()
	fun terminate()
	
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
		operator fun invoke(
				name: String,
				block: KotlmataDaemon.Initializer.(daemon: String) -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon<String> = KotlmataDaemonImpl(name, block)
		
		internal operator fun <T : DAEMON> invoke(
				key: T,
				block: KotlmataDaemon.Initializer.(daemon: T) -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon<T> = KotlmataDaemonImpl(key, block)
	}
	
	infix fun modify(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
	
	operator fun invoke(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit) = modify(block)
}

private class KotlmataDaemonImpl<T : DAEMON>(
		override val key: T,
		block: KotlmataDaemon.Initializer.(T) -> KotlmataMachine.Initializer.End,
		threadName: String = "KotlmataDaemon[$key]"
) : KotlmataMutableDaemon<T>
{
	private var logLevel = Log.logLevel
	
	private val machine: KotlmataMutableMachine<T>
	private val core: KotlmataMachine<String>
	
	private var onStart: () -> Unit = {}
	private var onPause: () -> Unit = {}
	private var onStop: () -> Unit = {}
	private var onResume: () -> Unit = {}
	private var onTerminate: () -> Unit = {}
	
	private var queue: PriorityBlockingQueue<Message>? = PriorityBlockingQueue()
	private val lock: Any = Any()
	
	init
	{
		logLevel.normal(key) { DAEMON_START_INIT }
		
		machine = KotlmataMutableMachine(key, "Daemon[$key]:   ") {
			/* For avoid log print for PreStart. */
			log level 0
			PreStart {}
			log level logLevel
			
			val initializer = InitializerImpl(block, this)
			PreStart x Message.Run::class %= initializer.initial
			start at PreStart
		}
		
		val modifyMachine: KotlmataStateAction.(Message.Modify) -> Unit = { modifyM ->
			machine modify modifyM.block
		}
		
		val postExpress: (SIGNAL) -> Unit = { signal ->
			val expressM = Message.Express(signal)
			logLevel.detail(key, expressM.signal, expressM.id) { DAEMON_REQUEST_EXPRESS }
			queue!!.offer(expressM)
		}
		
		val ignore: (SIGNAL, STATE) -> Unit = { signal, state ->
			if (signal is Message)
			{
				logLevel.normal(key, state, signal.id) { DAEMON_IGNORE_REQUEST }
			}
		}
		
		core = KotlmataMachine("$key@core", 0) {
			"pre-start" { state ->
				val startMachine: KotlmataStateAction.(Message) -> Unit = {
					logLevel.simple(key) { DAEMON_START }
					onStart()
					machine.input(Message.Run(), postExpress)
				}
				
				input signal Message.Run::class action startMachine
				input signal Message.Pause::class action startMachine
				input signal Message.Stop::class action startMachine
				input signal Message.Terminate::class action {}
				
				input signal Message.Modify::class action modifyMachine
				
				input action { signal -> ignore(signal, state) }
			}
			
			"run" { state ->
				input signal Message.Pause::class action {}
				input signal Message.Stop::class action {}
				input signal Message.Terminate::class action {}
				
				input signal Message.Express::class action { expressM ->
					machine.input(expressM.signal, postExpress)
				}
				input signal Message.Signal::class action { signalM ->
					machine.input(signalM.signal, postExpress)
				}
				input signal Message.TypedSignal::class action { typedM ->
					machine.input(typedM.signal, typedM.type, postExpress)
				}
				input signal Message.Modify::class action modifyMachine
				
				input action { message -> ignore(message, state) }
			}
			
			"pause" { state ->
				val stash: MutableList<Message> = ArrayList()
				
				val keep: KotlmataStateAction.(Message) -> Unit = { message ->
					logLevel.normal(key, message.id) { DAEMON_KEEP_REQUEST }
					stash += message
				}
				
				entry action {
					logLevel.simple(key) { DAEMON_PAUSE }
					onPause()
				}
				
				input signal Message.Run::class action {
					queue!! += stash
					logLevel.simple(key) { DAEMON_RESUME }
					onResume()
				}
				input signal Message.Stop::class action {}
				input signal Message.Terminate::class action {}
				
				input signal Message.Express::class action keep
				input signal Message.Signal::class action keep
				input signal Message.TypedSignal::class action keep
				input signal Message.Modify::class action keep
				
				input action { signal -> ignore(signal, state) }
				
				exit action {
					stash.clear()
				}
			}
			
			"stop" { state ->
				var stash: Message.Express? = null
				
				val cleanup: KotlmataStateAction.(Message) -> Unit = { currentM ->
					synchronized<Unit>(lock)
					{
						queue!!.removeIf { queueM ->
							(queueM.isMachineEvent && queueM.isEarlierThan(currentM)).apply {
								if (this)
								{
									logLevel.detail(key, queueM.id) { DAEMON_DROP_REQUEST }
								}
							}
						}
						stash?.let { expressM -> queue!!.offer(expressM) }
					}
				}
				
				entry action {
					logLevel.simple(key) { DAEMON_STOP }
					onStop()
				}
				
				input signal Message.Run::class action { runM ->
					cleanup(runM)
					logLevel.simple(key) { DAEMON_RESUME }
					onResume()
				}
				input signal Message.Pause::class action cleanup
				input signal Message.Terminate::class action {}
				
				input signal Message.Express::class action { expressM ->
					logLevel.normal(key, expressM.id) { DAEMON_KEEP_EXPRESS }
					stash = expressM
				}
				
				input action { signal -> ignore(signal, state) }
				
				exit action {
					stash = null
				}
			}
			
			"terminate" {
				entry action {
					logLevel.simple(key) { DAEMON_TERMINATE }
					onTerminate()
					Thread.currentThread().interrupt()
				}
			}
			
			"pre-start" x Message.Run::class %= "run"
			"pre-start" x Message.Pause::class %= "pause"
			"pre-start" x Message.Stop::class %= "stop"
			
			"run" x Message.Pause::class %= "pause"
			"run" x Message.Stop::class %= "stop"
			
			"pause" x Message.Run::class %= "run"
			"pause" x Message.Stop::class %= "stop"
			
			"stop" x Message.Run::class %= "run"
			"stop" x Message.Pause::class %= "pause"
			
			any x Message.Terminate::class %= "terminate"
			
			start at "pre-start"
		}
		
		thread(name = threadName, isDaemon = true, start = true) {
			try
			{
				while (true)
				{
					val message = queue!!.take()
					logLevel.normal(key, message.id) { DAEMON_START_REQUEST }
					core.input(message)
					logLevel.normal(key, message.id) { DAEMON_END_REQUEST }
				}
			}
			catch (e: InterruptedException)
			{
				synchronized(lock) {
					queue!!.clear()
					queue = null
				}
			}
		}
		
		logLevel.normal(key) { DAEMON_END_INIT }
	}
	
	override fun run()
	{
		synchronized<Unit>(lock) {
			val runM = Message.Run()
			logLevel.detail(key, runM, runM.id) { DAEMON_REQUEST }
			queue?.offer(runM)
		}
	}
	
	override fun pause()
	{
		synchronized<Unit>(lock) {
			val pauseM = Message.Pause()
			logLevel.detail(key, pauseM, pauseM.id) { DAEMON_REQUEST }
			queue?.offer(pauseM)
		}
	}
	
	override fun stop()
	{
		synchronized<Unit>(lock) {
			val stopM = Message.Stop()
			logLevel.detail(key, stopM, stopM.id) { DAEMON_REQUEST }
			queue?.offer(stopM)
		}
	}
	
	override fun terminate()
	{
		synchronized<Unit>(lock) {
			val terminateM = Message.Terminate()
			logLevel.detail(key, terminateM, terminateM.id) { DAEMON_REQUEST }
			queue?.offer(terminateM)
		}
	}
	
	override fun input(signal: SIGNAL, priority: Int)
	{
		synchronized<Unit>(lock) {
			val signalM = Message.Signal(signal, priority)
			logLevel.detail(key, signalM.signal, signalM.priority, signalM.id) { DAEMON_REQUEST_SIGNAL }
			queue?.offer(signalM)
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, priority: Int)
	{
		synchronized<Unit>(lock) {
			val typedM = Message.TypedSignal(signal, type as KClass<SIGNAL>, priority)
			logLevel.detail(key, typedM.signal, "${typedM.type.simpleName}::class", typedM.priority, typedM.id) { DAEMON_REQUEST_TYPED }
			queue?.offer(typedM)
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun modify(block: KotlmataMutableMachine.Modifier.(T) -> Unit)
	{
		synchronized<Unit>(lock) {
			val modifyM = Message.Modify(block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit)
			logLevel.detail(key, modifyM, modifyM.id) { DAEMON_REQUEST }
			queue?.offer(modifyM)
		}
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
		lateinit var initial: STATE
		
		override val log = object : KotlmataMachine.Initializer.Log
		{
			override fun level(level: Int)
			{
				this@InitializerImpl shouldNot expired
				initializer.log level level
				logLevel = level
			}
		}
		
		override val on = object : KotlmataDaemon.Initializer.On
		{
			override fun start(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				onStart = block
			}
			
			override fun pause(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				onPause = block
			}
			
			override fun stop(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				onStop = block
			}
			
			override fun resume(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				onResume = block
			}
			
			override fun terminate(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				onTerminate = block
			}
			
			override fun exception(block: (Exception) -> Unit) = initializer.on.exception(block)
		}
		
		override val start = object : KotlmataMachine.Initializer.Start
		{
			override fun at(state: STATE): KotlmataMachine.Initializer.End
			{
				this@InitializerImpl shouldNot expired
				
				/* For checking undefined initial state. */
				initializer.start at state
				
				initial = state
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
		class Run : Message(DAEMON_CONTROL)
		class Pause : Message(DAEMON_CONTROL)
		class Stop : Message(DAEMON_CONTROL)
		class Terminate : Message(DAEMON_CONTROL)
		
		class Express(val signal: SIGNAL) : Message(EXPRESS)
		
		class Signal(val signal: SIGNAL, priority: Int)
			: Message(if (priority > 0) MACHINE_EVENT + priority else MACHINE_EVENT)
		
		class TypedSignal(val signal: SIGNAL, val type: KClass<SIGNAL>, priority: Int)
			: Message(if (priority > 0) MACHINE_EVENT + priority else MACHINE_EVENT)
		
		class Modify(val block: KotlmataMutableMachine.Modifier.(DAEMON) -> Unit) : Message(MACHINE_EVENT)
		
		companion object
		{
			private const val DAEMON_CONTROL = -2
			private const val EXPRESS = -1
			private const val MACHINE_EVENT = 0
			
			val ticket: AtomicLong = AtomicLong(0)
		}
		
		val order = ticket.getAndIncrement()
		val id by lazy { hashCode().toString(16) }
		
		val isMachineEvent = priority >= MACHINE_EVENT
		
		fun isEarlierThan(other: Message): Boolean = order < other.order
		
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