package microarch.delivery.application.queries.dto

import java.util.UUID

data class CourierDto(
    val id: UUID,
    val name: String,
    val location: LocationDto,
)
