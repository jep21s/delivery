package microarch.delivery.adapters.`in`.http

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import microarch.delivery.BaseIntegrationTest
import microarch.delivery.application.commands.AssignOrderCommand
import microarch.delivery.application.commands.AssignOrderCommandHandler
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.ports.courier.CourierRepository
import microarch.delivery.core.ports.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@SpringBootTest(webEnvironment = RANDOM_PORT)
class DeliveryControllerIntegrationTest : BaseIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var assignOrderCommandHandler: AssignOrderCommandHandler

    @Autowired
    private lateinit var courierRepository: CourierRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    // ========================= createOrder =========================

    @Test
    fun `createOrder returns 201 with orderId`() {
        // Given — новый заказ
        val orderId = UUID.randomUUID()

        // When
        val response = post("/api/v1/orders", newOrderMap(orderId))

        // Then
        val body = json(response)
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.CREATED) },
            { assertThat(body["orderId"]?.asText()).describedAs("orderId").isEqualTo(orderId.toString()) },
        )
    }

    @Test
    fun `createOrder with zero volume returns 400 Error`() {
        // Given — заказ с нулевым объёмом
        val orderId = UUID.randomUUID()

        // When
        val response = post("/api/v1/orders", newOrderMap(orderId, volume = 0))

        // Then
        val body = json(response)
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.BAD_REQUEST) },
            { assertThat(body["code"]?.asInt()).describedAs("code").isEqualTo(400) },
            { assertThat(body["message"]?.asText()).describedAs("message").isNotBlank() },
        )
    }

    @Test
    fun `createOrder with empty country returns 400 Error`() {
        // Given — заказ с пустой страной
        val orderId = UUID.randomUUID()

        // When
        val response = post("/api/v1/orders", newOrderMap(orderId, country = ""))

        // Then
        val body = json(response)
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.BAD_REQUEST) },
            { assertThat(body["code"]?.asInt()).describedAs("code").isEqualTo(400) },
            { assertThat(body["message"]?.asText()).describedAs("message").isNotBlank() },
        )
    }

    // ========================= createCourier =========================

    @Test
    fun `createCourier returns 201 with courierId`() {
        // Given — новый курьер
        val request = mapOf("name" to "Ivan")

        // When
        val response = post("/api/v1/couriers", request)

        // Then
        val body = json(response)
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.CREATED) },
            { assertThat(body["courierId"]?.asText()).describedAs("courierId").isNotBlank() },
        )
    }

    // ========================= moveCourier =========================

    @Test
    fun `moveCourier returns 204 on success`() {
        // Given — курьер в локации (3, 3), перемещение на 1 клетку
        val courier = courierRepository.add(Courier.create("Ivan", LocationValue.createOrThrow(3, 3)))

        // When
        val response = post("/api/v1/couriers/${courier.id}/move", mapOf("x" to 3, "y" to 4))

        // Then
        assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `moveCourier on unknown courier returns 404 Error`() {
        // Given — несуществующий courierId
        val unknownId = UUID.randomUUID()

        // When
        val response = post("/api/v1/couriers/$unknownId/move", mapOf("x" to 5, "y" to 5))

        // Then
        val body = json(response)
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.NOT_FOUND) },
            { assertThat(body["code"]?.asInt()).describedAs("code").isEqualTo(404) },
            { assertThat(body["message"]?.asText()).describedAs("message").isNotBlank() },
        )
    }

    @Test
    fun `moveCourier with invalid location returns 409 Error`() {
        // Given — координата x=0 не проходит доменную валидацию (минимум 1)
        val courierId = createCourierViaHttp()

        // When
        val response = post("/api/v1/couriers/$courierId/move", mapOf("x" to 0, "y" to 5))

        // Then
        val body = json(response)
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.CONFLICT) },
            { assertThat(body["code"]?.asInt()).describedAs("code").isEqualTo(409) },
            { assertThat(body["message"]?.asText()).describedAs("message").isNotBlank() },
        )
    }

    // ========================= completeOrder =========================

    @Test
    fun `completeOrder returns 204 when courier is at order location`() {
        // Given — курьер и заказ в одной локации, заказ назначен курьеру
        val location = LocationValue.createOrThrow(5, 5)
        val courier = courierRepository.add(Courier.create("Courier", location))
        val order =
            orderRepository.add(
                Order.create(UUID.randomUUID(), location, VolumeValue(5)),
            )
        assignOrderCommandHandler.handle(AssignOrderCommand.create().getOrNull()!!)

        // When
        val response = post("/api/v1/couriers/${courier.id}/orders/${order.id}/complete")

        // Then
        assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `completeOrder on unknown courier returns 404 Error`() {
        // Given — заказ существует, но курьер — нет
        val orderId = createOrderViaHttp()
        val unknownCourierId = UUID.randomUUID()

        // When
        val response = post("/api/v1/couriers/$unknownCourierId/orders/$orderId/complete")

        // Then
        val body = json(response)
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.NOT_FOUND) },
            { assertThat(body["code"]?.asInt()).describedAs("code").isEqualTo(404) },
            { assertThat(body["message"]?.asText()).describedAs("message").isNotBlank() },
        )
    }

    @Test
    fun `completeOrder on order not assigned to courier returns 404 Error`() {
        // Given — и курьер, и заказ существуют, но заказ не назначен этому курьеру
        val courierId = createCourierViaHttp(name = "Courier A")
        val orderId = createOrderViaHttp()

        // When
        val response = post("/api/v1/couriers/$courierId/orders/$orderId/complete")

        // Then
        val body = json(response)
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.NOT_FOUND) },
            { assertThat(body["code"]?.asInt()).describedAs("code").isEqualTo(404) },
            { assertThat(body["message"]?.asText()).describedAs("message").isNotBlank() },
        )
    }

    // ========================= getCouriers =========================

    @Test
    fun `getCouriers returns 200 with all couriers`() {
        // Given — два курьера
        val id1 = createCourierViaHttp(name = "Alice")
        val id2 = createCourierViaHttp(name = "Bob")

        // When
        val response = get("/api/v1/couriers")

        // Then
        val ids = json(response).map { it["id"].asText() }.toSet()
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.OK) },
            { assertThat(ids).describedAs("contains Alice").contains(id1.toString()) },
            { assertThat(ids).describedAs("contains Bob").contains(id2.toString()) },
        )
    }

    // ========================= getOrders =========================

    @Test
    fun `getOrders returns 200 with active orders`() {
        // Given — один незавершённый заказ
        val orderId = createOrderViaHttp()

        // When
        val response = get("/api/v1/orders/active")

        // Then
        val ids = json(response).map { it["id"].asText() }.toSet()
        assertAll(
            { assertThat(response.statusCode).describedAs("status").isEqualTo(HttpStatus.OK) },
            { assertThat(ids).describedAs("contains created order").contains(orderId.toString()) },
        )
    }

    // ========================= Helpers =========================

    private fun url(path: String): String = "http://localhost:$port$path"

    private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

    private fun post(
        path: String,
        body: Any? = null,
    ): ResponseEntity<String> = restTemplate.postForEntity(url(path), HttpEntity(body, jsonHeaders()), String::class.java)

    private fun get(path: String): ResponseEntity<String> = restTemplate.getForEntity(url(path), String::class.java)

    private fun json(response: ResponseEntity<String>) = objectMapper.readTree(response.body)

    private fun createCourierViaHttp(name: String = "Ivan"): UUID {
        val response = post("/api/v1/couriers", mapOf("name" to name))
        return UUID.fromString(json(response)["courierId"].asText())
    }

    private fun createOrderViaHttp(orderId: UUID = UUID.randomUUID()): UUID {
        post("/api/v1/orders", newOrderMap(orderId))
        return orderId
    }

    private fun newOrderMap(
        orderId: UUID,
        country: String = "Russia",
        volume: Int = 5,
    ): Map<String, Any> =
        mapOf(
            "id" to orderId.toString(),
            "address" to
                mapOf(
                    "country" to country,
                    "city" to "Moscow",
                    "street" to "Tverskaya",
                    "house" to "1",
                    "apartment" to "42",
                ),
            "volume" to volume,
        )
}
