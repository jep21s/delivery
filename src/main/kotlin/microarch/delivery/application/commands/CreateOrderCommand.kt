package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import java.util.UUID
import libs.errs.Guard
import libs.errs.LogicError

class CreateOrderCommand private constructor(
    val orderId: UUID,
    val country: String,
    val city: String,
    val street: String,
    val house: String,
    val apartment: String,
    val volume: Int,
) {
    companion object {
        fun create(
            orderId: UUID,
            country: String,
            city: String,
            street: String,
            house: String,
            apartment: String,
            volume: Int,
        ): Either<LogicError, CreateOrderCommand> =
            either {
                Guard.againstNullOrEmpty(orderId, "orderId").bind()
                Guard.againstNullOrEmpty(country, "country").bind()
                Guard.againstNullOrEmpty(city, "city").bind()
                Guard.againstNullOrEmpty(street, "street").bind()
                Guard.againstNullOrEmpty(house, "house").bind()
                Guard.againstNullOrEmpty(apartment, "apartment").bind()
                Guard.againstLessOrEqual(volume, 0, "volume").bind()
                CreateOrderCommand(orderId, country, city, street, house, apartment, volume)
            }
    }
}
