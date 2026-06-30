package microarch.delivery.adapters.out.postgres.courier

import java.util.UUID
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.ports.courier.CourierRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class CourierRepositoryImpl(
    private val courierJpaRepository: CourierJpaRepository,
) : CourierRepository {
    @Transactional
    override fun add(courier: Courier): Courier = courierJpaRepository.save(courier)

    @Transactional
    override fun update(courier: Courier): Courier = courierJpaRepository.save(courier)

    override fun getById(id: UUID): Courier? = courierJpaRepository.findById(id).orElse(null)

    override fun getAll(): List<Courier> = courierJpaRepository.findAll()
}
