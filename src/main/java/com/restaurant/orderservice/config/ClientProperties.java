package com.restaurant.orderservice.config;

import java.net.URI;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "restaurant")
public record ClientProperties(

        @NotNull
        URI tastyServiceUri
) {


}
