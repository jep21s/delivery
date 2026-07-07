package microarch.delivery.adapters.out.grpc.geo

import arrow.core.right
import clients.geo.GeoProto
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.Status
import libs.errs.LogicError
import microarch.delivery.ApplicationProperties
import microarch.delivery.core.domain.model.LocationValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Интеграционный тест адаптера [GeoClientImpl]: поднимает настоящий gRPC-сервер
 * (in-JVM, на динамическом порту) и делает реальный gRPC-вызов.
 */
class GeoClientImplIntegrationTest {
    @Test
    fun `getLocation returns LocationValue from Geo response over real gRPC call`() {
        val fake = FakeGeoService().apply { location = location(5, 8) }
        withClient(fake) { client ->
            val result =
                client.getLocation(
                    country = "Россия",
                    city = "Москва",
                    street = "Тверская",
                    house = "1",
                    apartment = "2",
                )

            assertThat(result).isEqualTo(LocationValue.createOrThrow(5, 8).right())
        }
    }

    @Test
    fun `getLocation returns Left when Geo service returns a gRPC error`() {
        val fake = FakeGeoService().apply { error = Status.UNAVAILABLE.withDescription("down") }
        withClient(fake) { client ->
            val result =
                client.getLocation(
                    country = "Россия",
                    city = "Москва",
                    street = "Тверская",
                    house = "1",
                    apartment = "2",
                )

            assertThat(result.isLeft()).describedAs("isLeft").isTrue()
            val error = result.fold({ e: LogicError -> e }, { error("expected Left") })
            assertThat(error.code).describedAs("code").isEqualTo("geo.service.error")
        }
    }

    @Test
    fun `getLocation returns Left when Geo returns out-of-range coordinates`() {
        val fake = FakeGeoService().apply { location = location(99, 5) }
        withClient(fake) { client ->
            val result =
                client.getLocation(
                    country = "Россия",
                    city = "Москва",
                    street = "Тверская",
                    house = "1",
                    apartment = "2",
                )

            assertThat(result.isLeft())
                .describedAs("isLeft (domain validation rejects x=99)")
                .isTrue()
        }
    }

    // ========================= Helpers =========================

    private fun location(
        x: Int,
        y: Int,
    ): GeoProto.Location =
        GeoProto.Location
            .newBuilder()
            .setX(x)
            .setY(y)
            .build()

    private fun withClient(
        fake: FakeGeoService,
        block: (GeoClientImpl) -> Unit,
    ) {
        val server: Server =
            ServerBuilder
                .forPort(0)
                .addService(fake)
                .build()
                .start()
        try {
            val props =
                ApplicationProperties(
                    grpc =
                        ApplicationProperties.Grpc(
                            geoService =
                                ApplicationProperties.Grpc.GeoService(
                                    host = "localhost",
                                    port = server.port,
                                ),
                        ),
                )
            val client = GeoClientImpl(props)
            try {
                block(client)
            } finally {
                client.shutdown()
            }
        } finally {
            server.shutdown()
        }
    }
}
