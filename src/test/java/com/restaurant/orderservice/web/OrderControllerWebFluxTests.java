package com.restaurant.orderservice.web;

import com.restaurant.orderservice.domain.Order;
import com.restaurant.orderservice.domain.OrderService;
import com.restaurant.orderservice.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebFluxTest(OrderController.class)
public class OrderControllerWebFluxTests {

    @Autowired
    private WebTestClient webClient;
    @MockBean
    private OrderService orderService;

    @Test
    void whenFoodNotAvailableThenRejectOrder() {
        var orderRequest = new OrderRequest("4546745467", 3);
        var expectedOrder = OrderService.buildRejectedOrder(
                orderRequest.ref(), orderRequest.quantity());
        given(orderService.submitOrder(
                orderRequest.ref(), orderRequest.quantity())
        ).willReturn(Mono.just(expectedOrder));
        webClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).value(actualOrder -> {
                    assertThat(actualOrder).isNotNull();
                    assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
                });
    }
}