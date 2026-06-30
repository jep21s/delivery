package microarch.delivery.core.ports.courier

import java.util.UUID
import microarch.delivery.core.domain.model.courier.Courier

interface CourierRepository {
    fun add(courier: Courier): Courier

    fun update(courier: Courier): Courier

    fun getById(id: UUID): Courier?

    fun getAll(): List<Courier>
}
