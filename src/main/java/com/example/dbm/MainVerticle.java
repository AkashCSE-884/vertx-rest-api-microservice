package com.example.dbm;

import java.net.CookieHandler;


import com.example.dbm.dbconnection.DbConnection;
import com.example.dbm.middleware.JwtMiddleware;
import com.example.dbm.router.ApiRouter;
import com.example.dbm.router.WebRouter;
import com.example.dbm.service.DataValidator;
import com.example.dbm.service.SendMailService;
import com.example.dbm.service_container.ServiceContainer;
import io.vertx.core.AbstractVerticle;

import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {

        ServiceContainer serviceContainer = new ServiceContainer();
        Router router = Router.router(vertx);

        CorsHandler corsHandler = CorsHandler.create("*");
        corsHandler.allowedMethod(io.vertx.core.http.HttpMethod.GET);
        corsHandler.allowedMethod(io.vertx.core.http.HttpMethod.POST);
        corsHandler.allowedMethod(io.vertx.core.http.HttpMethod.PUT);
        corsHandler.allowedMethod(io.vertx.core.http.HttpMethod.DELETE);

        corsHandler.allowedHeader("Access-Control-Expose-Headers");
        corsHandler.allowedHeader("Authorization");
        corsHandler.allowedHeader("Content-Type");
        corsHandler.allowedHeader("Access-Control-Allow-Origin");
        corsHandler.allowedHeader("Access-Control-Allow-Headers");

        corsHandler.exposedHeader("Authorization");

        router.route().handler(corsHandler);

        // serviceContainer.resgisterDbConnction(new JwtMiddleware(vertx), vertx);

        // WebRouter webApiRouter = new WebRouter(vertx, router, serviceContainer);
        ApiRouter apiRouter = new ApiRouter(vertx, router, serviceContainer);
        // SendMailController sendMailController = new SendMailController(serviceContainer);


        serviceContainer.resgisterDbConnction(DbConnection.getInstance(vertx), vertx);
        serviceContainer.resgisterDbConnction(new JwtMiddleware(vertx), vertx);
        serviceContainer.resgisterDbConnction(new SendMailService(vertx), vertx);
        serviceContainer.registerService(DataValidator.class);

        vertx.createHttpServer()
                // .requestHandler(webApiRouter.getRouter())
                .requestHandler(apiRouter.getRouter())
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                });

    }

}

// vertx.createHttpServer()
// .requestHandler(webApiRouter.getRouter())
// .listen(8080);

// startPromise.complete();

// JsonArray params = new JsonArray().add(name, "parameter1").add("parameter2");

// dbConnection.callStoredFunction2("example_plan.filter_paginate", params,
// result -> {
// if (result.succeeded()) {
// String jsonResult = result.result();
// hc.test();
// System.out.println("Stored function result: ");

// } else {
// Throwable exception = result.cause();
// System.err.println("Error calling stored function: " +
// exception.getMessage());
// }
// });
