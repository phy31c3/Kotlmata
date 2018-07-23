package kr.co.plasticcity.kotlmata

object Kotlmata : KotlmataInterface by KotlmataImpl()
{
	class Config internal constructor(block: Config.() -> Unit)
	{
		private var valid: Boolean = true
		
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
		
		init
		{
			block()
			valid = false
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
}

internal interface KotlmataInterface
{
	infix fun init(block: Kotlmata.Config.() -> Unit)
	infix fun release(block: (() -> Unit))
}

private class KotlmataImpl : KotlmataInterface
{
	override fun init(block: Kotlmata.Config.() -> Unit)
	{
		Kotlmata.Config(block)
		/* should implement */
	}
	
	override fun release(block: () -> Unit)
	{
		/* should implement */
	}
}