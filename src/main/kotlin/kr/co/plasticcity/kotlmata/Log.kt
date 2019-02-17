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
		const val DAEMON_START_INIT = "KotlmataDaemon[%s]: >> Start init."
		const val DAEMON_END_INIT = "KotlmataDaemon[%s]: << End init."
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
		const val MACHINE_START_BUILD = "Kotlmata%s >> Start machine build."
		const val MACHINE_END_BUILD = "Kotlmata%s << End machine build."
		const val MACHINE_START_MODIFY = "Kotlmata%s >> Start modify. {current state: %s}"
		const val MACHINE_END_MODIFY = "Kotlmata%s << End modify. {current state: %s}"
		const val MACHINE_START_SIGNAL = "Kotlmata%s >> Start signal input. {signal: %s} {current state: %s}"
		const val MACHINE_END_SIGNAL = "Kotlmata%s << End signal input. {signal: %s} {current state: %s}"
		const val MACHINE_START_TYPED = "Kotlmata%s >> Start typed signal input. {signal: %s, type: %s} {current state: %s}"
		const val MACHINE_END_TYPED = "Kotlmata%s << End typed signal input. {signal: %s, type: %s} {current state: %s}"
		const val MACHINE_START_TRANSITION = "Kotlmata%s >> Start transition. (%s) x (%s) -> (%s)"
		const val MACHINE_END_TRANSITION = "Kotlmata%s << End transition. (%s) x (%s) -> (%s)"
		
		/* State */
		const val STATE_CREATED = "Kotlmata%s %s: Created"
		const val STATE_UPDATED = "Kotlmata%s %s: Updated"
		const val STATE_ENTRY_DEFAULT = "Kotlmata%s %s: Entry action default. {signal: %s}"
		const val STATE_ENTRY_SIGNAL = "Kotlmata%s %s: Entry action. {signal: %s}"
		const val STATE_ENTRY_TYPED = "Kotlmata%s %s: Entry action. {signal: %s, type: %s}"
		const val STATE_INPUT_DEFAULT = "Kotlmata%s %s: Input action default. {signal: %s}"
		const val STATE_INPUT_SIGNAL = "Kotlmata%s %s: Input action. {signal: %s}"
		const val STATE_INPUT_TYPED = "Kotlmata%s %s: Input action. {signal: %s, type: %s}"
		const val STATE_EXIT = "Kotlmata%s %s: Exit action. {signal: %s}"
		const val STATE_ENTRY_NONE = "Kotlmata%s %s: No entry action. {signal: %s}"
		const val STATE_INPUT_NONE = "Kotlmata%s %s: No input action. {signal: %s}"
		const val STATE_EXIT_NONE = "Kotlmata%s %s: No exit action. {signal: %s}"
		
		/*########################## WARN ##########################*/
		const val TRANSITION_FAILED = "Kotlmata%s Attempt transition to a non-existent state. (%s) x (%s) -> (%s)"
		const val OBTAIN_PRE_START = "Kotlmata%s Attempt to get 'pre-start' state: The 'pre-start' state can not be obtained."
		
		/*########################## ERROR ##########################*/
		const val EXPIRED_CONFIG = "Kotlmata: Use of expired 'Config' object: The object is only available inside the config block."
		const val EXPIRED_POST = "Kotlmata: Use of expired 'Post' object: The object is only available inside the post block."
		const val EXPIRED_MODIFIER = "Kotlmata%s Use of expired object: The object is only available inside the 'init' or 'modify' block."
		const val UNDEFINED_INITIAL_STATE = "Kotlmata%s The initial state '%s' is undefined."
	}
}

internal object Log
{
	private val none: (String) -> Unit = {}
	
	@Volatile
	var logLevel: Int = NORMAL
	
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