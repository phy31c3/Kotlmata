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
			entry("b") {
				input(it)
			}
			input("basic")
			input("basic", Any::class)
			exit()
		}
		
		state {
			delete action entry via ""
			delete action all
		}
		
		state.input(Any())
		
		initializer?.entry?.action { _ -> }
	}
	
	@Test
	fun machineTest()
	{
		KotlmataMachine {
			templates {
				"base" {
				
				}
			}
			
			"state1" {
				extends template "base"
			}
			
			"state2" {
			
			}
			
			"state1" x "signal" %= "state2"
			"state1" x String::class %= "state2"
			"state1" x any %= "state2"
			
			initialize origin state to "state"
		}
	}
}