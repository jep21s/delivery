package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import libs.ddd.DomainEventPublisher
import libs.errs.LogicError
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.ports.geo.GeoClient
import microarch.delivery.core.ports.order.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateOrderCommandHandlerImpl(
    private val orderRepository: OrderRepository,
    private val geoClient: GeoClient,
    private val domainEventPublisher: DomainEventPublisher,
) : CreateOrderCommandHandler {
    @Transactional
    override fun handle(command: CreateOrderCommand): Either<LogicError, Unit> =
        either {
            val location: LocationValue = getLocation(command).bind()
            val order =
                Order.create(
                    id = command.orderId,
                    location = location,
                    volume = VolumeValue(command.volume),
                )
            val saved = orderRepository.add(order)
            domainEventPublisher.publish(listOf(saved))
        }

    private fun getLocation(command: CreateOrderCommand): Either<LogicError, LocationValue> = geoClient.getLocation(street = command.street)
}
