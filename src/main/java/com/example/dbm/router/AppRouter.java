package com.example.dbm.router;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public class AppRouter {
    private final ServiceContainer serviceContainer;
    private static final int MAX_REQUESTS_PER_SECOND = 1;
    private int requestCounter = 0;

    public AppRouter(ServiceContainer serviceContainer, Vertx vertx) {
        this.serviceContainer = serviceContainer;
        vertx.setPeriodic(1000, id -> requestCounter = 0);
    }

    private void handleError(Exception e, RoutingContext routingContext) {
        String errorMessage;
        int statusCode;

        if (e instanceof ClassNotFoundException) {
            statusCode = 500;
            errorMessage = "Controller not found";
        } else if (e instanceof NoSuchMethodException) {
            statusCode = 500;
            errorMessage = "Method not found";
        } else if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException) {
            statusCode = 500;
            errorMessage = "Error invoking controller method";
        } else if (e instanceof IOException) {
            statusCode = 500;
            errorMessage = "Error processing request";
        } else if (e instanceof AccessDeniedException) {
            statusCode = 403;
            errorMessage = "Access denied";
        } else {
            statusCode = 500;
            errorMessage = "Server error";
        }

        String jsonResponse = String.format("{\"error\": \"%s\"}", errorMessage);
        routingContext.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(jsonResponse);
    }

    // public Handler<RoutingContext> routeHandler(String controllerName, String methodName) {
    //     return routingContext -> {

    //         try {

    //             String fullClassName = "com.example.dbm.controller." + controllerName;
    //             Class<?> controllerClass = Class.forName(fullClassName);

    //             if (!serviceContainer.isRegistered(controllerClass)) {
    //                 serviceContainer.registerService(controllerClass);
    //             }

    //             Object controllerInstance = serviceContainer.resolve(controllerClass);
    //             java.lang.reflect.Method method = controllerClass.getMethod(methodName, RoutingContext.class);
    //             method.invoke(controllerInstance, routingContext);

    //         } catch (Exception e) {
    //             handleError(e, routingContext);
    //         }

    //     };
    // }
    public Handler<RoutingContext> routeHandler(String controllerName, String methodName) {
        return routingContext -> {
           
            // if (requestCounter < MAX_REQUESTS_PER_SECOND) {

            //     requestCounter++;
                try {
                    String fullClassName = "com.example.dbm.controller." + controllerName;
                    Class<?> controllerClass = Class.forName(fullClassName);
    
                    if (!serviceContainer.isRegistered(controllerClass)) {
                        serviceContainer.registerService(controllerClass);
                    }
    
                    Object controllerInstance = serviceContainer.resolve(controllerClass);
                    java.lang.reflect.Method method = controllerClass.getMethod(methodName, RoutingContext.class);
                    method.invoke(controllerInstance, routingContext);
    
                } catch (Exception e) {
                    handleError(e, routingContext);
                }
            // } else {
            //     System.out.println("Too many requests. Please slow down.");
            //     routingContext.response()
            //         .setStatusCode(429)
            //         .end("Too many requests. Please slow down.");
            // }
        };
    }
}
