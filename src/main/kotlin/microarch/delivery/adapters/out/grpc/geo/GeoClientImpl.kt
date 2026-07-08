package microarch.delivery.adapters.out.grpc.geo

import arrow.core.Either
import arrow.core.left
import clients.geo.GeoGrpc
import clients.geo.GeoProto
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import jakarta.annotation.PreDestroy
import libs.errs.LogicError
import microarch.delivery.ApplicationProperties
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.ports.geo.GeoClient
import org.springframework.stereotype.Service

@Service
class GeoClientImpl(
    properties: ApplicationProperties,
) : GeoClient {
    private val channel: ManagedChannel =
        ManagedChannelBuilder
            .forAddress(properties.grpc.geoService.host, properties.grpc.geoService.port)
            .usePlaintext()
            .build()
    private val stub: GeoGrpc.GeoBlockingStub = GeoGrpc.newBlockingStub(channel)

    @PreDestroy
    fun shutdown() {
        if (!channel.isShutdown) {
            channel.shutdown()
        }
    }

    override fun getLocation(street: String): Either<LogicError, LocationValue> {
        val request =
            GeoProto.GetGeolocationRequest
                .newBuilder()
                .setStreet(street)
                .build()
        return try {
            val response = stub.getGeolocation(request)
            val location = response.location
            LocationValue.create(location.x, location.y)
        } catch (e: StatusRuntimeException) {
            LogicError
                .of(
                    "geo.service.error",
                    "Geo service call failed: ${e.status}",
                ).left()
        }
    }
}
