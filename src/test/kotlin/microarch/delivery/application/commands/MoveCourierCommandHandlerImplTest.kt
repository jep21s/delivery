package microarch.delivery.application.commands

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import libs.ddd.DomainEventPublisher
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.ports.courier.CourierRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class MoveCourierCommandHandlerImplTest {
    @Test
    fun `handle moves the courier one cell and persists the change`() {
        // Given — курьер в (5,5), перемещаем на соседнюю клетку
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = MoveCourierCommandHandlerImpl(courierRepository, publisher)

        val courier = Courier.create("courier", LocationValue.createOrThrow(5, 5))
        every { courierRepository.getById(courier.id) } returns courier
        every { courierRepository.update(any()) } returns courier
        every { publisher.publish(any()) } just Runs

        val command = MoveCourierCommand.create(courier.id, 6, 5).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        assertAll(
            { assertThat(result.isRight()).describedAs("handle is right").isTrue() },
            { assertThat(courier.location).describedAs("location updated").isEqualTo(LocationValue.createOrThrow(6, 5)) },
        )
        verify(exactly = 1) { courierRepository.update(courier) }
        verify { publisher.publish(any()) }
    }

    @Test
    fun `handle fails when the courier is not found`() {
        // Given — курьера нет в репозитории
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = MoveCourierCommandHandlerImpl(courierRepository, publisher)

        every { courierRepository.getById(any()) } returns null
        val command = MoveCourierCommand.create(UUID.randomUUID(), 2, 2).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        assertAll(
            { assertThat(result.isLeft()).describedAs("handle is left").isTrue() },
            { assertThat(result.leftOrNull()?.code).describedAs("error code").isEqualTo("record.not.found") },
        )
        verify(exactly = 0) { courierRepository.update(any()) }
        verify(exactly = 0) { publisher.publish(any()) }
    }

    @Test
    fun `handle fails when target is not one cell away and leaves the courier unchanged`() {
        // Given — целевая точка дальше одной клетки
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = MoveCourierCommandHandlerImpl(courierRepository, publisher)

        val courier = Courier.create("courier", LocationValue.createOrThrow(5, 5))
        every { courierRepository.getById(courier.id) } returns courier
        every { courierRepository.update(any()) } returns courier
        val command = MoveCourierCommand.create(courier.id, 7, 7).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        assertAll(
            { assertThat(result.isLeft()).describedAs("handle is left").isTrue() },
            { assertThat(result.leftOrNull()?.code).describedAs("error code").isEqualTo("400") },
            {
                assertThat(courier.location)
                    .describedAs("location unchanged")
                    .isEqualTo(LocationValue.createOrThrow(5, 5))
            },
        )
        verify(exactly = 0) { courierRepository.update(any()) }
        verify(exactly = 0) { publisher.publish(any()) }
    }
}
