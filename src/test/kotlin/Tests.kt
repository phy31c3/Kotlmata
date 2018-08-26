import kr.co.plasticcity.kotlmata.*
import org.junit.Before
import org.junit.Test

class Tests
{
	@Before
	fun init()
	{
		Kotlmata init {
			debugLogger = { log -> println("Kotlmata: $log") }
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
				input signal "goToState2" action { -> println("state2로 이동") }
				exit action { println("state1: 퇴장함수") }
			}
			
			"state2" {
				entry action { -> println("state2: 기본 진입함수") }
				input signal Number::class action { s -> println("state2: Number타입 진입함수: $s") }
				input signal 5 action { -> println("state3로 이동") }
				exit action { println("state2: 퇴장함수") }
			}
			
			"state3" {
				entry action { -> println("state3: 기본 진입함수") }
				input signal String::class action { s -> println("state3: String타입 진입함수: $s") }
				exit action { println("state3: 퇴장함수") }
			}
			
			"state1" x "goToState2" %= "state2"
			"state2" x 5 %= "state3"
			"state3" x "goToState1" %= "state1"
			
			initialize origin state to "state1"
		}
		
		machine.input("some string")
		machine.input("goToState2")
		machine.input(5, Number::class)
		machine.input(5)
		machine.input("goToState1")
		
		println("-----------------------------------")
		
		machine {
			has state "state1" then {
				println("state1 있음")
			} or {
				println("state1 없음")
			}
			
			update state "state1" set {
				input signal String::class action { s -> println("state1: 수정된 String타입 진입함수: $s") }
				delete action exit
			}
			
			insert state "state2" of {
				entry action { -> println("삽입된 state2") }
			}
			
			insert transition ("state1" x "goToState2") %= "state3"
			insert transition ("state1" x "goToState3") %= "state3"
		}
		
		machine.input("some string")
		machine.input("goToState2")
	}
}