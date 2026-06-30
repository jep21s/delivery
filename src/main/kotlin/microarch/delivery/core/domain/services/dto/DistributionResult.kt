package microarch.delivery.core.domain.services.dto

import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.domain.model.order.Order

data class DistributionResult(
    val courier: Courier,
    val order: Order,
)
