package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

interface Kotlmata
{
	companion object : Kotlmata by KotlmataImpl
	
	fun config(block: Config.() -> Unit)
	/**
	 * @param logLevel **0**: no log, **1**: simple, **2**: normal, **3**: detail (default value is **0**)
	 */
	fun start(logLevel: Int = NO_LOG)
	
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
	
	@KotlmataMarker
	interface Config
	{
		val print: Print
		
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
	
	@KotlmataMarker
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

private object KotlmataImpl : Kotlmata
{
	private var logLevel = NO_LOG
	
	private val daemons: MutableMap<DAEMON, KotlmataMutableDaemon<out DAEMON>> = HashMap()
	private val core: KotlmataDaemon<String>
	
	init
	{
		val cleanup = {
			daemons.forEach { (_, daemon) ->
				daemon.terminate()
			}
			daemons.clear()
		}
		
		core = KotlmataDaemon.create("Kotlmata@core") {
			on start { payload ->
				if (payload is Int)
				{
					logLevel = payload
				}
				logLevel.simple { KOTLMATA_START }
			}
			
			on resume { payload ->
				if (payload is Int)
				{
					logLevel = payload
				}
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
			
			"Core" {
				input signal Request.Fork::class action { forkR ->
					if (forkR.daemon !in daemons)
					{
						logLevel.detail(forkR, forkR.daemon) { KOTLMATA_COMMON }
						daemons[forkR.daemon] = KotlmataMutableDaemon.create(forkR.daemon, logLevel, forkR.block)
					}
					else
					{
						logLevel.normal(forkR, forkR.daemon) { KOTLMATA_COMMON_IGNORED_EXISTS }
					}
				}
				input signal Request.Modify::class action { modifyR ->
					if (modifyR.daemon in daemons)
					{
						logLevel.detail(modifyR, modifyR.daemon) { KOTLMATA_COMMON }
						daemons[modifyR.daemon]!! modify modifyR.block
					}
					else
					{
						logLevel.normal(modifyR, modifyR.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Request.Run::class action { runR ->
					if (runR.daemon in daemons)
					{
						logLevel.detail(runR, runR.daemon) { KOTLMATA_COMMON }
						daemons[runR.daemon]!!.run()
					}
					else
					{
						logLevel.normal(runR, runR.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Request.Pause::class action { pauseR ->
					if (pauseR.daemon in daemons)
					{
						logLevel.detail(pauseR, pauseR.daemon) { KOTLMATA_COMMON }
						daemons[pauseR.daemon]!!.pause()
					}
					else
					{
						logLevel.normal(pauseR, pauseR.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Request.Stop::class action { stopR ->
					if (stopR.daemon in daemons)
					{
						logLevel.detail(stopR, stopR.daemon) { KOTLMATA_COMMON }
						daemons[stopR.daemon]!!.stop()
					}
					else
					{
						logLevel.normal(stopR, stopR.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Request.Terminate::class action { terminateR ->
					if (terminateR.daemon in daemons)
					{
						logLevel.detail(terminateR, terminateR.daemon) { KOTLMATA_COMMON }
						daemons[terminateR.daemon]!!.terminate()
						daemons -= terminateR.daemon
					}
					else
					{
						logLevel.normal(terminateR, terminateR.daemon) { KOTLMATA_COMMON_IGNORED_NONE }
					}
				}
				input signal Request.Signal::class action { signalR ->
					if (signalR.daemon in daemons)
					{
						logLevel.detail("", signalR.signal, signalR.priority, signalR.daemon) { KOTLMATA_SIGNAL }
						daemons[signalR.daemon]!!.input(signalR.signal, signalR.priority)
					}
					else
					{
						logLevel.normal("", signalR.signal, signalR.priority, signalR.daemon) { KOTLMATA_SIGNAL_IGNORED }
					}
				}
				input signal Request.TypedSignal::class action { typedR ->
					if (typedR.daemon in daemons)
					{
						logLevel.detail("", typedR.signal, "${typedR.type.simpleName}::class", typedR.priority, typedR.daemon) { KOTLMATA_TYPED }
						daemons[typedR.daemon]!!.input(typedR.signal, typedR.type, typedR.priority)
					}
					else
					{
						logLevel.normal("", typedR.signal, "${typedR.type.simpleName}::class", typedR.priority, typedR.daemon) { KOTLMATA_TYPED_IGNORED }
					}
				}
				input signal Request.Post::class action { postR ->
					logLevel.detail { KOTLMATA_START_POST }
					PostImpl(postR.block)
					logLevel.detail { KOTLMATA_END_POST }
				}
			}
			
			start at "Core"
		}
	}
	
	override fun config(block: Kotlmata.Config.() -> Unit)
	{
		ConfigImpl(block)
	}
	
	override fun start(logLevel: Int)
	{
		core.run(logLevel)
	}
	
	override fun shutdown()
	{
		core.stop()
	}
	
	override fun release()
	{
		core.terminate()
	}
	
	override fun <T : DAEMON> fork(daemon: T) = object : Kotlmata.Of<T>
	{
		@Suppress("UNCHECKED_CAST")
		override fun of(block: KotlmataDaemon.Initializer.(T) -> KotlmataMachine.Initializer.End)
		{
			core.input(Request.Fork(daemon, block as KotlmataDaemon.Initializer.(DAEMON) -> KotlmataMachine.Initializer.End))
		}
	}
	
	override fun <T : DAEMON> modify(daemon: T) = object : Kotlmata.Set<T>
	{
		@Suppress("UNCHECKED_CAST")
		override fun set(block: KotlmataMutableMachine.Modifier.(T) -> Unit)
		{
			core.input(Request.Modify(daemon, block as KotlmataMutableMachine.Modifier.(DAEMON) -> Unit))
		}
	}
	
	override fun run(daemon: DAEMON)
	{
		core.input(Request.Run(daemon))
	}
	
	override fun pause(daemon: DAEMON)
	{
		core.input(Request.Pause(daemon))
	}
	
	override fun stop(daemon: DAEMON)
	{
		core.input(Request.Stop(daemon))
	}
	
	override fun terminate(daemon: DAEMON)
	{
		core.input(Request.Terminate(daemon))
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
					core.input(Request.TypedSignal(daemon, signal, type as KClass<SIGNAL>, priority))
				}
			}
			
			override fun to(daemon: DAEMON)
			{
				core.input(Request.TypedSignal(daemon, signal, type as KClass<SIGNAL>, 0))
			}
		}
		
		override fun priority(priority: Int) = object : Kotlmata.To
		{
			override fun to(daemon: DAEMON)
			{
				core.input(Request.Signal(daemon, signal, priority))
			}
		}
		
		override fun to(daemon: DAEMON)
		{
			core.input(Request.Signal(daemon, signal, 0))
		}
	}
	
	override fun post(block: Kotlmata.Post.() -> Unit)
	{
		core.input(Request.Post(block))
	}
	
	private class ConfigImpl internal constructor(
			block: Kotlmata.Config.() -> Unit
	) : Kotlmata.Config, Expirable({ Log.e { EXPIRED_CONFIG } })
	{
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
	
	private class PostImpl internal constructor(
			block: Kotlmata.Post.() -> Unit
	) : Kotlmata.Post, Expirable({ Log.e { EXPIRED_POST } })
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
				override fun of(block: KotlmataDaemon.Initializer.(T) -> KotlmataMachine.Initializer.End)
				{
					this@PostImpl shouldNot expired
					if (daemon !in daemons)
					{
						logLevel.detail("   Fork", daemon) { KOTLMATA_COMMON }
						daemons[daemon] = KotlmataMutableDaemon.create(daemon, logLevel, block)
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
				override fun set(block: KotlmataMutableMachine.Modifier.(T) -> Unit)
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
								logLevel.detail(tab, signal, "${type.simpleName}::class", priority, daemon) { KOTLMATA_TYPED }
								daemons[daemon]!!.input(signal, type, priority)
							}
							else
							{
								logLevel.normal(tab, signal, "${type.simpleName}::class", priority, daemon) { KOTLMATA_TYPED_IGNORED }
							}
						}
					}
					
					override fun to(daemon: DAEMON)
					{
						this@PostImpl shouldNot expired
						if (daemon in daemons)
						{
							logLevel.detail(tab, signal, "${type.simpleName}::class", 0, daemon) { KOTLMATA_TYPED }
							daemons[daemon]!!.input(signal, type)
						}
						else
						{
							logLevel.normal(tab, signal, "${type.simpleName}::class", 0, daemon) { KOTLMATA_TYPED_IGNORED }
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
							logLevel.detail(tab, signal, priority, daemon) { KOTLMATA_SIGNAL }
							daemons[daemon]!!.input(signal, priority)
						}
						else
						{
							logLevel.normal(tab, signal, priority, daemon) { KOTLMATA_SIGNAL_IGNORED }
						}
					}
				}
				
				override fun to(daemon: DAEMON)
				{
					this@PostImpl shouldNot expired
					if (daemon in daemons)
					{
						logLevel.detail(tab, signal, 0, daemon) { KOTLMATA_SIGNAL }
						daemons[daemon]!!.input(signal)
					}
					else
					{
						logLevel.normal(tab, signal, 0, daemon) { KOTLMATA_SIGNAL_IGNORED }
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
	
	private sealed class Request
	{
		class Fork(val daemon: DAEMON, val block: KotlmataDaemon.Initializer.(DAEMON) -> KotlmataMachine.Initializer.End) : Request()
		class Modify(val daemon: DAEMON, val block: KotlmataMutableMachine.Modifier.(DAEMON) -> Unit) : Request()
		
		class Run(val daemon: DAEMON) : Request()
		class Pause(val daemon: DAEMON) : Request()
		class Stop(val daemon: DAEMON) : Request()
		class Terminate(val daemon: DAEMON) : Request()
		
		class Signal(val daemon: DAEMON, val signal: SIGNAL, val priority: Int) : Request()
		class TypedSignal(val daemon: DAEMON, val signal: SIGNAL, val type: KClass<SIGNAL>, val priority: Int) : Request()
		
		class Post(val block: Kotlmata.Post.() -> Unit) : Request()
		
		override fun toString(): String
		{
			return this::class.simpleName?.toUpperCase() ?: super.toString()
		}
	}
}