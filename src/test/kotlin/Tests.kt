import kr.co.plasticcity.kotlmata.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.ref.WeakReference
import java.util.concurrent.PriorityBlockingQueue

class Tests
{
	@Before
	fun init()
	{
		Kotlmata.config {
			log level 3
			print debug ::println
			print warn ::println
			print error ::error
		}
		
		Kotlmata.start()
	}
	
	@After
	fun release()
	{
		Kotlmata.release()
	}
	
	@Test
	fun stateTest()
	{
		var expired: KotlmataState.Initializer? = null
		val state = KotlmataMutableState("s1") { _ ->
			expired = this
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
		
		expired?.entry?.action {}
	}
	
	@Test
	fun machineTest()
	{
		val machine = KotlmataMutableMachine("m1") { _ ->
			"state1" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal String::class action { println("$state: String 타입 입력함수: $it") }
				input signal "goToState2" action { println("state2로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state2" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal Number::class action { println("$state: Number 타입 입력함수: $it") }
				input signal 5 action { println("state3로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state3" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal String::class action { println("$state: String 타입 입력함수: $it") }
				exit action { println("$state: 퇴장함수") }
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
		
		machine { _ ->
			has state "state1" then {
				println("state1 있음")
			} or {
				println("state1 없음")
			}
			
			update state "state1" set { state ->
				input signal String::class action { println("$state: 수정된 String 타입 입력함수: $it") }
				delete action exit
			}
			
			insert state "state2" of { state ->
				entry action { println("삽입된 $state") }
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
		var shouldGC: WeakReference<KotlmataState.Initializer>? = null
		var expire: KotlmataMutableState.Modifier? = null
		val daemon = KotlmataMutableDaemon("d1") { _ ->
			log level 2
			
			"state1" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal String::class action { println("$state: String 타입 입력함수: $it") }
				input signal "goToState2" action { println("state2로 이동") }
				shouldGC = WeakReference(this)
			}
			
			"state2" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal Integer::class action { println("$state: Number 타입 입력함수: $it") }
				input signal 5 action { println("state3로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state3" { state ->
				entry action {
					println("$state: 기본 진입함수")
					"express input"
				}
				input signal String::class action { println("$state: String 타입 입력함수: $it") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state1" x "goToState2" %= "state2"
			"state2" x 5 %= "state3"
			"state3" x "goToState1" %= "state1"
			
			start at "state1"
		}
		
		daemon.input("any1")
		daemon.run()
		daemon.input("우선순위 1", 1)
		daemon.input("우선순위 -10", -10)
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
		daemon { _ ->
			"state1" x "goToState3" %= "state3"
			
			update state "state3" set { state ->
				expire = this
				entry action {
					println("$state: 수정된 기본 진입함수")
					"express input"
				}
			}
		}
		daemon.input("goToState3")
		daemon.input("이거 출력되어야 하는데..")
		
		Thread.sleep(500)
		
		daemon.terminate()
		
		System.gc()
		println("과연 GC 되었을까: ${shouldGC?.get()}")
		expire?.entry?.action {}
	}
	
	@Test
	fun kotlmataTest()
	{
		var expire: Kotlmata.Post? = null
		Kotlmata fork "daemon" of { _ ->
			log level 2
			
			"state1" { state ->
				entry action { println("데몬이 시작됨") }
				input signal String::class action { println("$state: String 타입 입력함수: $it") }
				input signal "goToState2" action { println("state2로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state2" { state ->
				entry action { println("$state: 기본 진입함수") }
				entry via "goToState2" action {
					println("null 리턴할거임")
					null
				}
				input signal Integer::class action { println("$state: Number 타입 입력함수: $it") }
				input signal String::class action { println("$state: String 타입 입력함수: $it") }
				input signal 5 action { println("state3로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state3" { state ->
				entry action {
					println("$state: 기본 진입함수")
					"express input"
				}
				input signal String::class action { println("$state: String 타입 입력함수: $it") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state1" x "goToState2" %= "state2"
			"state2" x 5 %= "state3"
			"state3" x "goToState1" %= "state1"
			"state3" x "goToState4" %= "state4"
			
			start at "state1"
		}
		
		Kotlmata input "무시해라1" to "daemon"
		Kotlmata input "무시해라2" to "daemon"
		Kotlmata modify "daemon" set {
			current
		}
		
		Thread.sleep(100)
		
		Kotlmata run "daemon"
		Kotlmata input "goToState2" to "daemon"
		Kotlmata input "한타임 쉬고" to "daemon"
		Kotlmata input "우선순위 5" priority 5 to "daemon"
		Kotlmata input "우선순위 4" priority 4 to "daemon"
		Kotlmata input "우선순위 3" priority 3 to "daemon"
		Kotlmata input "우선순위 2" priority 2 to "daemon"
		Kotlmata input "우선순위 1" priority 1 to "daemon"
		
		Thread.sleep(100)
		
		Kotlmata {
			expire = this
			has daemon "daemon" then {
				modify daemon "daemon" set { _ ->
					update state "state2" set { state ->
						input signal Integer::class action { println("$state: Post 에서 수정된 Number 타입 입력함수: $it") }
						exit action { println("$state: Post 에서 수정된 퇴장함수") }
					}
				}
			}
			
			input signal 3 to "daemon"
		}
		Kotlmata input 5 to "daemon"
		Kotlmata input "goToState4" to "daemon"
		
		Thread.sleep(100)
		
		Kotlmata.input("shutdown 보다 더 빨리 실행될까?")
		Kotlmata.shutdown()
		
		Thread.sleep(100)
		
		expire?.has?.daemon("")?.then {}
	}
	
	@Test
	fun commonTest()
	{
		val queue: PriorityBlockingQueue<Int> = PriorityBlockingQueue()
		
		Thread.currentThread().interrupt()
		
		queue.offer(0)
		queue.offer(1)
		queue.offer(2)
		
		println("이제 take 한다.")
		println("${queue.take()}")
		println("${queue.take()}")
		println("${queue.take()}")
	}
}