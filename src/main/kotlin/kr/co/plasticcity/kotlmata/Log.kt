package kr.co.plasticcity.kotlmata

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		/* Daemon */
		const val DAEMON_START_THREAD = "Daemon[%s]: Daemon thread is started. (name = %s, isDaemon = %s)"
		const val DAEMON_TERMINATE_THREAD = "Daemon[%s]: Daemon thread terminate. (name = %s, isDaemon = %s)"
		const val DAEMON_START_CREATE = "Daemon[%s]: daemon(name: %s) {"
		const val DAEMON_END_CREATE = "Daemon[%s]: }"
		const val DAEMON_REGISTER_ON_CREATE = "Daemon[%s]:%s     on create registered"
		const val DAEMON_REGISTER_ON_START = "Daemon[%s]:%s     on start registered"
		const val DAEMON_REGISTER_ON_PAUSE = "Daemon[%s]:%s     on pause registered"
		const val DAEMON_REGISTER_ON_STOP = "Daemon[%s]:%s     on stop registered"
		const val DAEMON_REGISTER_ON_RESUME = "Daemon[%s]:%s     on resume registered"
		const val DAEMON_REGISTER_ON_FINISH = "Daemon[%s]:%s     on finish registered"
		const val DAEMON_REGISTER_ON_DESTROY = "Daemon[%s]:%s     on destroy registered"
		const val DAEMON_START_REQUEST = "Daemon[%s]: %s(remain: %s) {"
		const val DAEMON_END_REQUEST = "Daemon[%s]: }"
		const val DAEMON_LIFECYCLE_CHANGED = "Daemon[%s]: lifecycle: %s -> %s"
		const val DAEMON_LIFECYCLE_CHANGED_TAB = "Daemon[%s]:     lifecycle: %s -> %s"
		const val DAEMON_ON_CREATE = "Daemon[%s]: onCreate()"
		const val DAEMON_ON_START = "Daemon[%s]:%s onStart(payload: %s)"
		const val DAEMON_ON_PAUSE = "Daemon[%s]:%s onPause(payload: %s)"
		const val DAEMON_ON_STOP = "Daemon[%s]:%s onStop(payload: %s)"
		const val DAEMON_ON_RESUME = "Daemon[%s]:%s onResume(payload: %s)"
		const val DAEMON_ON_FINISH = "Daemon[%s]: onFinish(payload: %s)"
		const val DAEMON_ON_DESTROY = "Daemon[%s]: onDestroy()"
		const val DAEMON_KEEP_REQUEST = "Daemon[%s]:     kept"
		const val DAEMON_IGNORE_REQUEST = "Daemon[%s]:     ignored"
		
		/* Machine */
		const val MACHINE_BUILD = "%s machine(name: %s) {"
		const val MACHINE_REGISTER_ON_TRANSITION = "%s     on transition registered"
		const val MACHINE_REGISTER_ON_ERROR = "%s     on error registered"
		const val MACHINE_UPDATE = "%s update(state: %s) {"
		const val MACHINE_INPUT = "%s input(signal: %s, payload: %s) {"
		const val MACHINE_TYPED_INPUT = "%s input(signal: %s, type: %s, payload: %s) {"
		const val MACHINE_TRANSITION = "%s [%s] x (%s) -> [%s]"
		const val MACHINE_TRANSITION_TAB = "%s     [%s] x (%s) -> [%s]"
		const val MACHINE_RETURN_SYNC_INPUT = "%s     return(signal: %s, type: %s, payload: %s)"
		const val MACHINE_START_AT = "%s     start at [%s]"
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
		const val DAEMON_INTERRUPTED = "Daemon[%s]: Daemon thread is unexpectedly interrupted."
		
		/*########################## ERROR ##########################*/
		const val EXPIRED_OBJECT = "%s Use of expired object: The object is only available inside the 'init' or 'update' block."
		const val UNDEFINED_START_STATE = "%s The start state '%s' is undefined."
		const val FAILED_TO_GET_STATE = "%s Attempted to get state %s that does not exist. (Make sure that state %s wasn't deleted at this time.)"
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
