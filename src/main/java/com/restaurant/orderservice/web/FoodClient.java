package com.restaurant.orderservice.web;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class FoodClient {

    private static final String FOOD_ROOT_API = "/food/";
    private final WebClient webClient;

    public FoodClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Food> getFoodByRef(String ref) {
        return webClient
                .get()
                .uri(FOOD_ROOT_API + ref)
                .retrieve()
                .bodyToMono(Food.class)
                .timeout(Duration.ofSeconds(3), Mono.empty())
                .onErrorResume(WebClientResponseException.NotFound.class,
                        exception -> Mono.empty())
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                .onErrorResume(Exception.class,
                        exception -> Mono.empty());
    }
}
