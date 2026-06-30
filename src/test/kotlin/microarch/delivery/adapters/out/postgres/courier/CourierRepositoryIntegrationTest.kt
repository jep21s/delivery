package microarch.delivery.adapters.out.postgres.courier

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.util.UUID
import microarch.delivery.PostgresContextInitializer
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.assignment.Assignment
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.ports.courier.CourierRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ContextConfiguration(initializers = [PostgresContextInitializer::class])
@Transactional
class CourierRepositoryIntegrationTest {
    @Autowired
    private lateinit var courierRepository: CourierRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Test
    fun `add persists a courier and getById reads it back`() {
        // Given — новый курьер
        val courier = Courier.create("Иван", LocationValue.createOrThrow(3, 7))

        // When
        val saved = courierRepository.add(courier)
        flushAndClear()
        val found = courierRepository.getById(saved.id)

        // Then
        assertAll(
            { assertThat(found).describedAs("courier found").isNotNull },
            { assertThat(found?.name).describedAs("name").isEqualTo("Иван") },
            { assertThat(found?.location).describedAs("location").isEqualTo(LocationValue.createOrThrow(3, 7)) },
            { assertThat(found?.maxVolume).describedAs("maxVolume").isEqualTo(VolumeValue(20)) },
            { assertThat(found?.assignments).describedAs("no assignments").isEmpty() },
        )
    }

    @Test
    fun `add persists a courier together with its assignment via cascade`() {
        // Given — курьер берёт заказ до сохранения
        val orderId = UUID.randomUUID()
        val courier = Courier.create("Пётр", LocationValue.createOrThrow(1, 1))
        courier.takeOrder(
            Courier.NewOrder(orderId, VolumeValue(5), LocationValue.createOrThrow(2, 2)),
        )

        // When
        val saved = courierRepository.add(courier)
        flushAndClear()
        val found = courierRepository.getById(saved.id)

        // Then
        assertAll(
            { assertThat(found?.assignments).describedAs("assignment persisted").hasSize(1) },
            { assertThat(found?.assignments?.first()?.orderId).describedAs("assignment orderId").isEqualTo(orderId) },
            { assertThat(found?.assignments?.first()?.volume).describedAs("assignment volume").isEqualTo(VolumeValue(5)) },
            { assertThat(found?.assignments?.first()?.status).describedAs("assignment status").isEqualTo(Assignment.Status.ASSIGNED) },
        )
    }

    @Test
    fun `update persists a new courier location`() {
        // Given — сохранённый курьер
        val courier = courierRepository.add(Courier.create("Маша", LocationValue.createOrThrow(1, 1)))
        flushAndClear()

        // When — перемещение на одну клетку и обновление
        courier.moveTo(LocationValue.createOrThrow(1, 2))
        courierRepository.update(courier)
        flushAndClear()
        val found = courierRepository.getById(courier.id)

        // Then
        assertThat(found?.location).describedAs("new location").isEqualTo(LocationValue.createOrThrow(1, 2))
    }

    @Test
    fun `getById returns null for unknown id`() {
        // When
        val found = courierRepository.getById(UUID.randomUUID())

        // Then
        assertThat(found).isNull()
    }

    @Test
    fun `getAll returns all persisted couriers`() {
        // Given
        val a = courierRepository.add(Courier.create("A", LocationValue.createOrThrow(1, 1)))
        val b = courierRepository.add(Courier.create("B", LocationValue.createOrThrow(2, 2)))
        flushAndClear()

        // When
        val all = courierRepository.getAll()

        // Then
        val ids = all.map { it.id }
        assertAll(
            { assertThat(ids).describedAs("contains courier A").contains(a.id) },
            { assertThat(ids).describedAs("contains courier B").contains(b.id) },
        )
    }

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }
}
