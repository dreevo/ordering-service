package com.restaurant.orderservice.web;

public record Food(

        String ref,
        String description,
        String chef,
        Double price
) {
}
