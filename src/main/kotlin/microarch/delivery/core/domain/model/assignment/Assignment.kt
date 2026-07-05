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
import java.util.UUID
import libs.ddd.BaseEntity
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue

@Entity
@ConsistentCopyVisibility
data class Assignment private constructor(
    override val id: UUID,
    @field:Column(name = "order_id")
    val orderId: UUID,
    @field:Embedded
    @field:AttributeOverride(
        name = "value",
        column = Column(name = "volume"),
    )
    val volume: VolumeValue,
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
    val location: LocationValue,
    @field:Column(name = "status")
    @field:Enumerated(EnumType.STRING)
    val status: Status,
) : BaseEntity<UUID>(id) {
    fun getCompletedAssignment(courierLocation: LocationValue): Either<Error, Assignment> {
        if (this.location != courierLocation) {
            return Error(
                "The courier is not close enough. " +
                    "Courier location: $courierLocation, " +
                    "Assignment location: ${this.location}",
            ).left()
        }
        if (this.status == Status.COMPLETED) {
            return Error("Assignment has completed already").left()
        }

        return copy(status = Status.COMPLETED).right()
    }

    enum class Status {
        ASSIGNED,
        COMPLETED,
    }

    companion object {
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
