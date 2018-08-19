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
		}.apply {
			entry(Any()) {}
			entry("signal") {}
			entry("a") {}
			entry("b") { signal ->
				input(signal)
			}
			input("basic")
			input("basic", Any::class)
			exit()
		}
		
		state {
			delete action input signal "next"
		}
		
		state.entry("b") { signal ->
			input(signal)
		}
		
		initializer?.entry?.action { _ -> }
	}
	
	@Test
	fun machineTest()
	{
		val machine = KotlmataMutableMachine {
			templates {
				"base" {
					input signal "base" action { -> println("템플릿 이벤트") }
				}
			}
			
			"state1" {
				extends template "base"
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
				entry action { -> println("state2: 기본 진입함수") }
				input signal String::class action { s -> println("state2: String타입 진입함수: $s") }
				exit action { println("state2: 퇴장함수") }
			}
			
			"state1" x "base" %= "state2"
			"state1" x String::class %= "state3"
			"state2" x any %= "state3"
			
			initialize origin state to "state1"
		}
		
		machine {
		
		}
	}
}