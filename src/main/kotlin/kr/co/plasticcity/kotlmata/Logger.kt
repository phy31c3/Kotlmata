package kr.co.plasticcity.kotlmata

internal object Logger
{
	var debugLogger: ((String) -> Unit) = {}
	var errorLogger: ((String) -> Unit) = {}
}