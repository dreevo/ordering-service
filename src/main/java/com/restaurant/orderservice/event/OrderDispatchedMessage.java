package com.restaurant.orderservice.event;

public record OrderDispatchedMessage(

        Long orderId
) {
}
