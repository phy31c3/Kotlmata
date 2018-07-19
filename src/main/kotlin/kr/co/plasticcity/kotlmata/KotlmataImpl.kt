package kr.co.plasticcity.kotlmata

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

internal class KotlmataImpl : KotlmataInterface
{
	private enum class State
	{
		FRESH, RUNNING, RELEASED
	}
	
	private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock(true)
	private val readLock: Lock = lock.readLock()
	private val writeLock: Lock = lock.writeLock()
	
	@Volatile
	private var state: State = State.FRESH
	@Volatile
	private lateinit var worker: Worker
	
	override fun init(block: Config.() -> Unit)
	{
		writeLock.lock()
		try
		{
			if (state != State.RUNNING)
			{
				Config(block)
				worker = Worker()
				state = State.RUNNING
				Logger.d(KOTLMATA_INITIALIZED)
			}
		}
		finally
		{
			writeLock.unlock()
		}
	}
	
	override fun release(block: () -> Unit)
	{
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	private class Worker
}