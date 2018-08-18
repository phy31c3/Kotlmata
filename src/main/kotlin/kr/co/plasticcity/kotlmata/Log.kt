package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/* Config */
		const val INVALID_CONFIG = "** Use of invalid Config object: The object is only available within the 'Kotlmata.init' function."
		
		/* State */
		const val INVALID_STATE_SETTER = "State[%s] : Use of invalid KotlmataMutableState.Modifier object: The object is only available within the initialization or modifying block."
	}
}

internal object Logger
{
	private val none: (String) -> Unit = {}
	var debugLogger: (String) -> Unit = none
	var errorLogger: (String) -> Unit = ::error
	
	inline fun d(vararg args: Any, log: Logs.Companion.() -> String)
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