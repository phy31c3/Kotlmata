@file:Suppress("FunctionName")

package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

@DslMarker
internal annotation class KotlmataMarker

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
	override fun toString(): String = this::class.simpleName ?: "Initial state for KotlmataDaemon"
}

@Suppress("ClassName")
internal object `Start KotlmataDaemon`
{
	override fun toString(): String = this::class.simpleName ?: "Start KotlmataDaemon"
}

internal const val tab: String = "    "

internal fun Any?.convertToSync() = when (this)
{
	null -> null
	is Unit -> null
	is Nothing -> null
	is FunctionDSL.Return -> this
	else /* this is SIGNAL */ -> FunctionDSL.Return(this)
}

internal object SignalsDefinableImpl : SignalsDefinable
{
	override fun <T1 : R, T2 : R, R : SIGNAL> T1.OR(signal: T2): Signals<R> = object : Signals<R>, MutableList<SIGNAL> by mutableListOf(this, signal)
	{ /* empty */ }
	
	override fun <T1 : R, T2 : R, R : SIGNAL> T1.OR(signal: KClass<T2>): Signals<R> = object : Signals<R>, MutableList<SIGNAL> by mutableListOf(this, signal)
	{ /* empty */ }
	
	override fun <T1 : R, T2 : R, R : SIGNAL> KClass<T1>.OR(signal: T2): Signals<R> = object : Signals<R>, MutableList<SIGNAL> by mutableListOf(this, signal)
	{ /* empty */ }
	
	override fun <T1 : R, T2 : R, R : SIGNAL> KClass<T1>.OR(signal: KClass<T2>): Signals<R> = object : Signals<R>, MutableList<SIGNAL> by mutableListOf(this, signal)
	{ /* empty */ }
	
	@Suppress("UNCHECKED_CAST")
	override fun <T1 : R, T2 : R, R : SIGNAL> Signals<T1>.OR(signal: T2): Signals<R>
	{
		add(signal)
		return this as Signals<R>
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun <T1 : R, T2 : R, R : SIGNAL> Signals<T1>.OR(signal: KClass<T2>): Signals<R>
	{
		add(signal)
		return this as Signals<R>
	}
}

internal open class Expirable internal constructor(private val block: () -> Nothing)
{
	@Volatile
	private var expire = false
	
	protected val expired: Expired = Expired()
	
	protected class Expired
	
	protected infix fun shouldNot(@Suppress("UNUSED_PARAMETER") expired: Expired)
	{
		if (expire)
		{
			block()
		}
	}
	
	protected infix fun not(@Suppress("UNUSED_PARAMETER") expired: Expired) = object : then
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

internal class Tester
{
	private val set = LinkedHashSet<(Any) -> Boolean>()
	
	@Suppress("UNCHECKED_CAST")
	operator fun <T> plusAssign(predicate: (T) -> Boolean)
	{
		set.add(predicate as (Any) -> Boolean)
	}
	
	fun remove(predicate: Any)
	{
		set.remove(predicate)
	}
	
	fun test(signal: Any): ((Any) -> Boolean)?
	{
		return set.firstOrNull { predicate ->
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
	
	inline fun test(signal: Any, onFind: ((Any) -> Boolean) -> Unit)
	{
		set.forEach { predicate ->
			try
			{
				if (predicate(signal))
				{
					onFind(predicate)
				}
			}
			catch (e: ClassCastException)
			{
				/* ignore */
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
