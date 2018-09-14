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
				name: String? = null,
				block: Initializer.() -> KotlmataMachine.Initializer.End
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
	
	val key: KEY
	
	fun run()
	fun pause()
	fun stop()
	fun terminate()
	
	fun input(signal: SIGNAL)
	fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
}

interface KotlmataMutableDaemon : KotlmataDaemon
{
	companion object
	{
		operator fun invoke(
				name: String? = null,
				block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon = KotlmataDaemonImpl(name, block)
		
		internal operator fun invoke(
				key: KEY,
				block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End
		): KotlmataMutableDaemon = KotlmataDaemonImpl(key, block)
	}
	
	infix fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
	
	operator fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit) = modify(block)
}

private class KotlmataDaemonImpl(
		key: KEY? = null,
		block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End
) : KotlmataMutableDaemon
{
	override val key: KEY = key ?: this
	
	private var logLevel = NORMAL
	
	private val queue: PriorityBlockingQueue<Message> = PriorityBlockingQueue()
	private val machine: KotlmataMutableMachine
	private val engine: KotlmataMachine
	
	private var onStart: () -> Unit = {}
	private var onPause: () -> Unit = {}
	private var onStop: () -> Unit = {}
	private var onResume: () -> Unit = {}
	private var onTerminate: () -> Unit = {}
	
	init
	{
		machine = KotlmataMutableMachine(this.key) {
			val initializer = InitializerImpl(block, this)
			Initial {}
			Initial x Message.Run::class %= initializer.initial
			start at Initial
		}
		
		engine = KotlmataMachine("${this.key}@engine") {
			log level 0
			
			val postQuickInput: (SIGNAL) -> Unit = {
				val m = Message.QuickInput(it)
				logLevel.detail(this@KotlmataDaemonImpl.key, m, m.signal, m.id) { DAEMON_POST_QUICK_INPUT }
				queue.offer(m)
			}
			
			val modifyMachine: (Message.Modify) -> Unit = {
				machine modify it.block
			}
			
			val ignoreMessage: (SIGNAL, String) -> Unit = { message, current ->
				if (logLevel.isDetail() && message is Message)
				{
					logLevel.detail(this@KotlmataDaemonImpl.key, current, message.id) { DAEMON_MESSAGE_IGNORED }
				}
			}
			
			"initial" {
				val startMachine: (Message) -> Unit = {
					logLevel.simple(this@KotlmataDaemonImpl.key) { DAEMON_START }
					onStart()
					machine.input(Message.Run(), postQuickInput)
				}
				
				input signal Message.Run::class action startMachine
				input signal Message.Pause::class action startMachine
				input signal Message.Stop::class action startMachine
				input signal Message.Terminate::class action {}
				
				input signal Message.Modify::class action modifyMachine
				
				input action { ignoreMessage(it, "initial") }
			}
			
			"run" {
				input signal Message.Pause::class action {}
				input signal Message.Stop::class action {}
				input signal Message.Terminate::class action {}
				
				input signal Message.QuickInput::class action {
					machine.input(it.signal, postQuickInput)
				}
				input signal Message.Input::class action {
					machine.input(it.signal, postQuickInput)
				}
				input signal Message.TypedInput::class action {
					machine.input(it.signal, it.type, postQuickInput)
				}
				input signal Message.Modify::class action modifyMachine
				
				input action { ignoreMessage(it, "run") }
			}
			
			"pause" {
				val stash: MutableList<Message> = ArrayList()
				
				val keep: (Message) -> Unit = {
					logLevel.detail(this@KotlmataDaemonImpl.key, it.id) { DAEMON_KEEP_MESSAGE }
					stash += it
				}
				
				entry action {
					logLevel.simple(this@KotlmataDaemonImpl.key) { DAEMON_PAUSE }
					onPause()
				}
				
				input signal Message.Run::class action {
					queue += stash
					logLevel.simple(this@KotlmataDaemonImpl.key) { DAEMON_RESUME }
					onResume()
				}
				input signal Message.Stop::class action {}
				input signal Message.Terminate::class action {}
				
				input signal Message.QuickInput::class action keep
				input signal Message.Input::class action keep
				input signal Message.TypedInput::class action keep
				input signal Message.Modify::class action keep
				
				input action { ignoreMessage(it, "pause") }
				
				exit action {
					stash.clear()
				}
			}
			
			"stop" {
				var quickInput: Message.QuickInput? = null
				
				fun cleanup(m: Message)
				{
					synchronized(queue)
					{
						queue.removeIf {
							(it.isEvent && it.isEarlierThan(m)).apply {
								if (this)
								{
									logLevel.detail(this@KotlmataDaemonImpl.key, it.id) { DAEMON_MESSAGE_DROPPED }
								}
							}
						}
						quickInput?.let { queue.offer(it) }
					}
				}
				
				entry action {
					logLevel.simple(this@KotlmataDaemonImpl.key) { DAEMON_STOP }
					onStop()
				}
				
				input signal Message.Run::class action {
					cleanup(it)
					logLevel.simple(this@KotlmataDaemonImpl.key) { DAEMON_RESUME }
					onResume()
				}
				input signal Message.Pause::class action ::cleanup
				input signal Message.Terminate::class action {}
				
				input signal Message.QuickInput::class action {
					logLevel.detail(this@KotlmataDaemonImpl.key, it.id) { DAEMON_KEEP_QUICK_INPUT }
					quickInput = it
				}
				
				input action { ignoreMessage(it, "stop") }
				
				exit action {
					quickInput = null
				}
			}
			
			"terminate" {
				entry action {
					logLevel.simple(this@KotlmataDaemonImpl.key) { DAEMON_TERMINATE }
					onTerminate()
					Thread.currentThread().interrupt()
				}
			}
			
			"initial" x Message.Run::class %= "run"
			"initial" x Message.Pause::class %= "pause"
			"initial" x Message.Stop::class %= "stop"
			
			"run" x Message.Pause::class %= "pause"
			"run" x Message.Stop::class %= "stop"
			
			"pause" x Message.Run::class %= "run"
			"pause" x Message.Stop::class %= "stop"
			
			"stop" x Message.Run::class %= "run"
			"stop" x Message.Pause::class %= "pause"
			
			any x Message.Terminate::class %= "terminate"
			
			start at "initial"
		}
		
		thread(name = "KotlmataDaemon[${this@KotlmataDaemonImpl.key}]", isDaemon = true, start = true) {
			try
			{
				while (true)
				{
					val m = queue.take()
					logLevel.detail(this@KotlmataDaemonImpl.key, m.id) { DAEMON_START_MESSAGE }
					engine.input(m)
					logLevel.detail(this@KotlmataDaemonImpl.key, m.id) { DAEMON_END_MESSAGE }
				}
			}
			catch (e: InterruptedException)
			{
				queue.clear()
			}
		}
	}
	
	override fun input(signal: SIGNAL)
	{
		synchronized(queue) {
			val m = Message.Input(signal)
			logLevel.detail(key, m, m.signal, m.id) { DAEMON_POST_INPUT }
			queue.offer(m)
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	{
		synchronized(queue) {
			val m = Message.TypedInput(signal, type as KClass<SIGNAL>)
			logLevel.detail(key, m, m.signal, m.type.simpleName, m.id) { DAEMON_POST_TYPED_INPUT }
			queue.offer(m)
		}
	}
	
	override fun run()
	{
		synchronized(queue) {
			val m = Message.Run()
			logLevel.detail(key, m, m.id) { DAEMON_POST_MESSAGE }
			queue.offer(m)
		}
	}
	
	override fun pause()
	{
		synchronized(queue) {
			val m = Message.Pause()
			logLevel.detail(key, m, m.id) { DAEMON_POST_MESSAGE }
			queue.offer(m)
		}
	}
	
	override fun stop()
	{
		synchronized(queue) {
			val m = Message.Stop()
			logLevel.detail(key, m, m.id) { DAEMON_POST_MESSAGE }
			queue.offer(m)
		}
	}
	
	override fun terminate()
	{
		synchronized(queue) {
			val m = Message.Terminate()
			logLevel.detail(key, m, m.id) { DAEMON_POST_MESSAGE }
			queue.offer(m)
		}
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		synchronized(queue) {
			val m = Message.Modify(block)
			logLevel.detail(key, m, m.id) { DAEMON_POST_MESSAGE }
			queue.offer(m)
		}
	}
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
	
	private inner class InitializerImpl internal constructor(
			block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End,
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
				
				/* For checking undefined initial state */
				initializer.start at state
				
				initial = state
				return KotlmataMachine.Initializer.End()
			}
		}
		
		init
		{
			block()
			expire()
		}
	}
	
	private sealed class Message(val priority: Int) : Comparable<Message>
	{
		class Run : Message(CONTROL)
		class Pause : Message(CONTROL)
		class Stop : Message(CONTROL)
		class Terminate : Message(CONTROL)
		
		class QuickInput(val signal: SIGNAL) : Message(QUICK)
		
		class Input(val signal: SIGNAL) : Message(EVENT)
		class TypedInput(val signal: SIGNAL, val type: KClass<SIGNAL>) : Message(EVENT)
		class Modify(val block: KotlmataMutableMachine.Modifier.() -> Unit) : Message(EVENT)
		
		companion object
		{
			private const val CONTROL = 2
			private const val QUICK = 1
			private const val EVENT = 0
			
			val ticket: AtomicLong = AtomicLong(0)
		}
		
		val order = ticket.getAndIncrement()
		val id by lazy { hashCode().toString(16) }
		
		val isEvent = priority == EVENT
		
		fun isEarlierThan(other: Message): Boolean = order < other.order
		
		override fun compareTo(other: Message): Int
		{
			val dP = other.priority - priority
			return if (dP != 0) dP
			else (order - other.order).toInt()
		}
		
		override fun toString(): String
		{
			return this::class.simpleName ?: super.toString()
		}
	}
}