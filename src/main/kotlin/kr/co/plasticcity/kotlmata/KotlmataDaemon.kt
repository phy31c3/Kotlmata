package kr.co.plasticcity.kotlmata

import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
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
	
	val key: Any
	var start: () -> Unit
	var pause: () -> Unit
	var stop: () -> Unit
	var resume: () -> Unit
	var terminate: () -> Unit
	
	fun run()
	fun pause()
	fun stop()
	fun terminate()
	
	fun input(signal: SIGNAL)
	fun <T : Any> input(signal: T, type: KClass<in T>)
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
		key: Any? = null,
		block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Init.End
) : KotlmataMutableDaemon
{
	override val key: Any = key ?: this
	override var start: () -> Unit = {}
	override var pause: () -> Unit = {}
	override var stop: () -> Unit = {}
	override var resume: () -> Unit = {}
	override var terminate: () -> Unit = {}
	
	val queue: PriorityBlockingQueue<Message> = PriorityBlockingQueue()
	val machine: KotlmataMutableMachine
	val fractal: KotlmataMachine
	
	init
	{
		machine = KotlmataMutableMachine(this.key) {
			val initializer = InitializerImpl(block, this)
			DaemonOrigin {}
			DaemonOrigin x Message.Run::class %= initializer.origin
			init origin state to DaemonOrigin
		}
		
		fractal = KotlmataMachine {
			TODO("not implemented")
		}
	}
	
	override fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		queue.add(Message.Modify(block))
	}
	
	override fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
	{
		queue.add(Message.Modify(block))
	}
	
	override fun input(signal: SIGNAL)
	{
		queue.add(Message.Signal(signal))
	}
	
	override fun <T : Any> input(signal: T, type: KClass<in T>)
	{
		queue.add(Message.TypedSignal(signal, type))
	}
	
	override fun run()
	{
		queue.add(Message.Run())
	}
	
	override fun pause()
	{
		queue.add(Message.Pause())
	}
	
	override fun stop()
	{
		queue.add(Message.Stop())
	}
	
	override fun terminate()
	{
		queue.add(Message.Terminate())
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
	class Signal(val signal: SIGNAL) : Message(0)
	class TypedSignal(val signal: SIGNAL, val type: KClass<out Any>) : Message(0)
	class Modify(val block: KotlmataMutableMachine.Modifier.() -> Unit) : Message(0)
	class Stash(val signal: SIGNAL) : Message(1)
	class Run : Message(2)
	class Pause : Message(2)
	class Stop : Message(2)
	class Terminate : Message(2)
	
	companion object
	{
		val stamp: AtomicLong = AtomicLong(Long.MAX_VALUE)
	}
	
	val timestamp = stamp.decrementAndGet()
	
	override fun compareTo(other: Message): Int
	{
		val dP = priority - other.priority
		return if (dP != 0) dP
		else (timestamp - other.timestamp).toInt()
	}
}