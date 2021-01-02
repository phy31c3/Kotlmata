package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

@DslMarker
internal annotation class KotlmataMarker

internal typealias `STATE or SIGNAL` = Any

internal object ActionReceiver : ActionDSL
internal object FunctionReceiver : FunctionDSL
internal class ErrorActionReceiver(override val throwable: Throwable) : ErrorActionDSL
internal class ErrorFunctionReceiver(override val throwable: Throwable) : ErrorFunctionDSL
internal class PayloadActionReceiver(override val payload: Any?) : PayloadActionDSL
internal class PayloadFunctionReceiver(override val payload: Any?) : PayloadFunctionDSL
internal class PayloadErrorActionReceiver(override val throwable: Throwable, override val payload: Any?) : PayloadErrorActionDSL
internal class PayloadErrorFunctionReceiver(override val throwable: Throwable, override val payload: Any?) : PayloadErrorFunctionDSL
internal class TransitionActionReceiver(override val count: Long) : TransitionActionDSL
internal class TransitionErrorActionReceiver(override val throwable: Throwable, override val count: Long) : TransitionErrorActionDSL

internal object CREATED
{
	override fun toString(): String = "CREATED"
}

internal const val tab: String = "   "

internal fun Any?.convertToSync() = when (this)
{
	null -> null
	is Unit -> null
	is Nothing -> null
	is FunctionDSL.Sync -> this
	else /* this is SIGNAL */ -> FunctionDSL.Sync(this)
}

internal fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> Expirable.or(lhs: T1, rhs: T2): StatesOrSignals<R>
{
	shouldNotExpired()
	return object : StatesOrSignals<R>, MutableList<SIGNAL> by mutableListOf(lhs, rhs)
	{ /* empty */ }
}

internal fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> Expirable.or(lhs: T1, rhs: KClass<T2>): StatesOrSignals<R>
{
	shouldNotExpired()
	return object : StatesOrSignals<R>, MutableList<SIGNAL> by mutableListOf(lhs, rhs)
	{ /* empty */ }
}

internal fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> Expirable.or(lhs: KClass<T1>, rhs: T2): StatesOrSignals<R>
{
	shouldNotExpired()
	return object : StatesOrSignals<R>, MutableList<SIGNAL> by mutableListOf(lhs, rhs)
	{ /* empty */ }
}

internal fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> Expirable.or(lhs: KClass<T1>, rhs: KClass<T2>): StatesOrSignals<R>
{
	shouldNotExpired()
	return object : StatesOrSignals<R>, MutableList<SIGNAL> by mutableListOf(lhs, rhs)
	{ /* empty */ }
}

@Suppress("UNCHECKED_CAST")
internal fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> Expirable.or(lhs: StatesOrSignals<T1>, rhs: T2): StatesOrSignals<R>
{
	shouldNotExpired()
	lhs.add(rhs)
	return lhs as StatesOrSignals<R>
}

@Suppress("UNCHECKED_CAST")
internal fun <T1 : R, T2 : R, R : `STATE or SIGNAL`> Expirable.or(lhs: StatesOrSignals<T1>, rhs: KClass<T2>): StatesOrSignals<R>
{
	shouldNotExpired()
	lhs.add(rhs)
	return lhs as StatesOrSignals<R>
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
	
	internal fun shouldNotExpired()
	{
		if (expire)
		{
			block()
		}
	}
}

internal class Predicates
{
	private val set = LinkedHashSet<(Any) -> Boolean>()
	
	@Suppress("UNCHECKED_CAST")
	fun <T> store(predicate: (T) -> Boolean)
	{
		set.add(predicate as (Any) -> Boolean)
	}
	
	fun remove(predicate: Any)
	{
		set.remove(predicate)
	}
	
	fun test(signal: Any): ((Any) -> Boolean)? = set.lastOrNull { predicate ->
		try
		{
			predicate(signal)
		}
		catch (e: ClassCastException)
		{
			false
		}
	}
}
