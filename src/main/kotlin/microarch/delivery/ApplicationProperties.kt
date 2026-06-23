package microarch.delivery

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app")
class ApplicationProperties {
    val grpc: Grpc = Grpc()

    val kafka: Kafka = Kafka()

    class Grpc {
        val geoService: GeoService = GeoService()

        class GeoService {
            var host: String = ""
            var port: Int = 0
        }
    }

    class Kafka {
        var basketEventsTopic: String = ""
        var orderEventsTopic: String = ""
    }
}
