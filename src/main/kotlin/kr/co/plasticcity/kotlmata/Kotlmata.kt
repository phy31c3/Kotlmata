package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface Kotlmata
{
	companion object : Kotlmata by KotlmataImpl()
	
	fun config(block: Config.() -> Unit)
	fun start()
	fun shutdown()
	
	infix fun fork(daemon: KEY): Of
	infix fun modify(daemon: KEY): Set
	
	infix fun run(daemon: KEY)
	infix fun pause(daemon: KEY)
	infix fun stop(daemon: KEY)
	infix fun terminate(daemon: KEY)
	
	infix fun <T : SIGNAL> input(signal: T): Type<T>
	
	infix fun post(block: Post.() -> Unit)
	
	operator fun invoke(block: Post.() -> Unit) = post(block)
	
	interface Config
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
	
	interface Type<T : SIGNAL> : Priority
	{
		infix fun type(type: KClass<in T>): Priority
	}
	
	interface Priority : To
	{
		infix fun priority(priority: Int): To
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
		}
	}
}

private class KotlmataImpl : Kotlmata
{
	private var logLevel = NORMAL
	
	private val map: MutableMap<KEY, KotlmataMutableDaemon> = HashMap()
	
	private val engine = KotlmataDaemon("Kotlmata") {
		log level 0
		
		on start {
			logLevel.simple { KOTLMATA_START }
		}
		
		on stop {
			logLevel.simple { KOTLMATA_SHUTDOWN }
			map.forEach { _, daemon ->
				daemon.terminate()
			}
			map.clear()
		}
		
		"kotlmata" {
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
				map[it.daemon]?.input(it.signal, it.priority)
			}
			input signal Message.TypedInput::class action {
				map[it.daemon]?.input(it.signal, it.type, it.priority)
			}
			input signal Message.Post::class action {
				PostImpl(it.block)
			}
		}
		
		start at "kotlmata"
	}
	
	override fun config(block: Kotlmata.Config.() -> Unit)
	{
		ConfigImpl(block)
	}
	
	override fun start()
	{
		engine.run()
	}
	
	override fun shutdown()
	{
		engine.stop()
	}
	
	override fun fork(daemon: KEY) = object : Kotlmata.Of
	{
		override fun of(block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End)
		{
			val m = Message.Fork(daemon, block)
			logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
			engine.input(m)
		}
	}
	
	override fun modify(daemon: KEY) = object : Kotlmata.Set
	{
		override fun set(block: KotlmataMutableMachine.Modifier.() -> Unit)
		{
			val m = Message.Modify(daemon, block)
			logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
			engine.input(m)
		}
	}
	
	override fun run(daemon: KEY)
	{
		val m = Message.Run(daemon)
		logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
		engine.input(m)
	}
	
	override fun pause(daemon: KEY)
	{
		val m = Message.Pause(daemon)
		logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
		engine.input(m)
	}
	
	override fun stop(daemon: KEY)
	{
		val m = Message.Stop(daemon)
		logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
		engine.input(m)
	}
	
	override fun terminate(daemon: KEY)
	{
		val m = Message.Terminate(daemon)
		logLevel.detail(m, daemon, m.id) { KOTLMATA_POST_MESSAGE_DAEMON }
		engine.input(m)
	}
	
	override fun <T : SIGNAL> input(signal: T) = object : Kotlmata.Type<T>
	{
		@Suppress("UNCHECKED_CAST")
		override fun type(type: KClass<in T>) = object : Kotlmata.Priority
		{
			override fun priority(priority: Int) = object : Kotlmata.To
			{
				override fun to(daemon: KEY)
				{
					val m = Message.TypedInput(daemon, signal, type as KClass<SIGNAL>, priority)
					logLevel.detail(m, m.signal, m.type, daemon, m.id) { KOTLMATA_POST_MESSAGE_TYPED_INPUT }
					engine.input(m)
				}
			}
			
			override fun to(daemon: KEY)
			{
				val m = Message.TypedInput(daemon, signal, type as KClass<SIGNAL>, 0)
				logLevel.detail(m, m.signal, m.type, daemon, m.id) { KOTLMATA_POST_MESSAGE_TYPED_INPUT }
				engine.input(m)
			}
		}
		
		override fun priority(priority: Int) = object : Kotlmata.To
		{
			override fun to(daemon: KEY)
			{
				val m = Message.Input(daemon, signal, priority)
				logLevel.detail(m, m.signal, daemon, m.id) { KOTLMATA_POST_MESSAGE_INPUT }
				engine.input(m)
			}
		}
		
		override fun to(daemon: KEY)
		{
			val m = Message.Input(daemon, signal, 0)
			logLevel.detail(m, m.signal, daemon, m.id) { KOTLMATA_POST_MESSAGE_INPUT }
			engine.input(m)
		}
	}
	
	override fun post(block: Kotlmata.Post.() -> Unit)
	{
		val m = Message.Post(block)
		logLevel.detail(m, m.id) { KOTLMATA_POST_MESSAGE }
		engine.input(m)
	}
	
	private inner class ConfigImpl internal constructor(
			block: Kotlmata.Config.() -> Unit
	) : Kotlmata.Config, Expirable({ Log.e { EXPIRED_CONFIG } })
	{
		override val log = object : Kotlmata.Config.Log
		{
			override fun level(level: Int)
			{
				this@ConfigImpl shouldNot expired
				logLevel = level
			}
		}
		
		override val print = object : Kotlmata.Config.Print
		{
			override fun debug(block: (String) -> Unit)
			{
				this@ConfigImpl shouldNot expired
				Log.debug = block
			}
			
			override fun warn(block: (String) -> Unit)
			{
				this@ConfigImpl shouldNot expired
				Log.warn = block
			}
			
			override fun error(block: (String) -> Unit)
			{
				this@ConfigImpl shouldNot expired
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
	) : Kotlmata.Post, Expirable({ Log.e { EXPIRED_CONFIG } })
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
			override fun <T : SIGNAL> signal(signal: T) = object : Kotlmata.Type<T>
			{
				override fun type(type: KClass<in T>) = object : Kotlmata.Priority
				{
					override fun priority(priority: Int) = object : Kotlmata.To
					{
						override fun to(daemon: KEY)
						{
							this@PostImpl shouldNot expired
							map[daemon]?.input(signal, type, priority)
						}
					}
					
					override fun to(daemon: KEY)
					{
						this@PostImpl shouldNot expired
						map[daemon]?.input(signal, type)
					}
				}
				
				override fun priority(priority: Int) = object : Kotlmata.To
				{
					override fun to(daemon: KEY)
					{
						this@PostImpl shouldNot expired
						map[daemon]?.input(signal, priority)
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
	
	private sealed class Message
	{
		class Fork(val daemon: KEY, val block: KotlmataDaemon.Initializer.() -> KotlmataMachine.Initializer.End) : Message()
		class Modify(val daemon: KEY, val block: KotlmataMutableMachine.Modifier.() -> Unit) : Message()
		
		class Run(val daemon: KEY) : Message()
		class Pause(val daemon: KEY) : Message()
		class Stop(val daemon: KEY) : Message()
		class Terminate(val daemon: KEY) : Message()
		
		class Input(val daemon: KEY, val signal: SIGNAL, val priority: Int) : Message()
		class TypedInput(val daemon: KEY, val signal: SIGNAL, val type: KClass<SIGNAL>, val priority: Int) : Message()
		
		class Post(val block: Kotlmata.Post.() -> Unit) : Message()
		
		val id by lazy { hashCode().toString(16) }
		
		override fun toString(): String
		{
			return this::class.simpleName ?: super.toString()
		}
	}
}