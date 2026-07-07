package microarch.delivery.application.queries.dto

import java.util.UUID

data class OrderDto(
    val id: UUID,
    val location: LocationDto,
)
