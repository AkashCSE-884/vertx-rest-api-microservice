package com.example.dbm.controller;

import java.util.concurrent.CompletableFuture;

import com.example.dbm.service.DataValidator;
import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class HomeController extends Controller {
    public int i;

    public HomeController(ServiceContainer serviceContainer) {
        super(serviceContainer);
        i = 0;

    }

    public void index(RoutingContext ctx) {

        String name = ctx.request().getParam("name");

        JsonObject data = new JsonObject()
                .put("name", "John Doe")
                .put("password", "securepassword");
        String user_id = validateToken(ctx);
        System.out.println("User id: " + user_id);
        DataValidator.ValidationResult results = this.dataValidator.validateData(ctx, data, "name", "password");
        if (results.isValid()) {
            JsonObject jsonResponse = new JsonObject()
                    .put("country", name);
                  
            dbConnection.callStoredFunctionF1("example_plan.filter_paginate", jsonResponse)
                    .onSuccess(result -> {
                        // System.out.print(result.getString("filter_paginate"));
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode());
                    })
                    .onFailure(error -> {
                        System.err.println("Error: " + error.getMessage());
                    });

        } else {
            data.put("error", results.getErrors());
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(data.toString());
            System.out.println("Validation failed. Errors: " + results.getErrors());
        }

    }

    public void test(RoutingContext ctx) {

        // String name = ctx.request().getParam("name");

        JsonObject jsonResponse = new JsonObject();
        // .put("country", name);

        // ctx.response()
        // .putHeader("Content-Type", "application/json")
        // .end(jsonResponse.encode());

        String procedureName = "example_plan.filter_paginate";
        // JsonArray params = new JsonArray().add(request.params.get("country"));

        CompletableFuture<String> future = this.dbConnection.callStoredFunction(procedureName, jsonResponse);

        future.thenAccept(result -> {
            ctx.response().end(result);
        }).exceptionally(exception -> {
            exception.printStackTrace();
            return null;
        });

    }

    public void testApi(RoutingContext ctx) {

        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            jsonObject.put("ip", getClientIp(ctx));
            this.dbConnection.callStoredFunctionF1("example_platform.data_test__upsert", jsonObject)
                    .onSuccess(result -> {
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode());
                    })
                    .onFailure(error -> {
                        System.err.println("Error: " + error.getMessage());
                    });
        });

    }

    public void getTestApi(RoutingContext ctx) {

        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = new JsonObject();

            this.dbConnection.callStoredFunctionF1("example_platform.data_test__select", jsonObject)
                    .onSuccess(result -> {
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode());
                    })
                    .onFailure(error -> {
                        System.err.println("Error: " + error.getMessage());
                    });
        });

    }

    // public void viewHtml(RoutingContext ctx) {
    // String name = ctx.request().getParam("name");

    // JsonObject jsonResponse = new JsonObject()
    // .put("country", name);
    // render(ctx, "index.html");
    // }
}

// String procedureName = "example_plan.filter_paginate";
// // JsonArray params = new JsonArray().add(request.params.get("country"));

// CompletableFuture<String> future =
// dbConnection.callStoredFunction(procedureName, jsonResponse);

// future.thenAccept(result -> {

// System.out.println(result);
// }).exceptionally(exception -> {
// exception.printStackTrace();
// return null;
// });