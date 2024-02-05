package com.restaurant.orderservice.domain;


import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
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
        @Version
        int version


) {
    public static Order of(String foodRef, String foodDescription, Integer quantity, Double foodPrice, OrderStatus orderStatus) {
        return new Order(null, foodRef, foodDescription, quantity, foodPrice, orderStatus, null, null, 0);
    }
}
