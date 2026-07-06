package microarch.delivery.adapters.`in`.http.mappers

import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Konverter
import microarch.delivery.adapters.inbound.http.model.Location
import microarch.delivery.adapters.inbound.http.model.Order
import microarch.delivery.application.queries.dto.LocationDto
import microarch.delivery.application.queries.dto.OrderDto

@Konverter(
    options = [
        Konfig(key = "konvert.enforce-not-null", value = "true"),
        Konfig(key = "konvert.invalid-mapping-strategy", value = "fail"),
    ],
)
interface OrderMapper {
    fun toResponseDto(dto: OrderDto): Order

    fun toResponseDto(dto: LocationDto): Location
}
