package com.restaurant.orderservice.domain;

import com.restaurant.orderservice.event.OrderAcceptedMessage;
import com.restaurant.orderservice.event.OrderDispatchedMessage;
import com.restaurant.orderservice.web.Food;
import com.restaurant.orderservice.web.FoodClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final FoodClient foodClient;
    private final StreamBridge streamBridge;


    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);


    public OrderService(OrderRepository orderRepository, FoodClient foodClient, StreamBridge streamBridge) {
        this.orderRepository = orderRepository;
        this.foodClient = foodClient;
        this.streamBridge = streamBridge;
    }

    public Flux<Order> getAllOrders(String userId) {
        return orderRepository.findAllByCreatedBy(userId);
    }




    public Mono<Order> submitOrder(String ref, int quantity) {
        return foodClient.getFoodByRef(ref).map(food -> buildAcceptedOrder(food, quantity))
                .defaultIfEmpty(buildRejectedOrder(ref, quantity))
                .flatMap(orderRepository::save)
                .doOnNext(this::publishOrderAcceptedEvent);
    }

    public static Order buildRejectedOrder(String ref, int quantity) {
        return Order.of(ref, null, quantity, null, OrderStatus.REJECTED);
    }

    public static Order buildAcceptedOrder(Food food, int quantity) {
        return Order.of(food.ref(), food.description() + " - " + food.chef(), quantity, food.price(), OrderStatus.ACCEPTED);
    }

    private void publishOrderAcceptedEvent(Order order) {
        if (!order.status().equals(OrderStatus.ACCEPTED)) {
            return;
        }
        var orderAcceptedMessage =
                new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event with id: {}", order.id());
        var result = streamBridge.send("acceptOrder-out-0",
                orderAcceptedMessage);
        log.info("Result of sending data for order with id {}: {}",
                order.id(), result);
    }


    public Flux<Order> consumeOrderDispatchedEvent(
            Flux<OrderDispatchedMessage> flux
    ) {
        return flux
                .flatMap(message ->
                        orderRepository.findById(message.orderId()))
                .map(this::buildDispatchedOrder)
                .flatMap(orderRepository::save);
    }

    private Order buildDispatchedOrder(Order existingOrder) {
        return new Order(
                existingOrder.id(),
                existingOrder.foodRef(),
                existingOrder.foodDescription(),
                existingOrder.quantity(),
                existingOrder.foodPrice(),
                OrderStatus.DISPATCHED,
                existingOrder.createdDate(),
                existingOrder.lastModifiedDate(),
                existingOrder.createdBy(),
                existingOrder.lastModifiedBy(),
                existingOrder.version()
        );
    }
}
