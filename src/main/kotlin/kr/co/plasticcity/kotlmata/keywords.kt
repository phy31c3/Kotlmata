package kr.co.plasticcity.kotlmata

import kotlin.reflect.KClass

object all
object any
object of

@KotlmataMarker
interface KotlmataDSL
{
	class Sync internal constructor(val signal: SIGNAL, val type: KClass<SIGNAL>? = null)
	
	@Suppress("UNCHECKED_CAST")
	infix fun <T : SIGNAL> T.type(type: KClass<in T>) = Sync(this, type as KClass<SIGNAL>)
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

typealias KotlmataStateDef<S> = KotlmataState.Init.(state: S) -> Unit
typealias KotlmataMachineDef<M> = KotlmataMachine.Init.(machine: M) -> KotlmataMachine.Init.End
typealias KotlmataDaemonDef<D> = KotlmataDaemon.Init.(daemon: D) -> KotlmataMachine.Init.End