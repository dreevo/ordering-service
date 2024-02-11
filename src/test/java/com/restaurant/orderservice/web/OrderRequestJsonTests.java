package com.restaurant.orderservice.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class OrderRequestJsonTests {

    @Autowired
    private JacksonTester<OrderRequest> json;

    @Test
    void testDeserialize() throws Exception {
        var content = """
                {
                    "ref": "467687956",
                    "quantity": 1
                }
                """;
        Assertions.assertThat(this.json.parse(content))
                .usingRecursiveComparison().isEqualTo(new OrderRequest("467687956", 1));
    }

}