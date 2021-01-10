package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

@DslMarker
internal annotation class KotlmataMarker

internal typealias `STATE or SIGNAL` = Any

internal class ErrorActionReceiver(override val throwable: Throwable) : ErrorActionDSL
internal class EntryActionReceiver(override val previousState: STATE) : EntryActionDSL
internal class EntryFunctionReceiver(override val previousState: STATE) : EntryFunctionDSL
internal class EntryErrorFunctionReceiver(override val previousState: STATE, override val throwable: Throwable) : EntryErrorFunctionDSL
internal class ExitActionReceiver(override val nextState: STATE) : ExitActionDSL
internal class ExitErrorActionReceiver(override val nextState: STATE, override val throwable: Throwable) : ExitErrorActionDSL
internal class PayloadActionReceiver(override val payload: Any?) : PayloadActionDSL
internal class PayloadFunctionReceiver(override val payload: Any?) : PayloadFunctionDSL
internal class PayloadErrorActionReceiver(override val payload: Any?, override val throwable: Throwable) : PayloadErrorActionDSL
internal class PayloadErrorFunctionReceiver(override val payload: Any?, override val throwable: Throwable) : PayloadErrorFunctionDSL
internal class TransitionActionReceiver(override val transitionCount: Long) : TransitionActionDSL
internal class TransitionErrorActionReceiver(override val transitionCount: Long, override val throwable: Throwable) : TransitionErrorActionDSL

@Suppress("ClassName")
internal object `Initial state for KotlmataDaemon`
{
	override fun toString(): String = this::class.simpleName ?: ""
}

@Suppress("ClassName")
internal object `Start KotlmataDaemon`
{
	override fun toString(): String = this::class.simpleName ?: ""
}

internal const val tab: String = "    "

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
	
	fun test(signal: Any): ((Any) -> Boolean)?
	{
		return set.reversed().firstOrNull { predicate ->
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
}

internal class Mutable2DMap<K1, K2, V>(private val map: MutableMap<K1, MutableMap<K2, V>> = HashMap()) : MutableMap<K1, MutableMap<K2, V>> by map
{
	operator fun get(key1: K1, key2: K2): V?
	{
		return map[key1]?.let { map2 ->
			map2[key2]
		}
	}
	
	operator fun set(key1: K1, key2: K2, value: V)
	{
		map[key1]?.also { map2 ->
			map2[key2] = value
		} ?: HashMap<K2, V>().also { map2 ->
			map[key1] = map2
			map2[key2] = value
		}
	}
}
