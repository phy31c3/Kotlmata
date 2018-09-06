package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		/* Agent */
		const val AGENT_TRANSITION = "Kotlmata%s[%s]: (%s) x (%s) -> (%s)"
		
		/* Daemon */
		const val DAEMON_START = "KotlmataDaemon[%s]: --------------------------------------- START ---------------------------------------"
		const val DAEMON_PAUSE = "KotlmataDaemon[%s]: --------------------------------------- PAUSE ---------------------------------------"
		const val DAEMON_STOP = "KotlmataDaemon[%s]: --------------------------------------- STOP ----------------------------------------"
		const val DAEMON_RESUME = "KotlmataDaemon[%s]: --------------------------------------- RESUME --------------------------------------"
		const val DAEMON_TERMINATE = "KotlmataDaemon[%s]: --------------------------------------- TERMINATE ------------------------------------"
		const val DAEMON_POST_MODIFY = "KotlmataDaemon[%s]: Post@ modification. {id: %s}"
		const val DAEMON_POST_SIGNAL = "KotlmataDaemon[%s]: Post@ signal. {signal: %s} {id: %s}"
		const val DAEMON_POST_TYPED_SIGNAL = "KotlmataDaemon[%s]: Post@ typedSignal. {signal: %s, type: %s} {id: %s}"
		const val DAEMON_POST_RUN = "KotlmataDaemon[%s]: Post@ run. {id: %s}"
		const val DAEMON_POST_PAUSE = "KotlmataDaemon[%s]: Post@ pause. {id: %s}"
		const val DAEMON_POST_STOP = "KotlmataDaemon[%s]: Post@ stop. {id: %s}"
		const val DAEMON_POST_TERMINATE = "KotlmataDaemon[%s]: Post@ terminate. {id: %s}"
		const val DAEMON_START_MESSAGE = "KotlmataDaemon[%s]: >> Start message. {id: %s}"
		const val DAEMON_STASH_MESSAGE = "KotlmataDaemon[%s]: Put message in a stash. {id: %s}"
		const val DAEMON_MESSAGE_DROPPED = "KotlmataDaemon[%s]: Message dropped. {id: %s}"
		const val DAEMON_MESSAGE_IGNORED = "KotlmataDaemon[%s]: Message ignored. {id: %s} {daemon state: %s}"
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
	
	inline fun d(vararg args: Any, log: Logs.Companion.() -> String)
	{
		if (debug != none)
		{
			debug(Logs.log().format(*args))
		}
	}
	
	inline fun w(vararg args: Any, log: Logs.Companion.() -> String)
	{
		if (warn != none)
		{
			warn(Logs.log().format(*args))
		}
	}
	
	inline fun e(vararg args: Any, log: Logs.Companion.() -> String): Nothing
	{
		error(Logs.log().format(*args))
		throw IllegalStateException(Logs.log().format(*args))
	}
}