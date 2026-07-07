package microarch.delivery.application.commands

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import libs.ddd.DomainEventPublisher
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.ports.courier.CourierRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class CreateCourierCommandHandlerImplTest {
    @Test
    fun `handle creates a courier with given name, random location, and publishes event`() {
        // Given
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = CreateCourierCommandHandlerImpl(courierRepository, publisher)

        val added = slot<Courier>()
        every { courierRepository.add(capture(added)) } answers { added.captured }
        every { publisher.publish(any()) } just Runs
        val command = CreateCourierCommand.create("Алиса").getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        val saved = added.captured
        assertAll(
            { assertThat(result.isRight()).describedAs("handle is right").isTrue() },
            { assertThat(result.getOrNull()).describedAs("returned id").isEqualTo(saved.id) },
            { assertThat(saved.name).describedAs("name").isEqualTo("Алиса") },
            { assertThat(saved.maxVolume).describedAs("maxVolume").isEqualTo(VolumeValue(20)) },
            { assertThat(saved.assignments).describedAs("no assignments").isEmpty() },
            { assertThat(saved.location.x).describedAs("location.x in range").isBetween(1, 10) },
            { assertThat(saved.location.y).describedAs("location.y in range").isBetween(1, 10) },
        )
        verify(exactly = 1) { courierRepository.add(any()) }
        verify { publisher.publish(any()) }
    }
}
