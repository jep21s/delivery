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
    fun `getCompletedAssignment succeeds when courier is at assignment location`(): List<DynamicTest> {
        // Given — координаты, где курьер находится в той же точке, что и задание
        data class Case(
            val location: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(1 to 1),
                Case(5 to 5),
                Case(10 to 10),
                Case(3 to 7),
                Case(1 to 10),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("complete assignment at ${case.location}") {
                // Given
                val location = LocationValue.createOrThrow(case.location.first, case.location.second)
                val assignment = Assignment.create(UUID.randomUUID(), VolumeValue(1), location)

                // When
                val result = assignment.getCompletedAssignment(location)

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("getCompletedAssignment is right")
                            .isTrue()
                    },
                    {
                        val completed = result.getOrNull()
                        assertThat(completed).describedAs("completed assignment").isNotNull
                        assertThat(completed?.status).describedAs("status").isEqualTo(Assignment.Status.COMPLETED)
                    },
                    {
                        assertThat(result.getOrNull()?.location)
                            .describedAs("location preserved")
                            .isEqualTo(location)
                    },
                    {
                        assertThat(result.getOrNull()?.orderId)
                            .describedAs("orderId preserved")
                            .isEqualTo(assignment.orderId)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `getCompletedAssignment fails when courier is not at assignment location`(): List<DynamicTest> {
        // Given — курьер находится в другой точке, чем задание
        data class Case(
            val assignmentLocation: Pair<Int, Int>,
            val courierLocation: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(1 to 1, 2 to 1),
                Case(5 to 5, 6 to 5),
                Case(3 to 7, 3 to 8),
                Case(1 to 1, 10 to 10),
                Case(10 to 1, 1 to 10),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("assignment at ${case.assignmentLocation}, courier at ${case.courierLocation} is rejected") {
                // Given
                val assignmentLocation =
                    LocationValue.createOrThrow(case.assignmentLocation.first, case.assignmentLocation.second)
                val courierLocation =
                    LocationValue.createOrThrow(case.courierLocation.first, case.courierLocation.second)
                val assignment = Assignment.create(UUID.randomUUID(), VolumeValue(1), assignmentLocation)

                // When
                val result = assignment.getCompletedAssignment(courierLocation)

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
                val completed = assignment.getCompletedAssignment(location).getOrNull()!!

                // When
                val result = completed.getCompletedAssignment(location)

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
