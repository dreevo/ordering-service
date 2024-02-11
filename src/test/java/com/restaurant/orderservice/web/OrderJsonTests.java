package com.restaurant.orderservice.web;

import com.restaurant.orderservice.domain.Order;
import com.restaurant.orderservice.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class OrderJsonTests {

    @Autowired
    private JacksonTester<Order> json;

    @Test
    void testSerialize() throws Exception {
        var order = new Order(394L, "4678974", "desc",
                1, 5.9, OrderStatus.ACCEPTED, Instant.now(), Instant.now(), 21);
        var jsonContent = json.write(order);
        assertThat(jsonContent).extractingJsonPathNumberValue("@.id")
                .isEqualTo(order.id().intValue());
        assertThat(jsonContent).extractingJsonPathStringValue("@.foodRef")
                .isEqualTo(order.foodRef());
        assertThat(jsonContent).extractingJsonPathStringValue("@.foodDescription")
                .isEqualTo(order.foodDescription());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.foodPrice")
                .isEqualTo(order.foodPrice());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.quantity")
                .isEqualTo(order.quantity());
        assertThat(jsonContent).extractingJsonPathStringValue("@.status")
                .isEqualTo(order.status().toString());
        assertThat(jsonContent).extractingJsonPathStringValue("@.createdDate")
                .isEqualTo(order.createdDate().toString());
        assertThat(jsonContent).extractingJsonPathStringValue("@.lastModifiedDate")
                .isEqualTo(order.lastModifiedDate().toString());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.version")
                .isEqualTo(order.version());
    }

}