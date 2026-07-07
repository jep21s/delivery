package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import libs.ddd.DomainEventPublisher
import libs.errs.GeneralErrors
import libs.errs.LogicError
import microarch.delivery.core.ports.courier.CourierRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MoveCourierCommandHandlerImpl(
    private val courierRepository: CourierRepository,
    private val domainEventPublisher: DomainEventPublisher,
) : MoveCourierCommandHandler {
    @Transactional
    override fun handle(command: MoveCourierCommand): Either<LogicError, Unit> =
        either {
            val courier =
                courierRepository.getById(command.courierId)
                    ?: raise(GeneralErrors.notFound("courier", command.courierId))
            courier.moveTo(command.location).bind()
            val saved = courierRepository.update(courier)
            domainEventPublisher.publish(listOf(saved))
        }
}
