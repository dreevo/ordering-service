package com.restaurant.orderservice.domain;


import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table(name = "orders")
public record Order(
        @Id
        Long id,

        String foodRef,
        String foodDescription,
        Integer quantity,
        Double foodPrice,
        OrderStatus status,

        @CreatedDate
        Instant createdDate,
        @LastModifiedDate
        Instant lastModifiedDate,
        @CreatedBy
        String createdBy,

        @LastModifiedBy
        String lastModifiedBy,

        @Version
        int version


) {
    public static Order of(String foodRef, String foodDescription, Integer quantity, Double foodPrice, OrderStatus orderStatus) {
        return new Order(null, foodRef, foodDescription, quantity, foodPrice, orderStatus,
                null, null, null, null, 0);
    }
}
