package com.example.dbm.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.dbm.service.DataValidator;
import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserController extends Controller {
    public UserController(ServiceContainer serviceContainer) {
        super(serviceContainer);

    }

    public void userProfileUpdate(RoutingContext ctx) {

        uploadFilesV2(ctx)
                .onComplete(ar -> {
                    JsonObject jsonObject = new JsonObject();
                    if (ar.succeeded()) {
                        List<String> filesPaths = ar.result();
                        int index = 0;
                        int i = 1;
                        for (String file : filesPaths) {
                            // System.out.println(index + " : " + file);
                            if (index == 0) {
                                jsonObject.put("logo_image_path", file);
                            }
                            if (index == 1) {
                                jsonObject.put("banner_image_path", file);
                            }

                            index++;
                            //
                        }

                        ctx.request().formAttributes()
                                .forEach(entry -> jsonObject.put(entry.getKey(), entry.getValue()));
                        jsonObject.put("user_id", validateToken(ctx));
                        jsonObject.put("ip", getClientIp(ctx));
                        // System.out.println(java.time.LocalDateTime.now());
                        this.dbConnection.callStoredFunctionF1("example_platform.user_profile__update", jsonObject)
                                .onSuccess(result -> {
                                    ctx.response()
                                            .putHeader("Content-Type", "application/json")
                                            .end(result.encode());
                                })
                                .onFailure(error -> {
                                    System.err.println("Error: " + error.getMessage());
                                });

                    } else {
                        Throwable cause = ar.cause();
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(ar.cause().getMessage());
                    }
                });

    }

    public void userProfileUpdateByAdmin(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {

            String user_id = validateToken(ctx);
            JsonObject jsonObject = new JsonObject();
            DataValidator.ValidationResult results = this.dataValidator.validateData(ctx, jsonObject, "user_id");
           

            if (results.isValid()) {
                jsonObject.put("error", results.getErrors());
                handleResponse(ctx, jsonObject);
            }
            jsonObject = buffer.toJsonObject();
            jsonObject.put("admin_user_id", user_id);
            // System.out.println(java.time.LocalDateTime.now());
            this.dbConnection.callStoredFunctionF1("example_platform.user_profile__update", jsonObject)
                    .onSuccess(result -> {
                        handleResponse(ctx, result);
                    })
                    .onFailure(error -> {
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(error.getMessage());
                    });
        });

    }

    public void userPaginateByAdmin(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            String user_id = validateToken(ctx);
            JsonObject jsonObject = buffer.toJsonObject();
            jsonObject.put("user_id", user_id);
            // System.out.println(java.time.LocalDateTime.now());
            this.dbConnection.callStoredFunctionF1("example_platform.user__paginate_by_admin", jsonObject)
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

    public Future<List<String>> uploadFilesV2(RoutingContext ctx) {
        Promise<List<String>> promise = Promise.promise();

        try {
            List<String> filesPaths = new ArrayList<>();

            Integer i = 0;

            ctx.request().setExpectMultipart(true)
                    .uploadHandler(upload -> {
                        // System.out.println("upload: " + ctx.request().getFormAttribute("user_id"));

                        String user_id = validateToken(ctx);
                        Path folder = Paths.get("uploads", "user", user_id, "profile");
                        // Path folder = Paths.get("src/main/java/com/example/dbm/pubic/uploads",
                        // "user", user_id, "ticket_support");

                        if (Files.isDirectory(folder)) {

                            try {
                                Files.walk(folder)
                                        .sorted(Comparator.reverseOrder()) // Reverse order to delete files first
                                        .map(Path::toFile)
                                        .forEach(File::delete);
                            } catch (IOException e) {
                                e.printStackTrace(); // Handle the exception as needed
                            }
                            try {

                                Files.createDirectories(folder);
                            } catch (IOException e) {
                                System.err.println("Failed to create directory: " + e.getMessage());
                                return;
                            }
                        }

                        upload.exceptionHandler(error -> {
                            System.err.println("File upload failed: " + error.getMessage());
                        });

                        var filename = upload.filename();
                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                        filename = filename.substring(0, filename.lastIndexOf('.')) + "_" + timestamp
                                + filename.substring(filename.lastIndexOf('.'));
                        var contentType = upload.contentType();
                        Path filePath = folder.resolve(filename);

                        Buffer buffer = Buffer.buffer();
                        upload.handler(buffer::appendBuffer);
                        upload.endHandler(v -> {
                            try {
                                // System.out.println("buffer.getBytes : "+buffer.getBytes());
                                Files.write(filePath, buffer.getBytes());

                                filesPaths.add(i, filePath.toString());
                                filesPaths.add(i, contentType);
                            } catch (IOException e) {
                                System.err.println("Failed to write file: " + e.getMessage());
                                return;
                            }
                        });

                        upload.resume();
                    })
                    .endHandler(v -> promise.complete(filesPaths))
                    .exceptionHandler(promise::fail);

        } catch (Exception e) {
            // Handle any exceptions
            promise.fail(e);
        }
        return promise.future();
    }

    public void sendMail(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            String email = jsonObject.getString("email");
            String subject = jsonObject.getString("subject");
            String message = jsonObject.getString("message");
            // this.sendMailService.sendMailV2(email, subject, message).isComplete(result
            // ->{
            // if(result.succeeded()){
            // ctx.response()
            // .putHeader("Content-Type", "application/json")
            // .end(new JsonObject().put("message", "Email sent").encode());
            // }else{
            // ctx.response()
            // .putHeader("Content-Type", "application/json")
            // .end(new JsonObject().put("message", "Email not sent").encode());
            // }
            // })

        });

    }
}
