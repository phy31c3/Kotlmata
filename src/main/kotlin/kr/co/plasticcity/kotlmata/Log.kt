package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		/* Machine */
		const val MACHINE_TRANSITION = "Machine[%s] : %s x %s -> %s"
		
		/*########################## ERROR ##########################*/
		/* Config */
		const val INVALID_CONFIG = "** Use of invalid Config object: The object is only available within the 'Kotlmata.init' function."
		
		/* State */
		const val INVALID_STATE_SETTER = "State[%s] : Use of invalid KotlmataMutableState.Modifier object: The object is only available within the initialization or modifying block."
		
		/* Machine */
		const val INVALID_MACHINE_SETTER = "Machine[%s] : Use of invalid KotlmataMutableMachine.Modifier object: The object is only available within the initialization or modifying block."
		const val INVALID_ORIGIN_STATE = "Machine[%s] : The origin state %s does not exist in machine."
	}
}

internal object Log
{
	private val none: (String) -> Unit = {}
	var debug: (String) -> Unit = none
	var error: (String) -> Unit = ::error
	
	inline fun d(vararg args: Any, log: Logs.Companion.() -> String)
	{
		if (debug != none)
		{
			debug(Logs.log().format(args))
		}
	}
	
	inline fun e(vararg args: Any, log: Logs.Companion.() -> String)
	{
		error(Logs.log().format(args))
	}
}