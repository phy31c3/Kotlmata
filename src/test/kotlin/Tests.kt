import kr.co.plasticcity.kotlmata.*
import org.junit.Before
import org.junit.Test
import java.lang.ref.WeakReference
import java.util.concurrent.PriorityBlockingQueue

class Tests
{
	@Before
	fun init()
	{
		KotlmataConfig {
			print debug ::println
			print warn ::println
			print error ::error
		}
	}
	
	@Test
	fun stateTest()
	{
		var expired: KotlmataState.Init? = null
		val predicate = { s: Int -> s < 10 }
		val state = KotlmataMutableState("s1", 3, "") {
			expired = this
			val lambda1: EntryAction<SIGNAL> = {
				println("기본 진입함수")
			}
			val lambda2: EntryAction<String> = { signal ->
				println("String 타입 진입함수: $signal")
			}
			entry function lambda1
			entry via String::class function lambda2
			entry via "a" function { println("a 진입함수") }
			entry via "b" function {
				println("b 진입함수")
				"next"
			}
			input function { println("기본 입력함수") }
			input signal Any::class function { s -> println("Any 타입 입력함수: $s") }
			input signal "next" function { println("진입함수에서 흘러들어옴") }
			input signal predicate action { s -> println("술어형 신호: $s") }
			exit action { println("퇴장함수") }
		}
		
		state.entry("", Any())
		state.entry("", "signal")
		state.entry("", "a")
		state.entry("", "b")?.let { signal ->
			state.input(signal)
		}
		state.input("basic")
		state.input("basic", Any::class)
		state.exit("basic", "")
		state.input(5)
		
		state {
			delete action input signal predicate
		}
		state.input(5)
		
		state.entry("", "b")?.let { signal ->
			state.input(signal)
		}
		state.entry("", "a")?.let { signal ->
			state.input(signal)
		}
		
		expired?.entry?.function {}
	}
	
