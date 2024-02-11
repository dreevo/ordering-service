package com.restaurant.orderservice.event;

public record OrderAcceptedMessage(

        Long orderId
) {
}
