package microarch.delivery

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(
    initializers = [
        PostgresContextInitializer::class,
        KafkaContextInitializer::class,
    ],
)
@Sql(
    statements = ["TRUNCATE TABLE assignments, couriers, orders RESTART IDENTITY CASCADE"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
)
abstract class BaseIntegrationTest
