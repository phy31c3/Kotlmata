package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		/* Kotlmata */
		const val KOTLMATA_START = "Kotlmata: START"
		const val KOTLMATA_RESTART = "Kotlmata: RESTART"
		const val KOTLMATA_SHUTDOWN = "Kotlmata: SHUTDOWN"
		const val KOTLMATA_RELEASE = "Kotlmata: RELEASE"
		const val KOTLMATA_COMMON = "Kotlmata: %s. {daemon: %s}"
		const val KOTLMATA_SIGNAL = "Kotlmata:%s Signal input. {signal: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_TYPED = "Kotlmata:%s Typed signal input. {signal: %s, type: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_COMMON_IGNORED_EXISTS = "Kotlmata: %s is ignored: The daemon is already exists. {daemon: %s}"
		const val KOTLMATA_COMMON_IGNORED_NONE = "Kotlmata: %s is ignored: The daemon is not exists. {daemon: %s}"
		const val KOTLMATA_SIGNAL_IGNORED = "Kotlmata:%s Signal input is ignored: The daemon is not exists. {signal: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_TYPED_IGNORED = "Kotlmata:%s Typed signal input is ignored: The daemon is not exists. {signal: %s, type: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_START_POST = "Kotlmata: >> Start post block."
		const val KOTLMATA_END_POST = "Kotlmata: << End post block."
		
		/* Daemon */
		const val DAEMON_START = "KotlmataDaemon[%s]:    START"
		const val DAEMON_PAUSE = "KotlmataDaemon[%s]:    PAUSE"
		const val DAEMON_STOP = "KotlmataDaemon[%s]:    STOP"
		const val DAEMON_RESUME = "KotlmataDaemon[%s]:    RESUME"
		const val DAEMON_TERMINATE = "KotlmataDaemon[%s]:    TERMINATE"
		const val DAEMON_REQUEST = "KotlmataDaemon[%s]: ## REQUEST@%s. {id: %s}"
		const val DAEMON_REQUEST_SIGNAL = "KotlmataDaemon[%s]: ## REQUEST@Signal input. {signal: %s, priority: %s} {id: %s}"
		const val DAEMON_REQUEST_TYPED = "KotlmataDaemon[%s]: ## REQUEST@Typed signal input. {signal: %s, type: %s, priority: %s} {id: %s}"
		const val DAEMON_REQUEST_EXPRESS = "KotlmataDaemon[%s]: ## REQUEST@Express Input. {signal: %s} {id: %s}"
		const val DAEMON_START_REQUEST = "KotlmataDaemon[%s]: >> Start request. {id: %s}"
		const val DAEMON_KEEP_REQUEST = "KotlmataDaemon[%s]:    Keep request. {id: %s}"
		const val DAEMON_KEEP_EXPRESS = "KotlmataDaemon[%s]:    Keep express Input. {id: %s}"
		const val DAEMON_DROP_REQUEST = "KotlmataDaemon[%s]:    Request dropped. {id: %s}"
		const val DAEMON_IGNORE_REQUEST = "KotlmataDaemon[%s]:    Request ignored. {daemon state: %s} {id: %s}"
		const val DAEMON_END_REQUEST = "KotlmataDaemon[%s]: << End request. {id: %s}"
		
		/* Machine */
		const val AGENT_TRANSITION = "Kotlmata%s[%s]: (%s) x (%s) -> (%s)"
		const val AGENT_TYPED_TRANSITION = "Kotlmata%s[%s]: (%s) x (%s as %s) -> (%s)"
		const val AGENT_INPUT = "Kotlmata%s[%s]: Input(%s). {current state: %s}"
		const val AGENT_TYPED_INPUT = "Kotlmata%s[%s]: Input(%s as %s). {current state: %s}"
		const val AGENT_MODIFY = "Kotlmata%s[%s]: Modify. {current state: %s}"
		
		/*########################## WARN ##########################*/
		/* Agent */
		const val OBTAIN_INITIAL = "Kotlmata%s[%s]: Attempt to get 'initial state': The initial state can not be obtained."
		
		/*########################## ERROR ##########################*/
		/* Kotlmata */
		const val EXPIRED_CONFIG = "Kotlmata: Use of expired 'Config' object: The object is only available within the 'Kotlmata.config' block."
		
		/* Agent */
		const val EXPIRED_AGENT_MODIFIER = "Kotlmata%s[%s]: Use of expired modifier object: The object is only available within the initialization or modification block."
		const val UNDEFINED_INITIAL_STATE = "Kotlmata%s[%s]: The initial state '%s' is not defined."
		
		/* State */
		const val EXPIRED_STATE_MODIFIER = "KotlmataState[%s]: Use of expired 'KotlmataMutableState.Modifier' object: The object is only available within the initialization or modification block."
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