package com.example.dbm.controller;

import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TutorialController extends Controller {
    public TutorialController(ServiceContainer serviceContainer) {
        super(serviceContainer);
    }

    public void tutorialUpsert(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            jsonObject.put("user_id", validateToken(ctx));
            jsonObject.put("ip", getClientIp(ctx));
            this.dbConnection.callStoredFunctionF1("example_platform.tutorial__upsert", jsonObject)
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

    public void tutorialPaginate(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            jsonObject.put("user_id", validateToken(ctx));
            jsonObject.put("ip", getClientIp(ctx));
            this.dbConnection.callStoredFunctionF1("example_platform.tutorial__paginate", jsonObject)
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
}
