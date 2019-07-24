package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

object all
object any
object of

@KotlmataMarker
interface KotlmataDSL
{
	data class SyncInput internal constructor(val signal: SIGNAL, val type: KClass<SIGNAL>? = null)
	
	@Suppress("UNCHECKED_CAST")
	infix fun <T : SIGNAL> T.asType(type: KClass<in T>) = SyncInput(this, type as KClass<SIGNAL>)
	
	sealed class InputActionReturn
	{
		internal object Consume : InputActionReturn()
		internal object Forward : InputActionReturn()
	}
	
	/**
	 * If input action returns this keyword, the signal is consumed and does not cause a state transition.
	 */
	val consume: InputActionReturn
	/**
	 * If input action returns this keyword or anything other than 'consume' (even null or Unit), the signal can cause a state transition.
	 */
	val forward: InputActionReturn
}

typealias KotlmataAction = KotlmataAction1R<SIGNAL, Unit>
typealias KotlmataActionR<R> = KotlmataAction1R<SIGNAL, R>
typealias KotlmataAction1<T> = KotlmataAction1R<T, Unit>
typealias KotlmataAction1R<T, R> = KotlmataDSL.(signal: T) -> R

typealias KotlmataFallback = KotlmataFallbackR<Unit>
typealias KotlmataFallbackR<R> = KotlmataDSL.(throwable: Throwable) -> R
typealias KotlmataFallback1<T> = KotlmataFallback1R<T, Unit>
typealias KotlmataFallback1R<T, R> = KotlmataDSL.(throwable: Throwable, signal: T) -> R

typealias KotlmataCallback = KotlmataDSL.(payload: Any?) -> Unit