import kr.co.plasticcity.kotlmata.Kotlmata
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
	}
}