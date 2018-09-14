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
		val has: Has
		val fork: Fork
		val modify: Modify
		val run: Run
		val pause: Pause
		val stop: Stop
		val terminate: Terminate
		val input: Input
		
		interface Has
		{
			infix fun daemon(daemon: KEY): Then
			
			interface Then
			{
				infix fun then(block: Post.() -> Unit): Or
			}
			
			interface Or : Finally
			{
				infix fun or(block: Post.() -> Unit): Finally
			}
			
			interface Finally
			{
				infix fun finally(block: Post.() -> Unit)
			}
		}
		
		interface Fork
		{
			infix fun daemon(daemon: KEY): Of
			
			interface Of
			{
				infix fun of(block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End)
			}
		}
		
		interface Modify
		{
			infix fun daemon(daemon: KEY): Set
			
			interface Set
			{
				infix fun set(block: KotlmataMutableMachine.Modifier.() -> Unit)
			}
		}
		
		interface Run
		{
			infix fun daemon(daemon: KEY)
		}
		
		interface Pause
		{
			infix fun daemon(daemon: KEY)
		}
		
		interface Stop
		{
			infix fun daemon(daemon: KEY)
		}
		
		interface Terminate
		{
			infix fun daemon(daemon: KEY)
		}
		
		interface Input
		{
			infix fun <T : SIGNAL> signal(signal: T): Type<T>
			
			interface Type<T : SIGNAL> : To
			{
				infix fun type(type: KClass<in T>): To
			}
			
			interface To
			{
				infix fun to(daemon: KEY)
			}
		}
	}
}

private class KotlmataImpl : Kotlmata
{
	private var logLevel = NORMAL
	
	private val queue: PriorityBlockingQueue<Message> = PriorityBlockingQueue()
	private val map: MutableMap<KEY, KotlmataMutableDaemon> = HashMap()
	
	private val engine: KotlmataMachine = KotlmataMachine("Kotlmata@engine") {
		log level 0
		
		"initial" {
			input signal Message.Init::class action {
				InitializerImpl(it.block)
				logLevel.simple { KOTLMATA_START }
			}
		}
		
		"run" {
			input signal Message.Fork::class action {
				if (it.daemon !in map)
				{
					map[it.daemon] = KotlmataMutableDaemon(it.daemon) {
						log level logLevel
						it.block.invoke(this)
					}
				}
			}
			input signal Message.Modify::class action {
				if (it.daemon in map)
				{
					map[it.daemon]!! modify it.block
				}
			}
			input signal Message.Run::class action {
				map[it.daemon]?.run()
			}
			input signal Message.Pause::class action {
				map[it.daemon]?.pause()
			}
			input signal Message.Stop::class action {
				map[it.daemon]?.stop()
			}
			input signal Message.Terminate::class action {
				map[it.daemon]?.terminate()
				map -= it.daemon
			}
			input signal Message.Input::class action {
				map[it.daemon]?.input(it.signal)
			}
			input signal Message.TypedInput::class action {
				map[it.daemon]?.input(it.signal, it.type)
			}
			input signal Message.Post::class action {
				PostImpl(it.block)
			}
		}
		
		"release" {
			entry via Message.Release::class action {
				logLevel.simple { KOTLMATA_RELEASE }
				map.forEach { _, daemon ->
					daemon.terminate()
				}
				map.clear()
				it.block()
				Thread.currentThread().interrupt()
			}
		}
		
		"initial" x Message.Init::class %= "run"
		any x Message.Release::class %= "release"
		
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
		/* Can't print log because Kotlmata isn't initialized yet */
		queue.offer(Message.Init(block))
	}
	
	override fun release(block: () -> Unit)
	{
		val m = Message.Release(block)
		logLevel.detail(m, m.id) { KOTLMATA_POST_MESSAGE }
		queue.offer(m)
	}
	
	override fun fork(daemon: KEY) = object : Kotlmata.Of
	{
		override fun of(block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End)
		{
			val m = Message.Fork(daemon, block)
			logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
			queue.offer(m)
		}
	}
	
	override fun modify(daemon: KEY) = object : Kotlmata.Set
	{
		override fun set(block: KotlmataMutableMachine.Modifier.() -> Unit)
		{
			val m = Message.Modify(daemon, block)
			logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
			queue.offer(m)
		}
	}
	
	override fun run(daemon: KEY)
	{
		val m = Message.Run(daemon)
		logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
		queue.offer(m)
	}
	
	override fun pause(daemon: KEY)
	{
		val m = Message.Pause(daemon)
		logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
		queue.offer(m)
	}
	
	override fun stop(daemon: KEY)
	{
		val m = Message.Stop(daemon)
		logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
		queue.offer(m)
	}
	
	override fun terminate(daemon: KEY)
	{
		val m = Message.Terminate(daemon)
		logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
		queue.offer(m)
	}
	
