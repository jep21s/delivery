package microarch.delivery.adapters.`in`.http

import arrow.core.getOrElse
import jakarta.validation.Valid
import java.util.UUID
import microarch.delivery.adapters.`in`.http.mappers.CourierMapperImpl
import microarch.delivery.adapters.`in`.http.mappers.OrderMapperImpl
import microarch.delivery.adapters.inbound.http.api.ApiApi
import microarch.delivery.adapters.inbound.http.model.Courier
import microarch.delivery.adapters.inbound.http.model.CreateCourierResponse
import microarch.delivery.adapters.inbound.http.model.CreateOrderResponse
import microarch.delivery.adapters.inbound.http.model.Location
import microarch.delivery.adapters.inbound.http.model.NewCourier
import microarch.delivery.adapters.inbound.http.model.NewOrder
import microarch.delivery.adapters.inbound.http.model.Order
import microarch.delivery.application.commands.CompleteOrderCommand
import microarch.delivery.application.commands.CompleteOrderCommandHandler
import microarch.delivery.application.commands.CreateCourierCommand
import microarch.delivery.application.commands.CreateCourierCommandHandler
import microarch.delivery.application.commands.CreateOrderCommand
import microarch.delivery.application.commands.CreateOrderCommandHandler
import microarch.delivery.application.commands.MoveCourierCommand
import microarch.delivery.application.commands.MoveCourierCommandHandler
import microarch.delivery.application.queries.GetAllCouriersQuery
import microarch.delivery.application.queries.GetAllCouriersQueryHandler
import microarch.delivery.application.queries.GetNotCompletedOrdersQuery
import microarch.delivery.application.queries.GetNotCompletedOrdersQueryHandler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class DeliveryController(
    private val createOrderCommandHandler: CreateOrderCommandHandler,
    private val createCourierCommandHandler: CreateCourierCommandHandler,
    private val moveCourierCommandHandler: MoveCourierCommandHandler,
    private val completeOrderCommandHandler: CompleteOrderCommandHandler,
    private val getAllCouriersQueryHandler: GetAllCouriersQueryHandler,
    private val getNotCompletedOrdersQueryHandler: GetNotCompletedOrdersQueryHandler,
) : ApiApi {
    override fun createOrder(
        @Valid @RequestBody newOrder: NewOrder,
    ): ResponseEntity<CreateOrderResponse> {
        val command =
            CreateOrderCommand
                .create(
                    orderId = newOrder.id,
                    country = newOrder.address.country,
                    city = newOrder.address.city,
                    street = newOrder.address.street,
                    house = newOrder.address.house,
                    apartment = newOrder.address.apartment,
                    volume = newOrder.volume,
                ).getOrElse { error -> return error.toErrorResponse() }

        return createOrderCommandHandler.handle(command).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = {
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(CreateOrderResponse(orderId = command.orderId))
            },
        )
    }

    override fun createCourier(
        @Valid @RequestBody newCourier: NewCourier,
    ): ResponseEntity<CreateCourierResponse> {
        val command =
            CreateCourierCommand
                .create(newCourier.name)
                .getOrElse { error -> return error.toErrorResponse() }

        return createCourierCommandHandler.handle(command).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { courierId ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(CreateCourierResponse(courierId = courierId))
            },
        )
    }

    override fun moveCourier(
        @PathVariable courierId: UUID,
        @Valid @RequestBody location: Location,
    ): ResponseEntity<Unit> {
        val command =
            MoveCourierCommand
                .create(courierId, location.x, location.y)
                .getOrElse { error -> return error.toErrorResponse() }

        return moveCourierCommandHandler.handle(command).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.noContent().build() },
        )
    }

    override fun completeOrder(
        @PathVariable courierId: UUID,
        @PathVariable orderId: UUID,
    ): ResponseEntity<Unit> {
        val command =
            CompleteOrderCommand
                .create(courierId, orderId)
                .getOrElse { error -> return error.toErrorResponse() }

        return completeOrderCommandHandler.handle(command).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.noContent().build() },
        )
    }

    override fun getCouriers(): ResponseEntity<List<Courier>> {
        val query =
            GetAllCouriersQuery
                .create()
                .getOrElse { error -> return error.toErrorResponse() }

        return getAllCouriersQueryHandler.handle(query).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { dtos -> ResponseEntity.ok(dtos.map { CourierMapperImpl.toResponseDto(it) }) },
        )
    }

    override fun getOrders(): ResponseEntity<List<Order>> {
        val query =
            GetNotCompletedOrdersQuery
                .create()
                .getOrElse { error -> return error.toErrorResponse() }

        return getNotCompletedOrdersQueryHandler.handle(query).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { dtos -> ResponseEntity.ok(dtos.map { OrderMapperImpl.toResponseDto(it) }) },
        )
    }
}
