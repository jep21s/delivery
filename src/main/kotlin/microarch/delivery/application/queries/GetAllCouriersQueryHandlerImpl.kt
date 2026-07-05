package microarch.delivery.application.queries

import arrow.core.Either
import arrow.core.right
import libs.errs.LogicError
import microarch.delivery.application.queries.dto.CourierDto
import microarch.delivery.application.queries.dto.LocationDto
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.ports.courier.CourierRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetAllCouriersQueryHandlerImpl(
    private val courierRepository: CourierRepository,
) : GetAllCouriersQueryHandler {
    @Transactional(readOnly = true)
    override fun handle(query: GetAllCouriersQuery): Either<LogicError, List<CourierDto>> =
        courierRepository
            .getAll()
            .map { it.toDto() }
            .right()

    private fun Courier.toDto() =
        CourierDto(
            id = id,
            name = name,
            location = LocationDto(location.x, location.y),
        )
}
