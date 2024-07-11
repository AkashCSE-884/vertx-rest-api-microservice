package com.example.dbm.dbconnection;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;

import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DbConnection {
    private static DbConnection instance;
    private final PgPool pool;

    private DbConnection(Vertx vertx) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(5432)
                .setHost("dev.example.com")
                .setDatabase("plan_example")
                .setUser("example")
                .setPassword("adminxp123");
        // .setHost("localhost")
        // .setDatabase("plan_example")
        // .setUser("postgres")
        // .setPassword("1");

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(20);

        this.pool = PgPool.pool(vertx, connectOptions, poolOptions);
    }

    public static synchronized DbConnection getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new DbConnection(vertx);
        }
        return instance;
    }

    public Future<JsonObject> callStoredFunctionF1(String procedureName, JsonObject params) {
        Promise<JsonObject> promise = Promise.promise();

        String sql = "SELECT " + procedureName + "($1)";
        pool.preparedQuery(sql)
                .execute(Tuple.of(params))
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        RowSet<Row> result = ar.result();
                        if (result.size() > 0) {
                            JsonObject jsonResult = result.iterator().next().toJson();

                            String[] substrings = procedureName.split("\\.");
                            jsonResult = new JsonObject(jsonResult.getString(substrings[1]));
                            // System.out.println(data);
                            promise.complete(jsonResult);
                        } else {
                            promise.complete(new JsonObject().put("message", "No rows found"));
                        }
                    } else {
                        // promise.fail(ar.cause().getMessage());
                        JsonObject errorResponse = new JsonObject()
                                .put("err_msg", extractErrorMessage(ar.cause().getMessage()));
                        promise.complete(errorResponse);
                    }
                });

        return promise.future();
    }

    public void callStoredFunction2(String procedureName, JsonArray params,
            Handler<AsyncResult<String>> resultHandler) {
        pool.getConnection(conn -> {
            if (conn.succeeded()) {
                SqlConnection connection = conn.result();

                Tuple tupleParams = Tuple.of(params.encode());

                connection.preparedQuery("SELECT " + procedureName + "($1)")
                        .execute(tupleParams, result -> {
                            connection.close();
                            if (result.succeeded()) {
                                RowSet<Row> rows = result.result();

                                if (rows.rowCount() > 0) {
                                    String jsonResult = rows.iterator().next().toJson().encode();
                                    resultHandler.handle(Future.succeededFuture(jsonResult));
                                } else {
                                    resultHandler.handle(Future.failedFuture("No rows returned."));
                                }
                            } else {
                                resultHandler.handle(Future.failedFuture(result.cause()));
                            }
                        });
            } else {
                resultHandler.handle(Future.failedFuture(conn.cause()));
            }
        });
    }

    public CompletableFuture<String> callStoredFunction(String procedureName, JsonObject params) {
        CompletableFuture<String> future = new CompletableFuture<>();

        pool.getConnection(connResult -> {
            if (connResult.succeeded()) {
                SqlConnection connection = connResult.result();
                Tuple tupleParams = Tuple.of(params);
                connection.preparedQuery("SELECT " + procedureName + "($1)")
                        .execute(tupleParams, queryResult -> {
                            connection.close();
                            if (queryResult.succeeded()) {
                                String jsonResult = queryResult.result().iterator().next().toJson().toString();
                                future.complete(jsonResult);
                            } else {
                                future.completeExceptionally(queryResult.cause());
                            }
                        });
            } else {
                future.completeExceptionally(connResult.cause());
            }
        });

        return future;
    }

    private String extractErrorMessage(String str) {
        Pattern pattern = Pattern.compile("ERROR: (.*) \\(.*\\)");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return str;
        }
    }

}
