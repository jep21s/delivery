package microarch.delivery

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
class ApplicationProperties(
    val grpc: Grpc = Grpc(),
    val kafka: Kafka = Kafka(),
) {
    class Grpc(
        val geoService: GeoService = GeoService(),
    ) {
        class GeoService(
            val host: String = "0.0.0.0",
            val port: Int = 5004,
        )
    }

    class Kafka(
        val basketEventsTopic: String = "basket.events",
        val orderEventsTopic: String = "order.events",
    )
}
