package com.example.dbm.controller;

import java.io.IOException;
import java.nio.file.Paths;

import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.RoutingContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class TicketSupportController extends Controller {
    public TicketSupportController(ServiceContainer serviceContainer) {
        super(serviceContainer);

    }

    public void ticketSupportUpsert(RoutingContext ctx) {
        JsonObject jsonObject = new JsonObject();
        ctx.request().setExpectMultipart(true);
        // ctx.request().endHandler(req -> {
        // System.out.println("req: " +
        // ctx.request().formAttributes().get("full_name"));
        // if (ctx.request().formAttributes().get("full_name") == null) {
        // jsonObject.put("err_msg", "full_name is required");
        // handleResponse(ctx, jsonObject);

        // } else {

        uploadFilesV2(ctx)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        List<String> filesPaths = ar.result();
                        int index = 0;
                        int i = 1;
                        for (String file : filesPaths) {
                            // System.out.println(index + " : " + file);
                            if (index % 2 != 0) {
                                // System.out.println("file: " + file);
                                // jsonObject.put("attachment_type_" + index, file);
                                jsonObject.put("attachment_path_" + i, file);
                                i++;

                            }
                            index++;
                            //
                        }

                        ctx.request().formAttributes()
                                .forEach(entry -> jsonObject.put(entry.getKey(), entry.getValue()));
                        jsonObject.put("user_id", validateToken(ctx));
                        jsonObject.put("ip", getClientIp(ctx))
                                .put("ticket_support_id", 0)
                                .put("email", "washif.hossain@example.com");
                        // System.out.println(jsonObject.encode());
                        this.dbConnection.callStoredFunctionF1("example_platform.ticket_support__upsert",
                                jsonObject)
                                .onSuccess(result -> {
                                    if (result.getString("err_msg") == null) {
                                        this.sendMailService
                                                .sendMailV2(jsonObject.getString("email"), jsonObject.getString("subject"),
                                                        getMailBody(jsonObject), filesPaths)
                                                .onComplete(mail_result -> {
                                                    if (mail_result.succeeded()) {
                                                        if (mail_result.result()) {
                                                            result.put("message",
                                                                    "Verification mail sent successfully");
                                                        } else {
                                                            result.put("message", "Failed to send verification mail");
                                                        }
                                                    } else {
                                                        System.err.println("Exception while sending email: "
                                                                + mail_result.cause().getMessage());
                                                        result.put("message", mail_result.cause().getMessage());

                                                    }
                                                });
                                    }
                                    ctx.response()
                                            .putHeader("Content-Type", "application/json")
                                            .end(result.encode());
                                })
                                .onFailure(error -> {
                                    System.err.println("Error: " + error.getMessage());
                                    ctx.response()
                                            .putHeader("Content-Type", "application/json")
                                            .end(error.getMessage());
                                });
                    } else {
                        Throwable cause = ar.cause();
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(ar.cause().getMessage());
                    }
                });
        // }
    }

    public void tutorialPaginate(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            jsonObject.put("user_id", validateToken(ctx));
            jsonObject.put("ip", getClientIp(ctx));
            this.dbConnection.callStoredFunctionF1("example_platform.tutorial__paginate", jsonObject)
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

    public void handleRequestForFile(RoutingContext ctx) {
        ctx.request().setExpectMultipart(true)
                .uploadHandler(upload -> {
                    String user_id = validateToken(ctx);
                    Path folder = Paths.get("src/main/java/com/example/dbm/pubic/uploads", "user", user_id,
                            "ticket_support");

                    if (!Files.isDirectory(folder)) {
                        try {
                            Files.createDirectories(folder);
                        } catch (IOException e) {
                            System.err.println("Failed to create directory: " + e.getMessage());
                            return;
                        }
                    }
                    System.out.println("filename: " + folder);
                    upload.exceptionHandler(error -> {
                        System.err.println("File upload failed: " + error.getMessage());
                    });
                    var filename = upload.filename();
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    filename = filename.substring(0, filename.lastIndexOf('.')) + "_" + timestamp
                            + filename.substring(filename.lastIndexOf('.'));
                    var contentType = upload.contentType();
                    Path filePath = folder.resolve(filename);
                    System.out.println("filename: " + filename);

                    Buffer buffer = Buffer.buffer();
                    upload.handler(buffer::appendBuffer);

                    try {
                        Files.write(filePath, buffer.getBytes());
                    } catch (IOException e) {
                        System.err.println("Failed to write file: " + e.getMessage());

                    }
                    upload.resume();
                })
                .endHandler(v -> {
                    ctx.response().end("File upload completed");
                });
    }

    public boolean uploadFile(RoutingContext ctx) {
        try {
            List<String> files_path = new ArrayList<String>();
            ctx.request().setExpectMultipart(true)
                    .uploadHandler(upload -> {
                        String user_id = validateToken(ctx);
                        Path folder = Paths.get("uploads", "user", user_id, "ticket_support");

                        if (!Files.isDirectory(folder)) {
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
                        System.out.println("buffer.getBytes : " + buffer.getBytes());
                        try {
                            Files.write(filePath, buffer.getBytes());
                        } catch (IOException e) {
                            System.err.println("Failed to write file: " + e.getMessage());
                            return;
                        }
                        upload.resume();
                    })
                    .endHandler(v -> {
                        ctx.response().end("File upload completed");
                    });

            return true; // Return true on successful upload
        } catch (Exception e) {
            // Handle any exceptions
            e.printStackTrace();
            return false; // Return false on failure
        }
    }

    public Future<List<String>> uploadFilesV2(RoutingContext ctx) {
        Promise<List<String>> promise = Promise.promise();

        try {
            List<String> filesPaths = new ArrayList<>();

            Integer i = 0;

            ctx.request().setExpectMultipart(true)
                    .uploadHandler(upload -> {
                        String user_id = validateToken(ctx);
                        Path folder = Paths.get("uploads", "user", user_id, "ticket_support");
                        // Path folder = Paths.get("src/main/java/com/example/dbm/pubic/uploads",
                        // "user", user_id, "ticket_support");

                        if (!Files.isDirectory(folder)) {
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

    public String getMailBody(JsonObject jsonObject) {
        String addTable = "";
        String tableValue = "";
        // System.out.println("is_replacement_request: " + jsonObject.getString("is_replacement_request").equals(true));
        if (Boolean.parseBoolean(jsonObject.getString("is_replacement_request"))) {
             System.out.println("is_replacement_request: " + jsonObject.getString("is_replacement_request").equals(true));
            addTable = "<th>Replacement Item</th>\r\n" + //
                    "  <th>Quantity</th>\r\n";
            tableValue = " <td>" + jsonObject.getString("product_name") + "</td>\r\n" + //
                    "  <td>" + jsonObject.getString("quantity") + "</td>\r\n";
        }
        String message = "<!DOCTYPE html>\r\n" + //
                "                <html>\r\n" + //
                "                <head>\r\n" + //
                "                <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />\r\n" + //
                "                <link href=\"https://fonts.googleapis.com/css?family=Muli:400,400i,700,700i\" rel=\"stylesheet\" />\r\n"
                + //
                "                <title>Email Template</title>\r\n" + //
                "                    <style>\r\n" + //
                "                        table {\r\n" + //
                "                        font-family: arial, sans-serif;\r\n" + //
                "                        border-collapse: collapse;\r\n" + //
                "                        width: 100%;\r\n" + //
                "                        }\r\n" + //
                "\r\n" + //
                "                        td, th {\r\n" + //
                "                        border: 1px solid #dddddd;\r\n" + //
                "                        text-align: left;\r\n" + //
                "                        padding: 8px;\r\n" + //
                "                        }\r\n" + //
                "\r\n" + //
                "                        tr:nth-child(even) {\r\n" + //
                "                        background-color: #dddddd;\r\n" + //
                "                        }\r\n" + //
                "                    </style>\r\n" + //
                "                </head>\r\n" + //
                "                <body class=\"body\" style=\"padding:0 !important; margin:0 !important; display:block !important; min-width:100% !important; width:100% !important; -webkit-text-size-adjust:none;\">\r\n"
                + //
                "                    <h4>" + jsonObject.getString("full_name") + "</h4>\r\n" + //
                "                    <h3 style=\"text-align:center;\">" + jsonObject.getString("subject") + "</h3>\r\n"
                + //
                "                    <p style=\"\">" + jsonObject.getString("message") + "</p>\r\n" + //
                "                    <table style=\"text-align:center;\">\r\n" + //
                "                        <thead>\r\n" + //
                "                        <tr>\r\n" + //
                "                            <th>SSM Reference Number</th>\r\n" + //
                "                            <th>Country</th>\r\n" + //
                addTable +
                "                        </tr>\r\n" + //
                "                        </thead>\r\n" + //
                "                        <tbody>\r\n" + //
                "                        <tr>\r\n" + //
                "                            <td>" + jsonObject.getString("device_serial_id") + "</td>\r\n" + //
                "                            <td>" + jsonObject.getString("country") + "</td>\r\n" + //
                tableValue +
                "                        </tr>\r\n" + //
                "                        </tbody>\r\n" + //
                "                    </table>\r\n" + //
                "                   <img src=\"cid:atc-1\" width=\\\"200\\\"/> \r\n" + //
                "                   <img src=\"cid:atc-2\" width=\\\"200\\\"/> \r\n" + //
                "                </body>\r\n" + //
                "                </html>";

        // return jsonObject;
        return message;
    }

}
