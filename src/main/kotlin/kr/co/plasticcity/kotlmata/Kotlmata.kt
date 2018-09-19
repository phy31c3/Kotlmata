package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface Kotlmata
{
	companion object : Kotlmata by KotlmataImpl()
	
	fun config(block: Config.() -> Unit)
	fun start()
	fun shutdown()
	fun release()
	
	infix fun <T : DAEMON> fork(daemon: T): Of<T>
	infix fun <T : DAEMON> modify(daemon: T): Set<T>
	
	infix fun run(daemon: DAEMON)
	infix fun pause(daemon: DAEMON)
	infix fun stop(daemon: DAEMON)
	infix fun terminate(daemon: DAEMON)
	
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
	
	interface Of<T : DAEMON>
	{
		infix fun of(block: KotlmataDaemon.Initializer.(daemon: T) -> KotlmataMachine.Initializer.End)
	}
	
	interface Set<T : DAEMON>
	{
		infix fun set(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
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
		infix fun to(daemon: DAEMON)
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
			infix fun daemon(daemon: DAEMON): Then
			
			interface Then
			{
				infix fun then(block: () -> Unit): Or
			}
			
			interface Or : Finally
			{
				infix fun or(block: () -> Unit): Finally
			}
			
			interface Finally
			{
				infix fun finally(block: () -> Unit)
			}
		}
		
		interface Fork
		{
			infix fun <T : DAEMON> daemon(daemon: T): Of<T>
			
			interface Of<T : DAEMON>
			{
				infix fun of(block: KotlmataDaemon.Initializer.(daemon: T) -> KotlmataMachine.Initializer.End)
			}
		}
		
		interface Modify
		{
			infix fun <T : DAEMON> daemon(daemon: T): Set<T>
			
			interface Set<T : DAEMON>
			{
				infix fun set(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
			}
		}
		
		interface Run
		{
			infix fun daemon(daemon: DAEMON)
		}
		
		interface Pause
		{
			infix fun daemon(daemon: DAEMON)
		}
		
		interface Stop
		{
			infix fun daemon(daemon: DAEMON)
		}
		
		interface Terminate
		{
			infix fun daemon(daemon: DAEMON)
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
	
	private val daemons: MutableMap<DAEMON, KotlmataMutableDaemon<DAEMON>> = HashMap()
	
	private val kotlmata: KotlmataDaemon
	
	init
	{
		val cleanup = {
			daemons.forEach { _, daemon ->
				daemon.terminate()
			}
			daemons.clear()
		}
		
		kotlmata = KotlmataDaemon("Kotlmata") { _ ->
			log level 0
			
			on start {
				logLevel.simple { KOTLMATA_START }
			}
			
			on resume {
				logLevel.simple { KOTLMATA_RESTART }
			}
			
			on stop {
				logLevel.simple { KOTLMATA_SHUTDOWN }
				cleanup()
			}
			
			on terminate {
				logLevel.simple { KOTLMATA_RELEASE }
				cleanup()
			}
			
			"kotlmata" { _ ->
				input signal Message.Fork::class action {
					if (it.daemon !in daemons)
					{
						logLevel.detail(it, it.daemon) { KOTLMATA_COMMON }
						daemons[it.daemon] = KotlmataMutableDaemon(it.daemon) { _ ->
							log level logLevel
							(it.block)(it.daemon)
						}
					}
					else
					{
						logLevel.normal(it, it.daemon) { KOTLMATA_COMMON_IGNORED_EXISTS }
					}
				}
				input signal Message.Modify::class action {
					if (it.daemon in daemons)
					{
						logLevel.detail("", it, it.daemon) { KOTLMATA_COMMON }
						daemons[it.daemon]!! modify it.block
					}
					else
					{
						logLevel.normal(it, it.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Message.Run::class action {
					if (it.daemon in daemons)
					{
						logLevel.detail(it, it.daemon) { KOTLMATA_COMMON }
						daemons[it.daemon]!!.run()
					}
					else
					{
						logLevel.normal(it, it.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Message.Pause::class action {
					if (it.daemon in daemons)
					{
						logLevel.detail(it, it.daemon) { KOTLMATA_COMMON }
						daemons[it.daemon]!!.pause()
					}
					else
					{
						logLevel.normal(it, it.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Message.Stop::class action {
					if (it.daemon in daemons)
					{
						logLevel.detail(it, it.daemon) { KOTLMATA_COMMON }
						daemons[it.daemon]!!.stop()
					}
					else
					{
						logLevel.normal(it, it.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Message.Terminate::class action {
					if (it.daemon in daemons)
					{
						logLevel.detail(it, it.daemon) { KOTLMATA_COMMON }
						daemons[it.daemon]!!.terminate()
						daemons -= it.daemon
					}
					else
					{
						logLevel.normal(it, it.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Message.Signal::class action {
					if (it.daemon in daemons)
					{
						logLevel.detail("", it.signal, it.priority, it.daemon) { KOTLMATA_SIGNAL }
						daemons[it.daemon]!!.input(it.signal, it.priority)
					}
					else
					{
						logLevel.normal("", it.signal, it.priority, it.daemon) { KOTLMATA_SIGNAL_IGNORED }
					}
				}
				input signal Message.TypedSignal::class action {
					if (it.daemon in daemons)
					{
						logLevel.detail("", it.signal, it.type, it.priority, it.daemon) { KOTLMATA_TYPED }
						daemons[it.daemon]!!.input(it.signal, it.type, it.priority)
					}
					else
					{
						logLevel.normal("", it.signal, it.type, it.priority, it.daemon) { KOTLMATA_TYPED_IGNORED }
					}
				}
				input signal Message.Post::class action {
					logLevel.detail { KOTLMATA_START_POST }
					PostImpl(it.block)
					logLevel.detail { KOTLMATA_END_POST }
				}
			}
			
			start at "kotlmata"
		}
	}
	
	override fun config(block: Kotlmata.Config.() -> Unit)
	{
		ConfigImpl(block)
	}
	
	override fun start()
	{
		kotlmata.run()
	}
	
	override fun shutdown()
	{
		kotlmata.stop()
	}
	
	override fun release()
	{
		kotlmata.terminate()
	}
	
	override fun <T : DAEMON> fork(daemon: T) = object : Kotlmata.Of<T>
	{
		@Suppress("UNCHECKED_CAST")
		override fun of(block: KotlmataDaemon.Initializer.(daemon: T) -> KotlmataMachine.Initializer.End)
		{
			kotlmata.input(Message.Fork(daemon, block as KotlmataDaemon.Initializer.(DAEMON) -> KotlmataMachine.Initializer.End))
		}
	}
	
	override fun <T : DAEMON> modify(daemon: T) = object : Kotlmata.Set<T>
	{
		@Suppress("UNCHECKED_CAST")
		override fun set(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
		{
			kotlmata.input(Message.Modify(daemon, block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit))
		}
	}
	
	override fun run(daemon: DAEMON)
	{
		kotlmata.input(Message.Run(daemon))
	}
	
	override fun pause(daemon: DAEMON)
	{
		kotlmata.input(Message.Pause(daemon))
	}
	
	override fun stop(daemon: DAEMON)
	{
		kotlmata.input(Message.Stop(daemon))
	}
	
	override fun terminate(daemon: DAEMON)
	{
		kotlmata.input(Message.Terminate(daemon))
	}
	
	override fun <T : SIGNAL> input(signal: T) = object : Kotlmata.Type<T>
	{
		@Suppress("UNCHECKED_CAST")
		override fun type(type: KClass<in T>) = object : Kotlmata.Priority
		{
			override fun priority(priority: Int) = object : Kotlmata.To
			{
				override fun to(daemon: DAEMON)
				{
					kotlmata.input(Message.TypedSignal(daemon, signal, type as KClass<SIGNAL>, priority))
				}
			}
			
			override fun to(daemon: DAEMON)
			{
				kotlmata.input(Message.TypedSignal(daemon, signal, type as KClass<SIGNAL>, 0))
			}
		}
		
		override fun priority(priority: Int) = object : Kotlmata.To
		{
			override fun to(daemon: DAEMON)
			{
				kotlmata.input(Message.Signal(daemon, signal, priority))
			}
		}
		
		override fun to(daemon: DAEMON)
		{
			kotlmata.input(Message.Signal(daemon, signal, 0))
		}
	}
	
	override fun post(block: Kotlmata.Post.() -> Unit)
	{
		kotlmata.input(Message.Post(block))
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
			override fun daemon(daemon: DAEMON) = object : Kotlmata.Post.Has.Then
			{
				override fun then(block: () -> Unit) = object : Kotlmata.Post.Has.Or
				{
					var or = true
					
					init
					{
						this@PostImpl shouldNot expired
						if (daemon in daemons)
						{
							block()
							or = false
						}
					}
					
					override fun or(block: () -> Unit): Kotlmata.Post.Has.Finally
					{
						this@PostImpl shouldNot expired
						if (or)
						{
							block()
						}
						return this
					}
					
					override fun finally(block: () -> Unit)
					{
						this@PostImpl shouldNot expired
						block()
					}
				}
			}
		}
		
		override val fork = object : Kotlmata.Post.Fork
		{
			override fun <T : DAEMON> daemon(daemon: T) = object : Kotlmata.Post.Fork.Of<T>
			{
				override fun of(block: KotlmataDaemon.Initializer.(daemon: T) -> KotlmataMachine.Initializer.End)
				{
					this@PostImpl shouldNot expired
					if (daemon !in daemons)
					{
						logLevel.detail("   Fork", daemon) { KOTLMATA_COMMON }
						daemons[daemon] = KotlmataMutableDaemon(daemon) {
							log level logLevel
							block(daemon)
						}
					}
					else
					{
						logLevel.normal("   Fork", daemon) { KOTLMATA_COMMON_IGNORED_EXISTS }
					}
				}
			}
		}
		
		override val modify = object : Kotlmata.Post.Modify
		{
			override fun <T : DAEMON> daemon(daemon: T) = object : Kotlmata.Post.Modify.Set<T>
			{
				@Suppress("UNCHECKED_CAST")
				override fun set(block: KotlmataMutableMachine.Modifier.(daemon: T) -> Unit)
				{
					this@PostImpl shouldNot expired
					if (daemon in daemons)
					{
						logLevel.detail("   Modify", daemon) { KOTLMATA_COMMON }
						daemons[daemon]!! modify block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit
					}
					else
					{
						logLevel.normal("   Modify", daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
			}
		}
		
		override val run = object : Kotlmata.Post.Run
		{
			override fun daemon(daemon: DAEMON)
			{
				this@PostImpl shouldNot expired
				if (daemon in daemons)
				{
					logLevel.detail("   Run", daemon) { KOTLMATA_COMMON }
					daemons[daemon]!!.run()
				}
				else
				{
					logLevel.normal("   Run", daemon) { KOTLMATA_COMMON_IGNORED_NONE }
				}
			}
		}
		
		override val pause = object : Kotlmata.Post.Pause
		{
			override fun daemon(daemon: DAEMON)
			{
				this@PostImpl shouldNot expired
				if (daemon in daemons)
				{
					logLevel.detail("   Pause", daemon) { KOTLMATA_COMMON }
					daemons[daemon]!!.pause()
				}
				else
				{
					logLevel.normal("   Pause", daemon) { KOTLMATA_COMMON_IGNORED_NONE }
				}
			}
		}
		
		override val stop = object : Kotlmata.Post.Stop
		{
			override fun daemon(daemon: DAEMON)
			{
				this@PostImpl shouldNot expired
				if (daemon in daemons)
				{
					logLevel.detail("   Stop", daemon) { KOTLMATA_COMMON }
					daemons[daemon]!!.stop()
				}
				else
				{
					logLevel.normal("   Stop", daemon) { KOTLMATA_COMMON_IGNORED_NONE }
				}
			}
		}
		
		override val terminate = object : Kotlmata.Post.Terminate
		{
			override fun daemon(daemon: DAEMON)
			{
				this@PostImpl shouldNot expired
				if (daemon in daemons)
				{
					logLevel.detail("   Terminate", daemon) { KOTLMATA_COMMON }
					daemons[daemon]!!.terminate()
					daemons -= daemon
				}
				else
				{
					logLevel.normal("   Terminate", daemon) { KOTLMATA_COMMON_IGNORED_NONE }
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
						override fun to(daemon: DAEMON)
						{
							this@PostImpl shouldNot expired
							if (daemon in daemons)
							{
								logLevel.detail("   ", signal, type, priority, daemon) { KOTLMATA_TYPED }
								daemons[daemon]!!.input(signal, type, priority)
							}
							else
							{
								logLevel.normal("   ", signal, type, priority, daemon) { KOTLMATA_TYPED_IGNORED }
							}
						}
					}
					
					override fun to(daemon: DAEMON)
					{
						this@PostImpl shouldNot expired
						if (daemon in daemons)
						{
							logLevel.detail("   ", signal, type, 0, daemon) { KOTLMATA_TYPED }
							daemons[daemon]!!.input(signal, type)
						}
						else
						{
							logLevel.normal("   ", signal, type, 0, daemon) { KOTLMATA_TYPED_IGNORED }
						}
					}
				}
				
				override fun priority(priority: Int) = object : Kotlmata.To
				{
					override fun to(daemon: DAEMON)
					{
						this@PostImpl shouldNot expired
						if (daemon in daemons)
						{
							logLevel.detail("   ", signal, priority, daemon) { KOTLMATA_SIGNAL }
							daemons[daemon]!!.input(signal, priority)
						}
						else
						{
							logLevel.normal("   ", signal, priority, daemon) { KOTLMATA_SIGNAL_IGNORED }
						}
					}
				}
				
				override fun to(daemon: DAEMON)
				{
					this@PostImpl shouldNot expired
					if (daemon in daemons)
					{
						logLevel.detail("   ", signal, 0, daemon) { KOTLMATA_SIGNAL }
						daemons[daemon]!!.input(signal)
					}
					else
					{
						logLevel.normal("   ", signal, 0, daemon) { KOTLMATA_SIGNAL_IGNORED }
					}
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
		class Fork(val daemon: DAEMON, val block: KotlmataDaemon.Initializer.(daemon: DAEMON) -> KotlmataMachine.Initializer.End) : Message()
		class Modify(val daemon: DAEMON, val block: KotlmataMutableMachine.Modifier.(daemon: DAEMON) -> Unit) : Message()
		
		class Run(val daemon: DAEMON) : Message()
		class Pause(val daemon: DAEMON) : Message()
		class Stop(val daemon: DAEMON) : Message()
		class Terminate(val daemon: DAEMON) : Message()
		
		class Signal(val daemon: DAEMON, val signal: SIGNAL, val priority: Int) : Message()
		class TypedSignal(val daemon: DAEMON, val signal: SIGNAL, val type: KClass<SIGNAL>, val priority: Int) : Message()
		
		class Post(val block: Kotlmata.Post.() -> Unit) : Message()
		
		override fun toString(): String
		{
			return this::class.simpleName ?: super.toString()
		}
	}
}