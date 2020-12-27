package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		/* Kotlmata */
		const val KOTLMATA_START = "Kotlmata: START"
		const val KOTLMATA_PAUSE = "Kotlmata: PAUSE"
		const val KOTLMATA_STOP = "Kotlmata: STOP"
		const val KOTLMATA_RESUME = "Kotlmata: RESUME"
		const val KOTLMATA_RELEASE = "Kotlmata: RELEASE"
		const val KOTLMATA_COMMON = "Kotlmata: %s {daemon: %s}"
		const val KOTLMATA_INPUT = "Kotlmata:%s Input signal {signal: %s, payload: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_TYPED_INPUT = "Kotlmata:%s Input typed signal {signal: %s, type: %s, payload: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_COMMON_IGNORED_EXISTS = "Kotlmata: %s is ignored: The daemon is already exists. {daemon: %s}"
		const val KOTLMATA_COMMON_IGNORED_NONE = "Kotlmata: %s is ignored: The daemon does not exist or invalid. {daemon: %s}"
		const val KOTLMATA_INPUT_IGNORED = "Kotlmata:%s Signal input is ignored: The daemon does not exist or invalid. {signal: %s, payload: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_TYPED_INPUT_IGNORED = "Kotlmata:%s Typed signal input is ignored: The daemon does not exist or invalid. {signal: %s, type: %s, payload: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_START_POST = "Kotlmata: >> Post block"
		const val KOTLMATA_END_POST = "Kotlmata: << Post block"
		
		/* Daemon */
		const val DAEMON_START_THREAD = "Daemon[%s]: Daemon thread is started. (name = %s, isDaemon = %s)"
		const val DAEMON_TERMINATE_THREAD = "Daemon[%s]: Daemon thread terminate. (name = %s, isDaemon = %s)"
		const val DAEMON_START_CREATE = "Daemon[%s]: >> CREATE"
		const val DAEMON_END_CREATE = "Daemon[%s]: << CREATE"
		const val DAEMON_START = "Daemon[%s]:%s START"
		const val DAEMON_PAUSE = "Daemon[%s]:%s PAUSE"
		const val DAEMON_STOP = "Daemon[%s]:%s STOP"
		const val DAEMON_RESUME = "Daemon[%s]:%s RESUME"
		const val DAEMON_TERMINATE = "Daemon[%s]:%s TERMINATE"
		const val DAEMON_DESTROY = "Daemon[%s]:%s DESTROY"
		const val DAEMON_PUT_REQUEST = "Daemon[%s]: ## Put REQUEST %s"
		const val DAEMON_START_REQUEST = "Daemon[%s]: >> REQUEST {remain: %s} %s"
		const val DAEMON_KEEP_REQUEST = "Daemon[%s]:    Keep REQUEST %s"
		const val DAEMON_STORE_REQUEST = "Daemon[%s]:    Store REQUEST %s"
		const val DAEMON_REMOVE_REQUEST = "Daemon[%s]:    Remove REQUEST %s"
		const val DAEMON_IGNORE_REQUEST = "Daemon[%s]:    Ignore REQUEST {daemon_lifecycle: %s} %s"
		const val DAEMON_END_REQUEST = "Daemon[%s]: << REQUEST {spent: %sms} %s"
		
		/* Machine */
		const val MACHINE_START_BUILD = "%s >> Build machine"
		const val MACHINE_END_BUILD = "%s << Build machine"
		const val MACHINE_START_MODIFY = "%s >> Modify {current_state: %s}"
		const val MACHINE_END_MODIFY = "%s << Modify {current_state: %s}"
		const val MACHINE_START_INPUT = "%s >> Input signal {signal: %s, payload: %s} {current_state: %s}"
		const val MACHINE_END_INPUT = "%s << Input signal {signal: %s, payload: %s} {current_state: %s}"
		const val MACHINE_START_TYPED_INPUT = "%s >> Input typed signal {signal: %s, type: %s, payload: %s} {current_state: %s}"
		const val MACHINE_END_TYPED_INPUT = "%s << Input typed signal {signal: %s, type: %s, payload: %s} {current_state: %s}"
		const val MACHINE_START_TRANSITION = "%s >> Transition [%s] x (%s) -> [%s]"
		const val MACHINE_END_TRANSITION = "%s << Transition"
		
		/* State */
		const val STATE_CREATED = "%s [%s] created"
		const val STATE_UPDATED = "%s [%s] updated"
		const val STATE_RUN_ENTRY_DEFAULT = "%s [%s] Run entry action {signal: %s}"
		const val STATE_RUN_ENTRY_DEFAULT_TYPED = "%s [%s] Run entry action {signal: %s, type: %s}"
		const val STATE_RUN_ENTRY_OBJECT = "%s [%s] Run entry via object action {signal: %s}"
		const val STATE_RUN_ENTRY_CLASS = "%s [%s] Run entry via %s action {signal: %s}"
		const val STATE_RUN_ENTRY_CLASS_TYPED = "%s [%s] Run entry via %s action {signal: %s, type: %s}"
		const val STATE_RUN_INPUT_DEFAULT = "%s [%s] Run input action {signal: %s}"
		const val STATE_RUN_INPUT_DEFAULT_TYPED = "%s [%s] Run input action {signal: %s, type: %s}"
		const val STATE_RUN_INPUT_OBJECT = "%s [%s] Run input signal object action {signal: %s}"
		const val STATE_RUN_INPUT_CLASS = "%s [%s] Run input signal %s action {signal: %s}"
		const val STATE_RUN_INPUT_CLASS_TYPED = "%s [%s] Run input signal %s action {signal: %s, type: %s}"
		const val STATE_RUN_EXIT_DEFAULT = "%s [%s] Run exit action {signal: %s}"
		const val STATE_RUN_EXIT_DEFAULT_TYPED = "%s [%s] Run exit action {signal: %s, type: %s}"
		const val STATE_RUN_EXIT_OBJECT = "%s [%s] Run exit via object action {signal: %s}"
		const val STATE_RUN_EXIT_CLASS = "%s [%s] Run exit via %s action {signal: %s}"
		const val STATE_RUN_EXIT_CLASS_TYPED = "%s [%s] Run exit via %s action {signal: %s, type: %s}"
		const val STATE_NO_ENTRY = "%s [%s] No entry action for {signal: %s}"
		const val STATE_NO_ENTRY_TYPED = "%s [%s] No entry action for {signal: %s, type: %s}"
		const val STATE_NO_INPUT = "%s [%s] No input action for {signal: %s}"
		const val STATE_NO_INPUT_TYPED = "%s [%s] No input action for {signal: %s, type: %s}"
		const val STATE_NO_EXIT = "%s [%s] No exit action for {signal: %s}"
		const val STATE_NO_EXIT_TYPED = "%s [%s] No exit action for {signal: %s, type: %s}"
		
		/*########################## WARN ##########################*/
		const val TRANSITION_FAILED = "%s Attempt transition to a non-existent state. [%s] x (%s) -> [%s]"
		
		/*########################## ERROR ##########################*/
		const val EXPIRED_CONFIG = "Kotlmata: Use of expired 'Config' object: The object is only available inside the config block."
		const val EXPIRED_POST = "Kotlmata: Use of expired 'Post' object: The object is only available inside the post block."
		const val EXPIRED_MODIFIER = "%s Use of expired object: The object is only available inside the 'init' or 'modify' block."
		const val UNDEFINED_START_STATE = "%s The start state '%s' is undefined."
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
