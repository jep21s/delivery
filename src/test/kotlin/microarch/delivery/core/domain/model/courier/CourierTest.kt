package microarch.delivery.core.domain.model.courier

import java.util.UUID
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.assignment.Assignment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertAll

class CourierTest {
    @TestFactory
    fun `create produces courier with preserved fields, default max volume and no assignments`(): List<DynamicTest> {
        // Given — параметры создаваемого курьера: имя и координаты
        data class Case(
            val name: String,
            val location: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case("Alice", 1 to 1),
                Case("Bob", 10 to 10),
                Case("Charlie", 5 to 5),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("create(name='${case.name}', location=${case.location}) succeeds") {
                // Given
                val location = LocationValue.createOrThrow(case.location.first, case.location.second)

                // When
                val courier = Courier.create(case.name, location)

                // Then
                assertAll(
                    { assertThat(courier.id).describedAs("id").isNotNull() },
                    { assertThat(courier.name).describedAs("name").isEqualTo(case.name) },
                    { assertThat(courier.location).describedAs("location").isEqualTo(location) },
                    { assertThat(courier.maxVolume).describedAs("maxVolume").isEqualTo(VolumeValue(20)) },
                    { assertThat(courier.assignments).describedAs("assignments").isEmpty() },
                )
            }
        }
    }

    @TestFactory
    fun `isCanTakeOrder respects total volume against max volume`(): List<DynamicTest> {
        // Given — существующие объёмы заказов курьера, объём нового заказа и ожидаемый результат
        data class Case(
            val existingVolumes: List<Int>,
            val newVolume: Int,
            val expected: Boolean,
        )

        val cases =
            listOf(
                Case(emptyList(), 20, true),
                Case(emptyList(), 1, true),
                Case(emptyList(), 21, false),
                Case(listOf(15), 5, true),
                Case(listOf(15), 6, false),
                Case(listOf(10, 5), 5, true),
                Case(listOf(10, 5), 6, false),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest(
                "existing=${case.existingVolumes}, new=${case.newVolume} => canTake=${case.expected}",
            ) {
                // Given
                val courier = Courier.create("courier", LocationValue.createOrThrow(5, 5))
                case.existingVolumes.forEach { volume ->
                    courier.takeOrder(newOrder(volume)).getOrNull()
                }

                // When
                val result = courier.isCanTakeOrder(VolumeValue(case.newVolume))

                // Then
                assertThat(result)
                    .describedAs("isCanTakeOrder for existing=${case.existingVolumes}, new=${case.newVolume}")
                    .isEqualTo(case.expected)
            }
        }
    }

