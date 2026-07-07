package microarch.delivery.application.queries

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.ports.courier.CourierRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class GetAllCouriersQueryHandlerImplTest {
    @Test
    fun `handle returns a dto for each courier`() {
        // Given — два курьера
        val courierRepository = mockk<CourierRepository>()
        val handler = GetAllCouriersQueryHandlerImpl(courierRepository)

        val c1 = Courier.create("Alice", LocationValue.createOrThrow(1, 2))
        val c2 = Courier.create("Bob", LocationValue.createOrThrow(3, 4))
        every { courierRepository.getAll() } returns listOf(c1, c2)

        // When
        val result = handler.handle(GetAllCouriersQuery.create().getOrNull()!!)

        // Then
        val dtos = result.getOrNull()!!
        val byId = dtos.associateBy { it.id }
        assertAll(
            { assertThat(result.isRight()).describedAs("handle is right").isTrue() },
            { assertThat(dtos).describedAs("two couriers").hasSize(2) },
            { assertThat(byId[c1.id]?.name).describedAs("c1 name").isEqualTo("Alice") },
            { assertThat(byId[c1.id]?.location?.x).describedAs("c1 x").isEqualTo(1) },
            { assertThat(byId[c1.id]?.location?.y).describedAs("c1 y").isEqualTo(2) },
            { assertThat(byId[c2.id]?.name).describedAs("c2 name").isEqualTo("Bob") },
            { assertThat(byId[c2.id]?.location?.x).describedAs("c2 x").isEqualTo(3) },
            { assertThat(byId[c2.id]?.location?.y).describedAs("c2 y").isEqualTo(4) },
        )
        verify(exactly = 1) { courierRepository.getAll() }
    }

    @Test
    fun `handle returns an empty list when there are no couriers`() {
        // Given — репозиторий пуст
        val courierRepository = mockk<CourierRepository>()
        val handler = GetAllCouriersQueryHandlerImpl(courierRepository)

        every { courierRepository.getAll() } returns emptyList()

        // When
        val result = handler.handle(GetAllCouriersQuery.create().getOrNull()!!)

        // Then
        assertThat(result.getOrNull()).describedAs("empty list").isEmpty()
        verify(exactly = 1) { courierRepository.getAll() }
    }
}
