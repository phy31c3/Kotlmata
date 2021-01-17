@file:Suppress("NonAsciiCharacters")

import kr.co.plasticcity.kotlmata.*
import org.junit.Before
import org.junit.Test
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch

class Tests
{
	private fun Map<String, Boolean>.verify() = forEach { (key, pass) ->
		assert(pass) {
			"\"$key\" failed"
		}
	}
	
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
	fun `#001 상태의 모든 유형의 진입동작이 제대로 불리는가`()
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
		KotlmataMachine("#001", 0) {
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
	fun `#002 상태의 모든 유형의 입력동작이 제대로 불리는가`()
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
		KotlmataMachine("#002", 0) {
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
	fun `#003 상태의 모든 유형의 퇴장동작이 제대로 불리는가`()
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
		KotlmataMachine("#003", 0) {
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
	fun `#004 상태의 'function'이 제대로 동작하는가`()
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
		
		KotlmataMachine("#004", 0) {
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
	fun `#005 상태의 'function'에서 타입지정과 payload 전달이 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"input as" to false,
			"entry as with" to false,
			"input with" to false
		)
		
		KotlmataMachine("#005", 0) {
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
	fun `#006 상태의 'intercept'가 제대로 동작하는가`()
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
		
		KotlmataMachine("#006", 0) {
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
	fun `#007 상태의 'on error'가 제대로 호출 되는가`()
	{
		val checklist = mutableMapOf(
			"on error" to false
		)
		
		KotlmataMachine("#007", 0) {
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
	fun `#008 상태 업데이트 시 상태동작이 잘 교체되는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"exit" to false,
			"entry" to false
		)
		
		val predicate = { s: Int -> s < 10 }
		
		KotlmataMutableMachine("#008", 0) {
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
	fun `#009 상태 업데이트 시 상태동작이 잘 삭제되는가`()
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
		
		KotlmataMutableMachine("#009", 0) {
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
	fun `#010 상태 템플릿이 잘 적용되는가`()
	{
		val checklist = mutableMapOf(
			"input" to false,
			"on error" to false
		)
		
		KotlmataMachine("#010", 0) {
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
	fun `#011 머신의 간략한 상태 정의가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"entry" to false,
			"entry signal" to false,
			"entry type" to false,
			"entry predicate" to false,
			"entry range" to false
		)
		
		KotlmataMachine("#011", 0) {
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
	fun `#012 머신의 모든 유형의 생성이 잘 되는가`()
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
		
		val m1 = KotlmataMachine("#012-1", 0) {
			"state" {
				input action {
					checklist["machine"] = true
				}
			}
			start at "state"
		}
		val m2 = KotlmataMachine("#012-2", 0) extends base("machine extends") by {
			start at "state"
		}
		val m3 by KotlmataMachine.lazy("#012-3", 0) {
			"state" {
				input action {
					checklist["machine lazy"] = true
				}
			}
			start at "state"
		}
		val m4 by KotlmataMachine.lazy("#012-4", 0) extends base("machine lazy extends") by {
			start at "state"
		}
		val m5 = KotlmataMutableMachine("#012-5", 0) {
			"state" {
				input action {
					checklist["mutable machine"] = true
				}
			}
			start at "state"
		}
		val m6 = KotlmataMutableMachine("#012-6", 0) extends base("mutable machine extends") by {
			start at "state"
		}
		val m7 by KotlmataMutableMachine.lazy("#012-7", 0) {
			"state" {
				input action {
					checklist["mutable machine lazy"] = true
				}
			}
			start at "state"
		}
		val m8 by KotlmataMutableMachine.lazy("#012-8", 0) extends base("mutable machine lazy extends") by {
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
	fun `#013 머신의 'on transition'이 제대로 호출 되는가`()
	{
		val checklist = mutableMapOf(
			"on transition" to false,
			"on transition catch" to false,
			"on transition final" to false
		)
		
		KotlmataMachine("#013", 0) {
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
	fun `#014 머신의 'on error'가 제대로 호출 되는가`()
	{
		val checklist = mutableMapOf(
			"on error action" to false,
			"on error callback" to false
		)
		
		KotlmataMachine("#014", 0) {
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
	fun `#015 머신의 모든 유형의 전이규칙이 잘 정의되는가`()
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
		
		KotlmataMutableMachine("#015", 0) {
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
	fun `#016 머신의 체인규칙이 잘 동작하는가`()
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
		
		KotlmataMachine("#016", 0) {
			
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
	fun `#017 머신 업데이트 시 현재 상태 체크가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"current is a" to false,
			"current is b" to false
		)
		
		KotlmataMutableMachine("#017", 0) {
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
	fun `#018 머신 업데이트 시 상태 'has' 체크가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"exists" to false,
			"none" to false
		)
		
		KotlmataMutableMachine("#018", 0) {
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
	fun `#019 머신 업데이트 시 상태 삭제가 잘 되는가`()
	{
		val checklist = mutableMapOf(
			"exists" to false,
			"deleted" to false
		)
		
		KotlmataMutableMachine("#019", 0) {
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
	fun `#020 머신 업데이트 시 전이규칙 'has' 체크가 잘 되는가`()
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
		
		KotlmataMutableMachine("#020", 0) {
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
	fun `#021 머신 업데이트 시 전이규칙 삭제가 잘 되는가`()
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
		
		KotlmataMutableMachine("#021", 0) {
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
	fun `#022 데몬의 모든 유형의 생성이 잘 되는가`()
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
		
		val latch = CountDownLatch(8)
		
		val base: DaemonBase = {
			on destroy {
				latch.countDown()
			}
		}
		
		val d1 = KotlmataDaemon("#022-1", 0) {
			on destroy {
				latch.countDown()
			}
			"state" action {
				checklist["daemon"] = true
			}
			start at "state"
		}
		val d2 = KotlmataDaemon("#022-2", 0) extends base by {
			"state" action {
				checklist["daemon extends"] = true
			}
			start at "state"
		}
		val d3 by KotlmataDaemon.lazy("#022-3", 0) {
			on destroy {
				latch.countDown()
			}
			"state" action {
				checklist["daemon lazy"] = true
			}
			start at "state"
		}
		val d4 by KotlmataDaemon.lazy("#022-4", 0) extends base by {
			"state" action {
				checklist["daemon lazy extends"] = true
			}
			start at "state"
		}
		val d5 = KotlmataMutableDaemon("#022-5", 0) {
			on destroy {
				latch.countDown()
			}
			"state" action {
				checklist["mutable daemon"] = true
			}
			start at "state"
		}
		val d6 = KotlmataMutableDaemon("#022-6", 0) extends base by {
			"state" action {
				checklist["mutable daemon extends"] = true
			}
			start at "state"
		}
		val d7 by KotlmataMutableDaemon.lazy("#022-7", 0) {
			on destroy {
				latch.countDown()
			}
			"state" action {
				checklist["mutable daemon lazy"] = true
			}
			start at "state"
		}
		val d8 by KotlmataMutableDaemon.lazy("#022-8", 0) extends base by {
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
		
		val daemon by KotlmataMutableDaemon.lazy("d1", 3) extends base by { daemon ->
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
			any("chain1", "chain2") x ("a" OR "b" OR "c") %= "state1"
			"state1" x "signal" %= "state6"
			"state6" x String::class %= self
			"state6" x "goToState7" %= "state7"
			"state7" x "goToState1" %= "state1"
			("state1" AND "state2") x ("d" OR "e") %= "state8"
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
}
