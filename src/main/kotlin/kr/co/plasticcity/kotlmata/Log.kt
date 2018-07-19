package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/* Config */
		const val INVALID_CONFIG = "Use invalid Config object. Config is only available within the 'Kotlmata.init' function"
	}
}

internal object Logger
{
	private val none: (String) -> Unit = {}
	var debugLogger: (String) -> Unit = none
	var errorLogger: (String) -> Unit = ::error
	
	fun d(vararg args: Any, log: Logs.Companion.() -> String)
	{
		if (debugLogger != none)
		{
			debugLogger(String.format(Logs.log(), args))
		}
	}
	
	inline fun e(vararg args: Any, log: Logs.Companion.() -> String)
	{
		errorLogger(String.format(Logs.log(), args))
	}
}