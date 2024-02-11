package com.restaurant.orderservice.web;

import com.restaurant.orderservice.web.Food;
import com.restaurant.orderservice.web.FoodClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

public class FoodClientTests {

    private MockWebServer mockWebServer;
    private FoodClient foodClient;

    @BeforeEach
    void setup() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        var webClient = WebClient.builder().baseUrl(mockWebServer.url("/").uri().toString()).build();
        this.foodClient = new FoodClient(webClient);
    }

    @AfterEach
    void clean() throws IOException {
        this.mockWebServer.shutdown();
    }


    @Test
    void whenFoodExistsThenReturnBook() {
        var foodRef = "1234567890";
        var mockResponse = new MockResponse().addHeader(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE).setBody(String.format("""
                {
                "ref": %s,
                "description": "desc1",
                "chef": "MrChef",
                "price": 9.90
                }
                """, foodRef));
        mockWebServer.enqueue(mockResponse);
        Mono<Food> food = foodClient.getFoodByRef(foodRef);
        StepVerifier.create(food).expectNextMatches(b -> b.ref().equals(foodRef)).verifyComplete();
    }

    @Test
    void whenFoodNotExistsThenReturnEmpty() {
        var foodRef = "1234567891";

        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(404);

        mockWebServer.enqueue(mockResponse);

        StepVerifier.create(foodClient.getFoodByRef(foodRef))
                .expectNextCount(0)
                .verifyComplete();
    }

}