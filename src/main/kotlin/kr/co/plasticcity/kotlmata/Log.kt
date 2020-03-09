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
		const val KOTLMATA_COMMON_IGNORED_NONE = "Kotlmata: %s is ignored: The daemon is not exists. {daemon: %s}"
		const val KOTLMATA_INPUT_IGNORED = "Kotlmata:%s Signal input is ignored: The daemon is not exists. {signal: %s, payload: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_TYPED_INPUT_IGNORED = "Kotlmata:%s Typed signal input is ignored: The daemon is not exists. {signal: %s, type: %s, payload: %s, priority: %s} {daemon: %s}"
		const val KOTLMATA_START_POST = "Kotlmata: >> Post block"
		const val KOTLMATA_END_POST = "Kotlmata: << Post block"
		
		/* Daemon */
		const val DAEMON_START_THREAD = "Daemon[%s]: Thread is started. (name = %s, isDaemon = %s)"
		const val DAEMON_TERMINATE_THREAD = "Daemon[%s]: Thread terminate. (name = %s, isDaemon = %s)"
		const val DAEMON_START_INIT = "Daemon[%s]: >> Init"
		const val DAEMON_END_INIT = "Daemon[%s]: << Init"
		const val DAEMON_START = "Daemon[%s]:%s START"
		const val DAEMON_PAUSE = "Daemon[%s]:%s PAUSE"
		const val DAEMON_STOP = "Daemon[%s]:%s STOP"
		const val DAEMON_RESUME = "Daemon[%s]:%s RESUME"
		const val DAEMON_TERMINATE = "Daemon[%s]:%s TERMINATE"
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
		const val STATE_ENTRY_DEFAULT = "%s [%s] run default entry action {signal: %s}"
		const val STATE_ENTRY_SIGNAL = "%s [%s] run entry action {signal: %s}"
		const val STATE_ENTRY_TYPED = "%s [%s] run entry action {signal: %s, type: %s}"
		const val STATE_INPUT_DEFAULT = "%s [%s] run default input action {signal: %s}"
		const val STATE_INPUT_SIGNAL = "%s [%s] run input action {signal: %s}"
		const val STATE_INPUT_TYPED = "%s [%s] run input action {signal: %s, type: %s}"
		const val STATE_EXIT = "%s [%s] run exit action {signal: %s}"
		const val STATE_ENTRY_NONE = "%s [%s] no entry action {signal: %s}"
		const val STATE_INPUT_NONE = "%s [%s] no input action {signal: %s}"
		const val STATE_EXIT_NONE = "%s [%s] no exit action {signal: %s}"
		
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