package kr.co.plasticcity.kotlmata

interface KotlmataMachine
{
	companion object
	{
		infix fun new(name: String): New = KotlmataMachineImpl(name)
		infix fun new(block: Initializer.() -> Initializer.End): KotlmataMachine = KotlmataMachineImpl(block)
	}
	
	interface New
	{
		infix fun mutable(block: Initializer.() -> Initializer.End): KotlmataMutableMachine
		infix fun immutable(block: Initializer.() -> Initializer.End): KotlmataMachine
	}
	
	interface Initializer
	{
		val start: Start
		
		interface Start
		{
			infix fun at(stateKey: Any): Initializer.End
		}
		
		class End internal constructor()
	}
}

interface KotlmataMutableMachine : KotlmataMachine
{
	infix fun set(block: Setter.() -> Unit): KotlmataMachine
	
	interface Setter
}

private class KotlmataMachineImpl(key: Any? = null, block: (KotlmataMachine.Initializer.() -> KotlmataMachine.Initializer.End)? = null)
	: KotlmataMutableMachine, KotlmataMachine.New
{
	private val key: Any = key ?: this
	
	init
	{
	
	}
	
	override fun mutable(block: KotlmataMachine.Initializer.() -> KotlmataMachine.Initializer.End): KotlmataMutableMachine
	{
		TODO("not implemented")
	}
	
	override fun immutable(block: KotlmataMachine.Initializer.() -> KotlmataMachine.Initializer.End): KotlmataMachine
	{
		TODO("not implemented")
	}
	
	override fun set(block: KotlmataMutableMachine.Setter.() -> Unit): KotlmataMachine
	{
		TODO("not implemented")
	}
}