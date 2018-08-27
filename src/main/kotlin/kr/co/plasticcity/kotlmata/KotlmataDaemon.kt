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
				block: Initializer.() -> KotlmataMachine.Initialize.End
		): KotlmataDaemon? = null
	}
	
	interface Initializer : KotlmataMachine.Initializer
	{
		val on: On
		override val initialize: KotlmataMachine.Initialize
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
				block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initialize.End
		): KotlmataDaemon? = null
	}
	
	operator fun invoke(block: KotlmataMutableMachine.Modifier.() -> Unit)
	
	infix fun modify(block: KotlmataMutableMachine.Modifier.() -> Unit)
}

private class KotlmataDaemonImpl(
		key: Any? = null,
		block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initialize.End
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
	val fractal: KotlmataMachine?
	
	init
	{
		val start = Any()
		machine = KotlmataMutableMachine(this.key) {
			start {}
			initialize origin state to start
		}
		fractal = null
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
			block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initialize.End
	) : KotlmataDaemon.Initializer, CanExpire({ Log.e(key) { EXPIRED_DAEMON_MODIFIER } })
	{
		override val on: KotlmataDaemon.On
			get() = TODO("not implemented")
		override val initialize: KotlmataMachine.Initialize
			get() = TODO("not implemented")
		
		override fun STATE.invoke(block: KotlmataState.Initializer.() -> Unit)
		{
			TODO("not implemented")
		}
		
		override fun STATE.x(signal: SIGNAL): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun STATE.x(signal: KClass<out Any>): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun STATE.x(keyword: any): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun any.x(signal: SIGNAL): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun any.x(signal: KClass<out Any>): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
		}
		
		override fun any.x(keyword: any): KotlmataMachine.TransitionLeft
		{
			TODO("not implemented")
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