package com.restaurant.orderservice.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "restaurant")
public record ClientProperties(

        @NotNull
        URI tastyServiceUri
) {


}
