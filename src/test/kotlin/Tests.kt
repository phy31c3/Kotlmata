import kr.co.plasticcity.kotlmata.Kotlmata
import kr.co.plasticcity.kotlmata.KotlmataState
import org.junit.Test

class Tests
{
	@Test
	fun basicTest()
	{
		Kotlmata init {
			debugLogger = ::println
			errorLogger = ::error
		}
		
		val action = {}
		
		KotlmataState new "key" set {
			entry action action
			entry via "signal" action action
			event input "signal" action action
			exit action action
		}
		
		Kotlmata release {
			/* do nothing */
		}
	}
}