package com.example.dbm.controller;

import java.nio.file.Paths;

import com.example.dbm.dbconnection.DbConnection;
import com.example.dbm.middleware.JwtMiddleware;
import com.example.dbm.service.DataValidator;
import com.example.dbm.service.SendMailService;
import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

public abstract class Controller {
    protected ServiceContainer serviceContainer;
    protected DbConnection dbConnection;
    protected JwtMiddleware jwtMiddleware;
    protected DataValidator dataValidator;
     protected SendMailService sendMailService;

    public Controller(ServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
        this.dbConnection = serviceContainer.resolve(DbConnection.class);
        this.jwtMiddleware = serviceContainer.resolve(JwtMiddleware.class);
        this.dataValidator = serviceContainer.resolve(DataValidator.class);
        this.sendMailService = serviceContainer.resolve(SendMailService.class);
    }


    public String validateToken(RoutingContext ctx) {
        User user = ctx.user();
        String user_id = null;
        if (user != null) {
            JsonObject principal = user.principal();
            user_id = principal.getString("id");
        }
        return user_id;
    }

    public String getClientIp(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        String clientIP = request.remoteAddress().host();
        // System.out.println("clientIP: " + clientIP);
        return clientIP;
    }

    protected void render(RoutingContext context, String viewName) {
        String absolutePath = Paths.get("src\\main\\java\\com\\example\\dbm\\views", viewName).toAbsolutePath()
                .toString();
        context.vertx().fileSystem().readFile(absolutePath, result -> {
            if (result.succeeded()) {
                context.response()
                        .putHeader("Content-Type", "text/html")
                        .end(result.result());
            } else {
                context.fail(result.cause());
            }
        });
    }

    public void handleResponse(RoutingContext ctx, JsonObject result) {
        // if (result.getJsonArray("ret_val") != null &&
        // !result.getJsonArray("ret_val").isEmpty()) {
        // String userId =
        // result.getJsonArray("ret_val").getJsonObject(0).getString("user_id");
        // String token = this.jwtMiddleware.generateToken(ctx, userId);
        // ctx.response()
        // .putHeader("Authorization", "Bearer " + token)
        // .end(result.encode());
        // } else {
        ctx.response()
                .putHeader("Content-Type", "application/json")

                .end(result.encode());
        // }
    }

        // protected void render(RoutingContext context, String viewName) {
    // context.vertx().fileSystem().readFile("views/" + viewName, result -> {
    // if (result.succeeded()) {
    // context.response()
    // .putHeader("Content-Type", "text/html")
    // .end(result.result());
    // } else {
    // context.fail(result.cause());
    // }
    // });
    // }

}
