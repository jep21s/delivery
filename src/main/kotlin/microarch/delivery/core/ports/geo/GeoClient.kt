package microarch.delivery.core.ports.geo

import arrow.core.Either
import libs.errs.LogicError
import microarch.delivery.core.domain.model.LocationValue

interface GeoClient {
    fun getLocation(street: String): Either<LogicError, LocationValue>
}
