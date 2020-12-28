package kr.co.plasticcity.kotlmata

import java.util.*
import kotlin.collections.ArrayList

internal typealias STATE = Any
internal typealias SIGNAL = Any
internal typealias STATE_OR_SIGNAL = Any
internal typealias MACHINE = Any
internal typealias DAEMON = MACHINE

@DslMarker
internal annotation class KotlmataMarker

internal object Action : ActionDSL
internal object Function : FunctionDSL
internal class Payload(override val payload: Any?) : PayloadDSL
internal class PayloadFunction(override val payload: Any?) : PayloadFunctionDSL
internal class ErrorAction(override val throwable: Throwable) : ErrorActionDSL
internal class ErrorFunction(override val throwable: Throwable) : ErrorFunctionDSL
internal class ErrorPayload(override val throwable: Throwable, override val payload: Any?) : ErrorPayloadDSL
internal class ErrorPayloadFunction(override val throwable: Throwable, override val payload: Any?) : ErrorPayloadFunctionDSL
internal class Transition : TransitionDSL
{
	override val count: Int = Transition.count++
	
	companion object
	{
		var count = 0
	}
}

internal fun Any?.convertToSync() = when (this)
{
	null -> null
	is Unit -> null
	is Nothing -> null
	is FunctionDSL.Sync -> this
	else /* this is SIGNAL */ -> FunctionDSL.Sync(this)
}

internal object stay

internal object CREATED
{
	override fun toString(): String = "CREATED"
}

internal open class Expirable internal constructor(private val block: () -> Nothing)
{
	@Volatile
	private var expire = false
	
	protected val expired: Expired = Expired()
	
	protected class Expired
	
	protected infix fun shouldNot(@Suppress("UNUSED_PARAMETER") keyword: Expired)
	{
		if (expire)
		{
			block()
		}
	}
	
	protected infix fun not(@Suppress("UNUSED_PARAMETER") keyword: Expired) = object : then
	{
		override fun then(block: () -> Unit)
		{
			if (!expire)
			{
				block()
			}
		}
	}
	
	protected interface then
	{
		infix fun then(block: () -> Unit)
	}
	
	protected fun expire()
	{
		expire = true
	}
}

internal const val tab: String = "   "

internal const val UNDEFINED = -1
internal const val NO_LOG = 0
internal const val SIMPLE = 1
internal const val NORMAL = 2
internal const val DETAIL = 3

internal inline fun Int.simple(vararg args: Any?, log: Logs.Companion.() -> String)
{
	if (this >= SIMPLE) Log.d(*args, log = log)
}

internal inline fun Int.normal(vararg args: Any?, log: Logs.Companion.() -> String)
{
	if (this >= NORMAL) Log.d(*args, log = log)
}

internal inline fun Int.detail(vararg args: Any?, log: Logs.Companion.() -> String)
{
	if (this >= DETAIL) Log.d(*args, log = log)
}

internal class Predicates
{
	private val predicates = ArrayList<Pair<UUID, (Any) -> Boolean>>()
	
	@Suppress("UNCHECKED_CAST")
	fun <T> store(predicate: (T) -> Boolean) = UUID.randomUUID().also { uuid ->
		predicates.add(uuid to predicate as (Any) -> Boolean)
	}
	
	fun test(signal: Any) = predicates.find { (_, predicate) ->
		try
		{
			predicate(signal)
		}
		catch (e: ClassCastException)
		{
			false
		}
	}?.first
}