    @TestFactory
    fun `takeOrder succeeds when order volume fits and creates an assignment`(): List<DynamicTest> {
        // Given — объём заказа помещается в лимит курьера
        data class Case(
            val volume: Int,
            val location: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(1, 5 to 5),
                Case(20, 1 to 1),
                Case(10, 10 to 10),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("take order with volume ${case.volume} at ${case.location}") {
                // Given
                val courier = Courier.create("courier", LocationValue.createOrThrow(1, 1))
                val newOrder = newOrder(case.volume, case.location)

                // When
                val result = courier.takeOrder(newOrder)

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("takeOrder is right")
                            .isTrue()
                    },
                    {
                        assertThat(courier.assignments)
                            .describedAs("assignments size")
                            .hasSize(1)
                    },
                    {
                        val assignment = courier.assignments.first()
                        assertThat(assignment.orderId).describedAs("orderId").isEqualTo(newOrder.id)
                        assertThat(assignment.volume).describedAs("volume").isEqualTo(newOrder.volume)
                        assertThat(assignment.location).describedAs("location").isEqualTo(newOrder.location)
                        assertThat(assignment.status).describedAs("status").isEqualTo(Assignment.Status.ASSIGNED)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `takeOrder fails when order volume exceeds max volume`(): List<DynamicTest> {
        // Given — объём заказа превышает лимит курьера
        data class Case(
            val existingVolumes: List<Int>,
            val newVolume: Int,
        )

        val cases =
            listOf(
                Case(emptyList(), 21),
                Case(listOf(15), 6),
                Case(listOf(10, 10), 1),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("cannot take new=${case.newVolume} with existing=${case.existingVolumes}") {
                // Given
                val courier = Courier.create("courier", LocationValue.createOrThrow(1, 1))
                case.existingVolumes.forEach { volume ->
                    courier.takeOrder(newOrder(volume)).getOrNull()
                }
                val initialAssignments = courier.assignments.size

                // When
                val result = courier.takeOrder(newOrder(case.newVolume))

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("takeOrder is left")
                            .isTrue()
                    },
                    {
                        assertThat(result.leftOrNull()?.code)
                            .describedAs("error code")
                            .isEqualTo("400")
                    },
                    {
                        assertThat(result.leftOrNull()?.message)
                            .describedAs("error message")
                            .contains("couldn't take new order")
                    },
                    {
                        assertThat(courier.assignments)
                            .describedAs("assignments unchanged")
                            .hasSize(initialAssignments)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `moveTo succeeds when target is exactly one cell away`(): List<DynamicTest> {
        // Given — курьер перемещается на одну соседнюю клетку
        data class Case(
            val from: Pair<Int, Int>,
            val to: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(5 to 5, 6 to 5),
                Case(5 to 5, 4 to 5),
                Case(5 to 5, 5 to 6),
                Case(5 to 5, 5 to 4),
                Case(1 to 1, 2 to 1),
                Case(10 to 10, 9 to 10),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("move from ${case.from} to ${case.to}") {
                // Given
                val courier = Courier.create("courier", LocationValue.createOrThrow(case.from.first, case.from.second))
                val target = LocationValue.createOrThrow(case.to.first, case.to.second)

                // When
                val result = courier.moveTo(target)

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("moveTo is right")
                            .isTrue()
                    },
                    {
                        assertThat(courier.location)
                            .describedAs("location updated")
                            .isEqualTo(target)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `moveTo fails when target is not one cell away`(): List<DynamicTest> {
        // Given — целевая точка находится на расстоянии, отличном от 1
        data class Case(
            val from: Pair<Int, Int>,
            val to: Pair<Int, Int>,
            val distance: Int,
        )

        val cases =
            listOf(
                Case(5 to 5, 5 to 5, 0),
                Case(5 to 5, 6 to 6, 2),
                Case(5 to 5, 7 to 5, 2),
                Case(5 to 5, 5 to 8, 3),
                Case(1 to 1, 10 to 10, 18),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("cannot move from ${case.from} to ${case.to} (distance=${case.distance})") {
                // Given
                val from = LocationValue.createOrThrow(case.from.first, case.from.second)
                val courier = Courier.create("courier", from)
                val target = LocationValue.createOrThrow(case.to.first, case.to.second)

                // When
                val result = courier.moveTo(target)

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("moveTo is left")
                            .isTrue()
                    },
                    {
                        assertThat(result.leftOrNull()?.code)
                            .describedAs("error code")
                            .isEqualTo("400")
                    },
                    {
                        assertThat(result.leftOrNull()?.message)
                            .describedAs("error message")
                            .contains("move only one cell at a time")
                    },
                    {
                        assertThat(courier.location)
                            .describedAs("location unchanged")
                            .isEqualTo(from)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `completeAssignment succeeds when courier is at or adjacent to assignment location`(): List<DynamicTest> {
        // Given — курьер находится в той же точке (distance=0) или соседней клетке (distance=1)
        data class Case(
            val courierLocation: Pair<Int, Int>,
            val assignmentLocation: Pair<Int, Int>,
            val expectedDistance: Int,
        )

        val cases =
            listOf(
                // distance = 0 — та же точка
                Case(1 to 1, 1 to 1, 0),
                Case(5 to 5, 5 to 5, 0),
                Case(10 to 10, 10 to 10, 0),
                Case(3 to 7, 3 to 7, 0),
                // distance = 1 — соседняя клетка
                Case(2 to 1, 1 to 1, 1),
                Case(6 to 5, 5 to 5, 1),
                Case(3 to 8, 3 to 7, 1),
                Case(4 to 5, 5 to 5, 1),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest(
                "courier at ${case.courierLocation}, assignment at ${case.assignmentLocation} " +
                    "(distance=${case.expectedDistance})",
            ) {
                // Given
                val courierLocation =
                    LocationValue.createOrThrow(case.courierLocation.first, case.courierLocation.second)
                val courier = Courier.create("courier", courierLocation)
                courier.takeOrder(newOrder(volume = 1, location = case.assignmentLocation)).getOrNull()
                val assignmentId = courier.assignments.first().id

                // When
                val result = courier.completeAssignment(assignmentId)

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("completeAssignment is right")
                            .isTrue()
                    },
                    {
                        assertThat(courier.assignments.first().status)
                            .describedAs("assignment status")
                            .isEqualTo(Assignment.Status.COMPLETED)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `completeAssignment fails when courier is not at assignment location`(): List<DynamicTest> {
        // Given — курьер находится на расстоянии > 1 от точки назначения
        data class Case(
            val courierLocation: Pair<Int, Int>,
            val assignmentLocation: Pair<Int, Int>,
            val expectedDistance: Int,
        )

        val cases =
            listOf(
                Case(1 to 1, 1 to 3, 2),
                Case(5 to 5, 7 to 5, 2),
                Case(3 to 7, 3 to 9, 2),
                Case(5 to 5, 6 to 6, 2),
                Case(1 to 1, 10 to 10, 18),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest(
                "courier at ${case.courierLocation}, assignment at ${case.assignmentLocation} " +
                    "(distance=${case.expectedDistance}) is rejected",
            ) {
                // Given
                val courierLocation =
                    LocationValue.createOrThrow(case.courierLocation.first, case.courierLocation.second)
                val assignmentLocation =
                    LocationValue.createOrThrow(case.assignmentLocation.first, case.assignmentLocation.second)
                val courier = Courier.create("courier", courierLocation)
                courier.takeOrder(newOrder(volume = 1, location = case.assignmentLocation)).getOrNull()
                val assignmentId = courier.assignments.first().id

                // When
                val result = courier.completeAssignment(assignmentId)

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("completeAssignment is left")
                            .isTrue()
                    },
                    {
                        assertThat(result.leftOrNull()?.message)
                            .describedAs("error message")
                            .contains("not close enough")
                    },
                    {
                        assertThat(courier.assignments.first().status)
                            .describedAs("assignment status unchanged")
                            .isEqualTo(Assignment.Status.ASSIGNED)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `completeAssignment fails when assignment id not found`(): List<DynamicTest> {
        // Given — курьер не имеет задания с указанным id
        val courier = Courier.create("courier", LocationValue.createOrThrow(5, 5))

        return listOf(
            DynamicTest.dynamicTest("complete unknown assignment is rejected") {
                // Given
                val unknownId = UUID.randomUUID()

                // When
                val result = courier.completeAssignment(unknownId)

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("completeAssignment is left")
                            .isTrue()
                    },
                    {
                        assertThat(result.leftOrNull()?.code)
                            .describedAs("error code")
                            .isEqualTo("404")
                    },
                    {
                        assertThat(result.leftOrNull()?.message)
                            .describedAs("error message")
                            .contains("doesn't has Assignment")
                    },
                )
            },
        )
    }

    private fun newOrder(
        volume: Int,
        location: Pair<Int, Int> = 1 to 1,
    ): Courier.NewOrder =
        Courier.NewOrder(
            id = UUID.randomUUID(),
            volume = VolumeValue(volume),
            location = LocationValue.createOrThrow(location.first, location.second),
        )
}
