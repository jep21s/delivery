package microarch.delivery.core.ports.geo

import arrow.core.Either
import libs.errs.LogicError
import microarch.delivery.core.domain.model.LocationValue

interface GeoClient {
    fun getLocation(
        country: String,
        city: String,
        street: String,
        house: String,
        apartment: String,
    ): Either<LogicError, LocationValue>
}
