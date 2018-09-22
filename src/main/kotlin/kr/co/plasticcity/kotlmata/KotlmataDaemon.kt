package kr.co.plasticcity.kotlmata

import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.reflect.KClass

interface KotlmataDaemon
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: Initializer.(name: DAEMON) -> KotlmataMachine.Initializer.End
		): KotlmataDaemon = KotlmataDaemonImpl(name, block)
	}
	
	interface Initializer : KotlmataMachine.Initializer
	{
		val on: On
		
		interface On
		{
			infix fun start(block: () -> Unit)
			infix fun pause(block: () -> Unit)
			infix fun stop(block: () -> Unit)
			infix fun resume(block: () -> Unit)
			infix fun terminate(block: () -> Unit)
		}
	}
	
	val key: DAEMON
	
	fun run()
	fun pause()
	fun stop()
	fun terminate()
	
	/**
	 * @param priority Smaller means higher. Priority must be greater than zero.
	 */
	fun input(signal: SIGNAL, priority: Int = 0)
	
	/**
	 * @param priority Smaller means higher. Priority must be greater than zero.
	 */
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>, priority: Int = 0)
}

interface KotlmataMutableDaemon<out T : DAEMON> : KotlmataDaemon
{
	companion object
	{
		operator fun invoke(
				name: String,
				block: KotlmataDaemon.Initializer.(name: String) -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon<String> = KotlmataDaemonImpl(name, block)
		
		internal operator fun <T : DAEMON> invoke(
				key: T,
				block: KotlmataDaemon.Initializer.(key: T) -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon<T> = KotlmataDaemonImpl(key, block)
	}
	
	infix fun modify(block: KotlmataMutableMachine.Modifier.(key: T) -> Unit)
	
	operator fun invoke(block: KotlmataMutableMachine.Modifier.(key: T) -> Unit) = modify(block)
}

private class KotlmataDaemonImpl<T : DAEMON>(
		override val key: T,
		block: KotlmataDaemon.Initializer.(key: T) -> KotlmataMachine.Initializer.End
) : KotlmataMutableDaemon<T>
{
	private var logLevel = Log.logLevel
	
	private val machine: KotlmataMutableMachine<T>
	private val engine: KotlmataMachine
	
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
		
		machine = KotlmataMutableMachine(key, "Daemon[$key]:   ") { _ ->
			/* For avoid log print for PreStart. */
			log level 0
			PreStart {}
			log level Log.logLevel
			
			val initializer = InitializerImpl(block, this)
			PreStart x Message.Run::class %= initializer.initial
			start at PreStart
		}
		
		val modifyMachine: (Message.Modify) -> Unit = {
			machine modify it.block
		}
		
		val postExpress: (SIGNAL) -> Unit = {
			val m = Message.Express(it)
			logLevel.detail(key, m.signal, m.id) { DAEMON_REQUEST_EXPRESS }
			queue!!.offer(m)
		}
		
		val ignore: (SIGNAL, STATE) -> Unit = { message, state ->
			if (message is Message)
			{
				logLevel.normal(key, state, message.id) { DAEMON_IGNORE_REQUEST }
			}
		}
		
		engine = KotlmataMachine("$key@engine", 0) { _ ->
			"pre-start" { state ->
				val startMachine: (Message) -> Unit = {
					logLevel.simple(key) { DAEMON_START }
					onStart()
					machine.input(Message.Run(), postExpress)
				}
				
				input signal Message.Run::class action startMachine
				input signal Message.Pause::class action startMachine
				input signal Message.Stop::class action startMachine
				input signal Message.Terminate::class action {}
				
				input signal Message.Modify::class action modifyMachine
				
				input action { ignore(it, state) }
			}
			
			"run" { state ->
				input signal Message.Pause::class action {}
				input signal Message.Stop::class action {}
				input signal Message.Terminate::class action {}
				
				input signal Message.Express::class action {
					machine.input(it.signal, postExpress)
				}
				input signal Message.Signal::class action {
					machine.input(it.signal, postExpress)
				}
				input signal Message.TypedSignal::class action {
					machine.input(it.signal, it.type, postExpress)
				}
				input signal Message.Modify::class action modifyMachine
				
				input action { ignore(it, state) }
			}
			
			"pause" { state ->
				val stash: MutableList<Message> = ArrayList()
				
				val keep: (Message) -> Unit = {
					logLevel.normal(key, it.id) { DAEMON_KEEP_REQUEST }
					stash += it
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
				
				input action { ignore(it, state) }
				
				exit action {
					stash.clear()
				}
			}
			
			"stop" { state ->
				var express: Message.Express? = null
				
				val cleanup = { m: Message ->
					synchronized<Unit>(lock)
					{
						queue!!.removeIf {
							(it.isEvent && it.isEarlierThan(m)).apply {
								if (this)
								{
									logLevel.detail(key, it.id) { DAEMON_DROP_REQUEST }
								}
							}
						}
						express?.let { queue!!.offer(it) }
					}
				}
				
				entry action {
					logLevel.simple(key) { DAEMON_STOP }
					onStop()
				}
				
				input signal Message.Run::class action {
					cleanup(it)
					logLevel.simple(key) { DAEMON_RESUME }
					onResume()
				}
				input signal Message.Pause::class action cleanup
				input signal Message.Terminate::class action {}
				
				input signal Message.Express::class action {
					logLevel.normal(key, it.id) { DAEMON_KEEP_EXPRESS }
					express = it
				}
				
				input action { ignore(it, state) }
				
				exit action {
					express = null
				}
			}
			
			"terminate" { _ ->
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
		
		thread(name = "KotlmataDaemon[$key]", isDaemon = true, start = true) {
			try
			{
				while (true)
				{
					val m = queue!!.take()
					logLevel.normal(key, m.id) { DAEMON_START_REQUEST }
					engine.input(m)
					logLevel.normal(key, m.id) { DAEMON_END_REQUEST }
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
			val m = Message.Run()
			logLevel.detail(key, m, m.id) { DAEMON_REQUEST }
			queue?.offer(m)
		}
	}
	
	override fun pause()
	{
		synchronized<Unit>(lock) {
			val m = Message.Pause()
			logLevel.detail(key, m, m.id) { DAEMON_REQUEST }
			queue?.offer(m)
		}
	}
	
	override fun stop()
	{
		synchronized<Unit>(lock) {
			val m = Message.Stop()
			logLevel.detail(key, m, m.id) { DAEMON_REQUEST }
			queue?.offer(m)
		}
	}
	
	override fun terminate()
	{
		synchronized<Unit>(lock) {
			val m = Message.Terminate()
			logLevel.detail(key, m, m.id) { DAEMON_REQUEST }
			queue?.offer(m)
		}
	}
	
	override fun input(signal: SIGNAL, priority: Int)
	{
		synchronized<Unit>(lock) {
			val m = Message.Signal(signal, priority)
			logLevel.detail(key, m.signal, m.priority, m.id) { DAEMON_REQUEST_SIGNAL }
			queue?.offer(m)
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>, priority: Int)
	{
		synchronized<Unit>(lock) {
			val m = Message.TypedSignal(signal, type as KClass<SIGNAL>, priority)
			logLevel.detail(key, m.signal, "${m.type.simpleName}::class", m.priority, m.id) { DAEMON_REQUEST_TYPED }
			queue?.offer(m)
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun modify(block: KotlmataMutableMachine.Modifier.(key: T) -> Unit)
	{
		synchronized<Unit>(lock) {
			val m = Message.Modify(block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit)
			logLevel.detail(key, m, m.id) { DAEMON_REQUEST }
			queue?.offer(m)
		}
	}
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
	
	private inner class InitializerImpl internal constructor(
			block: KotlmataDaemon.Initializer.(key: T) -> KotlmataMachine.Initializer.End,
			initializer: KotlmataMachine.Initializer
	) : KotlmataDaemon.Initializer, KotlmataMachine.Initializer by initializer, Expirable({ Log.e("Daemon", key) { EXPIRED_AGENT_MODIFIER } })
	{
		lateinit var initial: STATE
		
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
		}
		
		override val log = object : KotlmataMachine.Initializer.Log
		{
			override fun level(level: Int)
			{
				this@InitializerImpl shouldNot expired
				initializer.log level level
				logLevel = level
			}
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
		class Run : Message(CONTROL)
		class Pause : Message(CONTROL)
		class Stop : Message(CONTROL)
		class Terminate : Message(CONTROL)
		
		class Express(val signal: SIGNAL) : Message(EXPRESS)
		
		class Signal(val signal: SIGNAL, priority: Int)
			: Message(if (priority > 0) EVENT + priority else EVENT)
		
		class TypedSignal(val signal: SIGNAL, val type: KClass<SIGNAL>, priority: Int)
			: Message(if (priority > 0) EVENT + priority else EVENT)
		
		class Modify(val block: KotlmataMutableMachine.Modifier.(key: DAEMON) -> Unit) : Message(EVENT)
		
		companion object
		{
			private const val CONTROL = -2
			private const val EXPRESS = -1
			private const val EVENT = 0
			
			val ticket: AtomicLong = AtomicLong(0)
		}
		
		val order = ticket.getAndIncrement()
		val id by lazy { hashCode().toString(16) }
		
		val isEvent = priority >= EVENT
		
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