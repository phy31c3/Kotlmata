package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
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
		const val MACHINE_START_BUILD = "%s build {"
		const val MACHINE_START_UPDATE = "%s update (state: %s) {"
		const val MACHINE_START_INPUT = "%s input (state: %s, signal: %s, payload: %s) {"
		const val MACHINE_START_TYPED_INPUT = "%s input (state: %s, signal: %s, type: %s, payload: %s) {"
		const val MACHINE_START_TRANSITION = "%s [%s] x (%s) -> [%s]"
		const val MACHINE_START_TRANSITION_TAB = "%s     [%s] x (%s) -> [%s]"
		const val MACHINE_END = "%s }"
		
		/* State */
		const val STATE_CREATED = "%s [%s] created"
		const val STATE_UPDATED = "%s [%s] updated"
		const val STATE_RUN_ENTRY = "%s [%s] entry action"
		const val STATE_RUN_ENTRY_VIA = "%s [%s] entry via (%s) action"
		const val STATE_RUN_ENTRY_PREDICATE = "%s [%s] entry via predicate action"
		const val STATE_RUN_INPUT = "%s [%s] input action"
		const val STATE_RUN_INPUT_SIGNAL = "%s [%s] input signal (%s) action"
		const val STATE_RUN_INPUT_PREDICATE = "%s [%s] input signal predicate action"
		const val STATE_RUN_EXIT = "%s [%s] exit action"
		const val STATE_RUN_EXIT_VIA = "%s [%s] exit via (%s) action"
		const val STATE_RUN_EXIT_PREDICATE = "%s [%s] exit via predicate action"
		const val STATE_NO_ENTRY = "%s [%s] no entry action for (%s)"
		const val STATE_NO_INPUT = "%s [%s] no input action for (%s)"
		const val STATE_NO_EXIT = "%s [%s] no exit action for (%s)"
		
		/*########################## WARN ##########################*/
		const val TRANSITION_FAILED = "%s Attempt transition to a non-existent state. [%s] x (%s) -> [%s]"
		
		/*########################## ERROR ##########################*/
		const val EXPIRED_OBJECT = "%s Use of expired object: The object is only available inside the 'init' or 'update' block."
		const val UNDEFINED_START_STATE = "%s The start state '%s' is undefined."
	}
}

internal const val NO_LOG = 0
internal const val SIMPLE = 1
internal const val NORMAL = 2
internal const val DETAIL = 3

internal object Log
{
	@Volatile
	var debug: ((String) -> Unit)? = null
	
	@Volatile
	var warn: ((String) -> Unit)? = null
	
	@Volatile
	var error: (String) -> Unit = ::error
	
	fun Int.simple(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		if (this >= SIMPLE) debug?.invoke(Logs.log().format(*args))
	}
	
	fun Int.normal(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		if (this >= NORMAL) debug?.invoke(Logs.log().format(*args))
	}
	
	fun Int.detail(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		if (this >= DETAIL) debug?.invoke(Logs.log().format(*args))
	}
	
	fun w(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		warn?.invoke(Logs.log().format(*args))
	}
	
	fun e(vararg args: Any?, log: Logs.Companion.() -> String): Nothing
	{
		Logs.log().format(*args).also { formatted ->
			error(formatted)
			throw IllegalStateException(formatted)
		}
	}
}
