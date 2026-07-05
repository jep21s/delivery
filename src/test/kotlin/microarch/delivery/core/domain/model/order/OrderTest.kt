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
                val id = UUID.randomUUID()
                val location = LocationValue.createOrThrow(case.location.first, case.location.second)
                val volume = VolumeValue(case.volume)
                val order = Order.create(id, location, volume)

                // When
                val result = order.assignOrder()

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("getAssignedOrder is right")
                            .isTrue()
                    },
                    {
                        assertThat(order.status)
                            .describedAs("status")
                            .isEqualTo(OrderStatus.ASSIGNED)
                    },
                    {
                        assertThat(order.id)
                            .describedAs("id preserved")
                            .isEqualTo(id)
                    },
                    {
                        assertThat(order.location)
                            .describedAs("location preserved")
                            .isEqualTo(location)
                    },
                    {
                        assertThat(order.volume)
                            .describedAs("volume preserved")
                            .isEqualTo(volume)
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
                    order.assignOrder().getOrNull()!!
                    order
                },
                Case(OrderStatus.COMPLETED) { order ->
                    order.assignOrder().getOrNull()!!
                    order.completeOrder().getOrNull()!!
                    order
                },
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("cannot assign order in status ${case.status}") {
                // Given
                val order = case.setup(Order.create(UUID.randomUUID(), location, volume))

                // When
                val result = order.assignOrder()

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
    fun `completeOrder succeeds when status is ASSIGNED`(): List<DynamicTest> {
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
                val order = Order.create(UUID.randomUUID(), location, volume)
                order.assignOrder().getOrNull()!!

                // When
                val result = order.completeOrder()

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("completeOrder is right")
                            .isTrue()
                    },
                    {
                        assertThat(order.status)
                            .describedAs("status")
                            .isEqualTo(OrderStatus.COMPLETED)
                    },
                    {
                        assertThat(order.location)
                            .describedAs("location preserved")
                            .isEqualTo(location)
                    },
                    {
                        assertThat(order.volume)
                            .describedAs("volume preserved")
                            .isEqualTo(volume)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `completeOrder fails when status is not ASSIGNED`(): List<DynamicTest> {
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
                    order.assignOrder().getOrNull()!!
                    order.completeOrder().getOrNull()!!
                    order
                },
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("cannot complete order in status ${case.status}") {
                // Given
                val order = case.setup(Order.create(UUID.randomUUID(), location, volume))

                // When
                val result = order.completeOrder()

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("completeOrder is left")
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
