package kr.co.plasticcity.kotlmata

internal object Logger
{
	private val none: (String) -> Unit = {}
	var debugLogger: (String) -> Unit = none
	var errorLogger: (String) -> Unit = ::error
	
	fun d(format: String, vararg args: Any)
	{
		if (debugLogger != none)
		{
			debugLogger(String.format(format, args))
		}
	}
	
	fun e(format: String, vararg args: Any)
	{
		errorLogger(String.format(format, args))
	}
}