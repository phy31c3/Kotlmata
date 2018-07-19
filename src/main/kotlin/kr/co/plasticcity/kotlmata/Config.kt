package kr.co.plasticcity.kotlmata

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
				Logger.e(INVALID_CONFIG)
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
				Logger.e(INVALID_CONFIG)
			}
		}
}