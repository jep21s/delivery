package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import kotlin.random.Random
import libs.ddd.DomainEventPublisher
import libs.errs.LogicError
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.ports.courier.CourierRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateCourierCommandHandlerImpl(
    private val courierRepository: CourierRepository,
    private val domainEventPublisher: DomainEventPublisher,
) : CreateCourierCommandHandler {
    @Transactional
    override fun handle(command: CreateCourierCommand): Either<LogicError, Unit> =
        either {
            val courier =
                Courier.create(
                    name = command.name,
                    location =
                        LocationValue.createOrThrow(
                            Random.nextInt(LocationValue.MIN_COORDINATE_VALUE, LocationValue.MAX_COORDINATE_VALUE + 1),
                            Random.nextInt(LocationValue.MIN_COORDINATE_VALUE, LocationValue.MAX_COORDINATE_VALUE + 1),
                        ),
                )
            val saved = courierRepository.add(courier)
            domainEventPublisher.publish(listOf(saved))
        }
}
