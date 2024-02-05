package com.restaurant.orderservice.domain;

import com.restaurant.orderservice.web.Food;
import com.restaurant.orderservice.web.FoodClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final FoodClient foodClient;

    public OrderService(OrderRepository orderRepository, FoodClient foodClient) {
        this.orderRepository = orderRepository;
        this.foodClient = foodClient;
    }

    public Flux<Order> getAllOrders() {
        return this.orderRepository.findAll();
    }


    public static Order buildRejectedOrder(String ref, int quantity) {
        return Order.of(ref, null, quantity, null, OrderStatus.REJECTED);
    }

    public Mono<Order> submitOrder(String ref, int quantity) {
        return foodClient.getFoodByRef(ref).map(food -> buildAcceptedOrder(food, quantity))
                .defaultIfEmpty(buildRejectedOrder(ref, quantity))
                .flatMap(orderRepository::save);
    }

    public static Order buildAcceptedOrder(Food food, int quantity) {
        return Order.of(food.ref(), food.description() + " - " + food.chef(), quantity, food.price(), OrderStatus.ACCEPTED);
    }

}
