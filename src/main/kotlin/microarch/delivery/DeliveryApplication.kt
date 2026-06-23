package microarch.delivery

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableJpaRepositories(basePackages = ["microarch.delivery.adapters.out.postgres"])
@EntityScan(
    basePackages = [
        "microarch.delivery.core.domain.model",
        "microarch.delivery.adapters.out.postgres.outbox",
    ],
)
@SpringBootApplication
class DeliveryApplication

fun main(args: Array<String>) {
    runApplication<DeliveryApplication>(*args)
}
