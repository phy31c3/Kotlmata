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
	infix fun <T : SIGNAL> T.type(type: KClass<in T>) = SyncInput(this, type as KClass<SIGNAL>)
	
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

typealias KotlmataAction = KotlmataDSL.(signal: SIGNAL) -> Unit
typealias KotlmataActionR<R> = KotlmataDSL.(signal: SIGNAL) -> R
typealias KotlmataAction1<T> = KotlmataDSL.(signal: T) -> Unit
typealias KotlmataAction1R<T, R> = KotlmataDSL.(signal: T) -> R
typealias KotlmataAction2R<T, R> = KotlmataDSL.(signal: T, payload: Any?) -> R

typealias KotlmataError = KotlmataDSL.(throwable: Throwable) -> Unit
typealias KotlmataErrorR<R> = KotlmataDSL.(throwable: Throwable) -> R
typealias KotlmataError1<T> = KotlmataDSL.(throwable: Throwable, signal: T) -> Unit
typealias KotlmataError1R<T, R> = KotlmataDSL.(throwable: Throwable, signal: T) -> R

typealias KotlmataCallback = KotlmataDSL.(payload: Any?) -> Unit
typealias KotlmataFallback = KotlmataDSL.(throwable: Throwable) -> Unit
typealias KotlmataFallback1 = KotlmataDSL.(throwable: Throwable, payload: Any?) -> Unit

typealias KotlmataStateInit<S> = KotlmataState.Init.(state: S) -> Unit