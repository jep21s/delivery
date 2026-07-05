package microarch.delivery

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

private class DeliveryPostgresContainer(
    image: DockerImageName,
) : PostgreSQLContainer<DeliveryPostgresContainer>(image)

class PostgresContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertyValues
            .of(
                "spring.datasource.url=${postgres.jdbcUrl}",
                "spring.datasource.username=${postgres.username}",
                "spring.datasource.password=${postgres.password}",
            ).applyTo(applicationContext.environment)
    }

    companion object {
        private val postgres: DeliveryPostgresContainer =
            DeliveryPostgresContainer(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("delivery")
                .withUsername("username")
                .withPassword("secret")
                .apply { start() }
    }
}
