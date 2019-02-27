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
		const val DAEMON_START_THREAD = "Daemon[%s]: Thread is started. (name = %s, isDaemon = %s)"
		const val DAEMON_TERMINATE_THREAD = "Daemon[%s]: Thread is going to terminate. (name = %s, isDaemon = %s)"
		const val DAEMON_START_INIT = "Daemon[%s]: >> Start init."
		const val DAEMON_END_INIT = "Daemon[%s]: << End init."
		const val DAEMON_START = "Daemon[%s]:    START"
		const val DAEMON_PAUSE = "Daemon[%s]:    PAUSE"
		const val DAEMON_STOP = "Daemon[%s]:    STOP"
		const val DAEMON_RESUME = "Daemon[%s]:    RESUME"
		const val DAEMON_TERMINATE = "Daemon[%s]:    TERMINATE"
		const val DAEMON_REQUEST = "Daemon[%s]: ## REQUEST@%s. {id: %s}"
		const val DAEMON_REQUEST_SIGNAL = "Daemon[%s]: ## REQUEST@Signal input. {signal: %s, priority: %s} {id: %s}"
		const val DAEMON_REQUEST_TYPED = "Daemon[%s]: ## REQUEST@Typed signal input. {signal: %s, type: %s, priority: %s} {id: %s}"
		const val DAEMON_REQUEST_SYNC = "Daemon[%s]: ## REQUEST@Sync Input. {signal: %s} {id: %s}"
		const val DAEMON_START_REQUEST = "Daemon[%s]: >> Start request. {id: %s}"
		const val DAEMON_KEEP_REQUEST = "Daemon[%s]:    Keep request. {id: %s}"
		const val DAEMON_KEEP_SYNC = "Daemon[%s]:    Keep sync Input. {id: %s}"
		const val DAEMON_DROP_REQUEST = "Daemon[%s]:    Request dropped. {id: %s}"
		const val DAEMON_IGNORE_REQUEST = "Daemon[%s]:    Request ignored. {daemon state: %s} {id: %s}"
		const val DAEMON_END_REQUEST = "Daemon[%s]: << End request. {id: %s}"
		
		/* Machine */
		const val MACHINE_START_BUILD = "%s >> Start machine build."
		const val MACHINE_END_BUILD = "%s << End machine build."
		const val MACHINE_START_MODIFY = "%s >> Start modify. {current state: %s}"
		const val MACHINE_END_MODIFY = "%s << End modify. {current state: %s}"
		const val MACHINE_START_SIGNAL = "%s >> Start signal input. {signal: %s} {current state: %s}"
		const val MACHINE_END_SIGNAL = "%s << End signal input. {signal: %s} {current state: %s}"
		const val MACHINE_START_TYPED = "%s >> Start typed signal input. {signal: %s, type: %s} {current state: %s}"
		const val MACHINE_END_TYPED = "%s << End typed signal input. {signal: %s, type: %s} {current state: %s}"
		const val MACHINE_START_TRANSITION = "%s >> Start transition. (%s) x (%s) -> (%s)"
		const val MACHINE_END_TRANSITION = "%s << End transition."
		
		/* State */
		const val STATE_CREATED = "%s %s: Created"
		const val STATE_UPDATED = "%s %s: Updated"
		const val STATE_ENTRY_DEFAULT = "%s %s: Entry action default. {signal: %s}"
		const val STATE_ENTRY_SIGNAL = "%s %s: Entry action. {signal: %s}"
		const val STATE_ENTRY_TYPED = "%s %s: Entry action. {signal: %s, type: %s}"
		const val STATE_INPUT_DEFAULT = "%s %s: Input action default. {signal: %s}"
		const val STATE_INPUT_SIGNAL = "%s %s: Input action. {signal: %s}"
		const val STATE_INPUT_TYPED = "%s %s: Input action. {signal: %s, type: %s}"
		const val STATE_EXIT = "%s %s: Exit action. {signal: %s}"
		const val STATE_ENTRY_NONE = "%s %s: No entry action. {signal: %s}"
		const val STATE_INPUT_NONE = "%s %s: No input action. {signal: %s}"
		const val STATE_EXIT_NONE = "%s %s: No exit action. {signal: %s}"
		
		/*########################## WARN ##########################*/
		const val TRANSITION_FAILED = "%s Attempt transition to a non-existent state. (%s) x (%s) -> (%s)"
		const val OBTAIN_PRE_START = "%s Attempt to get 'pre-start' state: The 'pre-start' state can not be obtained."
		
		/*########################## ERROR ##########################*/
		const val EXPIRED_CONFIG = "Kotlmata: Use of expired 'Config' object: The object is only available inside the config block."
		const val EXPIRED_POST = "Kotlmata: Use of expired 'Post' object: The object is only available inside the post block."
		const val EXPIRED_MODIFIER = "%s Use of expired object: The object is only available inside the 'init' or 'modify' block."
		const val UNDEFINED_INITIAL_STATE = "%s The initial state '%s' is undefined."
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