package microarch.delivery

import clients.geo.GeoProto
import io.grpc.Server
import io.grpc.ServerBuilder
import microarch.delivery.adapters.out.grpc.geo.FakeGeoService
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * Поднимает in-JVM gRPC-стаб сервиса Geo для полноконтекстных Spring-тестов
 * (наследников [BaseIntegrationTest]) и пробрасывает host/port в свойства
 * приложения, чтобы [microarch.delivery.adapters.out.grpc.geo.GeoClientImpl]
 * ходил в рабочий стаб, а не в несуществующий Geo.
 */
class GeoGrpcContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertyValues
            .of(
                "app.grpc.geo-service.host=localhost",
                "app.grpc.geo-service.port=${geoServer.port}",
            ).applyTo(applicationContext.environment)
    }

    companion object {
        private val geoServer: Server =
            ServerBuilder
                .forPort(0)
                .addService(
                    FakeGeoService(
                        location =
                            GeoProto.Location
                                .newBuilder()
                                .setX(5)
                                .setY(5)
                                .build(),
                    ),
                ).build()
                .apply { start() }
    }
}
