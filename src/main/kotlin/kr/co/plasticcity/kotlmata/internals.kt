package kr.co.plasticcity.kotlmata

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
internal class Error(override val throwable: Throwable) : ErrorDSL
internal class ErrorFunction(override val throwable: Throwable) : ErrorFunctionDSL
internal class ErrorPayload(override val throwable: Throwable, override val payload: Any?) : ErrorPayloadDSL
internal class ErrorPayloadFunction(override val throwable: Throwable, override val payload: Any?) : ErrorPayloadFunctionDSL
internal class Transition : TransitionDSL
{
	override val transitionCount: Int = count++
	
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

internal object CONSTRUCTED
{
	override fun toString(): String = "CONSTRUCTED"
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
	if (this >= SIMPLE) Log.d(args = *args, log = log)
}

internal inline fun Int.normal(vararg args: Any?, log: Logs.Companion.() -> String)
{
	if (this >= NORMAL) Log.d(args = *args, log = log)
}

internal inline fun Int.detail(vararg args: Any?, log: Logs.Companion.() -> String)
{
	if (this >= DETAIL) Log.d(args = *args, log = log)
}
