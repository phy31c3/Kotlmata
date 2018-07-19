package kr.co.plasticcity.kotlmata

object Kotlmata : KotlmataInterface by KotlmataImpl()

internal interface KotlmataInterface
{
	infix fun init(block: Config.() -> Unit)
	infix fun release(block: (() -> Unit))
}

internal class KotlmataImpl : KotlmataInterface
{
	override fun init(block: Config.() -> Unit)
	{
		TODO("not implemented")
	}
	
	override fun release(block: () -> Unit)
	{
		TODO("not implemented")
	}
}

class Config internal constructor(validBlock: Config.() -> Unit)
{
	private var valid: Boolean = true
	
	init
	{
		validBlock()
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