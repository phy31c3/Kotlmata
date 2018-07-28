import kr.co.plasticcity.kotlmata.Kotlmata
import kr.co.plasticcity.kotlmata.KotlmataMachine
import kr.co.plasticcity.kotlmata.KotlmataState
import org.junit.Test

class Tests
{
	@Test
	fun basicTest()
	{
		Kotlmata init {
			debugLogger = ::println
			errorLogger = ::error
		}
		
		val action = {}
		
		KotlmataState new if (true)
		{
			"state0"
		}
		else
		{
			"state1"
		} set {
			entry action action
			entry via "signal" action action
			event input "signal" action action
			exit action action
		}
		
		val state = KotlmataState new "state"
		
		val mutableMachine = (KotlmataMachine new "machine").run {
			if (action != {})
			{
				immutable {
					start at "state0"
				}
			}
			else
			{
				mutable {
					start at "state0"
				}
			}
		}
		
		val immutableMachine = KotlmataMachine new {
			start at "state0"
		}
		
		Kotlmata release {
			/* do nothing */
		}
	}
	
	@Test
	fun kotlinPractice()
	{
		let {
			val i = 0
			i
		}
		run {
			val i = 0
			i
		}
		with(this) {
			val i = 0
			i
		}
		apply {
			val i = 0
			i
		}
		
		val block: () -> Any? = { println("block") }
		val call: (Any) -> Any? = { _ -> block() }
		call("param")
		
		val nil: Any? = "nil"
		nil?.apply {
			println("nil is $nil")
		} ?: println("nil is null")
		
		("" name "") {
		
		}
	}
}

infix fun String.name(name: String): (block: () -> Unit) -> Block
{
	return ::Block
}

class Block(block: () -> Unit)