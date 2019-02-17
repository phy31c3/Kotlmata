package kr.co.plasticcity.kotlmata

internal typealias STATE = Any
internal typealias MACHINE = Any
internal typealias DAEMON = MACHINE
internal typealias SIGNAL = Any

internal object PreStart
{
	override fun toString(): String = "pre-start"
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

@DslMarker
internal annotation class KotlmataMarker