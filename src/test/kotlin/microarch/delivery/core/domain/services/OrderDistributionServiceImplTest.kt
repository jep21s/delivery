package microarch.delivery.core.domain.services

import java.util.UUID
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.assignment.Assignment
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertAll

class OrderDistributionServiceImplTest {
    private val service = OrderDistributionServiceImpl()

    @TestFactory
    fun `distribute selects the closest courier that can take the order`(): List<DynamicTest> {
        // Given — расположение заказа и расположения курьеров с их дистанциями
        data class Case(
            val orderLocation: Pair<Int, Int>,
            val courierLocations: List<Pair<Int, Int>>,
            val winnerIndex: Int,
        )

        val cases =
            listOf(
                Case(5 to 5, listOf(5 to 5, 6 to 5, 1 to 1), 0),
                Case(5 to 5, listOf(1 to 1, 6 to 5, 10 to 10), 1),
                Case(3 to 3, listOf(4 to 4, 3 to 8, 1 to 1), 0),
                Case(9 to 9, listOf(1 to 1, 5 to 5, 8 to 9), 2),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("order at ${case.orderLocation} -> winner #${case.winnerIndex}") {
                // Given
                val order = order(case.orderLocation, volume = 1)
                val couriers = case.courierLocations.map { Courier.create("courier", LocationValue.createOrThrow(it.first, it.second)) }
                val expectedWinner = couriers[case.winnerIndex]

                // When
                val result = service.distribute(order, couriers)

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("distribute is right")
                            .isTrue()
                    },
                    {
                        assertThat(result.getOrNull()?.courier)
                            .describedAs("winner courier")
                            .isSameAs(expectedWinner)
                    },
                    {
                        assertThat(result.getOrNull()?.order?.status)
                            .describedAs("assigned order status")
                            .isEqualTo(OrderStatus.ASSIGNED)
                    },
                    {
                        assertThat(result.getOrNull()?.order?.id)
                            .describedAs("order id preserved")
                            .isEqualTo(order.id)
                    },
                    {
                        val winner = result.getOrNull()?.courier
                        assertThat(winner?.assignments).describedAs("assignment created").hasSize(1)
                        assertThat(winner?.assignments?.first()?.orderId)
                            .describedAs("assignment orderId")
                            .isEqualTo(order.id)
                        assertThat(winner?.assignments?.first()?.status)
                            .describedAs("assignment status")
                            .isEqualTo(Assignment.Status.ASSIGNED)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `distribute picks the only courier that fits even if not the closest`(): List<DynamicTest> {
        // Given — ближайший курьер переполнен, вмещает только дальний
        data class Case(
            val orderLocation: Pair<Int, Int>,
            val closeLocation: Pair<Int, Int>,
            val farLocation: Pair<Int, Int>,
        )

        val cases =
            listOf(
                Case(5 to 5, 5 to 5, 10 to 10),
                Case(3 to 3, 4 to 3, 9 to 9),
                Case(1 to 1, 1 to 2, 10 to 10),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("closest full, far fits -> far wins") {
                // Given
                val order = order(case.orderLocation, volume = 1)
                val closeCourier = Courier.create("close", LocationValue.createOrThrow(case.closeLocation.first, case.closeLocation.second))
                closeCourier.takeOrder(Courier.NewOrder(UUID.randomUUID(), VolumeValue(20), LocationValue.createOrThrow(1, 1))).getOrNull()
                val farCourier = Courier.create("far", LocationValue.createOrThrow(case.farLocation.first, case.farLocation.second))

                // When
                val result = service.distribute(order, listOf(closeCourier, farCourier))

                // Then
                assertAll(
                    {
                        assertThat(result.isRight())
                            .describedAs("distribute is right")
                            .isTrue()
                    },
                    {
                        assertThat(result.getOrNull()?.courier)
                            .describedAs("winner is far (fits) courier")
                            .isSameAs(farCourier)
                    },
                    {
                        assertThat(closeCourier.assignments)
                            .describedAs("full courier unchanged")
                            .hasSize(1)
                    },
                )
            }
        }
    }

    @TestFactory
    fun `distribute breaks distance tie by keeping first in list`(): List<DynamicTest> {
        // Given — два курьера на равном минимальном расстоянии, победитель — первый в списке
        val order = order(5 to 5, volume = 1)
        val first = Courier.create("first", LocationValue.createOrThrow(5, 6))
        val second = Courier.create("second", LocationValue.createOrThrow(6, 5))
        val swapped = listOf(second, first)

        return listOf(
            DynamicTest.dynamicTest("[first, second] -> first wins") {
                // When
                val result = service.distribute(order, listOf(first, second))

                // Then
                assertThat(result.getOrNull()?.courier).isSameAs(first)
            },
            DynamicTest.dynamicTest("[second, first] -> second wins (order swapped)") {
                // When
                val result = service.distribute(order, swapped)

                // Then
                assertThat(result.getOrNull()?.courier).isSameAs(second)
            },
        )
    }

    @TestFactory
    fun `distribute fails when all couriers are full`(): List<DynamicTest> {
        // Given — все курьеры переполнены, ни один не вмещает заказ
        data class Case(
            val existingVolume: Int,
            val orderVolume: Int,
        )

        val cases =
            listOf(
                Case(20, 1),
                Case(15, 6),
                Case(19, 2),
            )

        return cases.map { case ->
            DynamicTest.dynamicTest("couriers full(${case.existingVolume}), order ${case.orderVolume} -> error") {
                // Given
                val order = order(5 to 5, volume = case.orderVolume)
                val couriers =
                    listOf(
                        Courier.create("a", LocationValue.createOrThrow(5, 5)),
                        Courier.create("b", LocationValue.createOrThrow(6, 5)),
                    )
                couriers.forEach { c ->
                    c
                        .takeOrder(
                            Courier.NewOrder(UUID.randomUUID(), VolumeValue(case.existingVolume), LocationValue.createOrThrow(1, 1)),
                        ).getOrNull()
                }

                // When
                val result = service.distribute(order, couriers)

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("distribute is left")
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
                            .contains("no available courier")
                    },
                    {
                        couriers.forEachIndexed { i, c ->
                            assertThat(c.assignments)
                                .describedAs("courier #$i assignments unchanged")
                                .hasSize(1)
                        }
                    },
                )
            }
        }
    }

    @TestFactory
    fun `distribute fails when couriers list is empty`(): List<DynamicTest> {
        // Given — список курьеров пуст
        val order = order(5 to 5, volume = 1)

        return listOf(
            DynamicTest.dynamicTest("empty couriers list -> error") {
                // When
                val result = service.distribute(order, emptyList())

                // Then
                assertAll(
                    {
                        assertThat(result.isLeft())
                            .describedAs("distribute is left")
                            .isTrue()
                    },
                    {
                        assertThat(result.leftOrNull()?.message)
                            .describedAs("error message")
                            .contains("no available courier")
                    },
                )
            },
        )
    }

    @TestFactory
    fun `distribute does not mutate the original order`(): List<DynamicTest> {
        // Given — оригинальный заказ остаётся в статусе CREATED (иммутабельность)
        val orderLocation = 5 to 5
        val order = order(orderLocation, volume = 1)
        val courier = Courier.create("courier", LocationValue.createOrThrow(5, 5))

        return listOf(
            DynamicTest.dynamicTest("original order stays CREATED after distribute") {
                // When
                val result = service.distribute(order, listOf(courier))

                // Then
                assertAll(
                    {
                        assertThat(result.getOrNull()?.order?.status)
                            .describedAs("returned order is ASSIGNED")
                            .isEqualTo(OrderStatus.ASSIGNED)
                    },
                    {
                        assertThat(order.status)
                            .describedAs("original order unchanged")
                            .isEqualTo(OrderStatus.CREATED)
                    },
                )
            },
        )
    }

    private fun order(
        location: Pair<Int, Int>,
        volume: Int,
    ): Order =
        Order.create(
            id = UUID.randomUUID(),
            location = LocationValue.createOrThrow(location.first, location.second),
            volume = VolumeValue(volume),
        )
}
