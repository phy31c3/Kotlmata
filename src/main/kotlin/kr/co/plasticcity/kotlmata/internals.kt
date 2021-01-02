package kr.co.plasticcity.kotlmata

import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

@DslMarker
internal annotation class KotlmataMarker

internal typealias STATE = Any
internal typealias SIGNAL = Any
internal typealias STATE_OR_SIGNAL = Any
internal typealias MACHINE = Any
internal typealias DAEMON = MACHINE

internal object Action : ActionDSL
internal object Function : FunctionDSL
internal class Payload(override val payload: Any?) : PayloadDSL
internal class PayloadFunction(override val payload: Any?) : PayloadFunctionDSL
internal class ErrorAction(override val throwable: Throwable) : ErrorActionDSL
internal class ErrorFunction(override val throwable: Throwable) : ErrorFunctionDSL
internal class ErrorPayload(override val throwable: Throwable, override val payload: Any?) : ErrorPayloadDSL
internal class ErrorPayloadFunction(override val throwable: Throwable, override val payload: Any?) : ErrorPayloadFunctionDSL
internal class ErrorTransition(override val throwable: Throwable) : ErrorTransitionDSL, TransitionDSL by Transition()
internal class Transition : TransitionDSL
{
	override val count: Int = Transition.count.getAndIncrement()
	
	companion object
	{
		val count = AtomicInteger(0)
	}
}

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

internal fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> Expirable.or(lhs: T1, rhs: T2): StatesOrSignals<R>
{
	shouldNotExpired()
	return object : StatesOrSignals<R>, MutableList<SIGNAL> by mutableListOf(lhs, rhs)
	{ /* empty */ }
}

internal fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> Expirable.or(lhs: T1, rhs: KClass<T2>): StatesOrSignals<R>
{
	shouldNotExpired()
	return object : StatesOrSignals<R>, MutableList<SIGNAL> by mutableListOf(lhs, rhs)
	{ /* empty */ }
}

internal fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> Expirable.or(lhs: KClass<T1>, rhs: T2): StatesOrSignals<R>
{
	shouldNotExpired()
	return object : StatesOrSignals<R>, MutableList<SIGNAL> by mutableListOf(lhs, rhs)
	{ /* empty */ }
}

internal fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> Expirable.or(lhs: KClass<T1>, rhs: KClass<T2>): StatesOrSignals<R>
{
	shouldNotExpired()
	return object : StatesOrSignals<R>, MutableList<SIGNAL> by mutableListOf(lhs, rhs)
	{ /* empty */ }
}

@Suppress("UNCHECKED_CAST")
internal fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> Expirable.or(lhs: StatesOrSignals<T1>, rhs: T2): StatesOrSignals<R>
{
	shouldNotExpired()
	lhs.add(rhs)
	return lhs as StatesOrSignals<R>
}

@Suppress("UNCHECKED_CAST")
internal fun <T1 : R, T2 : R, R : STATE_OR_SIGNAL> Expirable.or(lhs: StatesOrSignals<T1>, rhs: KClass<T2>): StatesOrSignals<R>
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
