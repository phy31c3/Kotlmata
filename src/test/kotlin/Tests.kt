@file:Suppress("NonAsciiCharacters")

import kr.co.plasticcity.kotlmata.*
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch

class Tests
{
	private val logLevel = 3
	
	private fun Map<String, Boolean>.verify() = forEach { (key, pass) ->
		assert(pass) {
			"\"$key\" failed"
		}
	}
	
	private class Latch(private val count: Int)
	{
		private val latch = CountDownLatch(count)
		private val threads = mutableListOf<Thread>()
		
		operator fun invoke() = Thread.currentThread().let { thread ->
			if (threads.size >= count)
			{
				throw Exception("Latch overflow")
			}
			else if (threads.contains(thread))
			{
				throw Exception("Latch thread duplicated")
			}
			threads += Thread.currentThread()
			latch.countDown()
		}
		
		fun await()
		{
			latch.await()
			threads.forEach { it.join() }
		}
	}
	
	companion object
	{
		@BeforeClass
		@JvmStatic
		fun init()
		{
			KotlmataConfig {
				print debug System.out::println
				print warn System.err::println
				print error System.err::println
			}
		}
	}
	
	@After
	fun divider()
	{
		println("---------------------------------------------- end ----------------------------------------------")
	}
	
	@Test
	fun `S-001 상태의 모든 유형의 진입동작이 제대로 불리는가`()
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
		KotlmataMachine("S-001", logLevel) {
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
				entry via ("a" OR "b" OR "c") action {
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
	fun `S-002 상태의 모든 유형의 입력동작이 제대로 불리는가`()
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
		KotlmataMachine("S-002", logLevel) {
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
				input signal ("a" OR "b" OR "c") action {
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
	fun `S-003 상태의 모든 유형의 퇴장동작이 제대로 불리는가`()
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
		KotlmataMachine("S-003", logLevel) {
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
				exit via ("a" OR "b" OR "c") action {
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
	fun `S-004 상태의 'function'이 제대로 동작하는가`()
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
		
		KotlmataMachine("S-004", logLevel) {
			"input" {
				input function {
					checklist["input"] = true
					"a"
				}
				input signal "a" function {
					checklist["input signal"] = true
					"b" `as` CharSequence::class
				}
				input signal CharSequence::class function {
					checklist["input type"] = true
					"c"
				}
				input signal ("c" OR "d") function {
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
				input signal 1 action {}
			}
			"entry" {
				entry function {
					checklist["entry"] = true
					"a"
				}
				entry via "a" function {
					checklist["entry signal"] = true
					"b" `as` CharSequence::class
				}
				entry via CharSequence::class function {
					checklist["entry type"] = true
					"c"
				}
				entry via ("c" OR "d") function {
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
			"entry" x CharSequence::class %= self
			"entry" x 2 %= "finish"
			
			start at "input"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `S-005 상태의 'function'에서 타입지정과 payload 전달이 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"input as" to false,
			"entry as with" to false,
			"input with" to false
		)
		
		KotlmataMachine("S-005", logLevel) {
			"state1" {
				input signal Int::class function {
					"10" `as` CharSequence::class
				}
			}
			"state2" {
				entry via CharSequence::class function {
					checklist["input as"] = true
					1 `as` Int::class with 1
				}
				input signal Int::class function { signal ->
					checklist["entry as with"] = true
					signal + payload as Int with 2
				}
				input signal 2 action { signal ->
					if (signal - payload as Int == 0)
					{
						checklist["input with"] = true
					}
				}
			}
			
			"state1" x CharSequence::class %= "state2"
			
			start at "state1"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `S-006 상태의 'intercept'가 제대로 동작하는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"input intercept" to false,
			"input signal" to false,
			"input signal intercept" to false,
			"input signal final" to false,
			"entry" to false,
			"entry intercept" to false,
			"entry final" to false,
			"entry signal" to false,
			"entry signal intercept" to false,
			"finish" to false
		)
		
		KotlmataMachine("S-006", logLevel) {
			"input" {
				input action {
					checklist["input"] = true
					throw Exception()
				} intercept {
					checklist["input intercept"] = true
					"a"
				}
				input signal "a" function {
					checklist["input signal"] = true
					throw Exception()
				} intercept {
					checklist["input signal intercept"] = true
					1
				} finally {
					checklist["input signal final"] = true
				}
				input signal 1 action {}
			}
			"entry" {
				entry function {
					checklist["entry"] = true
					throw Exception()
				} intercept {
					checklist["entry intercept"] = true
					"a"
				} finally {
					checklist["entry final"] = true
				}
				entry via "a" action {
					checklist["entry signal"] = true
					throw Exception()
				} intercept {
					checklist["entry signal intercept"] = true
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
	fun `S-007 상태의 'on error'가 제대로 호출되는가`()
	{
		val checklist = mutableMapOf(
			"on error" to false
		)
		
		KotlmataMachine("S-007", logLevel) {
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
	fun `S-008 상태 업데이트 시 상태동작이 잘 교체되는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"exit" to false,
			"entry" to false
		)
		
		val predicate = { s: Int -> s < 10 }
		
		KotlmataMutableMachine("S-008", logLevel) {
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
	fun `S-009 상태 업데이트 시 상태동작이 잘 삭제되는가`()
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
		
		KotlmataMutableMachine("S-009", logLevel) {
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
	fun `S-010 상태 템플릿이 잘 적용되는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"on error" to false
		)
		
		KotlmataMachine("S-010", logLevel) {
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
	fun `S-011 상태동작의 'prevState' 및 'nextState' 프로퍼티가 잘 동작하는가`()
	{
		val checklist = mutableMapOf(
			"next state" to false,
			"prev state" to false
		)
		
		KotlmataMachine("S-011", logLevel) {
			"a" {
				exit action {
					checklist["next state"] = nextState == "b"
				}
			}
			"b" {
				entry action {
					checklist["prev state"] = prevState == "a"
				}
			}
			
			"a" x any %= "b"
			
			start at "a"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-001 머신의 간략한 상태 정의가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"entry" to false,
			"entry signal" to false,
			"entry type" to false,
			"entry predicate" to false,
			"entry range" to false
		)
		
		KotlmataMachine("M-001", logLevel) {
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
	fun `M-002 머신의 모든 유형의 생성이 잘 되는가`()
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
		
		fun template(check: String): MachineTemplate = {
			"state" {
				input action {
					checklist[check] = true
				}
			}
		}
		
		val m1 = KotlmataMachine("M-002-1", logLevel) {
			"state" {
				input action {
					checklist["machine"] = true
				}
			}
			start at "state"
		}
		val m2 = KotlmataMachine("M-002-2", logLevel) extends template("machine extends") by {
			start at "state"
		}
		val m3 by KotlmataMachine.lazy("M-002-3", logLevel) {
			"state" {
				input action {
					checklist["machine lazy"] = true
				}
			}
			start at "state"
		}
		val m4 by KotlmataMachine.lazy("M-002-4", logLevel) extends template("machine lazy extends") by {
			start at "state"
		}
		val m5 = KotlmataMutableMachine("M-002-5", logLevel) {
			"state" {
				input action {
					checklist["mutable machine"] = true
				}
			}
			start at "state"
		}
		val m6 = KotlmataMutableMachine("M-002-6", logLevel) extends template("mutable machine extends") by {
			start at "state"
		}
		val m7 by KotlmataMutableMachine.lazy("M-002-7", logLevel) {
			"state" {
				input action {
					checklist["mutable machine lazy"] = true
				}
			}
			start at "state"
		}
		val m8 by KotlmataMutableMachine.lazy("M-002-8", logLevel) extends template("mutable machine lazy extends") by {
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
	fun `M-003 머신의 'on transition'이 제대로 호출되는가`()
	{
		val checklist = mutableMapOf(
			"on transition" to false,
			"on transition catch" to false,
			"on transition final" to false
		)
		
		KotlmataMachine("M-003", logLevel) {
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
	fun `M-004 머신의 'on error'가 제대로 호출되는가`()
	{
		val checklist = mutableMapOf(
			"on error action" to false,
			"on error callback" to false
		)
		
		KotlmataMachine("M-004", logLevel) {
			on transition { _, _, _ ->
				throw Exception("on error callback")
			}
			on error {
				checklist[throwable.message!!] = true
			}
			
			"state1" {
				input action {
					throw Exception("on error action")
				}
			}
			"state2" {}
			
			"state1" x any %= "state2"
			
			start at "state1"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-005 머신의 모든 유형의 전이규칙이 잘 정의되는가`()
	{
		val checklist = mutableMapOf(
			"state x signal" to false,
			"state x type" to false,
			"state x signals" to false,
			"state x any" to false,
			"state x except" to false,
			"state x predicate" to false,
			"state x range" to false,
			"states x signal" to false,
			"states x type" to false,
			"states x signals" to false,
			"states x any" to false,
			"states x except" to false,
			"states x predicate" to false,
			"states x range" to false,
			"any x signal" to false,
			"any x type" to false,
			"any x signals" to false,
			"any x any" to false,
			"any x except" to false,
			"any x predicate" to false,
			"any x range" to false,
			"except x signal" to false,
			"except x type" to false,
			"except x signals" to false,
			"except x any" to false,
			"except x except" to false,
			"except x predicate" to false,
			"except x range" to false
		)
		
		KotlmataMutableMachine("M-005", logLevel) {
			"0" {}
			"1" action { checklist["state x signal"] = true }
			"2" action { checklist["state x type"] = true }
			"3" action { checklist["state x signals"] = true }
			"4" action { checklist["state x any"] = true }
			"5" action { checklist["state x except"] = true }
			"6" action { checklist["state x predicate"] = true }
			"7" action { checklist["state x range"] = true }
			
			"1a" action { checklist["states x signal"] = true }
			"1b" action { checklist["states x signal"] = true }
			"2a" action { checklist["states x type"] = true }
			"2b" action { checklist["states x type"] = true }
			"3a" action { checklist["states x signals"] = true }
			"3b" action { checklist["states x signals"] = true }
			"4a" action { checklist["states x any"] = true }
			"4b" action { checklist["states x any"] = true }
			"5a" action { checklist["states x except"] = true }
			"5b" action { checklist["states x except"] = true }
			"6a" action { checklist["states x predicate"] = true }
			"6b" action { checklist["states x predicate"] = true }
			"7a" action { checklist["states x range"] = true }
			"7b" action { checklist["states x range"] = true }
			
			"1c" action { checklist["any x signal"] = true }
			"2c" action { checklist["any x type"] = true }
			"3c" action { checklist["any x signals"] = true }
			"4c" action { checklist["any x any"] = true }
			"5c" action { checklist["any x except"] = true }
			"6c" action { checklist["any x predicate"] = true }
			"7c" action { checklist["any x range"] = true }
			
			"1d" action { checklist["except x signal"] = true }
			"2d" action { checklist["except x type"] = true }
			"3d" action { checklist["except x signals"] = true }
			"4d" action { checklist["except x any"] = true }
			"5d" action { checklist["except x except"] = true }
			"6d" action { checklist["except x predicate"] = true }
			"7d" action { checklist["except x range"] = true }
			
			"0" x 0 %= "1"
			"1" x Int::class %= "2"
			"2" x (2 OR 3) %= "3"
			"3" x any %= "4"
			"4" x any(3, 5) %= "5"
			"5" x { s: Int -> s == 5 } %= "6"
			"6" x 6..7 %= "7"
			
			("7" AND "8") x 0 %= "1a"
			("1a" AND "1b") x Int::class %= "2b"
			("2a" AND "2b") x (2 OR 3) %= "3a"
			("3a" AND "3b") x any %= "4b"
			("4a" AND "4b") x any(3, 5) %= "5b"
			("5a" AND "5b") x { s: Int -> s == 5 } %= "6a"
			("6a" AND "6b") x 6..7 %= "7b"
			
			any x 0 %= "1c"
			any x Int::class %= "2c"
			any x (2 OR 3) %= "3c"
			any x any %= "4c"
			any x { s: Int -> s == 5 } %= "6c"
			any x 6..7 %= "7c"
			
			any("6c", "1d") x "0" %= "1d"
			any("3c", "4c", "3d", "4d") x String::class %= "2d"
			any("1d", "3d") x ("2" OR "22") %= "3d"
			any("4d", "6d") x { s: String -> s == "5" } %= "6d"
			any("5d", "7d") x "6".."7" %= "7d"
			
			start at "0"
		}.also { machine ->
			machine.input(0)
			machine.input(1)
			machine.input(2)
			machine.input(3)
			machine.input(4)
			machine.input(5)
			machine.input(6)
			
			machine.input(0)
			machine.input(1)
			machine.input(2)
			machine.input(3)
			machine.input(4)
			machine.input(5)
			machine.input(6)
			
			machine.input(0)
			machine.input(1)
			machine.input(2)
			machine.input("3")
			machine {
				any x any("3", "5") %= "5c"
			}
			machine.input("4")
			machine.input(5)
			machine.input(6)
			
			machine.input("0")
			machine.input("1")
			machine.input("2")
			machine {
				any("2d", "4d") x any %= "4d"
			}
			machine.input("3")
			machine {
				any("3d", "5d") x any("3", "5") %= "5d"
			}
			machine.input("4")
			machine.input("5")
			machine.input("6")
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-006 머신의 체인규칙이 잘 동작하는가`()
	{
		val checklist = mutableMapOf(
			"signal" to false,
			"type" to false,
			"signals" to false,
			"predicate" to false,
			"range" to false,
			"any" to false,
			"except" to false
		)
		
		KotlmataMachine("M-006", logLevel) {
			
			"a" {}
			"b" {}
			"c" {
				entry via 0 action {
					checklist["signal"] = true
				}
				entry via Char::class action {
					checklist["type"] = true
				}
				entry via "1" action {
					checklist["signals"] = true
				}
				entry via { s: Int -> s in 1..4 } action {
					checklist["predicate"] = true
				}
				entry via 5..9 action {
					checklist["range"] = true
				}
			}
			
			"d" {}
			"e" {}
			"f" action {
				checklist["any"] = true
			}
			"g" {}
			"h" via 2 action {
				checklist["except"] = true
			}
			
			chain from "a" to "b" to "c" to "a" via 0
			chain from "a" to "b" to "c" to "a" via Char::class
			chain from "a" to "b" to "c" to "a" via ("0" OR "1" OR "2")
			chain from "a" to "b" to "c" to "a" via { s: Int -> s in 1..4 }
			chain from "a" to "b" to "c" to "d" via 5..9
			
			chain from "d" to "e" to "f" via any
			chain from "f" to "g" to "h" via any(0, String::class)
			
			start at "a"
		}.also { machine ->
			machine.input(0)
			machine.input(0)
			machine.input(0)
			
			machine.input('0')
			machine.input('0')
			machine.input('0')
			
			machine.input("0")
			machine.input("1")
			machine.input("2")
			
			machine.input(1)
			machine.input(2)
			machine.input(3)
			
			machine.input(5)
			machine.input(6)
			machine.input(7)
			
			machine.input("unknown")
			machine.input(Any())
			
			machine.input(0)
			machine.input("0")
			machine.input(1)
			machine.input(2)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-007 머신 업데이트 시 현재 상태 체크가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"current is a" to false,
			"current is b" to false
		)
		
		KotlmataMutableMachine("M-007", logLevel) {
			"a" {}
			"b" {}
			
			"a" x 0 %= "b"
			
			start at "a"
		}.also { machine ->
			machine {
				if (currentState == "a") checklist["current is a"] = true
			}
			machine.input(0)
			machine {
				if (currentState == "b") checklist["current is b"] = true
			}
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-008 머신 업데이트 시 상태 'has' 체크가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"exists" to false,
			"none" to false
		)
		
		KotlmataMutableMachine("M-008", logLevel) {
			"a" {}
			start at "a"
		}.also { machine ->
			machine {
				if (has state "a") checklist["exists"] = true
				if (!(has state "b")) checklist["none"] = true
			}
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-009 머신 업데이트 시 상태 삭제가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"exists" to false,
			"deleted" to false
		)
		
		KotlmataMutableMachine("M-009", logLevel) {
			"a" {}
			start at "a"
		}.also { machine ->
			machine {
				if (has state "a") checklist["exists"] = true
				delete state "a"
				if (!(has state "a")) checklist["deleted"] = true
			}
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-010 머신 업데이트 시 전이규칙 'has' 체크가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"state x signal" to false,
			"state x type" to false,
			"state x any" to false,
			"state x predicate" to false,
			"any x signal" to false,
			"any x type" to false,
			"any x any" to false,
			"any x predicate" to false
		)
		
		val predicate = { s: Int -> s == 0 }
		
		KotlmataMutableMachine("M-010", logLevel) {
			"state" {}
			
			"state" x 0 %= self
			"state" x Int::class %= self
			"state" x any %= self
			"state" x predicate %= self
			any x 0 %= self
			any x Int::class %= self
			any x any %= self
			any x predicate %= self
			
			start at "state"
		}.also { machine ->
			machine {
				if (has rule ("state" x 0)) checklist["state x signal"] = true
				if (has rule ("state" x Int::class)) checklist["state x type"] = true
				if (has rule ("state" x any)) checklist["state x any"] = true
				if (has rule ("state" x predicate)) checklist["state x predicate"] = true
				if (has rule (any x 0)) checklist["any x signal"] = true
				if (has rule (any x Int::class)) checklist["any x type"] = true
				if (has rule (any x any)) checklist["any x any"] = true
				if (has rule (any x predicate)) checklist["any x predicate"] = true
			}
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-011 머신 업데이트 시 전이규칙 삭제가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"state x signal" to true,
			"state x type" to true,
			"state x any" to true,
			"state x predicate" to true,
			"any x signal" to true,
			"any x type" to true,
			"any x any" to true,
			"any x predicate" to true
		)
		
		val predicate = { s: Int -> s == 0 }
		
		KotlmataMutableMachine("M-011", logLevel) {
			val template: StateTemplate<String> = { state ->
				entry action {
					checklist[state] = false
				}
			}
			
			"state" {}
			"state x signal" extends template
			"state x type" extends template
			"state x any" extends template
			"state x predicate" extends template
			"any x signal" extends template
			"any x type" extends template
			"any x any" extends template
			"any x predicate" extends template
			
			"state" x 0 %= "state x signal"
			"state" x Int::class %= "state x type"
			"state" x any %= "state x any"
			"state" x predicate %= "state x predicate"
			any x 0 %= "any x signal"
			any x Int::class %= "any x type"
			any x any %= "any x any"
			any x predicate %= "any x predicate"
			
			start at "state"
		}.also { machine ->
			machine {
				delete rule ("state" x 0)
				delete rule ("state" x Int::class)
				delete rule ("state" x any)
				delete rule ("state" x predicate)
				delete rule (any x 0)
				delete rule (any x Int::class)
				delete rule (any x any)
				delete rule (any x predicate)
			}
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-012 머신의 동기입력이 잘 동작하는가`()
	{
		val checklist = mutableMapOf(
			"input signal" to false,
			"input signal type" to false,
			"input signal payload" to false,
			"input signal type payload" to false,
			"entry signal" to false,
			"entry signal type" to false,
			"entry signal payload" to false,
			"entry signal type payload" to false
		)
		
		KotlmataMachine("M-012", logLevel) {
			"a" {
				input signal "0" function {
					"1"
				}
				input signal "1" function {
					checklist["input signal"] = true
					"2" `as` CharSequence::class
				}
				input signal CharSequence::class function {
					checklist["input signal type"] = true
					"3" with "4"
				}
				input signal "3" function {
					checklist["input signal payload"] = true
					payload as String `as` String::class with 0
				}
				input signal String::class function {
					payload
				}
				entry via 0 action {
					checklist["entry signal type payload"] = true
				}
			}
			"b" {
				entry via 0 function {
					checklist["input signal type payload"] = true
					"5"
				}
				entry via "5" function {
					checklist["entry signal"] = true
					"6" `as` CharSequence::class
				}
				entry via CharSequence::class function {
					checklist["entry signal type"] = true
					"7" with "8"
				}
				entry via "7" function {
					checklist["entry signal payload"] = true
					payload as String `as` String::class with 0
				}
				entry via String::class function {
					payload
				}
			}
			
			"a" x 0 %= "b"
			"b" x any %= "b"
			"b" x 0 %= "a"
			
			start at "a"
		}.also { machine ->
			machine.input("0")
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-013 머신의 'stay' 리턴이 잘 동작하는가`()
	{
		val checklist = mutableMapOf(
			"stay" to true
		)
		
		KotlmataMachine("M-013", logLevel) {
			"a" {
				input function {
					stay
				}
			}
			"b" action {
				checklist["stay"] = false
			}
			
			"a" x any %= "b"
			
			start at "a"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-014 머신의 'transitionCount'가 잘 전달되는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"exit" to false,
			"entry" to false
		)
		
		KotlmataMachine("M-014", logLevel) {
			"a" {
				input action {
					if (transitionCount == 0L)
					{
						checklist["input"] = true
					}
				}
				exit action {
					if (transitionCount == 0L)
					{
						checklist["exit"] = true
					}
				}
			}
			"b" {
				entry action {
					if (transitionCount == 1L)
					{
						checklist["entry"] = true
					}
				}
			}
			
			"a" x 0 %= "b"
			
			start at "a"
		}.also { machine ->
			machine.input(0)
		}
		
		checklist.verify()
	}
	
	@Test
	fun `M-015 머신 해제가 잘 동작하는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"update" to false,
			"clear" to false,
			"alive" to false,
			"released" to false
		)
		
		lateinit var reference: WeakReference<Any>
		
		KotlmataMutableMachine("M-015", logLevel) {
			val resource = Any()
			
			"a" {
				input signal 0 action {
					reference = WeakReference(resource)
					checklist["input"] = true
				}
				input signal 1 action {
					checklist["input"] = false
				}
				on clear {
					checklist["clear"] = true
				}
			}
			
			start at "a"
		}.also { machine ->
			machine.input(0)
			machine {
				checklist["update"] = true
			}
			System.gc()
			if (reference.get() != null)
			{
				checklist["alive"] = true
			}
			
			machine.release()
			
			machine.input(1)
			machine {
				checklist["update"] = false
			}
			System.gc()
			if (reference.get() == null)
			{
				checklist["released"] = true
			}
		}
		
		checklist.verify()
	}
	
	@Test
	fun `D-001 데몬의 모든 유형의 생성이 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"daemon" to false,
			"daemon extends" to false,
			"daemon lazy" to false,
			"daemon lazy extends" to false,
			"mutable daemon" to false,
			"mutable daemon extends" to false,
			"mutable daemon lazy" to false,
			"mutable daemon lazy extends" to false
		)
		
		val latch = Latch(8)
		
		val template: DaemonTemplate = {
			latch()
		}
		
		val d1 = KotlmataDaemon("D-001-1", logLevel) {
			latch()
			"state" action {
				checklist["daemon"] = true
			}
			start at "state"
		}
		val d2 = KotlmataDaemon("D-001-2", logLevel) extends template by {
			"state" action {
				checklist["daemon extends"] = true
			}
			start at "state"
		}
		val d3 by KotlmataDaemon.lazy("D-001-3", logLevel) {
			latch()
			"state" action {
				checklist["daemon lazy"] = true
			}
			start at "state"
		}
		val d4 by KotlmataDaemon.lazy("D-001-4", logLevel) extends template by {
			"state" action {
				checklist["daemon lazy extends"] = true
			}
			start at "state"
		}
		val d5 = KotlmataMutableDaemon("D-001-5", logLevel) {
			latch()
			"state" action {
				checklist["mutable daemon"] = true
			}
			start at "state"
		}
		val d6 = KotlmataMutableDaemon("D-001-6", logLevel) extends template by {
			"state" action {
				checklist["mutable daemon extends"] = true
			}
			start at "state"
		}
		val d7 by KotlmataMutableDaemon.lazy("D-001-7", logLevel) {
			latch()
			"state" action {
				checklist["mutable daemon lazy"] = true
			}
			start at "state"
		}
		val d8 by KotlmataMutableDaemon.lazy("D-001-8", logLevel) extends template by {
			"state" action {
				checklist["mutable daemon lazy extends"] = true
			}
			start at "state"
		}
		
		d1.run()
		d2.run()
		d3.run()
		d4.run()
		d5.run()
		d6.run()
		d7.run()
		d8.run()
		
		d1.terminate()
		d2.terminate()
		d3.terminate()
		d4.terminate()
		d5.terminate()
		d6.terminate()
		d7.terminate()
		d8.terminate()
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-002 데몬의 생명주기 콜백이 잘 호출되는가`()
	{
		val checklist = mutableMapOf(
			"on create" to false,
			"on create catch" to false,
			"on create final" to false,
			"on start" to false,
			"on start catch" to false,
			"on start final" to false,
			"on pause" to false,
			"on pause catch" to false,
			"on pause final" to false,
			"on stop" to false,
			"on stop catch" to false,
			"on stop final" to false,
			"on resume" to false,
			"on resume catch" to false,
			"on resume final" to false,
			"on finish" to false,
			"on finish catch" to false,
			"on finish final" to false,
			"on destroy" to false,
			"on destroy catch" to false,
			"on destroy final" to false,
			"on fatal" to true
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-002", logLevel) {
			latch()
			on create {
				checklist["on create"] = true
				throw Exception()
			} catch {
				checklist["on create catch"] = true
			} finally {
				checklist["on create final"] = true
			}
			on start {
				if (payload == 0)
					checklist["on start"] = true
				throw Exception()
			} catch {
				if (payload == 0)
					checklist["on start catch"] = true
			} finally {
				if (payload == 0)
					checklist["on start final"] = true
			}
			on pause {
				if (payload == 0)
					checklist["on pause"] = true
				throw Exception()
			} catch {
				if (payload == 0)
					checklist["on pause catch"] = true
			} finally {
				if (payload == 0)
					checklist["on pause final"] = true
			}
			on stop {
				if (payload == 1)
					checklist["on stop"] = true
				throw Exception()
			} catch {
				if (payload == 1)
					checklist["on stop catch"] = true
			} finally {
				if (payload == 1)
					checklist["on stop final"] = true
			}
			on resume {
				if (payload == 2)
					checklist["on resume"] = true
				throw Exception()
			} catch {
				if (payload == 2)
					checklist["on resume catch"] = true
			} finally {
				if (payload == 2)
					checklist["on resume final"] = true
			}
			on finish {
				if (payload == 3)
					checklist["on finish"] = true
				throw Exception()
			} catch {
				if (payload == 3)
					checklist["on finish catch"] = true
			} finally {
				if (payload == 3)
					checklist["on finish final"] = true
				throw Exception()
			}
			on destroy {
				checklist["on destroy"] = true
				throw Exception()
			} catch {
				checklist["on destroy catch"] = true
			} finally {
				checklist["on destroy final"] = true
			}
			on fatal {
				checklist["on fatal"] = false
			}
			
			"a" {}
			
			start at "a"
		}.also { daemon ->
			daemon.pause(0)
			daemon.stop(1)
			daemon.run(2)
			daemon.terminate(3)
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-003 데몬의 입력 우선순위가 잘 동작하는가`()
	{
		val checklist = mutableMapOf(
			"priority 0" to false,
			"priority 1" to true
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-003", logLevel) { daemon ->
			latch()
			"a" {
				input signal 0 action {
					checklist["priority 0"] = true
				}
				input signal 1 action {
					checklist["priority 1"] = !checklist["priority 1"]!!
				}
				input signal 2 action {
					checklist["priority 1"] = !checklist["priority 1"]!!
					daemon.terminate()
				}
			}
			
			start at "a"
		}.also { daemon ->
			daemon.pause()
			daemon.input(1, priority = 1)
			daemon.input(2, priority = 1)
			daemon.input(0, priority = 0)
			daemon.run()
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-004 데몬 'stop' 시 입력 삭제, 동기입력 유지 등이 잘 동작하는가`()
	{
		val checklist = mutableMapOf(
			"should delete" to true,
			"should ignore" to true,
			"should alive" to false,
			"sync" to false
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-004", logLevel) { daemon ->
			latch()
			"a" {
				input signal 0 function {
					daemon.input("should delete")
					daemon.stop()
					daemon.input("should ignore")
					daemon.run()
					daemon.input("should alive")
					"sync"
				}
				input signal "should delete" action { signal ->
					checklist[signal] = false
				}
				input signal "should ignore" action { signal ->
					checklist[signal] = false
				}
				input signal "sync" action { signal ->
					checklist[signal] = true
				}
				input signal "should alive" action { signal ->
					checklist[signal] = true
					daemon.terminate()
				}
			}
			
			start at "a"
		}.also { daemon ->
			daemon.run()
			daemon.input(0)
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-005 데몬 소멸 시 자원 정리가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"alive" to false,
			"clear" to false
		)
		
		val latch = Latch(1)
		val create = CountDownLatch(1)
		val destroy = CountDownLatch(1)
		
		lateinit var reference: WeakReference<Any>
		
		val daemon = KotlmataDaemon("D-005", logLevel) {
			latch()
			val resource = Any()
			
			on create {
				reference = WeakReference(resource)
				create.countDown()
			}
			on destroy {
				destroy.countDown()
			}
			"a" {}
			
			start at "a"
		}
		
		create.await()
		
		System.gc()
		if (reference.get() != null)
		{
			checklist["alive"] = true
		}
		
		daemon.terminate()
		
		destroy.await()
		
		System.gc()
		if (reference.get() == null)
		{
			checklist["clear"] = true
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-006 데몬 초기화 도중 에러 발생 시 처리가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"on create" to true,
			"on destroy" to true,
			"on error" to true,
			"on fatal" to false,
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-006", logLevel) {
			latch()
			on create {
				checklist["on create"] = false
			}
			on destroy {
				checklist["on destroy"] = false
			}
			on error {
				checklist["on error"] = false
			}
			on fatal {
				checklist["on fatal"] = true
			}
			
			start at "a"
		}.also { daemon ->
			daemon.run()
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-007 데몬 'on create'에서 에러 발생 시 처리가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"on create" to false,
			"on start" to true,
			"on finish" to true,
			"on destroy" to false,
			"on fatal" to false
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-007", logLevel) {
			latch()
			on create {
				checklist["on create"] = true
				throw Exception("D-007 on create 예외 발생")
			}
			on start {
				checklist["on start"] = false
			}
			on finish {
				checklist["on finish"] = false
			}
			on destroy {
				checklist["on destroy"] = true
			}
			on fatal {
				checklist["on fatal"] = true
			}
			
			"a" { /* empty */ }
			
			start at "a"
		}.also { daemon ->
			daemon.run()
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-008 데몬 'on start'에서 에러 발생 시 처리가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"on create" to false,
			"on start" to false,
			"on finish" to true,
			"on destroy" to false,
			"on fatal" to false
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-008", logLevel) {
			latch()
			on create {
				checklist["on create"] = true
			}
			on start {
				checklist["on start"] = true
				throw Exception("D-008 on start 예외 발생")
			}
			on finish {
				checklist["on finish"] = false
			}
			on destroy {
				checklist["on destroy"] = true
			}
			on fatal {
				checklist["on fatal"] = true
			}
			
			"a" { /* empty */ }
			
			start at "a"
		}.also { daemon ->
			daemon.run()
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-009 데몬 'on finish'에서 에러 발생 시 처리가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"on create" to false,
			"on start" to false,
			"on finish" to false,
			"on destroy" to false,
			"on fatal" to true
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-009", logLevel) {
			latch()
			on create {
				checklist["on create"] = true
			}
			on start {
				checklist["on start"] = true
			}
			on finish {
				checklist["on finish"] = true
				throw Exception("D-009 on finish 예외 발생")
			}
			on destroy {
				checklist["on destroy"] = true
			}
			on fatal {
				checklist["on fatal"] = false
			}
			
			"a" { /* empty */ }
			
			start at "a"
		}.also { daemon ->
			daemon.run()
			daemon.terminate()
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-010 데몬의 상태동작에서 에러 발생 시 처리가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"on create" to false,
			"on start" to false,
			"on finish" to false,
			"on destroy" to false,
			"on fatal" to false
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-010", logLevel) {
			latch()
			on create {
				checklist["on create"] = true
			}
			on start {
				checklist["on start"] = true
			}
			on finish {
				checklist["on finish"] = true
			}
			on destroy {
				checklist["on destroy"] = true
			}
			on fatal {
				checklist["on fatal"] = true
			}
			
			"a" {
				input action {
					throw Exception("D-010 상태동작에서 예외 발생")
				}
			}
			"b" { /* empty */ }
			
			"a" x any %= "b"
			
			start at "a"
		}.also { daemon ->
			daemon.run()
			daemon.input(0)
		}
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-011 데몬 인터럽트 시 처리가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"on create" to false,
			"on start" to false,
			"on finish" to false,
			"on destroy" to false,
			"on fatal" to true
		)
		
		val latch = Latch(1)
		val started = CountDownLatch(1)
		
		lateinit var thread: Thread
		
		KotlmataDaemon("D-011", logLevel) {
			latch()
			thread = Thread.currentThread()
			
			on create {
				checklist["on create"] = true
			}
			on start {
				checklist["on start"] = true
			}
			on finish {
				checklist["on finish"] = true
			}
			on destroy {
				checklist["on destroy"] = true
			}
			on fatal {
				checklist["on fatal"] = false
			}
			
			"a" {
				input signal 1 action {
					started.countDown()
				}
			}
			
			start at "a"
		}.also { daemon ->
			daemon.run()
			daemon.input(0)
			daemon.input(1)
			daemon.input(2)
			daemon.input(3)
		}
		
		started.await()
		
		thread.interrupt()
		
		latch.await()
		checklist.verify()
	}
	
	@Test
	fun `D-012 데몬 종료 시 상태의 'on clear'가 잘 호출되는가`()
	{
		val checklist = mutableMapOf(
			"on clear" to false,
			"on clear catch" to false,
			"on clear final" to false
		)
		
		val latch = Latch(1)
		
		KotlmataDaemon("D-012", logLevel) {
			latch()
			"a" {
				on clear {
					checklist["on clear"] = true
					throw Exception("D-012 on clear 호출")
				} catch {
					checklist["on clear catch"] = true
				} finally {
					checklist["on clear final"] = true
				}
			}
			
			start at "a"
		}.also { daemon ->
			daemon.run()
			daemon.terminate()
		}
		
		latch.await()
		checklist.verify()
	}
}
