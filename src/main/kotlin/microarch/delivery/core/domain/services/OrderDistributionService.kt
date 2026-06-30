package microarch.delivery.core.domain.services

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import libs.errs.LogicError
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.services.dto.DistributionResult
import org.springframework.stereotype.Service

interface OrderDistributionService {
    fun distribute(
        order: Order,
        couriers: List<Courier>,
    ): Either<LogicError, DistributionResult>
}

@Service
class OrderDistributionServiceImpl : OrderDistributionService {
    override fun distribute(
        order: Order,
        couriers: List<Courier>,
    ): Either<LogicError, DistributionResult> {
        val winner: Courier =
            getWinnerCourier(couriers, order)
                ?: return LogicError
                    .of(
                        code = "400",
                        message = "Couldn't distribute order ${order.id}: no available courier.",
                    ).left()

        return either {
            val assignedOrder: Order =
                order
                    .getAssignedOrder()
                    .bind()
            winner
                .takeOrder(
                    Courier.NewOrder(
                        id = order.id,
                        volume = order.volume,
                        location = order.location,
                    ),
                ).bind()
            DistributionResult(winner, assignedOrder)
        }
    }

    private fun getWinnerCourier(
        couriers: List<Courier>,
        order: Order,
    ): Courier? =
        couriers
            .filter { it.isCanTakeOrder(order.volume) }
            .minByOrNull { it.location.distanceTo(order.location) }
}
