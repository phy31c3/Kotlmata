import kr.co.plasticcity.kotlmata.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class Tests
{
	@Before
	fun init()
	{
		Kotlmata init {
			print debug ::println
			print warn ::println
			print error ::println
		}
	}
	
	@After
	fun release()
	{
		Kotlmata release {
			/* do nothing */
		}
	}
	
	@Test
	fun stateTest()
	{
		var initializer: KotlmataState.Initializer? = null
		val state = KotlmataMutableState(name = "s1") {
			initializer = this
			
			entry action { println("기본 진입함수") }
			entry via String::class action { println("String 타입 진입함수: $it") }
			entry via "a" action { println("a 진입함수") }
			entry via "b" action {
				println("b 진입함수")
				"next"
			}
			input action { println("기본 입력함수") }
			input signal Any::class action { println("Any 타입 입력함수: $it") }
			input signal "next" action { println("진입함수에서 흘러들어옴") }
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
		state.exit("basic")
		
		state {
			delete action input signal "next"
		}
		
		state.entry("b") { signal ->
			state.input(signal)
		}
		
		initializer?.entry?.action {}
	}
	
	@Test
	fun machineTest()
	{
		val machine = KotlmataMutableMachine(name = "m1") {
			"state1" {
				entry action { println("state1: 기본 진입함수") }
				input signal String::class action { println("state1: String 타입 입력함수: $it") }
				input signal "goToState2" action { println("state2로 이동") }
				exit action { println("state1: 퇴장함수") }
			}
			
			"state2" {
				entry action { println("state2: 기본 진입함수") }
				input signal Number::class action { println("state2: Number 타입 입력함수: $it") }
				input signal 5 action { println("state3로 이동") }
				exit action { println("state2: 퇴장함수") }
			}
			
			"state3" {
				entry action { println("state3: 기본 진입함수") }
				input signal String::class action { println("state3: String 타입 입력함수: $it") }
				exit action { println("state3: 퇴장함수") }
			}
			
			"state1" x "goToState2" %= "state2"
			"state2" x 5 %= "state3"
			"state3" x "goToState1" %= "state1"
			
			start at "state1"
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
				input signal String::class action { println("state1: 수정된 String 타입 입력함수: $it") }
				delete action exit
			}
			
			insert state "state2" of {
				entry action { println("삽입된 state2") }
			}
			
			insert transition ("state1" x "goToState2") %= "state3"
			insert transition ("state1" x "goToState3") %= "state3"
		}
		
		machine.input("some string")
		machine.input("goToState2")
		
		var modifier: KotlmataMutableMachine.Modifier? = null
		machine {
			println("현재 상태: $current")
			modifier = this
		}
		
		println("현재 상태를 외부에서 확인: ${modifier?.current}")
	}
	
	@Test
	fun daemonTest()
	{
		var initializer: KotlmataDaemon.Initializer? = null
		val daemon = KotlmataMutableDaemon {
			log level 3
			
			"state1" {
				entry action { println("state1: 기본 진입함수") }
				input signal String::class action { println("state1: String 타입 입력함수: $it") }
				input signal "goToState2" action { println("state2로 이동") }
				exit action { println("state1: 퇴장함수") }
			}
			
			"state2" {
				entry action { println("state2: 기본 진입함수") }
				input signal Integer::class action { println("state2: Number 타입 입력함수: $it") }
				input signal 5 action { println("state3로 이동") }
				exit action { println("state2: 퇴장함수") }
			}
			
			"state3" {
				entry action {
					println("state3: 기본 진입함수")
					"quick input"
				}
				input signal String::class action { println("state3: String 타입 입력함수: $it") }
				exit action { println("state3: 퇴장함수") }
			}
			
			"state1" x "goToState2" %= "state2"
			"state2" x 5 %= "state3"
			"state3" x "goToState1" %= "state1"
			
			initializer = this
			
			start at "state1"
		}
		
		daemon.input("any1")
		daemon.run()
		daemon.input("1")
		daemon.input("2")
		daemon.pause()
		daemon.input("goToState2")
		daemon.run()
		
		Thread.sleep(100)
		
		daemon.input(3)
		daemon.input(5)
		daemon.stop()
		daemon.input(100)
		daemon.input(100)
		daemon.run()
		daemon.input(200)
		daemon.stop()
		daemon.input(4)
		daemon.input(5)
		
		Thread.sleep(100)
		
		daemon.pause()
		daemon.input(3)
		daemon.input(5)
		daemon.input("goToState1", String::class)
		daemon.input("goToState1")
		
		Thread.sleep(100)
		
		daemon.run()
		daemon {
			"state1" x "goToState3" %= "state3"
		}
		daemon.input("goToState3")
		
		Thread.sleep(500)
		
		daemon.terminate()
		
		initializer?.log?.level(0)
	}
}