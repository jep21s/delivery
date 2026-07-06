package microarch.delivery.adapters.`in`.http.mappers

import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Konverter
import microarch.delivery.adapters.inbound.http.model.Courier
import microarch.delivery.adapters.inbound.http.model.Location
import microarch.delivery.application.queries.dto.CourierDto
import microarch.delivery.application.queries.dto.LocationDto

@Konverter(
    options = [
        Konfig(key = "konvert.enforce-not-null", value = "true"),
        Konfig(key = "konvert.invalid-mapping-strategy", value = "fail"),
    ],
)
interface CourierMapper {
    fun toResponseDto(dto: CourierDto): Courier

    fun toResponseDto(dto: LocationDto): Location
}
