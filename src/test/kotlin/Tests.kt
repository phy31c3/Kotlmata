@file:Suppress("NonAsciiCharacters")

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
	
	private fun Map<String, Boolean>.verify() = forEach { (key, pass) ->
		assert(pass) {
			"\"$key\" failed"
		}
	}
	
	@Test
	fun `모든 유형의 진입동작이 제대로 불리는가`()
	{
		val checklist = mutableMapOf(
			"entry" to false,
			"entry catch" to false,
			"entry final" to false,
			"entry signal" to false,
			"entry signal catch" to false,
			"entry signal final" to false,
			"entry type" to false,
			"entry type catch" to false,
			"entry type final" to false,
			"entry signals" to false,
			"entry signals catch" to false,
			"entry signals final" to false,
			"entry predicate" to false,
			"entry predicate catch" to false,
			"entry predicate final" to false,
			"entry range" to false,
			"entry range catch" to false,
			"entry range final" to false
		)
		KotlmataMachine("machine") {
			"state" {
				entry action {
					checklist["entry"] = true
					throw Exception()
				} catch {
					checklist["entry catch"] = true
				} finally {
					checklist["entry final"] = true
				}
				entry via "signal" action {
					checklist["entry signal"] = true
					throw Exception()
				} catch {
					checklist["entry signal catch"] = true
				} finally {
					checklist["entry signal final"] = true
				}
				entry via String::class action {
					checklist["entry type"] = true
					throw Exception()
				} catch {
					checklist["entry type catch"] = true
				} finally {
					checklist["entry type final"] = true
				}
				entry via ("a" or "b" or "c") action {
					checklist["entry signals"] = true
					throw Exception()
				} catch {
					checklist["entry signals catch"] = true
				} finally {
					checklist["entry signals final"] = true
				}
				entry via { s: String -> s.startsWith("pre") } action {
					checklist["entry predicate"] = true
					throw Exception()
				} catch {
					checklist["entry predicate catch"] = true
				} finally {
					checklist["entry predicate final"] = true
				}
				entry via 0..5 action {
					checklist["entry range"] = true
					throw Exception()
				} catch {
					checklist["entry range catch"] = true
				} finally {
					checklist["entry range final"] = true
				}
			}
			
			"state" x any %= self
			
			start at "state"
		}.also { machine ->
			machine.input(10)
			machine.input("signal")
			machine.input("string")
			machine.input("a")
			machine.input("predicate")
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `모든 유형의 입력동작이 제대로 불리는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"input catch" to false,
			"input final" to false,
			"input signal" to false,
			"input signal catch" to false,
			"input signal final" to false,
			"input type" to false,
			"input type catch" to false,
			"input type final" to false,
			"input signals" to false,
			"input signals catch" to false,
			"input signals final" to false,
			"input predicate" to false,
			"input predicate catch" to false,
			"input predicate final" to false,
			"input range" to false,
			"input range catch" to false,
			"input range final" to false
		)
		KotlmataMachine("machine") {
			"state" {
				input action {
					checklist["input"] = true
					throw payload as Exception
				} catch {
					checklist["input catch"] = true
				} finally {
					checklist["input final"] = true
				}
				input signal "signal" action {
					checklist["input signal"] = true
					throw payload as Exception
				} catch {
					checklist["input signal catch"] = true
				} finally {
					checklist["input signal final"] = true
				}
				input signal String::class action {
					checklist["input type"] = true
					throw payload as Exception
				} catch {
					checklist["input type catch"] = true
				} finally {
					checklist["input type final"] = true
				}
				input signal ("a" or "b" or "c") action {
					checklist["input signals"] = true
					throw payload as Exception
				} catch {
					checklist["input signals catch"] = true
				} finally {
					checklist["input signals final"] = true
				}
				input signal { s: String -> s.startsWith("pre") } action {
					checklist["input predicate"] = true
					throw payload as Exception
				} catch {
					checklist["input predicate catch"] = true
				} finally {
					checklist["input predicate final"] = true
				}
				input signal 0..5 action {
					checklist["input range"] = true
					throw payload as Exception
				} catch {
					checklist["input range catch"] = true
				} finally {
					checklist["input range final"] = true
				}
			}
			
			"state" x any %= stay
			
			start at "state"
		}.also { machine ->
			machine.input(10, Exception())
			machine.input("signal", Exception())
			machine.input("string", Exception())
			machine.input("b", Exception())
			machine.input("predicate", Exception())
			machine.input(3, Exception())
		}
		
		checklist.verify()
	}
	
	@Test
	fun `모든 유형의 퇴장동작이 제대로 불리는가`()
	{
		val checklist = mutableMapOf(
			"exit" to false,
			"exit catch" to false,
			"exit final" to false,
			"exit signal" to false,
			"exit signal catch" to false,
			"exit signal final" to false,
			"exit type" to false,
			"exit type catch" to false,
			"exit type final" to false,
			"exit signals" to false,
			"exit signals catch" to false,
			"exit signals final" to false,
			"exit predicate" to false,
			"exit predicate catch" to false,
			"exit predicate final" to false,
			"exit range" to false,
			"exit range catch" to false,
			"exit range final" to false
		)
		KotlmataMachine("machine") {
			"state" {
				exit action {
					checklist["exit"] = true
					throw Exception()
				} catch {
					checklist["exit catch"] = true
				} finally {
					checklist["exit final"] = true
				}
				exit via "signal" action {
					checklist["exit signal"] = true
					throw Exception()
				} catch {
					checklist["exit signal catch"] = true
				} finally {
					checklist["exit signal final"] = true
				}
				exit via String::class action {
					checklist["exit type"] = true
					throw Exception()
				} catch {
					checklist["exit type catch"] = true
				} finally {
					checklist["exit type final"] = true
				}
				exit via ("a" or "b" or "c") action {
					checklist["exit signals"] = true
					throw Exception()
				} catch {
					checklist["exit signals catch"] = true
				} finally {
					checklist["exit signals final"] = true
				}
				exit via { s: String -> s.startsWith("pre") } action {
					checklist["exit predicate"] = true
					throw Exception()
				} catch {
					checklist["exit predicate catch"] = true
				} finally {
					checklist["exit predicate final"] = true
				}
				exit via 0..5 action {
					checklist["exit range"] = true
					throw Exception()
				} catch {
					checklist["exit range catch"] = true
				} finally {
					checklist["exit range final"] = true
				}
			}
			
			"state" x any %= self
			
			start at "state"
		}.also { machine ->
			machine.input(10)
			machine.input("signal")
			machine.input("string")
			machine.input("c")
			machine.input("predicate")
			machine.input(5)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `모든 상태함수가 제대로 동작하는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"input signal" to false,
			"input type" to false,
			"input signals" to false,
			"input predicate" to false,
			"input range" to false,
			"entry" to false,
			"entry signal" to false,
			"entry type" to false,
			"entry signals" to false,
			"entry predicate" to false,
			"entry range" to false,
			"finish" to false
		)
		
		KotlmataMachine("machine") {
			"input" {
				input function {
					checklist["input"] = true
					"a"
				}
				input signal "a" function {
					checklist["input signal"] = true
					"b" `as` String::class
				}
				input signal String::class function {
					checklist["input type"] = true
					"c"
				}
				input signal ("c" or "d") function {
					checklist["input signals"] = true
					"e"
				}
				input signal { s: String -> s == "e" } function {
					checklist["input predicate"] = true
					"f"
				}
				input signal "f".."g" function {
					checklist["input range"] = true
					1
				}
				input signal 1 action {
					/* nothing */
				}
			}
			"entry" {
				entry function {
					checklist["entry"] = true
					"a"
				}
				entry via "a" function {
					checklist["entry signal"] = true
					"b" `as` String::class
				}
				entry via String::class function {
					checklist["entry type"] = true
					"c"
				}
				entry via ("c" or "d") function {
					checklist["entry signals"] = true
					"e"
				}
				entry via { s: String -> s == "e" } function {
					checklist["entry predicate"] = true
					"f"
				}
				entry via "f".."g" function {
					checklist["entry range"] = true
					2
				}
			}
			"finish" {
				entry action {
					checklist["finish"] = true
				}
			}
			
			"input" x 1 %= "entry"
			"entry" x String::class %= self
			"entry" x 2 %= "finish"
			
			start at "input"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `상태의 'on error'가 제대로 호출 되는가`()
	{
		val checklist = mutableMapOf(
			"on error" to false
		)
		
		KotlmataMachine("machine") {
			"state" {
				input action {
					throw Exception()
				}
				on error {
					checklist["on error"] = true
				}
			}
			
			start at "state"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `상태 업데이트 시 상태동작이 잘 교체되는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"exit" to false,
			"entry" to false
		)
		
		val predicate = { s: Int -> s < 10 }
		
		KotlmataMutableMachine("machine") {
			"state" {
				entry via predicate action {
					checklist["entry"] = false
				}
				input action {
					checklist["input"] = false
				}
				exit via 0 action {
					checklist["exit"] = false
				}
			}
			
			"state" x any %= self
			
			start at "state"
		}.also { machine ->
			machine {
				"state" update {
					entry via predicate action {
						checklist["entry"] = true
					}
					input action {
						checklist["input"] = true
					}
					exit via 0 action {
						checklist["exit"] = true
					}
				}
			}
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `상태 업데이트 시 상태동작이 잘 삭제되는가`()
	{
		val checklist = mutableMapOf(
			"input" to true,
			"input signal" to true,
			"input type" to true,
			"input signals" to true,
			"input predicate" to true,
			"exit" to true,
			"entry" to true,
			"entry all" to true,
			"input all" to true,
			"exit all" to true,
			"all" to true
		)
		
		val predicate = { s: Int -> s in 0..1 }
		
		KotlmataMutableMachine("machine") {
			"state1" {
				input action {
					checklist["input"] = false
				}
				input signal 0 action {
					checklist["input signal"] = false
				}
				input signal Int::class action {
					checklist["input type"] = false
				}
				input signal predicate action {
					checklist["input predicate"] = false
				}
				exit action {
					checklist["exit"] = false
				}
			}
			"state2" {
				entry action {
					checklist["entry"] = false
				}
				entry via 0..1 action {
					checklist["entry all"] = false
				}
				input signal 1..2 action {
					checklist["input all"] = false
				}
				exit via 1..2 action {
					checklist["exit all"] = false
				}
			}
			"state3" {
				entry action {
					checklist["all"] = false
				}
			}
			
			"state1" x 0 %= "state2"
			"state2" x 1 %= "state3"
			
			start at "state1"
		}.also { machine ->
			machine {
				"state1" update {
					delete action input signal 0
					delete action input signal predicate
					delete action input signal Int::class
					delete action input
					delete action exit
				}
				"state2" update {
					delete action entry via all
					delete action entry
					delete action input signal all
					delete action exit via all
				}
				"state3" update {
					delete action all
				}
			}
			machine.input(0)
			machine.input(1)
			machine.input(2)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `상태 템플릿이 잘 적용되는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"on error" to false
		)
		
		KotlmataMachine("machine") {
			val template: StateTemplate<STATE> = {
				on error {
					checklist["on error"] = true
				}
			}
			
			"state" extends template with {
				input action {
					checklist["input"] = true
					throw Exception()
				}
			}
			
			start at "state"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `간략한 상태 정의가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"entry" to false,
			"entry signal" to false,
			"entry type" to false,
			"entry predicate" to false,
			"entry range" to false
		)
		
		KotlmataMachine("machine") {
			"a" {}
			"b" function {
				checklist["entry"] = true
				it
			}
			"c" via 0 function {
				checklist["entry signal"] = true
				it
			}
			"d" via Int::class function {
				checklist["entry type"] = true
				it
			}
			"e" via { s: Int -> s == 0 } function {
				checklist["entry predicate"] = true
				it
			}
			"f" via 0..1 action {
				checklist["entry range"] = true
			}
			
			chain from "a" to "b" to "c" to "d" to "e" to "f" via 0
			
			start at "a"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `모든 유형의 머신 생성이 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"machine" to false,
			"machine extends" to false,
			"machine lazy" to false,
			"machine lazy extends" to false,
			"mutable machine" to false,
			"mutable machine extends" to false,
			"mutable machine lazy" to false,
			"mutable machine lazy extends" to false
		)
		
		fun base(check: String): MachineBase = {
			"state" {
				input action {
					checklist[check] = true
				}
			}
		}
		
		val m1 = KotlmataMachine("m1") {
			"state" {
				input action {
					checklist["machine"] = true
				}
			}
			start at "state"
		}
		val m2 = KotlmataMachine("m2") extends base("machine extends") by {
			start at "state"
		}
		val m3 by KotlmataMachine.lazy("m3") {
			"state" {
				input action {
					checklist["machine lazy"] = true
				}
			}
			start at "state"
		}
		val m4 by KotlmataMachine.lazy("m4") extends base("machine lazy extends") by {
			start at "state"
		}
		val m5 = KotlmataMutableMachine("m1") {
			"state" {
				input action {
					checklist["mutable machine"] = true
				}
			}
			start at "state"
		}
		val m6 = KotlmataMutableMachine("m6") extends base("mutable machine extends") by {
			start at "state"
		}
		val m7 by KotlmataMutableMachine.lazy("m7") {
			"state" {
				input action {
					checklist["mutable machine lazy"] = true
				}
			}
			start at "state"
		}
		val m8 by KotlmataMutableMachine.lazy("m8") extends base("mutable machine lazy extends") by {
			start at "state"
		}
		
		m1.input(0)
		m2.input(0)
		m3.input(0)
		m4.input(0)
		m5.input(0)
		m6.input(0)
		m7.input(0)
		m8.input(0)
		
		checklist.verify()
	}
	
	@Test
	fun `머신의 'on transition'이 제대로 호출 되는가`()
	{
		val checklist = mutableMapOf(
			"on transition" to false,
			"on transition catch" to false,
			"on transition final" to false
		)
		
		KotlmataMachine("machine") {
			on transition { _, _, _ ->
				checklist["on transition"] = true
				throw Exception()
			} catch { _, _, _ ->
				checklist["on transition catch"] = true
			} finally { _, _, _ ->
				checklist["on transition final"] = true
			}
			
			"state" {}
			
			"state" x any %= self
			
			start at "state"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun machineTest()
	{
		val base: MachineBase = {
			on error {
				println("템플릿에서 정의: on error")
			}
			on transition { from, signal, to ->
				println("on transition : [$transitionCount] $from x $signal -> $to")
			}
		}
		
		val machine by KotlmataMutableMachine.lazy("m1", 3) extends base by {
			"state1" { state ->
				entry function { println("$state: 기본 진입함수") }
				input signal String::class function { s -> println("$state: String 타입 입력함수: $s") }
				input signal "goToState2" function { println("state2로 이동") }
				exit action { println("$state: 퇴장함수") }
			}
			
			"state2" { state ->
				entry function { println("$state: 기본 진입함수") }
				input signal 5 function { s -> println("$state: Number 타입 입력함수: $s") }
				input signal Number::class function { println("state3로 이동") }
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
			"state2" x Number::class %= "state3"
			"state3" x "goToState1" %= "state1"
			"state1" x any.of("goToState4-1", "goToState4-2", "goToState4-3") %= "state4"
			"simple" x "goToSimple" %= "state1"
			any.except("simple") x "goToSimple" %= "simple"
			
			start at "state1"
		}
		
		machine.input("some string")
		machine.input("goToState2")
		machine.input(5)
		machine.input(5, Number::class)
		machine.input("error")
		machine.input("goToState1")
		machine.input("goToState4-3")
		machine.input(1)
		machine.input("3")
		machine.input("goToSimple")
		machine.input("goToSimple")
		
		println("-----------------------------------")
		
		machine {
			if (has state "State1")
			{
				println("state1 있음")
			}
			else
			{
				println("state1 없음")
			}
			
			"state1" update { state ->
				input signal String::class function { s -> println("$state: 수정된 String 타입 입력함수: $s") }
				delete action entry via { s: Int -> s < 10 }
			}
			
			"state1" x "goToState3" %= "state3"
		}
		
		machine.input("some string")
		machine.input("goToState2")
		
		var update: KotlmataMutableMachine.Update? = null
		machine {
			println("현재 상태: $currentState")
			update = this
		}
		
		println("현재 상태를 외부에서 확인: ${update?.currentState}")
	}
	
	@Test
	fun daemonTest()
	{
		var shouldGC: WeakReference<KotlmataState.Init>? = null
		var expire: KotlmataMutableState.Update? = null
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
			
			"state3" update { state ->
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
