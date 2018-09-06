package kr.co.plasticcity.kotlmata

internal typealias KEY = Any
internal typealias STATE = Any
internal typealias SIGNAL = Any

internal object DaemonInitial
{
	override fun toString(): String = "initial"
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

internal inline fun Int.simple(vararg args: Any, log: Logs.Companion.() -> String)
{
	if (this >= 1) Log.d(args = *args, log = log)
}

internal inline fun Int.detail(vararg args: Any, log: Logs.Companion.() -> String)
{
	if (this >= 2) Log.d(args = *args, log = log)
}

internal fun Int.isDetail() = this >= 2