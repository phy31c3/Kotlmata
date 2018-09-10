package kr.co.plasticcity.kotlmata

import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.reflect.KClass

interface Kotlmata
{
	companion object : Kotlmata by KotlmataImpl()
	
	infix fun init(block: Initializer.() -> Unit)
	infix fun release(block: (() -> Unit))
	
	infix fun fork(daemon: KEY): Of
	infix fun modify(daemon: KEY): Set
	
	infix fun run(daemon: KEY)
	infix fun pause(daemon: KEY)
	infix fun stop(daemon: KEY)
	infix fun terminate(daemon: KEY)
	
	infix fun <T : SIGNAL> input(signal: T): Type<T>
	
	infix fun post(block: Post.() -> Unit)
	
	operator fun invoke(block: Post.() -> Unit) = post(block)
	
	interface Initializer
	{
		val log: Log
		val print: Print
		
		interface Log
		{
			/**
			 * @param level **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **2**)
			 */
			infix fun level(level: Int)
		}
		
		interface Print
		{
			infix fun debug(block: (log: String) -> Unit)
			infix fun warn(block: (log: String) -> Unit)
			infix fun error(block: (log: String) -> Unit)
		}
	}
	
	interface Of
	{
		infix fun of(block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End)
	}
	
	interface Set
	{
		infix fun set(block: KotlmataMutableMachine.Modifier.() -> Unit)
	}
	
	interface Type<T : SIGNAL> : To
	{
		infix fun type(type: KClass<in T>): To
	}
	
	interface To
	{
		infix fun to(daemon: KEY)
	}
	
	interface Post
	{
	
	}
}

private class KotlmataImpl : Kotlmata
{
	private var logLevel = NORMAL
	
	private val queue: PriorityBlockingQueue<Message> = PriorityBlockingQueue()
	private val map: MutableMap<KEY, KotlmataMutableDaemon> = HashMap()
	
	private val engine: KotlmataMachine = KotlmataMachine("Kotlmata@engine") {
		"initial" {
			TODO("not implemented")
		}
		
		"run" {
			TODO("not implemented")
		}
		
		"release" {
			TODO("not implemented")
		}
		
		start at "initial"
	}
	
	init
	{
		thread(name = "Kotlmata", isDaemon = true, start = true) {
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
	
	override fun init(block: Kotlmata.Initializer.() -> Unit)
	{
		queue.offer(Message.Init(block))
	}
	
	override fun release(block: () -> Unit)
	{
		queue.offer(Message.Release(block))
	}
	
	override fun fork(daemon: KEY) = object : Kotlmata.Of
	{
		override fun of(block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End)
		{
			queue.offer(Message.Fork(daemon, block))
		}
	}
	
	override fun modify(daemon: KEY) = object : Kotlmata.Set
	{
		override fun set(block: KotlmataMutableMachine.Modifier.() -> Unit)
		{
			queue.offer(Message.Modify(daemon, block))
		}
	}
	
	override fun run(daemon: KEY)
	{
		queue.offer(Message.Run(daemon))
	}
	
	override fun pause(daemon: KEY)
	{
		queue.offer(Message.Pause(daemon))
	}
	
	override fun stop(daemon: KEY)
	{
		queue.offer(Message.Stop(daemon))
	}
	
	override fun terminate(daemon: KEY)
	{
		queue.offer(Message.Terminate(daemon))
	}
	
	override fun <T : SIGNAL> input(signal: T) = object : Kotlmata.Type<T>
	{
		override fun type(type: KClass<in T>) = object : Kotlmata.To
		{
			@Suppress("UNCHECKED_CAST")
			override fun to(daemon: KEY)
			{
				queue.offer(Message.TypedInput(daemon, signal, type as KClass<SIGNAL>))
			}
		}
		
		override fun to(daemon: KEY)
		{
			queue.offer(Message.Input(daemon, signal))
		}
	}
	
	override fun post(block: Kotlmata.Post.() -> Unit)
	{
		TODO("not implemented")
	}
	
	private inner class InitializerImpl internal constructor(
			block: Kotlmata.Initializer.() -> Unit
	) : Kotlmata.Initializer, Expirable({ Log.e { EXPIRED_INITIALIZER } })
	{
		override val log = object : Kotlmata.Initializer.Log
		{
			override fun level(level: Int)
			{
				logLevel = level
			}
		}
		
		override val print = object : Kotlmata.Initializer.Print
		{
			override fun debug(block: (String) -> Unit)
			{
				this@InitializerImpl shouldNot expired
				Log.debug = block
			}
			
			override fun warn(block: (String) -> Unit)
			{
				this@InitializerImpl shouldNot expired
				Log.warn = block
			}
			
			override fun error(block: (String) -> Unit)
			{
				this@InitializerImpl shouldNot expired
				Log.error = block
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
		class Init(val block: Kotlmata.Initializer.() -> Unit) : Message(CONTROL)
		class Release(val block: () -> Unit) : Message(CONTROL)
		
		class Fork(val daemon: KEY, val block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End) : Message(EVENT)
		class Modify(val daemon: KEY, val block: KotlmataMutableMachine.Modifier.() -> Unit) : Message(EVENT)
		
		class Run(val daemon: KEY) : Message(EVENT)
		class Pause(val daemon: KEY) : Message(EVENT)
		class Stop(val daemon: KEY) : Message(EVENT)
		class Terminate(val daemon: KEY) : Message(EVENT)
		
		class Input(val daemon: KEY, val signal: SIGNAL) : Message(EVENT)
		class TypedInput(val daemon: KEY, val signal: SIGNAL, val type: KClass<SIGNAL>) : Message(EVENT)
		
		companion object
		{
			private const val CONTROL = 1
			private const val EVENT = 0
			
			val ticket: AtomicLong = AtomicLong(0)
		}
		
		val order = ticket.getAndIncrement()
		val id by lazy { hashCode().toString(16) }
		
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