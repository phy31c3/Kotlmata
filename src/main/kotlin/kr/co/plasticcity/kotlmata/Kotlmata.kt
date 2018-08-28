package kr.co.plasticcity.kotlmata

interface Kotlmata
{
	companion object : Kotlmata by KotlmataImpl()
	
	infix fun init(block: Initializer.() -> Unit)
	infix fun release(block: (() -> Unit))
	
	interface Initializer
	{
		val print: Print
	}
	
	interface Print
	{
		infix fun debug(block: (String) -> Unit)
		infix fun error(block: (String) -> Unit)
	}
}

private class KotlmataImpl : Kotlmata
{
	override fun init(block: Kotlmata.Initializer.() -> Unit)
	{
		InitializerImpl(block)
	}
	
	override fun release(block: () -> Unit)
	{
		TODO("not implemented")
	}
	
	private inner class InitializerImpl internal constructor(
			block: Kotlmata.Initializer.() -> Unit
	) : Kotlmata.Initializer, Expirable({ Log.e { EXPIRED_INITIALIZER } })
	{
		override val print = object : Kotlmata.Print
		{
			override fun debug(block: (String) -> Unit)
			{
				this@InitializerImpl shouldNot expired
				Log.debug = block
			}
			
			override fun error(block: (String) -> Unit)
			{
				this@InitializerImpl shouldNot expired
				Log.error = block
			}
		}
		
		init
		{
			block()
			expire()
		}
	}
}