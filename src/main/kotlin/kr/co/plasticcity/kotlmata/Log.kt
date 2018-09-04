package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		/* Agent */
		const val AGENT_TRANSITION = "Kotlmata%s[%s]: (%s) x (%s) -> (%s)"
		
		/*########################## WARNING ##########################*/
		/* Agent */
		const val OBTAIN_DAEMON_INITIAL = "Kotlmata%s[%s]: Attempt to get 'Daemon initial state': Daemon initial state can not be obtained."
		
		/*########################## ERROR ##########################*/
		/* Config */
		const val EXPIRED_INITIALIZER = "Kotlmata: Use of expired 'Config' object: The object is only available within the 'Kotlmata.init' block."
		
		/* State */
		const val EXPIRED_STATE_MODIFIER = "KotlmataState[%s]: Use of expired 'KotlmataMutableState.Modifier' object: The object is only available within the initialization or modification block."
		
		/* Agent */
		const val EXPIRED_AGENT_MODIFIER = "Kotlmata%s[%s]: Use of expired modifier object: The object is only available within the initialization or modification block."
		const val UNDEFINED_INITIAL_STATE = "Kotlmata%s[%s]: The initial state '%s' is not defined."
	}
}

internal object Log
{
	private val none: (String) -> Unit = {}
	var debug: (String) -> Unit = none
	var warning: (String) -> Unit = none
	var error: (String) -> Unit = ::error
	
	inline fun d(vararg args: Any, log: Logs.Companion.() -> String)
	{
		if (debug != none)
		{
			debug(Logs.log().format(*args))
		}
	}
	
	inline fun w(vararg args: Any, log: Logs.Companion.() -> String)
	{
		if (warning != none)
		{
			warning(Logs.log().format(*args))
		}
	}
	
	inline fun e(vararg args: Any, log: Logs.Companion.() -> String): Nothing
	{
		error(Logs.log().format(*args))
		throw IllegalStateException(Logs.log().format(*args))
	}
}