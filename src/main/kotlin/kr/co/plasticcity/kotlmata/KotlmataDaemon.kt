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
				block: Initializer.() -> KotlmataMachine.Init.End
		): KotlmataDaemon = KotlmataDaemonImpl(name, block)
	}
	
	interface Initializer : KotlmataMachine.Initializer
	{
		val on: On
		override val init: KotlmataMachine.Init
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
				block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Init.End
		): KotlmataDaemon = KotlmataDaemonImpl(name, block)
	}
	
	operator fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit)
	
	infix fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
}

private class KotlmataDaemonImpl(
		key: KEY? = null,
		block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Init.End
) : KotlmataMutableDaemon
{
	override val key: KEY = key ?: this
	
	var start: () -> Unit = {}
	var pause: () -> Unit = {}
	var stop: () -> Unit = {}
	var resume: () -> Unit = {}
	var terminate: () -> Unit = {}
	
	val queue: PriorityBlockingQueue<Message> = PriorityBlockingQueue()
	val engine: KotlmataMachine
	val machine: KotlmataMutableMachine
	
	init
	{
		machine = KotlmataMutableMachine(this.key) {
			val initializer = InitializerImpl(block, this)
			DaemonOrigin {}
			DaemonOrigin x Message.Run::class %= initializer.origin
			init origin state to DaemonOrigin
		}
		
		engine = KotlmataMachine {
			DaemonOrigin {
				input signal Message.Run::class action { start() }
				input signal Message.Pause::class action { start() }
				input signal Message.Stop::class action { start() }
				input signal Message.Terminate::class action { start() }
				input signal Message.Modify::class action {
					machine modify it.block
				}
			}
			
			Message.Run::class {
				entry via Message.Pause::class action { resume() }
				entry via Message.Stop::class action { resume() }
				
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
			
			Message.Pause::class {
				val temp: MutableList<Message> = ArrayList()
				
				entry action { pause() }
				
				input signal Message.Run::class action { queue += temp }
				input signal Message.Stash::class action { temp += it }
				input signal Message.Signal::class action { temp += it }
				input signal Message.TypedSignal::class action { temp += it }
				input signal Message.Modify::class action { temp += it }
				
				exit action {
					temp.clear()
				}
			}
			
			Message.Stop::class {
				entry action { stop() }
				
				input signal Message.Run::class action { m ->
					queue.removeIf {
						it.isEvent() && it.isEarlierThan(m)
					}
				}
				input signal Message.Pause::class action { m ->
					queue.removeIf {
						it.isEvent() && it.isEarlierThan(m)
					}
				}
			}
			
			Message.Terminate::class{
				entry action {
					terminate()
					Thread.currentThread().interrupt()
				}
			}
			
			DaemonOrigin x Message.Run::class %= Message.Run::class
			DaemonOrigin x Message.Pause::class %= Message.Pause::class
			DaemonOrigin x Message.Stop::class %= Message.Stop::class
			
			Message.Run::class x Message.Pause::class %= Message.Pause::class
			Message.Run::class x Message.Stop::class %= Message.Stop::class
			
			Message.Pause::class x Message.Run::class %= Message.Run::class
			Message.Pause::class x Message.Stop::class %= Message.Stop::class
			
			Message.Stop::class x Message.Run::class %= Message.Run::class
			Message.Stop::class x Message.Pause::class %= Message.Pause::class
			
			any x Message.Terminate::class %= Message.Terminate::class
			
			init origin state to DaemonOrigin
		}
		
		thread(name = "KotlmataDaemon[key]", isDaemon = true, start = true) {
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
			block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Init.End,
			initializer: KotlmataMachine.Initializer
	) : KotlmataDaemon.Initializer, KotlmataMachine.Initializer by initializer, Expirable({ Log.e(key) { EXPIRED_DAEMON_MODIFIER } })
	{
		lateinit var origin: STATE
		
		override val on: KotlmataDaemon.On = object : KotlmataDaemon.On
		{
			override fun start(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				start = block
			}
			
			override fun pause(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				pause = block
			}
			
			override fun stop(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				stop = block
			}
			
			override fun resume(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				resume = block
			}
			
			override fun terminate(block: () -> Unit)
			{
				this@InitializerImpl shouldNot expired
				terminate = block
			}
		}
		
		override val init: KotlmataMachine.Init = object : KotlmataMachine.Init
		{
			override fun origin(keyword: state) = object : KotlmataMachine.Init.to
			{
				override fun to(state: STATE): KotlmataMachine.Init.End
				{
					this@InitializerImpl shouldNot expired
					origin = state
					return KotlmataMachine.Init.End()
				}
			}
		}
		
		init
		{
			block()
			expire()
		}
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
	
	override fun compareTo(other: Message): Int
	{
		val dP = priority - other.priority
		return if (dP != 0) dP
		else (other.order - order).toInt()
	}
	
	fun isEvent(): Boolean = priority == EVENT
	fun isEarlierThan(other: Message): Boolean = order < other.order
}