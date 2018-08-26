package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		/* Machine */
		const val MACHINE_TRANSITION = "KotlmataMachine[%s]: (%s) x (%s) -> (%s)"
		
		/*########################## ERROR ##########################*/
		/* Config */
		const val EXPIRED_CONFIG = "Kotlmata: Use of expired 'Config' object: The object is only available within the 'Kotlmata.init' block."
		
		/* State */
		const val EXPIRED_STATE_SETTER = "KotlmataState[%s]: Use of expired 'KotlmataMutableState.Modifier' object: The object is only available within the initialization or modifying block."
		
		/* Machine */
		const val EXPIRED_MACHINE_SETTER = "KotlmataMachine[%s]: Use of expired 'KotlmataMutableMachine.Modifier' object: The object is only available within the initialization or modifying block."
		const val NULL_ORIGIN_STATE = "KotlmataMachine[%s]: The origin state %s does not exist in machine."
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
			debug(Logs.log().format(*args))
		}
	}
	
	inline fun e(vararg args: Any, log: Logs.Companion.() -> String): Nothing
	{
		error(Logs.log().format(*args))
		throw IllegalStateException(Logs.log())
	}
}