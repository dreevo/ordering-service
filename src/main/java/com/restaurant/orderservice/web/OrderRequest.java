package com.restaurant.orderservice.web;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
        @NotBlank(message = "The food ref must be defined.")
        String ref,
        @NotNull(message = "The food quantity must be defined.")
        @Min(value = 1, message = "You must order atleast 1 item.")
        @Max(value = 5, message = "You cannot order more than 5 items.")
        Integer quantity

) {
}
