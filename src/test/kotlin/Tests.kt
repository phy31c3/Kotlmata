import kr.co.plasticcity.kotlmata.*
import org.junit.Test

class Tests
{
	@Test
	fun stateTest()
	{
		Kotlmata init {
			debugLogger = ::println
			errorLogger = ::error
		}
		
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
		val machine = KotlmataMutableMachine {
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
			
			"state1" x "base" %= "state2"
			"state1" x String::class %= "state3"
			"state2" x any %= "state3"
			any x "signal" %= "state1"
			any x any %= "state2"
			
			initialize origin state to "state1"
		}
		
		machine {
			has state "state1" then {
				println("state1 있음")
			} or {
				println("state1 없음")
			}
			
			has transition ("state1" x "base") then {
				println("state1 x base 있음")
			} or {
				println("state1 x base 없음")
			}
			
			insert state "state4" of {
				entry action { -> println("state4: 기본 진입함수") }
				input signal String::class action { s -> println("state4: String타입 진입함수: $s") }
				exit action { println("state4: 퇴장함수") }
			}
			
			insert or replace state "state4" of {
				entry action { -> println("state4: 기본 진입함수") }
				input signal String::class action { s -> println("state4: String타입 진입함수: $s") }
				exit action { println("state4: 퇴장함수") }
			}
			
			replace state "state5" of {
				entry action { -> println("state5: 기본 진입함수") }
				input signal String::class action { s -> println("state5: String타입 진입함수: $s") }
				exit action { println("state5: 퇴장함수") }
			}
			
			update state "state4" set {
				entry action { -> println("state4: 수정된 기본 진입함수") }
				delete action exit
			} or {
				entry action { -> println("state5: 기본 진입함수") }
				input signal String::class action { s -> println("state5: String타입 진입함수: $s") }
				exit action { println("state5: 퇴장함수") }
			}
			
			insert transition ("state1" x "base") %= "state3"
			insert or update transition ("state1" x "base") %= "state3"
			update transition ("state1" x "base") %= "state4"
			
			delete state "state1"
			delete state all
			
			delete transition ("state1" x String::class)
			delete transition (any x String::class)
			delete transition ("state2" x any)
			delete transition (any x any)
			delete transition of state "state1"
			delete transition all
		}
		
		machine.input("base")
		machine.input("base", Any::class)
	}
}