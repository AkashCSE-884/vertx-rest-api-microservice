package com.example.dbm.router;

import com.example.dbm.middleware.JwtMiddleware;
import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.Vertx;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.JWTAuthHandler;

public class WebRouter extends AppRouter {
    private Router router;
    private ServiceContainer serviceContainer;
    private Vertx vertx;
    private JwtMiddleware jwtMiddleware;

    public WebRouter(Vertx vertx, Router router, ServiceContainer serviceContainer) {
        super(serviceContainer, vertx);
        this.serviceContainer = serviceContainer;
        this.router = router;
        this.vertx = vertx;

    }

    private void routes() {
        // Configure routes for web APIs
        // router.get("/api/data").handler(this::handleDataApi);

        JWTAuthHandler jwtAuthHandler = this.jwtMiddleware.createJWTAuthHandler();
        router.post("/login").handler(routeHandler("LoginController", "generateToken"));
        router.route("/api/*").handler(jwtAuthHandler);
        router.get("/api/home").handler(routeHandler("HomeController", "index"));
        router.get("/test").handler(ctx -> {
            ctx.response().end("Hello World!");
        });
        // Authentication routes
        // router.post("/register").handler(routeHandler("AuthController", "register"));
        // router.post("/logout").handler(routeHandler("AuthController", "logout"));
        // router.post("/refresh-token").handler(routeHandler("AuthController", "refreshToken"));
        
        //create four routes for each crud operation
       
        // router.get("/protected-route/data").handler(routeHandler("HomeController", "index"));
        // router.get("/protected-route/data").handler(jwtAuthHandler).handler(routeHandler("HomeController", "index"));

        // router.get("/login").handler(ctx -> jwtMiddleware.createToken("username"));
        // router.get("/prc").handler(jwtMiddleware.validateToken());

        // router.get("/login").respond(ctx -> Future.succeededFuture(new
        // JsonObject().put("token",jwtMiddleware.createToken("username"))));
        // router.get("/").respond(ctx -> Future.succeededFuture(new
        // JsonObject().put("hello", "world")));

        // router.post("/login").handler(jwtMiddleware::generateToken);
        router.route().last().handler(ctx -> {
            ctx.response()
                .setStatusCode(404)
                .putHeader("Content-Type", "application/json")
                .end("{\"error\": \"Resource not found\"}");
        });
    }

    public Router getRouter() {
        this.jwtMiddleware = serviceContainer.resolve(JwtMiddleware.class);
        routes();
        return router;
    }


}

// private Handler<RoutingContext> createRouteHandler(Class controllerName,
// String methodName) {
// return routingContext -> {
// try {
// // Convert the controller name string to a Class object
// // System.out.println(controllerName);
// // Class<?> controllerClass = controllerClass;
// // System.out.println("asdf " + controllerClass.getName().getClass());
// // Resolve the actual controller instance
// Object controllerInstance = serviceContainer.resolve(controllerClass.);

// // Check if the instance was not found and register it
// if (controllerInstance == null) {
// controllerInstance =
// controllerClass.getDeclaredConstructor(ServiceContainer.class).newInstance(serviceContainer);
// serviceContainer.registerService(controllerClass.getName().getClass());
// }

// // Find the appropriate method using reflection
// java.lang.reflect.Method method =
// controllerInstance.getClass().getMethod(methodName, RoutingContext.class);

// // Invoke the desired method of the resolved controller instance
// method.invoke(controllerInstance, routingContext);
// } catch (Exception e) {
// routingContext.fail(e);
// }
// };
// }

// }
// private void handleDataApi(RoutingContext context) {
// HomeController controller =
// this.serviceContainer.resolve(HomeController.class);
// controller.index(context);
// context.response().end("API Data");
// }

// Handle the request
// this.serviceContainer.HomeController.test();
// JsonObject jsonResponse = new JsonObject()
// .put("country", "Bangladesh");

// String procedureName = "example_plan.filter_paginate";
// // JsonArray params = new JsonArray().add(request.params.get("country"));
// DbConnection dbConnection = DbConnection.getInstance(vertx);
// CompletableFuture<String> future =
// dbConnection.callStoredFunction(procedureName, jsonResponse);

// // Handle the CompletableFuture result asynchronously without blocking
// future.thenAccept(result -> {
// routingContext.response().end(result);
// // System.out.println(result); // Handle the result here
// }).exceptionally(exception -> {
// exception.printStackTrace(); // Handle exceptions here
// return null;
// });

//create a method two accept a class and method name and return a handler
