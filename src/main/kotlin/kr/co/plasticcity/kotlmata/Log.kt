package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

internal class Logs
{
	companion object
	{
		/*########################## DEBUG ##########################*/
		
		/* State */
		const val STATE_RUN_ENTRY = "%s [%s] entry action"
		const val STATE_RUN_ENTRY_VIA = "%s [%s] entry via (%s) action"
		const val STATE_RUN_ENTRY_PREDICATE = "%s [%s] entry via predicate(or range) action"
		const val STATE_RUN_INPUT = "%s [%s] input action"
		const val STATE_RUN_INPUT_SIGNAL = "%s [%s] input signal (%s) action"
		const val STATE_RUN_INPUT_PREDICATE = "%s [%s] input signal predicate(or range) action"
		const val STATE_RUN_EXIT = "%s [%s] exit action"
		const val STATE_RUN_EXIT_VIA = "%s [%s] exit via (%s) action"
		const val STATE_RUN_EXIT_PREDICATE = "%s [%s] exit via predicate(or range) action"
		const val STATE_ON_CLEAR = "%s [%s] on clear"
		const val STATE_NO_ENTRY = "%s [%s] no entry action"
		const val STATE_NO_INPUT = "%s [%s] no input action"
		const val STATE_NO_EXIT = "%s [%s] no exit action"
		
		/* Machine */
		const val MACHINE_BUILD = "%s machine(name: %s) {"
		const val MACHINE_SET_ON_TRANSITION = "%s     set(on: transition)"
		const val MACHINE_SET_ON_ERROR = "%s     set(on: error)"
		const val MACHINE_UPDATE = "%s update(state: [%s]) {"
		const val MACHINE_ADD_STATE = "%s     add(state: [%s])"
		const val MACHINE_UPDATE_STATE = "%s     update(state: [%s])"
		const val MACHINE_DELETE_STATE = "%s     delete(state: [%s])"
		const val MACHINE_DELETE_STATE_ALL = "%s     delete(state: all)"
		const val MACHINE_ADD_RULE = "%s     add(rule: [%s] x (%s) -> [%s])"
		const val MACHINE_DELETE_RULE = "%s     delete(rule: [%s] x (%s))"
		const val MACHINE_DELETE_RULE_ALL = "%s     delete(rule: all)"
		const val MACHINE_INPUT = "%s input(signal: (%s), type: %s, payload: %s) {"
		const val MACHINE_TRANSITION = "%s [%s] x (%s) -> [%s]"
		const val MACHINE_RETURN_SYNC_INPUT = "%s     return(signal: (%s), type: %s, payload: %s)"
		const val MACHINE_START_AT = "%s     start at [%s]"
		const val MACHINE_RELEASE = "%s release {"
		const val MACHINE_DONE = "%s     done"
		const val MACHINE_END = "%s }"
		
		/* Daemon */
		const val DAEMON_START_THREAD = "Daemon[%s]: Daemon thread is started. (name = %s, isDaemon = %s)"
		const val DAEMON_TERMINATE_THREAD = "Daemon[%s]: Daemon thread is terminated. (name = %s, isDaemon = %s)"
		const val DAEMON_START_CREATE = "Daemon[%s]: daemon(name: %s) {"
		const val DAEMON_START_REQUEST = "Daemon[%s]: %s(remain: %s) {"
		const val DAEMON_TERMINATE = "Daemon[%s]: terminate(reason: %s) {"
		const val DAEMON_SET_ON_CREATE = "Daemon[%s]:%s     set(on: create)"
		const val DAEMON_SET_ON_START = "Daemon[%s]:%s     set(on: start)"
		const val DAEMON_SET_ON_PAUSE = "Daemon[%s]:%s     set(on: pause)"
		const val DAEMON_SET_ON_STOP = "Daemon[%s]:%s     set(on: stop)"
		const val DAEMON_SET_ON_RESUME = "Daemon[%s]:%s     set(on: resume)"
		const val DAEMON_SET_ON_FINISH = "Daemon[%s]:%s     set(on: finish)"
		const val DAEMON_SET_ON_DESTROY = "Daemon[%s]:%s     set(on: destroy)"
		const val DAEMON_SET_ON_FATAL = "Daemon[%s]:%s     set(on: fatal)"
		const val DAEMON_LIFECYCLE_CHANGED = "Daemon[%s]: lifecycle: %s -> %s"
		const val DAEMON_LIFECYCLE_CHANGED_TAB = "Daemon[%s]:     lifecycle: %s -> %s"
		const val DAEMON_ON_CREATE = "Daemon[%s]: onCreate()"
		const val DAEMON_ON_START = "Daemon[%s]:%s onStart(payload: %s)"
		const val DAEMON_ON_PAUSE = "Daemon[%s]:%s onPause(payload: %s)"
		const val DAEMON_ON_STOP = "Daemon[%s]:%s onStop(payload: %s)"
		const val DAEMON_ON_RESUME = "Daemon[%s]:%s onResume(payload: %s)"
		const val DAEMON_ON_FINISH = "Daemon[%s]:%s onFinish(payload: %s)"
		const val DAEMON_ON_DESTROY = "Daemon[%s]: onDestroy()"
		const val DAEMON_KEEP_REQUEST = "Daemon[%s]:     kept"
		const val DAEMON_IGNORE_REQUEST = "Daemon[%s]:     ignored"
		const val DAEMON_END = "Daemon[%s]: }"
		
		/*########################## WARN ##########################*/
		const val MACHINE_TRANSITION_FAILED = "%s Attempt transition to a non-existent state. [%s] x (%s) -> [%s]"
		const val MACHINE_USING_RELEASED_MACHINE = "%s Attempt to use a released machine. Please check the 'machine.isReleased' property."
		const val MACHINE_CANNOT_DELETE_CURRENT_STATE = "%s The current state cannot be deleted. (state: [%s])"
		const val DAEMON_UNINTENDED_TERMINATION = "Daemon[%s]: Unintended termination occurred. (reason: %s)"
		const val DAEMON_FATAL_ERROR = "Daemon[%s]: Fatal error occurred. (%s)"
		
		/*########################## ERROR ##########################*/
		const val EXPIRED_OBJECT = "%s Use of expired object: The object is only available inside the 'init' or 'update' block."
		const val UNDEFINED_START_STATE = "%s The start state [%s] is undefined."
		const val FAILED_TO_GET_STATE = "%s Attempted to get state [%s] that does not exist. (Make sure that state [%s] wasn't deleted at this time.)"
	}
}

