package kr.co.plasticcity.kotlmata

interface KotlmataConfig
{
	companion object
	{
		operator fun invoke(block: KotlmataConfig.() -> Unit) = object : KotlmataConfig
		{
			override val print: Print = object : Print
			{
				override fun debug(block: (log: String) -> Unit)
				{
					Log.debug = block
				}
				
				override fun warn(block: (log: String) -> Unit)
				{
					Log.warn = block
				}
				
				override fun error(block: (log: String) -> Unit)
				{
					Log.error = block
				}
			}
		}.block()
	}
	
	val print: Print
	
	interface Print
	{
		infix fun debug(block: (log: String) -> Unit)
		infix fun warn(block: (log: String) -> Unit)
		infix fun error(block: (log: String) -> Unit)
	}
}
