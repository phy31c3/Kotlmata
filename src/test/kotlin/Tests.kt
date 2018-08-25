import kr.co.plasticcity.kotlmata.*
import org.junit.Before
import org.junit.Test

class Tests
{
	@Before
	fun init()
	{
		Kotlmata init {
			debugLogger = ::println
			errorLogger = ::error
		}
	}
	
	@Test
	fun stateTest()
	{
		var initializer: KotlmataState.Initializer? = null
		val state = KotlmataMutableState {
			initializer = this
			
			entry action { -> println("기본 진입함수") }
			entry via String::class action { s -> println("String타입 진입함수: $s") }
			entry via "a" action { -> println("a 진입함수") }
			entry via "b" action { ->
				println("b 진입함수")
				"next"
			}
			input action { -> println("기본 입력함수") }
			input signal Any::class action { s -> println("Any타입 입력함수: $s") }
			input signal "next" action { -> println("진입함수에서 흘러들어옴") }
			exit action { println("퇴장함수") }
		}
		
		state.entry(Any()) {}
		state.entry("signal") {}
		state.entry("a") {}
		state.entry("b") { signal ->
			state.input(signal)
		}
		state.input("basic")
		state.input("basic", Any::class)
		state.exit()
		
		state {
			delete action input signal "next"
		}
		
		state.entry("b") { signal ->
			state.input(signal)
		}
		
		initializer?.entry?.action { -> }
	}
	
	@Test
	fun machineTest()
	{
		val machine = KotlmataMutableMachine("m1") {
			"state1" {
				entry action { -> println("state1: 기본 진입함수") }
				input signal String::class action { s -> println("state1: String타입 진입함수: $s") }
				exit action { println("state1: 퇴장함수") }
			}
			
			"state2" {
				entry action { -> println("state2: 기본 진입함수") }
				input signal String::class action { s -> println("state2: String타입 진입함수: $s") }
				exit action { println("state2: 퇴장함수") }
			}
			
			"state3" {
				entry action { -> println("state3: 기본 진입함수") }
				input signal String::class action { s -> println("state3: String타입 진입함수: $s") }
				exit action { println("state3: 퇴장함수") }
			}
			
			"state1" x "to state2" %= "state2"
			
			initialize origin state to "state1"
		}
		
		machine.input("to state2")
	}
}