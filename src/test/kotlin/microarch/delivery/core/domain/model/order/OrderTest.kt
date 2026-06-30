package microarch.delivery.core.domain.model.order

import java.util.UUID
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertAll

class OrderTest {
    @TestFactory
    fun `create produces order with CREATED status and preserved fields`(): List<DynamicTest> {
        // Given — параметры создаваемого заказа: координаты и объём
        data class Case(
            val location: Pair<Int, Int>,
            val volume: Int,
        )

        val cases =
            listOf(
                Case(1 to 1, 1),
                Case(10 to 10, 5),
                Case(3 to 7, 20),
                Case(1 to 10, 15),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("create(location=${case.location}, volume=${case.volume}) succeeds") {
                // Given
                val id = UUID.randomUUID()
                val location = LocationValue.createOrThrow(case.location.first, case.location.second)
                val volume = VolumeValue(case.volume)

                // When
                val order = Order.create(id, location, volume)

                // Then
                assertAll(
                    { assertThat(order.id).describedAs("id").isEqualTo(id) },
                    { assertThat(order.location).describedAs("location").isEqualTo(location) },
                    { assertThat(order.volume).describedAs("volume").isEqualTo(volume) },
                    { assertThat(order.status).describedAs("status").isEqualTo(OrderStatus.CREATED) },
                )
            }
        }
    }

    @TestFactory
    fun `getAssignedOrder succeeds when status is CREATED`(): List<DynamicTest> {
        // Given — заказ в начальном статусе CREATED, переводим в ASSIGNED
        data class Case(
            val location: Pair<Int, Int>,
            val volume: Int,
        )

        val cases =
            listOf(
                Case(1 to 1, 1),
                Case(5 to 5, 10),
                Case(10 to 10, 20),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("assign order at ${case.location} with volume ${case.volume}") {
                // Given
                val location = LocationValue.createOrThrow(case.location.first, case.location.second)
                val volume = VolumeValue(case.volume)
                val order = Order.create(UUID.randomUUID(), location, volume)

                // When
                val result = order.getAssignedOrder()

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("getAssignedOrder is right")
                            .isTrue()
                    },
                    {
                        val assigned = result.getOrNull()
                        assertThat(assigned).describedAs("assigned order").isNotNull
                        assertThat(assigned?.status).describedAs("status").isEqualTo(OrderStatus.ASSIGNED)
                    },
                    {
                        assertThat(result.getOrNull()?.id)
                            .describedAs("id preserved")
                            .isEqualTo(order.id)
                    },
                    {
                        assertThat(result.getOrNull()?.location)
                            .describedAs("location preserved")
                            .isEqualTo(order.location)
                    },
                    {
                        assertThat(result.getOrNull()?.volume)
                            .describedAs("volume preserved")
                            .isEqualTo(order.volume)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `getAssignedOrder fails when status is not CREATED`(): List<DynamicTest> {
        // Given — заказ уже назначен или завершён, повторное назначение недопустимо
        data class Case(
            val status: OrderStatus,
            val setup: (Order) -> Order,
        )

        val location = LocationValue.createOrThrow(5, 5)
        val volume = VolumeValue(10)

        val cases =
            listOf(
                Case(OrderStatus.ASSIGNED) { order ->
                    order.getAssignedOrder().getOrNull()!!
                },
                Case(OrderStatus.COMPLETED) { order ->
                    order
                        .getAssignedOrder()
                        .getOrNull()!!
                        .getCompletedOrder()
                        .getOrNull()!!
                },
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("cannot assign order in status ${case.status}") {
                // Given
                val order = case.setup(Order.create(UUID.randomUUID(), location, volume))

                // When
                val result = order.getAssignedOrder()

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("getAssignedOrder is left")
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
                            .contains("Couldn't assign order")
                    },
                )
            }
        }
    }

    @TestFactory
    fun `getCompletedOrder succeeds when status is ASSIGNED`(): List<DynamicTest> {
        // Given — назначенный заказ (ASSIGNED) переводим в COMPLETED
        data class Case(
            val location: Pair<Int, Int>,
            val volume: Int,
        )

        val cases =
            listOf(
                Case(1 to 1, 1),
                Case(5 to 5, 10),
                Case(10 to 10, 20),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("complete assigned order at ${case.location} with volume ${case.volume}") {
                // Given
                val location = LocationValue.createOrThrow(case.location.first, case.location.second)
                val volume = VolumeValue(case.volume)
                val assigned = Order.create(UUID.randomUUID(), location, volume).getAssignedOrder().getOrNull()!!

                // When
                val result = assigned.getCompletedOrder()

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("getCompletedOrder is right")
                            .isTrue()
                    },
                    {
                        val completed = result.getOrNull()
                        assertThat(completed).describedAs("completed order").isNotNull
                        assertThat(completed?.status).describedAs("status").isEqualTo(OrderStatus.COMPLETED)
                    },
                    {
                        assertThat(result.getOrNull()?.id)
                            .describedAs("id preserved")
                            .isEqualTo(assigned.id)
                    },
                    {
                        assertThat(result.getOrNull()?.location)
                            .describedAs("location preserved")
                            .isEqualTo(assigned.location)
                    },
                    {
                        assertThat(result.getOrNull()?.volume)
                            .describedAs("volume preserved")
                            .isEqualTo(assigned.volume)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `getCompletedOrder fails when status is not ASSIGNED`(): List<DynamicTest> {
        // Given — заказ в статусе CREATED (ещё не назначен) или уже COMPLETED
        data class Case(
            val status: OrderStatus,
            val setup: (Order) -> Order,
        )

        val location = LocationValue.createOrThrow(5, 5)
        val volume = VolumeValue(10)

        val cases =
            listOf(
                Case(OrderStatus.CREATED) { order -> order },
                Case(OrderStatus.COMPLETED) { order ->
                    order
                        .getAssignedOrder()
                        .getOrNull()!!
                        .getCompletedOrder()
                        .getOrNull()!!
                },
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("cannot complete order in status ${case.status}") {
                // Given
                val order = case.setup(Order.create(UUID.randomUUID(), location, volume))

                // When
                val result = order.getCompletedOrder()

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("getCompletedOrder is left")
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
                            .contains("Couldn't complete order")
                    },
                )
            }
        }
    }
}
