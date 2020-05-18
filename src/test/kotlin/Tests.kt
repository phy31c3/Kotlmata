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
		Kotlmata.config {
			print debug ::println
			print warn ::println
			print error ::error
		}
	}
	
	@Test
	fun stateTest()
	{
		var expired: KotlmataState.Init? = null
		val state by KotlmataMutableState.lazy("s1") {
			expired = this
			val lambda1: EntryAction<SIGNAL> = {
				println("기본 진입함수")
			}
			val lambda2: EntryAction<String> = { signal ->
				println("String 타입 진입함수: $signal")
			}
			entry action lambda1
			entry via String::class action lambda2
			entry via "a" action { println("a 진입함수") }
			entry via "b" action {
				println("b 진입함수")
				"next"
			}
			input action { println("기본 입력함수") }
			input signal Any::class action { s -> println("Any 타입 입력함수: $s") }
			input signal "next" action { println("진입함수에서 흘러들어옴") }
			exit action { println("퇴장함수") }
		}
		
		state.entry(Any())
		state.entry("signal")
		state.entry("a")
		state.entry("b")?.let { signal ->
			state.input(signal)
		}
		state.input("basic")
		state.input("basic", Any::class)
		state.exit("basic")
		
		state {
			delete action input signal "next"
		}
		
		state.entry("b")?.let { signal ->
			state.input(signal)
		}
		state.entry("a")?.let { signal ->
			state.input(signal)
		}
		
		expired?.entry?.action {}
	}
	
	@Test
	fun machineTest()
	{
		fun template(msg: String, block: MachineTemplate<String>): MachineTemplate<String> = { machine ->
			on error {
				println("$msg: on error")
			}
			
			block(machine)
			
			start at "state1"
		}
		
		val machine by KotlmataMutableMachine.lazy("m1", 1) extends template("템플릿에서 정의") {
			"state1" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal String::class action { s -> println("$state: String 타입 입력함수: $s") }
				input signal "goToState2" action { println("state2로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state2" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal Number::class action { s -> println("$state: Number 타입 입력함수: $s") }
				input signal 5 action { println("state3로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state3" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal String::class action { s -> println("$state: String 타입 입력함수: $s") }
				input signal "error" action { throw RuntimeException() }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state4" { state ->
				entry via ("goToState4-1" or "goToState4-2" or "goToState4-3") action { signal ->
					println("$state: 다중 신호 진입함수: $signal")
				}
				input signal ("3" or 1 or 2) action { signal ->
					println("$state: 다중 신호 입력함수: $signal")
				}
			}
			
			"simple" via String::class action { state ->
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
			
			update state "state1" with { state ->
				input signal String::class action { s -> println("$state: 수정된 String 타입 입력함수: $s") }
				delete action exit
			}
			
			insert state "state2" with { state ->
				entry action { println("삽입된 $state") }
			}
			
			insert rule ("state1" x "goToState2") %= "state3"
			insert rule ("state1" x "goToState3") %= "state3"
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
		var shouldGC: WeakReference<KotlmataState.Init>? = null
		var expire: KotlmataMutableState.Modifier? = null
		var thread: Thread? = null
		
		fun template(msg: String, block: DaemonTemplate<String>): DaemonTemplate<String> = { daemon ->
			on error {
				println("$msg: on error")
			}
			
			block(daemon)
			
			start at "state1"
		}
		
		val daemon by KotlmataMutableDaemon.lazy("d1", 3) extends template("템플릿에서 정의") {
			on start {
				thread = Thread.currentThread()
				throw Exception("onStart 에서 예외 발생")
			} catch {
				println("onStart Fallback: $throwable")
			}
			on terminate {
				println("데몬이 종료됨")
			}
			
			fun defaultExit(msg: String, block: StateTemplate<String>): StateTemplate<String> = { state ->
				exit action {
					println(msg)
				}
				block(state)
			}
			
			val defaultEnter: (String, StateTemplate<String>) -> StateTemplate<String> = fun(msg: String, block: StateTemplate<String>): StateTemplate<String> = { state ->
				entry action {
					println(msg)
				}
				block(state)
			}
			
			"state1" extends defaultExit("템플릿으로 정의된 퇴장함수 호출됨") { state ->
				entry action {
					println("$state: 기본 진입함수")
					null
				}
				input signal String::class action { s ->
					println("$state: String 타입 입력함수: $s")
					null
				}
				input signal "goToState2" action { println("state2로 이동") }
				input signal "goToError" action {
					throw Exception("에러1 발생")
				}
				input signal "payload" action { signal ->
					println("$state: signal = $signal, payload = $payload")
				}
				shouldGC = WeakReference(this)
			}
			
			"state2" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal Integer::class action { s -> println("$state: Number 타입 입력함수: $s") }
				input signal 5 action { println("state3로 이동") }
				input signal "error" action { throw Exception("state2에서 강제 예외 발생") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state3" { state ->
				entry action {
					println("$state: 기본 진입함수")
					"entry sync"
				}
				input signal String::class action { s -> println("$state: String 타입 입력함수: $s") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state4" { state ->
				entry action {
					Thread.sleep(10)
					println("$state: 기본 진입함수")
					"goToState1" type Any::class payload "It's a payload"
				}
				input signal Any::class action { signal ->
					println("$state: Any 타입 입력함수: $signal, $payload")
				}
				exit action { println("$state: 퇴장함수") }
			}
			
			"error" extends defaultEnter("템플릿으로 정의된 진입함수 호출됨") {
				input signal "error" action {
					throw Exception("에러2 발생")
				}
				error action { throwable ->
					println("상태 Fallback")
					println(throwable)
				}
			}
			
			"errorSync" { state ->
				entry action {
					throw Exception("에러3 발생")
				} catch { signal ->
					println("진입동작 Fallback")
					println("$state: catch 진입: $signal")
					println(throwable)
					"goToState5"
				}
			}
			
			"state5" { state ->
				entry action { println("$state: 기본 진입함수") }
				input signal "sync" action {
					println("sync 는 흡수되고 input sync 로 전이한다")
					"input sync"
				}
			}
			
			"state6" { state ->
				entry via String::class action {
					println("$state: String::class 진입함수")
				}
				entry via "signal" action {
					println("$state: 'signal' 진입함수")
				}
			}
			
			"state7" { state ->
				exit action {
					println("$state: 퇴장함수")
				}
			}
			
			"chain1" { state ->
				entry action { println("$state: 기본 진입함수") }
			}
			"chain2" { state ->
				entry action { println("$state: 기본 진입함수") }
			}
			"chain3" { state ->
				entry action { println("$state: 기본 진입함수") }
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
			
			update state "state3" with { state ->
				expire = this
				entry action {
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
		daemon.input("goToState1")
		
		Thread.sleep(500)
		
		thread?.interrupt()
		
		Thread.sleep(500)
		
		System.gc()
		println("과연 GC 되었을까: ${shouldGC?.get()}")
		expire?.entry?.action {}
	}
	
	@Test
	fun kotlmataTest()
	{
		var expire: Kotlmata.Post? = null
		Kotlmata.start(2)
		Kotlmata fork "daemon" with {
			
			on start {
				println("데몬 on start: payload = $payload")
			}
			
			"state1" { state ->
				entry action { println("데몬이 시작됨") }
				input signal String::class action { s -> println("$state: String 타입 입력함수: $s") }
				input signal "goToState2" action { println("state2로 이동") }
				input signal "payload" action { signal ->
					println("signal: $signal, payload: $payload")
				}
				exit action { println("$state: 퇴장함수") }
			}
			
			"state2" { state ->
				entry action { println("$state: 기본 진입함수") }
				entry via "goToState2" action {
					println("null 리턴할거임")
					null
				}
				input signal Integer::class action { s -> println("$state: Number 타입 입력함수: $s") }
				input signal String::class action { s -> println("$state: String 타입 입력함수: $s") }
				input signal 5 action { println("state3로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state3" { state ->
				entry action {
					println("$state: 기본 진입함수")
					"sync input"
				}
				input signal String::class action { s -> println("$state: String 타입 입력함수: $s") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state1" x "goToState2" %= "state2"
			"state2" x 5 %= "state3"
			"state3" x "goToState1" %= "state1"
			"state3" x "goToState1" %= "state1"
			
			start at "state1"
		}
		
		Kotlmata input "무시해라1" to "daemon"
		Kotlmata input "무시해라2" to "daemon"
		Kotlmata modify "daemon" with {
			println("현재 상태: $current")
		}
		
		Thread.sleep(100)
		
		Kotlmata.run("daemon", "payload")
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
				modify daemon "daemon" with {
					update state "state2" with { state ->
						input signal Integer::class action { s -> println("$state: Post 에서 수정된 Number 타입 입력함수: $s") }
						exit action { println("$state: Post 에서 수정된 퇴장함수") }
					}
				}
			}
			
			input signal 3 to "daemon"
		}
		Kotlmata input 5 to "daemon"
		Kotlmata input "goToState1" to "daemon"
		Kotlmata input "payload" payload "this is a payload" to "daemon"
		
		Thread.sleep(100)
		
		Kotlmata input "stop 보다 더 빨리 실행될까?" to "daemon"
		Kotlmata.stop()
		Kotlmata.release()
		
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