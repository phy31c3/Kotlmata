package kr.co.plasticcity.kotlmata

import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

interface Kotlmata
{
	companion object : Kotlmata by KotlmataImpl()
	
	infix fun init(block: Initializer.() -> Unit)
	infix fun release(block: (() -> Unit))
	
	interface Initializer
	{
		val log: Log
		val print: Print
	}
	
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

private class KotlmataImpl : Kotlmata
{
	private var logLevel = NORMAL
	
	private val queue: PriorityBlockingQueue<Message> = PriorityBlockingQueue()
	
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
		synchronized(queue) {
			queue.offer(Message.Init(block))
		}
	}
	
	override fun release(block: () -> Unit)
	{
		synchronized(queue) {
			queue.offer(Message.Release(block))
		}
	}
	
	private inner class InitializerImpl internal constructor(
			block: Kotlmata.Initializer.() -> Unit
	) : Kotlmata.Initializer, Expirable({ Log.e { EXPIRED_INITIALIZER } })
	{
		override val log = object : Kotlmata.Log
		{
			override fun level(level: Int)
			{
				logLevel = level
			}
		}
		
		override val print = object : Kotlmata.Print
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