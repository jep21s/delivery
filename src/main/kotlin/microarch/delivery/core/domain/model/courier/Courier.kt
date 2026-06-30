package microarch.delivery.core.domain.model.courier

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.MapKey
import jakarta.persistence.OneToMany
import java.util.UUID
import libs.ddd.Aggregate
import libs.errs.LogicError
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.assignment.Assignment
import microarch.delivery.core.domain.model.sum

@Entity
@ConsistentCopyVisibility
data class Courier private constructor(
    override val id: UUID,
    @field:Column(name = "name")
    val name: String,
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
        column = Column(name = "max_volume"),
    )
    val maxVolume: VolumeValue,
    @field:OneToMany(
        cascade = [
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH,
        ],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @field:MapKey("id")
    private var _assignments: MutableMap<UUID, Assignment>,
) : Aggregate<UUID>(id) {
    val assignments: List<Assignment>
        get() = _assignments.values.toList()

    fun isCanTakeOrder(newOrderVolume: VolumeValue): Boolean {
        val currentAssignmentsVolume: VolumeValue =
            assignments
                .map { it.volume }
                .sum()
        return (currentAssignmentsVolume + newOrderVolume) <= this.maxVolume
    }

    fun takeOrder(newOrder: NewOrder): Either<LogicError, Unit> {
        if (!isCanTakeOrder(newOrder.volume)) {
            return LogicError
                .of(
                    code = "400",
                    message =
                        "Courier couldn't take new order. " +
                            "Courier id: ${this.id}, " +
                            "name: ${this.name}, " +
                            "order id: ${newOrder.id}",
                ).left()
        }
        val assignment =
            Assignment.create(
                orderId = newOrder.id,
                volume = newOrder.volume,
                location = newOrder.location,
            )
        _assignments[assignment.id] = assignment
        return Unit.right()
    }

    fun completeAssignment(assignmentId: UUID): Either<LogicError, Unit> {
        val assignment =
            _assignments[assignmentId]
                ?: return LogicError
                    .of(
                        "404",
                        "Couldn't complete Assignment, " +
                            "cause courier ${this.id} doesn't has Assignment $assignmentId",
                    ).left()

        return assignment
            .getCompletedAssignment(this.location)
            .map { completedAssignment: Assignment ->
                _assignments[completedAssignment.id] = completedAssignment
            }
    }

    fun moveTo(newLocation: LocationValue): Either<LogicError, Unit> {
        if (location.distanceTo(newLocation) != MOVE_STEP) {
            return LogicError
                .of(
                    code = "400",
                    message =
                        "Courier couldn't move to new location. " +
                            "Courier id: ${this.id}, " +
                            "current location: ${this.location}, " +
                            "new location: $newLocation. " +
                            "Courier can move only one cell at a time.",
                ).left()
        }
        location = newLocation
        return Unit.right()
    }

    data class NewOrder(
        val id: UUID,
        val volume: VolumeValue,
        val location: LocationValue,
    )

    companion object {
        private const val MAX_VOLUME = 20
        private const val MOVE_STEP = 1

        fun create(
            name: String,
            location: LocationValue,
        ) = Courier(
            id = UUID.randomUUID(),
            name = name,
            location = location,
            maxVolume = VolumeValue(MAX_VOLUME),
            _assignments = mutableMapOf(),
        )
    }
}
