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
		override val start: KotlmataMachine.Initializer.Start
	}
	
	interface On
	{
		infix fun start(block: () -> Unit)
		infix fun pause(block: () -> Unit)
		infix fun stop(block: () -> Unit)
		infix fun resume(block: () -> Unit)
		infix fun terminate(block: () -> Unit)
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
		): KotlmataDaemon = KotlmataDaemonImpl(name, block)
	}
	
	operator fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit)
	
	infix fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
}

private class KotlmataDaemonImpl(
		key: KEY? = null,
		block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End
) : KotlmataMutableDaemon
{
	override val key: KEY = key ?: this
	
	var onStart: () -> Unit = {}
	var onPause: () -> Unit = {}
	var onStop: () -> Unit = {}
	var onResume: () -> Unit = {}
	var onTerminate: () -> Unit = {}
	
	val queue: PriorityBlockingQueue<Message> = PriorityBlockingQueue()
	val engine: KotlmataMachine
	val machine: KotlmataMutableMachine
	
	init
	{
		machine = KotlmataMutableMachine(this.key) {
			val initializer = InitializerImpl(block, this)
			DaemonInitial {}
			DaemonInitial x Message.Run::class %= initializer.initial
			start at DaemonInitial
		}
		
		engine = KotlmataMachine("${this@KotlmataDaemonImpl.key}@engine") {
			"initial" {
				input signal Message.Run::class action {
					onStart()
					machine.input(Message.Run())
				}
				input signal Message.Pause::class action {
					onStart()
					machine.input(Message.Run())
				}
				input signal Message.Stop::class action {
					onStart()
					machine.input(Message.Run())
				}
				
				input signal Message.Modify::class action {
					machine modify it.block
				}
			}
			
			"run" {
				input signal Message.Stash::class action { m ->
					machine.input(m.signal) {
						queue.offer(Message.Stash(it))
					}
				}
				input signal Message.Signal::class action { m ->
					machine.input(m.signal) {
						queue.offer(Message.Stash(it))
					}
				}
				input signal Message.TypedSignal::class action { m ->
					machine.input(m.signal, m.type) {
						queue.offer(Message.Stash(it))
					}
				}
				input signal Message.Modify::class action {
					machine modify it.block
				}
			}
			
			"pause" {
				val temp: MutableList<Message> = ArrayList()
				
				entry action { onPause() }
				
				input signal Message.Run::class action {
					queue += temp
					onResume()
				}
				input signal Message.Stash::class action { temp += it }
				input signal Message.Signal::class action { temp += it }
				input signal Message.TypedSignal::class action { temp += it }
				input signal Message.Modify::class action { temp += it }
				
				exit action {
					temp.clear()
				}
			}
			
			"stop" {
				entry action { onStop() }
				
				fun arrange(m: Message)
				{
					synchronized(queue)
					{
						queue.removeIf {
							it.isEvent && it.isEarlierThan(m)
						}
					}
				}
				
				input signal Message.Run::class action { m ->
					arrange(m)
					onResume()
				}
				input signal Message.Pause::class action { m ->
					arrange(m)
				}
			}
			
			"terminate" {
				entry action {
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
					engine.input(queue.take())
				}
			}
			catch (e: InterruptedException)
			{
				queue.clear()
			}
		}
	}
	
	override fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		synchronized(queue) {
			queue.offer(Message.Modify(block))
		}
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		synchronized(queue) {
			queue.offer(Message.Modify(block))
		}
	}
	
	override fun input(signal: SIGNAL)
	{
		synchronized(queue) {
			queue.offer(Message.Signal(signal))
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T : SIGNAL> input(signal: T, type: KClass<in T>)
	{
		synchronized(queue) {
			queue.offer(Message.TypedSignal(signal, type as KClass<SIGNAL>))
		}
	}
	
	override fun run()
	{
		synchronized(queue) {
			queue.offer(Message.Run())
		}
	}
	
	override fun pause()
	{
		synchronized(queue) {
			queue.offer(Message.Pause())
		}
	}
	
	override fun stop()
	{
		synchronized(queue) {
			queue.offer(Message.Stop())
		}
	}
	
	override fun terminate()
	{
		synchronized(queue) {
			queue.offer(Message.Terminate())
		}
	}
	
	private inner class InitializerImpl internal constructor(
			block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End,
			initializer: KotlmataMachine.Initializer
	) : KotlmataDaemon.Initializer, KotlmataMachine.Initializer by initializer, Expirable({ Log.e(key) { EXPIRED_DAEMON_MODIFIER } })
	{
		lateinit var initial: STATE
		
		override val on = object : KotlmataDaemon.On
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
		
		override val start = object : KotlmataMachine.Initializer.Start
		{
			override fun at(state: STATE): KotlmataMachine.Initializer.End
			{
				this@InitializerImpl shouldNot expired
				
				/* for checking undefined initial state */
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
	
	override fun toString(): String
	{
		return hashCode().toString(16)
	}
}

private sealed class Message(val priority: Int) : Comparable<Message>
{
	class Run : Message(CONTROL)
	class Pause : Message(CONTROL)
	class Stop : Message(CONTROL)
	class Terminate : Message(CONTROL)
	
	class Stash(val signal: SIGNAL) : Message(STASH)
	
	class Signal(val signal: SIGNAL) : Message(EVENT)
	class TypedSignal(val signal: SIGNAL, val type: KClass<SIGNAL>) : Message(EVENT)
	class Modify(val block: KotlmataMutableMachine.Modifier.() -> Unit) : Message(EVENT)
	
	companion object
	{
		private const val CONTROL = 2
		private const val STASH = 1
		private const val EVENT = 0
		
		val ticket: AtomicLong = AtomicLong(0)
	}
	
	val order = ticket.getAndIncrement()
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