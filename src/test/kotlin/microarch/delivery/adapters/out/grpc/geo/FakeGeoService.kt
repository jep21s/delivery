package microarch.delivery.adapters.out.grpc.geo

import clients.geo.GeoGrpc
import clients.geo.GeoProto
import io.grpc.Status
import io.grpc.stub.StreamObserver

/**
 * In-JVM реализация Geo-сервиса для интеграционных тестов.
 * Возвращает [location] при успехе либо gRPC-ошибку [error], если задана.
 */
class FakeGeoService(
    var location: GeoProto.Location = GeoProto.Location.getDefaultInstance(),
    var error: Status? = null,
) : GeoGrpc.GeoImplBase() {
    override fun getGeolocation(
        request: GeoProto.GetGeolocationRequest,
        responseObserver: StreamObserver<GeoProto.GetGeolocationReply>,
    ) {
        val err = error
        if (err != null) {
            responseObserver.onError(err.asRuntimeException())
            return
        }
        val reply =
            GeoProto.GetGeolocationReply
                .newBuilder()
                .setLocation(location)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}
