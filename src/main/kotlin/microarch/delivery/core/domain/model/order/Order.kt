package microarch.delivery.core.domain.model.order

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID
import libs.ddd.Aggregate
import libs.errs.LogicError
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue

@Entity
@Table(name = "orders")
class Order private constructor(
    override var id: UUID,
    @field:Embedded
    @field:AttributeOverrides(
        AttributeOverride(
            name = "x",
            column = Column(name = "x_coordinate"),
        ),
        AttributeOverride(
            name = "y",
            column = Column(name = "y_coordinate"),
        ),
    )
    var location: LocationValue,
    @field:Embedded
    @field:AttributeOverride(
        name = "value",
        column = Column(name = "volume"),
    )
    var volume: VolumeValue,
    @field:Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: OrderStatus,
) : Aggregate<UUID>(id) {
    fun assignOrder(): Either<LogicError, Unit> {
        if (this.status != OrderStatus.CREATED) {
            return LogicError
                .of(
                    "400",
                    "Couldn't assign order, cause current order status is not ${OrderStatus.CREATED}. " +
                        "Current status: ${this.status}",
                ).left()
        }
        status = OrderStatus.ASSIGNED

        return Unit.right()
    }

    fun completeOrder(): Either<LogicError, Unit> {
        if (this.status != OrderStatus.ASSIGNED) {
            return LogicError
                .of(
                    "400",
                    "Couldn't complete order, cause current order status is not ${OrderStatus.ASSIGNED}. " +
                        "Current status: ${this.status}",
                ).left()
        }
        this.status = OrderStatus.COMPLETED

        return Unit.right()
    }

    companion object {
        fun create(
            id: UUID,
            location: LocationValue,
            volume: VolumeValue,
        ) = Order(
            id = id,
            location = location,
            volume = volume,
            status = OrderStatus.CREATED,
        )
    }
}
