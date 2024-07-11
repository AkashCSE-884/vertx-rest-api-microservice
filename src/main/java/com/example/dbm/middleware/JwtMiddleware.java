package com.example.dbm.middleware;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import java.util.Date;

public class JwtMiddleware {
    // private final JWTAuth jwtAuth;
    private Vertx vertx;
    private JWTAuth provider;

    public JwtMiddleware(Vertx vertx) {
        this.vertx = vertx;
        provider = JWTAuth.create(vertx, new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer("keyboard catw example_platform_2023")));
    }

    public String generateToken(RoutingContext ctx, String id) {
        String token = provider.generateToken(
                new JsonObject().put("id", id),
                new JWTOptions().setAlgorithm("HS256").setExpiresInMinutes(48000)); // 1440 minutes = 1 day
                // 48000 minutes = 1 month
        return token;
    }

    public String generateTokenToVerifyUser(RoutingContext ctx, String email, String password) {

        String token = provider.generateToken(
                new JsonObject().put("id", email).put("password", password),
                new JWTOptions().setAlgorithm("HS256").setExpiresInMinutes(120));
        return token;
    }

    public JWTAuthHandler createJWTAuthHandler() {
        return JWTAuthHandler.create(provider);
    }

}
