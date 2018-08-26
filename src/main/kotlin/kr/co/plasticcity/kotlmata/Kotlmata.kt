package kr.co.plasticcity.kotlmata

object Kotlmata : KotlmataInterface by KotlmataImpl()
{
	class Config internal constructor(block: Config.() -> Unit) : CanExpire({ Log.e { EXPIRED_CONFIG } })
	{
		var debugLogger: ((String) -> Unit)
			get() = Log.debug
			set(value)
			{
				this shouldNot expired
				Log.debug = value
			}
		
		var errorLogger: ((String) -> Unit)
			get() = Log.error
			set(value)
			{
				this shouldNot expired
				Log.error = value
			}
		
		init
		{
			block()
			expire()
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