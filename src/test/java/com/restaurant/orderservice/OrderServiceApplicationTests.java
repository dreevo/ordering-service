package com.restaurant.orderservice;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.orderservice.domain.Order;
import com.restaurant.orderservice.domain.OrderStatus;
import com.restaurant.orderservice.event.OrderAcceptedMessage;
import com.restaurant.orderservice.web.Food;
import com.restaurant.orderservice.web.FoodClient;
import com.restaurant.orderservice.web.OrderRequest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class OrderServiceApplicationTests {

    private static KeycloakToken johnTokens;
    private static KeycloakToken willTokens;

    @Container
    private static final KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:19.0")
            .withRealmImportFile("test-realm-config.json");

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
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakContainer.getAuthServerUrl() + "realms/restaurant");
    }

    private static String r2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%s/%s", postgresql.getHost(),
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgresql.getDatabaseName());
    }

    @BeforeAll
    static void generateAccessTokens() {
        WebClient webClient = WebClient.builder()
                .baseUrl(keycloakContainer.getAuthServerUrl() + "realms/restaurant/protocol/openid-connect/token")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();

        johnTokens = authenticateWith("john", "password", webClient);
        willTokens = authenticateWith("will", "password", webClient);
    }

    @Test
    void whenGetOrdersThenReturn() throws IOException {
        String foodRef = "1234567893";
        Food food = new Food(foodRef, "desc", "Mr Chef", 9.90);
        given(foodClient.getFoodByRef(foodRef)).willReturn(Mono.just(food));
        OrderRequest orderRequest = new OrderRequest(foodRef, 1);
        Order expectedOrder = webTestClient.post().uri("/orders")
                .headers(headers -> headers.setBearerAuth(willTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient.get().uri("/orders")
                .headers(headers -> headers.setBearerAuth(willTokens.accessToken()))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class).value(orders -> {
                    List<Long> orderIds = orders.stream()
                            .map(Order::id)
                            .collect(Collectors.toList());
                    assertThat(orderIds).contains(expectedOrder.id());
                });
    }

    @Test
    void whenGetOrdersForAnotherUserThenNotReturned() throws IOException {
        String foodRef = "1234567893";
        Food food = new Food(foodRef, "desc", "Mr Chef", 9.90);
        given(foodClient.getFoodByRef(foodRef)).willReturn(Mono.just(food));
        OrderRequest orderRequest = new OrderRequest(foodRef, 1);

        Order orderByWill = webTestClient.post().uri("/orders")
                .headers(headers -> headers.setBearerAuth(willTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();
        assertThat(orderByWill).isNotNull();
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(orderByWill.id()));

        Order orderByJohn = webTestClient.post().uri("/orders")
                .headers(headers -> headers.setBearerAuth(johnTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();
        assertThat(orderByJohn).isNotNull();
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(orderByJohn.id()));

        webTestClient.get().uri("/orders")
                .headers(headers -> headers.setBearerAuth(willTokens.accessToken()))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class)
                .value(orders -> {
                    List<Long> orderIds = orders.stream()
                            .map(Order::id)
                            .collect(Collectors.toList());
                    assertThat(orderIds).contains(orderByWill.id());
                    assertThat(orderIds).doesNotContain(orderByJohn.id());
                });
    }

    @Test
    void whenPostRequestAndFoodExistsThenOrderAccepted() throws IOException {
        String foodRef = "1234567899";
        Food food = new Food(foodRef, "desc", "Mr Chef", 9.90);
        given(foodClient.getFoodByRef(foodRef)).willReturn(Mono.just(food));
        OrderRequest orderRequest = new OrderRequest(foodRef, 3);

        Order createdOrder = webTestClient.post().uri("/orders")
                .headers(headers -> headers.setBearerAuth(willTokens.accessToken()))
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
                .headers(headers -> headers.setBearerAuth(willTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.foodRef()).isEqualTo(orderRequest.ref());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
    }


    private static KeycloakToken authenticateWith(String username, String password, WebClient webClient) {
        return webClient
                .post()
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "restaurant-test")
                        .with("username", username)
                        .with("password", password)
                )
                .retrieve()
                .bodyToMono(KeycloakToken.class)
                .block();
    }

    private record KeycloakToken(String accessToken) {

        @JsonCreator
        private KeycloakToken(@JsonProperty("access_token") final String accessToken) {
            this.accessToken = accessToken;
        }

    }

}