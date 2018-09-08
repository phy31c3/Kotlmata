package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		/* Agent */
		const val AGENT_TRANSITION = "Kotlmata%s[%s]: (%s) x (%s) -> (%s)"
		const val AGENT_TYPED_TRANSITION = "Kotlmata%s[%s]: (%s) x (%s as %s) -> (%s)"
		const val AGENT_INPUT = "Kotlmata%s[%s]: Input(%s). {current state: %s}"
		const val AGENT_TYPED_INPUT = "Kotlmata%s[%s]: Input(%s as %s). {current state: %s}"
		const val AGENT_MODIFY = "Kotlmata%s[%s]: Modify. {current state: %s}"
		
		/* Daemon */
		const val DAEMON_START = "KotlmataDaemon[%s]: --------------------------------------- START ---------------------------------------"
		const val DAEMON_PAUSE = "KotlmataDaemon[%s]: --------------------------------------- PAUSE ---------------------------------------"
		const val DAEMON_STOP = "KotlmataDaemon[%s]: --------------------------------------- STOP ----------------------------------------"
		const val DAEMON_RESUME = "KotlmataDaemon[%s]: --------------------------------------- RESUME --------------------------------------"
		const val DAEMON_TERMINATE = "KotlmataDaemon[%s]: --------------------------------------- TERMINATE ------------------------------------"
		const val DAEMON_POST_MESSAGE = "KotlmataDaemon[%s]: Post@%s. {id: %s}"
		const val DAEMON_POST_INPUT = "KotlmataDaemon[%s]: Post@%s. {signal: %s} {id: %s}"
		const val DAEMON_POST_TYPED_INPUT = "KotlmataDaemon[%s]: Post@%s. {signal: %s, type: %s} {id: %s}"
		const val DAEMON_POST_QUICK_INPUT = "KotlmataDaemon[%s]: Post@%s. {signal: %s} {id: %s}"
		const val DAEMON_START_MESSAGE = "KotlmataDaemon[%s]: >> Start message. {id: %s}"
		const val DAEMON_KEEP_MESSAGE = "KotlmataDaemon[%s]:    Keep message. {id: %s}"
		const val DAEMON_KEEP_QUICK_INPUT = "KotlmataDaemon[%s]:    Keep QuickInput. {id: %s}"
		const val DAEMON_MESSAGE_DROPPED = "KotlmataDaemon[%s]:    Message dropped. {id: %s}"
		const val DAEMON_MESSAGE_IGNORED = "KotlmataDaemon[%s]:    Message ignored. {id: %s} {daemon state: %s}"
		const val DAEMON_END_MESSAGE = "KotlmataDaemon[%s]: << End message. {id: %s}"
		
		/*########################## WARN ##########################*/
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
	var warn: (String) -> Unit = none
	var error: (String) -> Unit = ::error
	
	inline fun d(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		if (debug != none)
		{
			debug(Logs.log().format(*args))
		}
	}
	
	inline fun w(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		if (warn != none)
		{
			warn(Logs.log().format(*args))
		}
	}
	
	inline fun e(vararg args: Any?, log: Logs.Companion.() -> String): Nothing
	{
		error(Logs.log().format(*args))
		throw IllegalStateException(Logs.log().format(*args))
	}
}