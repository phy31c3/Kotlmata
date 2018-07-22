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
		
		Kotlmata release {
			/* do nothing */
		}
		
		val action = {}
		
		KotlmataState new "tag" set {
			entry action action
			entry via "signal" action action
			input signal "signal" action action
			exit action action
		}
	}
}