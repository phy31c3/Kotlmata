package kr.co.plasticcity.kotlmata

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

internal class KotlmataImpl : KotlmataInterface
{
	private enum class State
	{
		NOT_INIT, RUNNING, RELEASED
	}
	
	@Volatile
	private var state: State = State.NOT_INIT
	
	private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock(true)
	private val readlock: Lock = lock.readLock()
	private val writelock: Lock = lock.writeLock()
	
	override fun init(block: Config.() -> Unit)
	{
		writelock.lock()
		try
		{
			if (state != State.RUNNING)
			{
				state = State.RUNNING
				Config().block()
			}
		}
		finally
		{
			writelock.unlock()
		}
	}
}
