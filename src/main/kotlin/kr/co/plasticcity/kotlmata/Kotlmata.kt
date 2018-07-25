package kr.co.plasticcity.kotlmata

object Kotlmata : KotlmataInterface by KotlmataImpl()
{
	class Config internal constructor(block: Config.() -> Unit)
	{
		@Volatile
		private var expired: Boolean = false
		
		var debugLogger: ((String) -> Unit)
			get() = Logger.debugLogger
			set(value)
			{
				expired should { return }
				Logger.debugLogger = value
			}
		
		var errorLogger: ((String) -> Unit)
			get() = Logger.errorLogger
			set(value)
			{
				expired should { return }
				Logger.errorLogger = value
			}
		
		init
		{
			block()
			expired = true
		}
		
		private inline infix fun Boolean.should(block: () -> Unit)
		{
			if (expired)
			{
				Logger.e { INVALID_CONFIG }
				block()
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