	@Test
	fun machineTest()
	{
		fun template(msg: String, block: MachineTemplate): MachineTemplate = { machine ->
			on error {
				println("$msg: on error")
			}
			on transition { from, signal, to ->
				println("on transition : [$transitionCount] $from x $signal -> $to")
			}
			
			block(machine)
			
			start at "state1"
		}
		
		val machine by KotlmataMutableMachine.lazy("m1", 2) by template("템플릿에서 정의") {
			"state1" { state ->
				entry function { println("$state: 기본 진입함수") }
				input signal String::class function { s -> println("$state: String 타입 입력함수: $s") }
				input signal "goToState2" function { println("state2로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state2" { state ->
				entry function { println("$state: 기본 진입함수") }
				input signal Number::class function { s -> println("$state: Number 타입 입력함수: $s") }
				input signal 5 function { println("state3로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state3" { state ->
				entry function { println("$state: 기본 진입함수") }
				input signal String::class function { s -> println("$state: String 타입 입력함수: $s") }
				input signal "error" function { throw RuntimeException() }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state4" { state ->
				entry via ("goToState4-1" or "goToState4-2" or "goToState4-3") function { signal ->
					println("$state: 다중 신호 진입함수: $signal")
				}
				input signal ("3" or 1 or 2) function { signal ->
					println("$state: 다중 신호 입력함수: $signal")
				}
			}
			
			"simple" via String::class function { state ->
				println("$state: 간략한 상태 정의")
				println("$state: 예외 발생")
				throw Exception("예외")
			} catch { throwable ->
				println("simple: Fallback")
				println(throwable)
			}
			
			"state1" x "goToState2" %= "state2"
			"state2" x 5 %= "state3"
			"state3" x "goToState1" %= "state1"
			"state1" x any.of("goToState4-1", "goToState4-2", "goToState4-3") %= "state4"
			"simple" x "goToSimple" %= "state1"
			any.except("simple") x "goToSimple" %= "simple"
			
			start at "state1"
		}
		
		machine.input("some string")
		machine.input("goToState2")
		machine.input(5, Number::class)
		machine.input(5)
		machine.input("error")
		machine.input("goToState1")
		machine.input("goToState4-3")
		machine.input(1)
		machine.input("3")
		machine.input("goToSimple")
		machine.input("goToSimple")
		
		println("-----------------------------------")
		
		machine {
			has state "state1" then {
				println("state1 있음")
			} or {
				println("state1 없음")
			}
			
			update state "state1" by { state ->
				input signal String::class function { s -> println("$state: 수정된 String 타입 입력함수: $s") }
				delete action entry via { s: Int -> s < 10 }
			}
			
			insert state "state2" by { state ->
				entry function { println("삽입된 $state") }
			}
			
			insert rule ("state1" x "goToState2") %= "state3"
			insert rule ("state1" x "goToState3") %= "state3"
		}
		
		machine.input("some string")
		machine.input("goToState2")
		
		var modify: KotlmataMutableMachine.Modify? = null
		machine {
			println("현재 상태: $currentState")
			modify = this
		}
		
		println("현재 상태를 외부에서 확인: ${modify?.currentState}")
	}
	
	@Test
	fun daemonTest()
	{
		var shouldGC: WeakReference<KotlmataState.Init>? = null
		var expire: KotlmataMutableState.Modify? = null
		var thread: Thread? = null
		
		val base: DaemonBase = {
			on error {
				println("템플릿에서 정의: $throwable")
			}
			on fatal {
				println("치명적인 에러: $throwable")
			}
			
			on transition { from, _, _ ->
				if (from == "state1")
				{
					throw Exception("on transition 에러 발생!!")
				}
			} catch { _, _, _ ->
				println("${throwable}: on transition catch 에서 해결")
			}
		}
		
		val daemon by KotlmataMutableDaemon.lazy("d1", 2) extends base by { daemon ->
			on create {
				println("--------------------- 데몬이 생성됨")
				thread = Thread.currentThread()
			}
			on start {
				println("--------------------- 데몬이 시작됨")
				throw Exception("onStart 에서 예외 발생")
			} catch {
				println("onStart Fallback: $throwable")
			}
			on pause {
				println("--------------------- 데몬이 정지됨")
			}
			on stop {
				println("--------------------- 데몬이 중지됨")
			}
			on resume {
				println("--------------------- 데몬이 재개됨")
			}
			on finish {
				println("--------------------- 데몬이 종료됨")
			}
			on destroy {
				println("--------------------- 데몬이 소멸됨")
			}
			
			val defaultExit: StateTemplate<CharSequence> = {
				exit action {
					println("템플릿으로 정의된 퇴장함수 호출됨")
				}
			}
			
			val defaultEnter: (String, StateTemplate<String>) -> StateTemplate<String> = fun(msg: String, block: StateTemplate<String>): StateTemplate<String> = { state ->
				entry function {
					println(msg)
				}
				block(state)
			}
			
			"state1" extends defaultExit with { state ->
				entry action {
					println("$state: 기본 진입함수")
				}
				input signal String::class function { s ->
					println("$state: String 타입 입력함수: $s")
					null
				}
				input signal "goToState2" function { println("state2로 이동") }
				input signal "goToError" function {
					throw Exception("에러1 발생")
				}
				input signal "payload" function { signal ->
					println("$state: signal = $signal, payload = $payload")
				}
				shouldGC = WeakReference(this)
			}
			
			"state2" { state ->
				entry function { println("$state: 기본 진입함수") }
				input signal Integer::class function { s -> println("$state: Number 타입 입력함수: $s") }
				input signal 5 function { println("state3로 이동") }
				input signal "error" function { throw Exception("state2에서 강제 예외 발생") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state3" { state ->
				entry function {
					println("$state: 기본 진입함수")
					"entry sync"
				}
				input signal String::class function { s -> println("$state: String 타입 입력함수: $s") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state4" { state ->
				entry function {
					Thread.sleep(10)
					println("$state: 기본 진입함수")
					"goToState1" `as` Any::class with "It's a payload"
				}
				input signal Any::class function { signal ->
					println("$state: Any 타입 입력함수: $signal, $payload")
				}
				exit action { println("$state: 퇴장함수") }
			}
			
			"error" extends defaultEnter("템플릿으로 정의된 진입함수 호출됨") {
				input signal "error" function {
					throw Exception("에러2 발생")
				}
				on error {
					println("상태 Fallback")
					println(throwable)
				}
			}
			
			"errorSync" { state ->
				entry function {
					throw Exception("에러3 발생")
				} intercept { signal ->
					println("진입동작 Fallback")
					println("$state: catch 진입: $signal")
					println(throwable)
					"goToState5"
				}
			}
			
			"state5" { state ->
				entry function { println("$state: 기본 진입함수") }
				input signal "sync" function {
					println("sync 는 흡수되고 input sync 로 전이한다")
					"input sync"
				}
			}
			
			"state6" { state ->
				entry via String::class function {
					println("$state: String::class 진입함수")
				}
				entry via "signal" function {
					println("$state: 'signal' 진입함수")
				}
			}
			
			"state7" { state ->
				entry action {
					println("action 내부에서 파라미터 daemon 인스턴스에 입력하기")
					daemon.input("goToState1")
				}
				exit action {
					println("$state: 퇴장함수")
				}
			}
			"state8" { state ->
				entry via String::class function {
					println("$state: String::class 진입함수")
				}
				entry via "signal" function {
					println("$state: 'signal' 진입함수")
				}
			}
			
			val template: StateTemplate<String> = {
				entry via String::class function {
					println("템플릿으로 extends 된 문구")
				}
			}
			
			"state9" extends template with {
				exit action {
					println("템플릿으로 extends 후 추가 정의된 문구")
				}
			}
			"state10" { state ->
				entry via String::class function {
					println("$state: String::class 진입함수")
				}
				input signal String::class action { signal ->
					println("$state: $signal 입력됨")
				}
				exit action {
					println("$state: 기본 퇴장함수")
				}
				exit via String::class action { signal ->
					println("$state: $signal 신호를 통한 퇴장함수")
				}
			}
			"state11" { state ->
				entry via String::class function {
					println("$state: String::class 진입함수")
				}
				input signal { signal: String -> signal.startsWith("return") } action { signal ->
					println("$state: $signal 통해 논리신호로 들어옴")
				}
				input signal { signal: String -> signal.startsWith("retu") } action { signal ->
					println("$state: $signal 통해 논리신호로 들어옴")
				}
				input signal { signal: String -> signal.startsWith("retur") } action { signal ->
					println("$state: $signal 통해 논리신호로 들어옴. 여러 비슷한 조건 중 마지막에 정의한 조건에 들어옴.")
				}
				exit action {
					println("$state: 기본 퇴장함수")
				}
				exit via { signal: String -> signal.startsWith("return") } action { signal ->
					println("$state: $signal 통해 퇴장. 입력과 논리는 같으나 퇴장 신호는 여기에 걸림")
				}
			}
			"state12" { state ->
				entry via "a".."z" function { signal ->
					println("$state: Predicate 진입함수. signal = $signal")
				}
				input signal 1..10 action { signal ->
					println("$state: 1 <= $signal <= 10")
				}
			}
			
			"chain1" { state ->
				entry function { println("$state: 기본 진입함수") }
			}
			"chain2" { state ->
				entry function { println("$state: 기본 진입함수") }
			}
			"chain3" { state ->
				entry function { println("$state: 기본 진입함수") }
			}
			
			"state1" x "goToState2" %= "state2"
			"state2" x 5 %= "state3"
			"state3" x "goToState1" %= "state1"
			"state3" x "goToState4" %= "state4"
			"state4" x Any::class %= "state1"
			"state1" x "goToError" %= "error"
			"error" x "error" %= "errorSync"
			"errorSync" x "goToState5" %= "state5"
			"state5" x String::class %= "state1"
			chain from "state1" to "chain1" to "chain2" to "chain3" via "next"
			any.except("chain1", "chain2") x ("a" or "b" or "c") %= "state1"
			"state1" x "signal" %= "state6"
			"state6" x String::class %= self
			"state6" x "goToState7" %= "state7"
			"state7" x "goToState1" %= "state1"
			("state1" or "state2") x ("d" or "e") %= "state8"
			"state8" x "goToState9" %= "state9"
			"state9" x "goToState10" %= "state10"
			"state10" x "out" %= "state11"
			"state11" x { signal: String -> signal.startsWith("return to") } %= "state12"
			"state12" x 11..12 %= "state1"
			
			start at "state1"
		}
		
		daemon.input("any1")
		daemon.run()
		daemon.input("우선순위 10", priority = 10)
		daemon.input("우선순위 0", priority = 0)
		daemon.input("우선순위 -10", priority = -10)
		daemon.pause()
		daemon.input("goToState2")
		daemon.run()
		
		Thread.sleep(100)
		
		daemon.input("error")
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
			
			update state "state3" by { state ->
				expire = this
				entry function {
					println("$state: 수정된 기본 진입함수")
					"수정된 entry sync"
				}
			}
		}
		daemon.input("goToState3")
		daemon.input("이거 출력되어야 하는데..")
		
		Thread.sleep(100)
		
		daemon.input("goToState4")
		
		Thread.sleep(5)
		
		daemon.pause()
		daemon.input("pause 상태일 때 들어간 신호")
		
		Thread.sleep(100)
		
		daemon.stop()
		daemon.input("stop 상태일 때 들어간 신호")
		
		Thread.sleep(100)
		
		daemon.input("run 직전에 들어간 신호")
		daemon.run()
		daemon.input("현재 상태는 state1이어야 함")
		
		Thread.sleep(100)
		
		daemon.input("goToError")
		daemon.input("error")
		daemon.input("sync")
		
		Thread.sleep(100)
		
		daemon.input("payload", "this is a payload")
		
		Thread.sleep(100)
		
		daemon.input("next")
		daemon.input("next")
		daemon.input("next")
		daemon.input("next")
		daemon.input("c")
		
		Thread.sleep(100)
		
		daemon.input("signal")
		daemon.input("signal", String::class)
		daemon.input("goToState7")
		
		Thread.sleep(100)
		
		daemon.input("d")
		
		Thread.sleep(100)
		
		daemon.input("goToState9")
		daemon.input("goToState10")
		daemon.input("string")
		daemon.input("out")
		
		Thread.sleep(100)
		
		daemon.input("return")
		daemon.input("return to")
		daemon.input(0)
		daemon.input(1)
		daemon.input(10)
		daemon.input(11)
		
		Thread.sleep(500)
		
		thread?.interrupt()
		
		Thread.sleep(500)
		
		System.gc()
		println("과연 GC 되었을까: ${shouldGC?.get()}")
		expire?.entry?.function {}
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
