package com.restaurant.orderservice;

import com.restaurant.orderservice.domain.Order;
import com.restaurant.orderservice.domain.OrderStatus;
import com.restaurant.orderservice.event.OrderAcceptedMessage;
import com.restaurant.orderservice.web.Food;
import com.restaurant.orderservice.web.FoodClient;
import com.restaurant.orderservice.web.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import static org.mockito.BDDMockito.given;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class OrderServiceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutputDestination output;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private FoodClient foodClient;

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderServiceApplicationTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);
    }

    private static String r2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%s/%s", postgresql.getHost(),
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgresql.getDatabaseName());
    }

    @Test
    void whenGetOrdersThenReturn() throws IOException {
        String foodRef = "1234567893";
        Food food = new Food(foodRef, "desc", "Mr Chef", 9.90);
        given(foodClient.getFoodByRef(foodRef)).willReturn(Mono.just(food));
        OrderRequest orderRequest = new OrderRequest(foodRef, 1);
        Order expectedOrder = webTestClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class).value(orders -> {
                    assertThat(orders.stream().filter(order -> order.foodRef().equals(foodRef)).findAny()).isNotEmpty();
                });
    }

    @Test
    void whenPostRequestAndFoodExistsThenOrderAccepted() throws IOException {
        String foodRef = "1234567899";
        Food food = new Food(foodRef, "desc", "Mr Chef", 9.90);
        given(foodClient.getFoodByRef(foodRef)).willReturn(Mono.just(food));
        OrderRequest orderRequest = new OrderRequest(foodRef, 3);

        Order createdOrder = webTestClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.foodRef()).isEqualTo(orderRequest.ref());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.foodDescription()).isEqualTo(food.description() + " - " + food.chef());
        assertThat(createdOrder.foodPrice()).isEqualTo(food.price());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);

        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(createdOrder.id()));
    }

    @Test
    void whenPostRequestAndFoodNotExistsThenOrderRejected() {
        String foodRef = "1234567894";
        given(foodClient.getFoodByRef(foodRef)).willReturn(Mono.empty());
        OrderRequest orderRequest = new OrderRequest(foodRef, 3);

        Order createdOrder = webTestClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.foodRef()).isEqualTo(orderRequest.ref());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
    }

}