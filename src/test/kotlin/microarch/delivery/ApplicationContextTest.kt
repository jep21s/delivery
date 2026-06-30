package microarch.delivery

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(
    initializers = [
        PostgresContextInitializer::class,
        KafkaContextInitializer::class,
    ],
)
class ApplicationContextTest {
    @Test
    fun `context loads`() {
        // start context
    }
}