	override fun <T : SIGNAL> input(signal: T) = object : Kotlmata.Type<T>
	{
		override fun type(type: KClass<in T>) = object : Kotlmata.To
		{
			@Suppress("UNCHECKED_CAST")
			override fun to(daemon: KEY)
			{
				val m = Message.TypedInput(daemon, signal, type as KClass<SIGNAL>)
				logLevel.detail(m, m.signal, m.type, daemon, m.id) { KOTLMATA_POST_MESSAGE_TYPED_INPUT }
				queue.offer(m)
			}
		}
		
		override fun to(daemon: KEY)
		{
			val m = Message.Input(daemon, signal)
			logLevel.detail(m, m.signal, daemon, m.id) { KOTLMATA_POST_MESSAGE_INPUT }
			queue.offer(m)
		}
	}
	
	override fun post(block: Kotlmata.Post.() -> Unit)
	{
		val m = Message.Post(block)
		logLevel.detail(m, m.id) { KOTLMATA_POST_MESSAGE }
		queue.offer(m)
	}
	
	private inner class InitializerImpl internal constructor(
			block: Kotlmata.Initializer.() -> Unit
	) : Kotlmata.Initializer, Expirable({ Log.e { EXPIRED_INITIALIZER } })
	{
		override val log = object : Kotlmata.Initializer.Log
		{
			override fun level(level: Int)
			{
				this@InitializerImpl shouldNot expired
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
	
	private inner class PostImpl internal constructor(
			block: Kotlmata.Post.() -> Unit
	) : Kotlmata.Post, Expirable({ Log.e { EXPIRED_INITIALIZER } })
	{
		override val has = object : Kotlmata.Post.Has
		{
			override fun daemon(daemon: KEY) = object : Kotlmata.Post.Has.Then
			{
				override fun then(block: Kotlmata.Post.() -> Unit) = object : Kotlmata.Post.Has.Or
				{
					var or = true
					
					init
					{
						this@PostImpl shouldNot expired
						if (daemon in map)
						{
							block()
							or = false
						}
					}
					
					override fun or(block: Kotlmata.Post.() -> Unit): Kotlmata.Post.Has.Finally
					{
						this@PostImpl shouldNot expired
						if (or)
						{
							block()
						}
						return this
					}
					
					override fun finally(block: Kotlmata.Post.() -> Unit)
					{
						this@PostImpl shouldNot expired
						block()
					}
				}
			}
		}
		
		override val fork = object : Kotlmata.Post.Fork
		{
			override fun daemon(daemon: KEY) = object : Kotlmata.Post.Fork.Of
			{
				override fun of(block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End)
				{
					this@PostImpl shouldNot expired
					if (daemon !in map)
					{
						map[daemon] = KotlmataMutableDaemon(daemon) {
							log level logLevel
							block()
						}
					}
				}
			}
		}
		
		override val modify = object : Kotlmata.Post.Modify
		{
			override fun daemon(daemon: KEY) = object : Kotlmata.Post.Modify.Set
			{
				override fun set(block: KotlmataMutableMachine.Modifier.() -> Unit)
				{
					this@PostImpl shouldNot expired
					if (daemon in map)
					{
						map[daemon]!! modify block
					}
				}
			}
		}
		
		override val run = object : Kotlmata.Post.Run
		{
			override fun daemon(daemon: KEY)
			{
				this@PostImpl shouldNot expired
				map[daemon]?.run()
			}
		}
		
		override val pause = object : Kotlmata.Post.Pause
		{
			override fun daemon(daemon: KEY)
			{
				this@PostImpl shouldNot expired
				map[daemon]?.pause()
			}
		}
		
		override val stop = object : Kotlmata.Post.Stop
		{
			override fun daemon(daemon: KEY)
			{
				this@PostImpl shouldNot expired
				map[daemon]?.stop()
			}
		}
		
		override val terminate = object : Kotlmata.Post.Terminate
		{
			override fun daemon(daemon: KEY)
			{
				this@PostImpl shouldNot expired
				map[daemon]?.let {
					it.terminate()
					map -= daemon
				}
			}
		}
		
		override val input = object : Kotlmata.Post.Input
		{
			override fun <T : SIGNAL> signal(signal: T) = object : Kotlmata.Post.Input.Type<T>
			{
				override fun type(type: KClass<in T>) = object : Kotlmata.Post.Input.To
				{
					override fun to(daemon: KEY)
					{
						this@PostImpl shouldNot expired
						map[daemon]?.input(signal, type)
					}
				}
				
				override fun to(daemon: KEY)
				{
					this@PostImpl shouldNot expired
					map[daemon]?.input(signal)
				}
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
		
		class Post(val block: Kotlmata.Post.() -> Unit) : Message(EVENT)
		
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