package com.example.dbm.router;

import java.util.logging.Logger;

import com.example.dbm.middleware.JwtMiddleware;
import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;

public class ApiRouter extends AppRouter {
    private Router router;
    private ServiceContainer serviceContainer;
    private Vertx vertx;
    private JwtMiddleware jwtMiddleware;

    public ApiRouter(Vertx vertx, Router router, ServiceContainer serviceContainer) {
        super(serviceContainer, vertx);
        this.serviceContainer = serviceContainer;
        this.router = router;
        this.vertx = vertx;

    }

    private void routes() {
        // router.get("/").handler(ctx -> {
        // ctx.response().end("Welcome to example Platform API");
        // });
        JWTAuthHandler jwtAuthHandler = this.jwtMiddleware.createJWTAuthHandler();

        

        router.post("/user/login").handler(routeHandler("AuthController", "userLogin"));
        router.post("/user/register").handler(routeHandler("AuthController", "userRegister"));
        router.get("/user/verify").handler(routeHandler("AuthController", "verifyUser"));

        router.post("/user/verify/forgot-password").handler(routeHandler("AuthController", "updateTokenByEmail"));
        router.post("/user/forgot-password").handler(routeHandler("AuthController", "forgotPassword"));

        router.post("/device/omega2pro").handler(routeHandler("HomeController", "testApi"));
        router.get("/device/omega2pro").handler(routeHandler("HomeController", "getTestApi"));

        // router.post("/device/ssm/country").handler(routeHandler("DeviceController",
        // "totalSSMCountByCuntry"));
        // router.post("/device/energy/country").handler(routeHandler("DeviceController",
        // "energyConsumptionReportByCountry"));
        router.post("/device/summary-activity").handler(routeHandler("DeviceController", "deviceSummaryDataForHome"));

        router.route("/api/*").handler(jwtAuthHandler);
        router.route().handler(jwtAuthHandler).failureHandler(ctx -> {
            Throwable failure = ctx.failure();
            if (failure instanceof DecodeException) {
                ctx.response()
                        .setStatusCode(401)
                        .putHeader("Content-Type", "application/json")
                        .end("{\"error\": \"Invalid token format\"}");
            } else {
                ctx.next();
            }
        });
        // router.route().handler(ctx -> {
        //     try {
        //         // Attempt to decode the token
        //         JsonObject decodedToken = new JsonObject(ctx.request().getHeader("Authorization"));
        
        //         // Continue with other handling logic...
        //     } catch (DecodeException e) {
        //         // Handle the exception (e.g., provide a response)
        //         ctx.response()
        //             .setStatusCode(401)
        //             .putHeader("Content-Type", "application/json")
        //             .end("{\"error\": \"Invalid token format\"}");
        //     }
        // });
        router.post("/api/user/logout").handler(routeHandler("AuthController", "logout"));

        router.post("/api/admin/user/paginate/id").handler(routeHandler("UserController", "userPaginateByAdmin"));
        router.post("/api/user/update/id").handler(routeHandler("UserController", "userProfileUpdate"));
        router.post("/api/user/update/admin").handler(routeHandler("UserController", "userProfileUpdateByAdmin"));
       
        router.get("/index").handler(routeHandler("HomeController", "index"));
        router.post("/create").handler(routeHandler("HomeController", "create"));

        router.post("/api/country/list").handler(routeHandler("CountryController", "getCountryList"));
        router.post("/api/tutorial/upsert").handler(routeHandler("TutorialController", "tutorialUpsert"));
        router.post("/api/tutorial/paginate").handler(routeHandler("TutorialController", "tutorialPaginate"));

        router.post("/api/img")
                .handler(routeHandler("TicketSupportController", "uploadFilesV2"));

        router.post("/api/ticket-support/request")
                .handler(routeHandler("TicketSupportController", "ticketSupportUpsert"));
        router.post("/api/activity-log").handler(routeHandler("ActivityLogController", "activityLogPaginate"));

        router.route().last().handler(ctx -> {
            ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\": \"Resource not found\"}");
        });

        vertx.exceptionHandler(exception -> {
            System.out.println("Unhandled exception: " + exception.getMessage());
            exception.printStackTrace();
        });

    }

    public Router getRouter() {
        this.jwtMiddleware = serviceContainer.resolve(JwtMiddleware.class);
        routes();
        return router;
    }

}
