package kr.co.plasticcity.kotlmata

object Kotlmata : KotlmataInterface by KotlmataImpl()

internal interface KotlmataInterface
{
	infix fun init(block: DisposableConfig.() -> Unit)
	infix fun release(block: (() -> Unit))
}

private class KotlmataImpl : KotlmataInterface
{
	override fun init(block: DisposableConfig.() -> Unit)
	{
		DisposableConfig(block)
	}
	
	override fun release(block: () -> Unit)
	{
		TODO("not implemented")
	}
}

class DisposableConfig internal constructor(block: DisposableConfig.() -> Unit)
{
	private var valid: Boolean = true
	
	init
	{
		block()
		valid = false
	}
	
	var debugLogger: ((String) -> Unit)
		get() = Logger.debugLogger
		set(value)
		{
			if (valid)
			{
				Logger.debugLogger = value
			}
			else
			{
				Logger.e { INVALID_CONFIG }
			}
		}
	
	var errorLogger: ((String) -> Unit)
		get() = Logger.errorLogger
		set(value)
		{
			if (valid)
			{
				Logger.errorLogger = value
			}
			else
			{
				Logger.e { INVALID_CONFIG }
			}
		}
}