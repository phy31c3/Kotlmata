package kr.co.plasticcity.kotlmata

object Kotlmata : KotlmataInterface by KotlmataImpl()

internal interface KotlmataInterface
{
	infix fun init(block: Config.() -> Unit)
	infix fun release(block: (() -> Unit))
}