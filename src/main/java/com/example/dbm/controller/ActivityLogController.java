package com.example.dbm.controller;

import com.example.dbm.service_container.ServiceContainer;


import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ActivityLogController extends Controller {

    public ActivityLogController(ServiceContainer serviceContainer) {
        super(serviceContainer);
    }

    public void activityLogPaginate(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            String user_id = validateToken(ctx);
            JsonObject jsonObject = buffer.toJsonObject();
            jsonObject.put("user_id", user_id);
            // System.out.println(java.time.LocalDateTime.now());
            this.dbConnection.callStoredFunctionF1("example_platform.activity_log__paginate", jsonObject)
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
