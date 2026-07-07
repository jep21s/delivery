package microarch.delivery.core.domain.model.assignment

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
import libs.ddd.BaseEntity
import libs.errs.LogicError
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue

@Entity
@Table(name = "assignments")
class Assignment private constructor(
    override val id: UUID,
    @field:Column(name = "order_id")
    var orderId: UUID,
    @field:Embedded
    @field:AttributeOverride(
        name = "value",
        column = Column(name = "volume"),
    )
    var volume: VolumeValue,
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
    @field:Column(name = "status")
    @field:Enumerated(EnumType.STRING)
    var status: Status,
) : BaseEntity<UUID>(id) {
    fun completeAssignment(courierLocation: LocationValue): Either<LogicError, Unit> {
        val distance = this.location.distanceTo(courierLocation)
        if (distance > COMPLETE_PROXIMITY) {
            return LogicError
                .of(
                    code = "400",
                    message =
                        "The courier is not close enough. " +
                            "Courier location: $courierLocation, " +
                            "Assignment location: ${this.location}, " +
                            "distance: $distance, " +
                            "max allowed distance: $COMPLETE_PROXIMITY",
                ).left()
        }
        if (this.status != Status.ASSIGNED) {
            return LogicError
                .of("400", "Assignment has completed already")
                .left()
        }

        this.status = Status.COMPLETED
        return Unit.right()
    }

    enum class Status {
        ASSIGNED,
        COMPLETED,
    }

    companion object {
        private const val COMPLETE_PROXIMITY = 1

        fun create(
            orderId: UUID,
            volume: VolumeValue,
            location: LocationValue,
        ): Assignment =
            Assignment(
                id = UUID.randomUUID(),
                orderId = orderId,
                volume = volume,
                location = location,
                status = Status.ASSIGNED,
            )
    }
}
