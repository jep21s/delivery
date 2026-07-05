package microarch.delivery

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

class KafkaContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertyValues
            .of("spring.kafka.bootstrap-servers=${kafka.bootstrapServers}")
            .applyTo(applicationContext.environment)
    }

    companion object {
        private val kafka: KafkaContainer =
            KafkaContainer(DockerImageName.parse("apache/kafka:3.7.2"))
                .apply { start() }
    }
}
