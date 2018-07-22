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
		/* should implement */
	}
	
	override fun release(block: () -> Unit)
	{
		/* should implement */
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
			ifValid { Logger.debugLogger = value }
		}
	
	var errorLogger: ((String) -> Unit)
		get() = Logger.errorLogger
		set(value)
		{
			ifValid { Logger.errorLogger = value }
		}
	
	private fun ifValid(block: () -> Unit)
	{
		if (valid)
		{
			block()
		}
		else
		{
			Logger.e { INVALID_CONFIG }
		}
	}
}