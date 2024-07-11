package com.example.dbm.controller;

import com.example.dbm.middleware.JwtMiddleware;
import com.example.dbm.service_container.ServiceContainer;


import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class LoginController extends Controller {
    private JwtMiddleware jwtMiddleware;

    public LoginController(ServiceContainer serviceContainer) {
        super(serviceContainer);

    }

    public void generateToken(RoutingContext ctx) {

        this.jwtMiddleware = serviceContainer.resolve(JwtMiddleware.class);

        ctx.request().bodyHandler(buffer -> {
            // String requestBody = buffer.toString();
            JsonObject jsonObject = buffer.toJsonObject();
            String name = jsonObject.getString("name");
            String id = jsonObject.getString("id");

            if (id == null || id.isEmpty()) {
                JsonObject errorResponse = new JsonObject()
                        .put("error", "ID parameter is required and cannot be empty.");
                ctx.response()
                        .setStatusCode(401)
                        .end(errorResponse.encode());

            }
            System.out.println("Received request body id: " + id);
            String token = this.jwtMiddleware.generateToken(ctx, id);
            // System.out.print(token);
            ctx.response()
                    .putHeader("Authorization", "Bearer " + token)
                    .end("{\"status\": \"success\"}");
        });
        // String id = ctx.request().getParam("id");
        // JsonObject payload = ctx.getBodyAsJson();
        // String requestBody = ctx.getBodyAsString();
        // System.out.println("Received request body: " + requestBody);
        // System.out.println(payload);
        // if (id == null || id.isEmpty()) {

        // JsonObject errorResponse = new JsonObject()
        // .put("error", "ID parameter is required and cannot be empty.");
        // ctx.response()
        // .setStatusCode(401)
        // .end(errorResponse.encode());

        // }

        // String token = this.jwtMiddleware.generateToken(ctx);

        // ctx.response().putHeader("Authorization", "Bearer " + token);
        // JsonObject response = new JsonObject().put("token", token);
        // ctx.response().end(response.encode());
        // String token = jwtMiddleware::generateToken(ctx)

    }
}
