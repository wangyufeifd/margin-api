package com.margin.api;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Application class
 */
@ExtendWith(VertxExtension.class)
class ApplicationTest {

    @Test
    void testApplicationExists() {
        assertNotNull(Application.class);
    }

    @Test
    void testMainMethodExists() throws NoSuchMethodException {
        assertNotNull(Application.class.getMethod("main", String[].class));
    }

    @Test
    void testMainVerticleDeployment(Vertx vertx, VertxTestContext testContext) {
        JsonObject config = new JsonObject()
                .put("http.port", 8081)
                .put("http.host", "localhost");
        
        MainVerticle verticle = new MainVerticle(config);
        
        vertx.deployVerticle(verticle, testContext.succeeding(id -> {
            testContext.completeNow();
        }));
    }

    @Test
    void testHealthEndpoint(Vertx vertx, VertxTestContext testContext) {
        JsonObject config = new JsonObject()
                .put("http.port", 8082)
                .put("http.host", "localhost");
        
        MainVerticle verticle = new MainVerticle(config);
        WebClient client = WebClient.create(vertx);
        
        vertx.deployVerticle(verticle, testContext.succeeding(id -> {
            client.get(8082, "localhost", "/health")
                .send(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        assertEquals(200, response.statusCode());
                        JsonObject body = response.bodyAsJsonObject();
                        assertEquals("UP", body.getString("status"));
                        testContext.completeNow();
                    });
                }));
        }));
    }
}

