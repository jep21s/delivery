package microarch.delivery.adapters.out.postgres.courier

import java.util.UUID
import microarch.delivery.core.domain.model.courier.Courier
import org.springframework.data.jpa.repository.JpaRepository

interface CourierJpaRepository : JpaRepository<Courier, UUID>
