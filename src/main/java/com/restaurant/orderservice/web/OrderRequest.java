package com.restaurant.orderservice.web;


import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record OrderRequest(
        @NotBlank(message = "The food ref must be defined.")
        String ref,
        @NotNull(message = "The food quantity must be defined.")
        @Min(value = 1, message = "You must order at least 1 item.")
        @Max(value = 5, message = "You cannot order more than 5 items.")
        Integer quantity

) {
}