internal const val tab: String = "    "

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
		if (this >= SIMPLE) debug?.invoke(log(Logs).reformat(*args))
	}
	
	fun Int.normal(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		if (this >= NORMAL) debug?.invoke(log(Logs).reformat(*args))
	}
	
	fun Int.detail(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		if (this >= DETAIL) debug?.invoke(log(Logs).reformat(*args))
	}
	
	fun w(vararg args: Any?, log: Logs.Companion.() -> String)
	{
		warn?.invoke(log(Logs).reformat(*args))
	}
	
	fun e(vararg args: Any?, log: Logs.Companion.() -> String): Nothing
	{
		log(Logs).reformat(*args).also { formatted ->
			error(formatted)
			throw IllegalStateException(formatted.replace(" +".toRegex(), " "))
		}
	}
	
	private fun String.reformat(vararg args: Any?): String
	{
		@Suppress("UNCHECKED_CAST")
		args as Array<Any?>
		
		var cursor = -1
		var token = ""
		
		this.forEach {
			when (token)
			{
				"" ->
				{
					if (it == '%' || it == '(')
					{
						token += it
					}
				}
				"%" ->
				{
					if (it == 's')
					{
						token = ""
						++cursor
					}
					else token = ""
				}
				"(" ->
				{
					if (it == '%')
					{
						token += it
					}
					else token = ""
				}
				"(%" ->
				{
					if (it == 's')
					{
						token += it
						++cursor
					}
					else token = ""
				}
				"(%s" ->
				{
					if (it == ')')
					{
						token = ""
						when (val arg = args[cursor])
						{
							is KClass<*> -> args[cursor] = "${arg.java.simpleName}::class"
							is String -> args[cursor] = "\"$arg\""
							is Char -> args[cursor] = "'$arg'"
						}
					}
					else token = ""
				}
			}
		}
		
		return this.format(*args)
	}
}
