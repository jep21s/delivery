package microarch.delivery.core.domain.model.assignment

import java.util.UUID
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertAll

class AssignmentTest {
    @TestFactory
    fun `create produces assignment with ASSIGNED status and preserved fields`(): List<DynamicTest> {
        // Given — параметры создаваемого задания: orderId, объём и координаты
        data class Case(
            val volume: Int,
            val location: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(1, 1 to 1),
                Case(10, 10 to 10),
                Case(5, 3 to 7),
                Case(7, 1 to 10),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("create(volume=${case.volume}, location=${case.location}) succeeds") {
                // Given
                val orderId = UUID.randomUUID()
                val volume = VolumeValue(case.volume)
                val location = LocationValue.createOrThrow(case.location.first, case.location.second)

                // When
                val assignment = Assignment.create(orderId, volume, location)

                // Then
                assertAll(
                    { assertThat(assignment.id).describedAs("id").isNotNull() },
                    { assertThat(assignment.orderId).describedAs("orderId").isEqualTo(orderId) },
                    { assertThat(assignment.volume).describedAs("volume").isEqualTo(volume) },
                    { assertThat(assignment.location).describedAs("location").isEqualTo(location) },
                    { assertThat(assignment.status).describedAs("status").isEqualTo(Assignment.Status.ASSIGNED) },
                )
            }
        }
    }

    @TestFactory
    fun `getCompletedAssignment succeeds when courier is at or adjacent to assignment location`(): List<DynamicTest> {
        // Given — курьер находится в той же точке (distance=0) или в соседней клетке (distance=1)
        data class Case(
            val assignmentLocation: Pair<Int, Int>,
            val courierLocation: Pair<Int, Int>,
            val expectedDistance: Int,
        )

        val cases =
            listOf(
                // distance = 0 — та же точка
                Case(1 to 1, 1 to 1, 0),
                Case(5 to 5, 5 to 5, 0),
                Case(10 to 10, 10 to 10, 0),
                Case(3 to 7, 3 to 7, 0),
                Case(1 to 10, 1 to 10, 0),
                // distance = 1 — одна соседняя клетка по горизонтали/вертикали
                Case(1 to 1, 2 to 1, 1),
                Case(5 to 5, 6 to 5, 1),
                Case(5 to 5, 4 to 5, 1),
                Case(3 to 7, 3 to 8, 1),
                Case(3 to 7, 3 to 6, 1),
                Case(10 to 10, 9 to 10, 1),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest(
                "complete assignment at ${case.assignmentLocation}, courier at ${case.courierLocation} " +
                    "(distance=${case.expectedDistance})",
            ) {
                // Given
                val assignmentLocation =
                    LocationValue.createOrThrow(case.assignmentLocation.first, case.assignmentLocation.second)
                val courierLocation =
                    LocationValue.createOrThrow(case.courierLocation.first, case.courierLocation.second)
                val assignment = Assignment.create(UUID.randomUUID(), VolumeValue(1), assignmentLocation)

                // When
                val result = assignment.completeAssignment(courierLocation)

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("getCompletedAssignment is right")
                            .isTrue()
                    },
                    {
                        assertThat(assignment.status)
                            .describedAs("status")
                            .isEqualTo(Assignment.Status.COMPLETED)
                    },
                    {
                        assertThat(assignment.location)
                            .describedAs("location preserved")
                            .isEqualTo(assignmentLocation)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `getCompletedAssignment fails when courier is farther than proximity threshold`(): List<DynamicTest> {
        // Given — курьер находится на расстоянии > 1 от точки назначения
        data class Case(
            val assignmentLocation: Pair<Int, Int>,
            val courierLocation: Pair<Int, Int>,
            val expectedDistance: Int,
        )

        val cases =
            listOf(
                Case(1 to 1, 1 to 3, 2),
                Case(5 to 5, 7 to 5, 2),
                Case(3 to 7, 3 to 9, 2),
                Case(5 to 5, 6 to 6, 2),
                Case(1 to 1, 10 to 10, 18),
                Case(10 to 1, 1 to 10, 18),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest(
                "assignment at ${case.assignmentLocation}, courier at ${case.courierLocation} " +
                    "(distance=${case.expectedDistance}) is rejected",
            ) {
                // Given
                val assignmentLocation =
                    LocationValue.createOrThrow(case.assignmentLocation.first, case.assignmentLocation.second)
                val courierLocation =
                    LocationValue.createOrThrow(case.courierLocation.first, case.courierLocation.second)
                val assignment = Assignment.create(UUID.randomUUID(), VolumeValue(1), assignmentLocation)

                // When
                val result = assignment.completeAssignment(courierLocation)

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("getCompletedAssignment is left")
                            .isTrue()
                    },
                    {
                        assertThat(result.leftOrNull()?.message)
                            .describedAs("error message")
                            .contains("not close enough")
                    },
                )
            }
        }
    }

    @TestFactory
    fun `getCompletedAssignment fails when assignment is already completed`(): List<DynamicTest> {
        // Given — задание уже завершено, повторное завершение недопустимо
        data class Case(
            val location: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(1 to 1),
                Case(5 to 5),
                Case(10 to 10),
                Case(3 to 7),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("already completed at ${case.location} is rejected") {
                // Given
                val location = LocationValue.createOrThrow(case.location.first, case.location.second)
                val assignment = Assignment.create(UUID.randomUUID(), VolumeValue(1), location)
                assignment.completeAssignment(location).getOrNull()!!

                // When
                val result = assignment.completeAssignment(location)

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("getCompletedAssignment is left")
                            .isTrue()
                    },
                    {
                        assertThat(result.leftOrNull()?.message)
                            .describedAs("error message")
                            .contains("completed already")
                    },
                )
            }
        }
    }
}
