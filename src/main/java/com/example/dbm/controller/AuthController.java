package com.example.dbm.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.dbm.middleware.JwtMiddleware;
import com.example.dbm.service_container.ServiceContainer;

import io.netty.util.internal.SystemPropertyUtil;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import java.time.Year;

public class AuthController extends Controller {

    public AuthController(ServiceContainer serviceContainer) {
        super(serviceContainer);

    }

    public void userLogin(RoutingContext ctx) {

        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            jsonObject.put("ip", getClientIp(ctx));
            this.dbConnection.callStoredFunctionF1("example_platform.user__login", jsonObject)
                    .onSuccess(result -> {
                        JsonArray ret_val = result.getJsonArray("ret_val");
                        if (result.getJsonArray("ret_val") != null && !result.getJsonArray("ret_val").isEmpty()) {
                            String userId = result.getJsonArray("ret_val").getJsonObject(0).getString("user_id");
                            String token = this.jwtMiddleware.generateToken(ctx, userId);
                            ctx.response()
                                    .putHeader("Authorization", "Bearer " + token)
                                    .end(result.encode());
                        } else {
                            // ctx.response()
                            // .putHeader("Content-Type", "application/json")
                            // .end(result.encode());
                            this.handleResponse(ctx, result);
                        }

                    })
                    .onFailure(error -> {
                        System.err.println("Error: " + error.getMessage());
                    });

        });
    }

    public void userRegister(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            String verifyToken = this.jwtMiddleware.generateTokenToVerifyUser(ctx, jsonObject.getString("email"),
                    jsonObject.getString("password"));
            jsonObject.put("ip", getClientIp(ctx));
            if (jsonObject.getInteger("user_id") == 0 || jsonObject.getInteger("user_id") == null) {
                jsonObject.put("user_id", 0);
                jsonObject.put("token", verifyToken);
            }
            this.dbConnection.callStoredFunctionF1("example_platform.user__upsert", jsonObject)
                    .onSuccess(result -> {
                        if (result.getString("err_msg") == null || result.getString("err_msg").isEmpty()) {
                            result.put("token", verifyToken);
                            String fullName = jsonObject.getString("fname") + " "
                                    + jsonObject.getString("lname");
                            String body = createAndGetVerifyMailBody(
                                    "http://api.example.com/user/verify?token=" + verifyToken, fullName);
                            this.sendMailService.sendMailV2(jsonObject.getString("email"), "Verify your mail", body, List.of())
                                    .onComplete(mail_result -> {
                                        if (mail_result.succeeded()) {
                                            if (mail_result.result()) {
                                                result.put("message", "Verification mail sent successfully");
                                            } else {
                                                result.put("message", "Failed to send verification mail");
                                            }
                                        } else {
                                            System.err.println("Exception while sending email: "
                                                    + mail_result.cause().getMessage());
                                            result.put("message", mail_result.cause().getMessage());

                                        }
                                    });
                            // Boolean status =
                            // this.sendMailService.sendMail(jsonObject.getString("email"),"Verify Token",
                            // body);
                            // if (status == true) {
                            // result.put("message", "Verification mail sent successfully");
                            // } else {
                            // result.put("message", "Failed to send verification mail");
                            // }
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
        });
    }

    public void forgotPassword(RoutingContext ctx) {

        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            jsonObject.put("ip", getClientIp(ctx));
            this.dbConnection.callStoredFunctionF1("example_platform.user__forgot_password", jsonObject)
                    .onSuccess(result -> {
                        // if (result.getBoolean("status", false)) {
                        //     // try {
                        //     //     // result.put("token", token);
                        //     //     ctx.response()
                        //     //             .setStatusCode(302)
                        //     //             .putHeader("Location", "http://localhost:4200/setPassword")
                        //     //             .end();
                        //     // } catch (Exception e) {
                        //     //     ctx.response()
                        //     //             .setStatusCode(500)
                        //     //             .putHeader("Content-Type", "application/json")
                        //     //             .end(new JsonObject().put("error", "Failed to redirect").encode());
                        //     // }
                        // }
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode());
                    })
                    .onFailure(error -> {
                        System.err.println("Error: " + error.getMessage());
                                                ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end( error.getMessage());
                    });

        });

    }

    public void logout(RoutingContext ctx) {

        HttpServerRequest request = ctx.request();
        request.headers().remove("Authorization");
        // Boolean status = this.sendMailService.sendMail("akash.sushil@example.com",
        // "Verify Token", body);

        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Logged out successfully").encode());
    }

    public void verifyUser(RoutingContext ctx) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("token", ctx.request().getParam("token"));
        jsonObject.put("ip", getClientIp(ctx));
        // System.out.println("token: " + jsonObject);
        this.dbConnection.callStoredFunctionF1("example_platform.verify__user", jsonObject)
                .onSuccess(result -> {
                    Boolean status = result.getBoolean("status", false);
                    if (status == true) {
                        try {
                            // result.put("token", token);
                            ctx.response()
                                    .setStatusCode(302)
                                    .putHeader("Location", "http://localhost:4200/login?isExpired=false")
                                    .end();
                        } catch (Exception e) {
                            ctx.response()
                                    .setStatusCode(500)
                                    .putHeader("Content-Type", "application/json")
                                    .end(new JsonObject().put("error", "Failed to redirect").encode());
                        }
                    } else {
                        ctx.response()
                                    .setStatusCode(302)
                                    .putHeader("Location", "http://localhost:4200/register?isExpired=true")
                                    .end();
                    }
                })
                .onFailure(error -> {
                    System.err.println("Error: " + error.getMessage());
                                            ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(error.getMessage());
                });
    }

    public void updateTokenByEmail(RoutingContext ctx) {
        ctx.request().bodyHandler(buffer -> {
            JsonObject jsonObject = buffer.toJsonObject();
            String token = this.jwtMiddleware.generateToken(ctx, jsonObject.getString("email"));
            jsonObject.put("token", token);
            jsonObject.put("ip", getClientIp(ctx));
            // System.out.println("token: " + jsonObject);
            this.dbConnection.callStoredFunctionF1("example_platform.update__token_by_email", jsonObject)
                    .onSuccess(result -> {
                        Boolean status = result.getBoolean("status", false);
                        String fullName = result.getString("full_name");
                        String body = createAndGetVerifyMailBody("http://localhost:4200/setPassword/" + token, fullName);
                        this.sendMailService.sendMailV2(jsonObject.getString("email"), "Reset Password", body, List.of())
                                .onComplete(mail_result -> {
                                    if (mail_result.succeeded()) {
                                        if (mail_result.result()) {
                                            result.put("message", "Verification mail sent successfully");
                                        } else {
                                            result.put("message", "Failed to send verification mail");
                                        }
                                    } else {
                                        System.err.println("Exception while sending email: "
                                                + mail_result.cause().getMessage());
                                        result.put("message", mail_result.cause().getMessage());

                                    }
                                });
                        if (status == true) {
                            result.put("token", token);
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
        });
    }

    public String createAndGetVerifyMailBody(String token, String user_name) {
        int currentYear = Year.now().getValue();
        String body = "<!DOCTYPE html>\r\n" + //
                "      <html>\r\n" + //
                "      <head>\r\n" + //
                "        <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />\r\n" + //
                "        <link href=\"https://fonts.googleapis.com/css?family=Muli:400,400i,700,700i\" rel=\"stylesheet\" />\r\n"
                + //
                "        <title>Email Template</title>\r\n" + //
                "        \r\n" + //
                "      \r\n" + //
                "        <style type=\"text/css\" media=\"screen\">\r\n" + //
                "          /* Linked Styles */\r\n" + //
                "          body { padding:0 !important; margin:0 !important; display:block !important; min-width:100% !important; width:100% !important; -webkit-text-size-adjust:none }\r\n"
                + //
                "          a { color:#66c7ff; text-decoration:none }\r\n" + //
                "          p { padding:0 !important; margin:0 !important } \r\n" + //
                "          img { -ms-interpolation-mode: bicubic; /* Allow smoother rendering of resized image in Internet Explorer */ }\r\n"
                + //
                "          .mcnPreviewText { display: none !important; }\r\n" + //
                "      \r\n" + //
                "              \r\n" + //
                "          /* Mobile styles */\r\n" + //
                "          @media only screen and (max-device-width: 480px), only screen and (max-width: 480px) {\r\n" + //
                "            .mobile-shell { width: 100% !important; min-width: 100% !important; }\r\n" + //
                "            .bg { background-size: 100% auto !important; -webkit-background-size: 100% auto !important; }\r\n"
                + //
                "            \r\n" + //
                "            .text-header,\r\n" + //
                "            .m-center { text-align: center !important; }\r\n" + //
                "            \r\n" + //
                "            .center { margin: 0 auto !important; }\r\n" + //
                "            .container { padding: 20px 10px !important }\r\n" + //
                "            \r\n" + //
                "            .td { width: 100% !important; min-width: 100% !important; }\r\n" + //
                "      \r\n" + //
                "            .m-br-15 { height: 15px !important; }\r\n" + //
                "            .p30-15 { padding: 30px 15px !important; }\r\n" + //
                "      \r\n" + //
                "            .m-td,\r\n" + //
                "            .m-hide { display: none !important; width: 0 !important; height: 0 !important; font-size: 0 !important; line-height: 0 !important; min-height: 0 !important; }\r\n"
                + //
                "      \r\n" + //
                "            .m-block { display: block !important; }\r\n" + //
                "      \r\n" + //
                "            .fluid-img img { width: 100% !important; max-width: 100% !important; height: auto !important; }\r\n"
                + //
                "      \r\n" + //
                "            .column,\r\n" + //
                "            .column-top,\r\n" + //
                "            .column-empty,\r\n" + //
                "            .column-empty2,\r\n" + //
                "            .column-dir-top { float: left !important; width: 100% !important; display: block !important; }\r\n"
                + //
                "      \r\n" + //
                "            .column-empty { padding-bottom: 10px !important; }\r\n" + //
                "            .column-empty2 { padding-bottom: 30px !important; }\r\n" + //
                "      \r\n" + //
                "            .content-spacing { width: 15px !important; }\r\n" + //
                "          }\r\n" + //
                "        </style>\r\n" + //
                "      </head>\r\n" + //
                "      <body class=\"body\" style=\"padding:0 !important; margin:0 !important; display:block !important; min-width:100% !important; width:100% !important; -webkit-text-size-adjust:none;\">\r\n"
                + //
                "        <table width=\"90%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-image: url(https://example.com/assets/images/emails/verify_bg.png);background-size: cover;background-repeat: no-repeat;margin: auto;\">\r\n"
                + //
                "          <tr>\r\n" + //
                "            <td align=\"center\" valign=\"top\">\r\n" + //
                "              <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" class=\"mobile-shell\">\r\n"
                + //
                "                <tr>\r\n" + //
                "                  <td class=\"td container\" style=\"width:650px; min-width:650px; font-size:0pt; line-height:0pt; margin:0; font-weight:normal; padding:25px 0px;\">\r\n"
                + //
                "                    <!-- Header -->\r\n" + //
                "                    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n" + //
                "                      <tr>\r\n" + //
                "                        <td class=\"p30-15\" style=\"padding: 0px 10px 10px 10px;\">\r\n" + //
                "                          <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                            <tr>\r\n" + //
                "                              <th class=\"column-top\" style=\"font-size:0pt; line-height:0pt; padding:0; margin:0; font-weight:normal; vertical-align:top;\">\r\n"
                + //
                "                                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"img m-center\" style=\"font-size:0pt; line-height:0pt; text-align:left;\"><img src=\"https://example.com/assets/images/emails/smb_tick.png\" width=\"200\"/></td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                </table>\r\n" + //
                "                              </th>\r\n" + //
                "                              <th class=\"column\" style=\"font-size:0pt; line-height:0pt; padding:0; margin:0; font-weight:normal; vertical-align:middle;\">\r\n"
                + //
                "                                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"h3 pb20\" style=\"color:#ffffff; font-family:Arial,sans-serif; font-size:25px; line-height:32px; text-align:center; padding-bottom:20px;\">WELCOME TO THE SSM SYSTEM PLATFORM!</td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                </table>\r\n" + //
                "                              </th>\r\n" + //
                "                              <th class=\"column\" style=\"font-size:0pt; line-height:0pt; padding:0; margin:0; font-weight:normal;\">\r\n"
                + //
                "                                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text-header\" style=\"color:#475c77; font-family:'Muli', Arial,sans-serif; font-size:12px; line-height:16px; text-align:right;\"><img src=\"https://example.com/assets/images/emails/plan-logo.png\" width=\"150\" style=\"visibility: hidden; display:none;\"/></td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                </table>\r\n" + //
                "                              </th>\r\n" + //
                "                            </tr>\r\n" + //
                "                          </table>\r\n" + //
                "                        </td>\r\n" + //
                "                      </tr>\r\n" + //
                "                    </table>\r\n" + //
                "                    <!-- END Header -->\r\n" + //
                "      \r\n" + //
                "                    <!-- Article / Full Width Image + Title + Copy + Button -->\r\n" + //
                "                    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n" + //
                "                      <tr>\r\n" + //
                "                        <td style=\"padding-bottom: 10px;\">\r\n" + //
                "                          <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#ffffff\">\r\n"
                + //
                "                            <tr>\r\n" + //
                "                              <td class=\"p30-15\" style=\"padding: 30px 30px;\">\r\n" + //
                "                                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:20px; line-height:32px; text-align:left; padding-bottom:20px;\">Hello "
                + user_name + ",</td>\r\n" + //
                "                                  </tr>\r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:14px; line-height:26px; text-align:left; padding-bottom:10px;\">We have received a request to reset your password for your account..</td>\r\n"
                + //
                "                                    \r\n" + //
                "                                  </tr>\r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:14px; line-height:26px; text-align:left; padding-bottom:20px;\">Please click the button below to reset your password for the Smart Solar Media System Platform.</td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                  <!-- Button -->\r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td align=\"left\" style=\"padding-bottom: 20px;\">\r\n" + //
                "                                      <table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n" + //
                "                                        <tr>\r\n" + //
                "                                          <td class=\"pb-20\" style=\"background:#ffcc09; color:#000000; font-family:Arial,sans-serif; font-size:14px; line-height:18px; padding:12px 30px; text-align:center; border-radius:0px 22px 22px 22px; font-weight:bold;\"><a href=\""
                + token
                + "\" target=\"_blank\" class=\"link-white\" style=\"color:#000000; text-decoration:none;\"><span class=\"link-white\" style=\"color:#000000; text-decoration:none;\">Verify Email</span></a></td>\r\n"
                + //
                "                                        </tr>\r\n" + //
                "                                      </table>\r\n" + //
                "                                    </td>\r\n" + //
                "                                  </tr>\r\n" + //
                "                                  \r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:20px; line-height:32px; text-align:left; padding-bottom:10px;\">Best Regards,</td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:20px; line-height:32px; text-align:left; padding-bottom:10px;\">The example Team</td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                  <!-- END Button -->\r\n" + //
                "                                </table>\r\n" + //
                "                              </td>\r\n" + //
                "                            </tr>\r\n" + //
                "                          </table>\r\n" + //
                "                        </td>\r\n" + //
                "                      </tr>\r\n" + //
                "                    </table>\r\n" + //
                "                    <!-- END Article / Full Width Image + Title + Copy + Button -->\r\n" + //
                "      \r\n" + //
                "                    <!-- Footer -->\r\n" + //
                "                    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n" + //
                "                      <tr>\r\n" + //
                "                        <td class=\"p30-15 bbrr\" style=\"padding: 30px 10px; border-radius:0px 0px 26px 26px;\">\r\n"
                + //
                "                          <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                            <tr>\r\n" + //
                "                              <td class=\"pb10\" style=\"color:#ffffff; font-family:Arial,sans-serif; font-size:16px; line-height:20px; text-align:right; padding-bottom:10px;\">Copyright©"
                + currentYear + " example. All rights reserved</td>\r\n"
                + //
                "                              <td class=\"text-header\" style=\"color:#475c77; font-family:Arial,sans-serif; font-size:12px; line-height:16px; text-align:right;\"><img src=\"https://example.com/assets/images/emails/powered.png\" width=\"200\"/></td>\r\n"
                + //
                "                            </tr>\r\n" + //
                "                          </table>\r\n" + //
                "                        </td>\r\n" + //
                "                      </tr>\r\n" + //
                "                    </table>\r\n" + //
                "                    <!-- END Footer -->\r\n" + //
                "                  </td>\r\n" + //
                "                </tr>\r\n" + //
                "              </table>\r\n" + //
                "            </td>\r\n" + //
                "          </tr>\r\n" + //
                "        </table>\r\n" + //
                "      </body>\r\n" + //
                "      </html>";
        return body;
    }


     public String createAndGetResetPasswordBody(String token, String user_name) {
        int currentYear = Year.now().getValue();
        String body = "<!DOCTYPE html>\r\n" + //
                "      <html>\r\n" + //
                "      <head>\r\n" + //
                "        <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />\r\n" + //
                "        <link href=\"https://fonts.googleapis.com/css?family=Muli:400,400i,700,700i\" rel=\"stylesheet\" />\r\n"
                + //
                "        <title>Email Template</title>\r\n" + //
                "        \r\n" + //
                "      \r\n" + //
                "        <style type=\"text/css\" media=\"screen\">\r\n" + //
                "          /* Linked Styles */\r\n" + //
                "          body { padding:0 !important; margin:0 !important; display:block !important; min-width:100% !important; width:100% !important; -webkit-text-size-adjust:none }\r\n"
                + //
                "          a { color:#66c7ff; text-decoration:none }\r\n" + //
                "          p { padding:0 !important; margin:0 !important } \r\n" + //
                "          img { -ms-interpolation-mode: bicubic; /* Allow smoother rendering of resized image in Internet Explorer */ }\r\n"
                + //
                "          .mcnPreviewText { display: none !important; }\r\n" + //
                "      \r\n" + //
                "              \r\n" + //
                "          /* Mobile styles */\r\n" + //
                "          @media only screen and (max-device-width: 480px), only screen and (max-width: 480px) {\r\n" + //
                "            .mobile-shell { width: 100% !important; min-width: 100% !important; }\r\n" + //
                "            .bg { background-size: 100% auto !important; -webkit-background-size: 100% auto !important; }\r\n"
                + //
                "            \r\n" + //
                "            .text-header,\r\n" + //
                "            .m-center { text-align: center !important; }\r\n" + //
                "            \r\n" + //
                "            .center { margin: 0 auto !important; }\r\n" + //
                "            .container { padding: 20px 10px !important }\r\n" + //
                "            \r\n" + //
                "            .td { width: 100% !important; min-width: 100% !important; }\r\n" + //
                "      \r\n" + //
                "            .m-br-15 { height: 15px !important; }\r\n" + //
                "            .p30-15 { padding: 30px 15px !important; }\r\n" + //
                "      \r\n" + //
                "            .m-td,\r\n" + //
                "            .m-hide { display: none !important; width: 0 !important; height: 0 !important; font-size: 0 !important; line-height: 0 !important; min-height: 0 !important; }\r\n"
                + //
                "      \r\n" + //
                "            .m-block { display: block !important; }\r\n" + //
                "      \r\n" + //
                "            .fluid-img img { width: 100% !important; max-width: 100% !important; height: auto !important; }\r\n"
                + //
                "      \r\n" + //
                "            .column,\r\n" + //
                "            .column-top,\r\n" + //
                "            .column-empty,\r\n" + //
                "            .column-empty2,\r\n" + //
                "            .column-dir-top { float: left !important; width: 100% !important; display: block !important; }\r\n"
                + //
                "      \r\n" + //
                "            .column-empty { padding-bottom: 10px !important; }\r\n" + //
                "            .column-empty2 { padding-bottom: 30px !important; }\r\n" + //
                "      \r\n" + //
                "            .content-spacing { width: 15px !important; }\r\n" + //
                "          }\r\n" + //
                "        </style>\r\n" + //
                "      </head>\r\n" + //
                "      <body class=\"body\" style=\"padding:0 !important; margin:0 !important; display:block !important; min-width:100% !important; width:100% !important; -webkit-text-size-adjust:none;\">\r\n"
                + //
                "        <table width=\"90%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-image: url(https://example.com/assets/images/emails/verify_bg.png);background-size: cover;background-repeat: no-repeat;margin: auto;\">\r\n"
                + //
                "          <tr>\r\n" + //
                "            <td align=\"center\" valign=\"top\">\r\n" + //
                "              <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" class=\"mobile-shell\">\r\n"
                + //
                "                <tr>\r\n" + //
                "                  <td class=\"td container\" style=\"width:650px; min-width:650px; font-size:0pt; line-height:0pt; margin:0; font-weight:normal; padding:25px 0px;\">\r\n"
                + //
                "                    <!-- Header -->\r\n" + //
                "                    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n" + //
                "                      <tr>\r\n" + //
                "                        <td class=\"p30-15\" style=\"padding: 0px 10px 10px 10px;\">\r\n" + //
                "                          <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                            <tr>\r\n" + //
                "                              <th class=\"column-top\" style=\"font-size:0pt; line-height:0pt; padding:0; margin:0; font-weight:normal; vertical-align:top;\">\r\n"
                + //
                "                                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"img m-center\" style=\"font-size:0pt; line-height:0pt; text-align:left;\"><img src=\"https://example.com/assets/images/emails/smb_tick.png\" width=\"200\"/></td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                </table>\r\n" + //
                "                              </th>\r\n" + //
                "                              <th class=\"column\" style=\"font-size:0pt; line-height:0pt; padding:0; margin:0; font-weight:normal; vertical-align:middle;\">\r\n"
                + //
                "                                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"h3 pb20\" style=\"color:#ffffff; font-family:Arial,sans-serif; font-size:25px; line-height:32px; text-align:center; padding-bottom:20px;\">WELCOME TO THE SSM SYSTEM PLATFORM!</td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                </table>\r\n" + //
                "                              </th>\r\n" + //
                "                              <th class=\"column\" style=\"font-size:0pt; line-height:0pt; padding:0; margin:0; font-weight:normal;\">\r\n"
                + //
                "                                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text-header\" style=\"color:#475c77; font-family:'Muli', Arial,sans-serif; font-size:12px; line-height:16px; text-align:right;\"><img src=\"https://example.com/assets/images/emails/plan-logo.png\" width=\"150\" style=\"visibility: hidden; display:none;\"/></td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                </table>\r\n" + //
                "                              </th>\r\n" + //
                "                            </tr>\r\n" + //
                "                          </table>\r\n" + //
                "                        </td>\r\n" + //
                "                      </tr>\r\n" + //
                "                    </table>\r\n" + //
                "                    <!-- END Header -->\r\n" + //
                "      \r\n" + //
                "                    <!-- Article / Full Width Image + Title + Copy + Button -->\r\n" + //
                "                    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n" + //
                "                      <tr>\r\n" + //
                "                        <td style=\"padding-bottom: 10px;\">\r\n" + //
                "                          <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#ffffff\">\r\n"
                + //
                "                            <tr>\r\n" + //
                "                              <td class=\"p30-15\" style=\"padding: 30px 30px;\">\r\n" + //
                "                                <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:20px; line-height:32px; text-align:left; padding-bottom:20px;\">Hello "
                + user_name + ",</td>\r\n" + //
                "                                  </tr>\r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:14px; line-height:26px; text-align:left; padding-bottom:10px;\">You have successfully registered to the example SSM platform.</td>\r\n"
                + //
                "                                    \r\n" + //
                "                                  </tr>\r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:14px; line-height:26px; text-align:left; padding-bottom:20px;\">Please click the button below to verify your email and have access to the Smart Solar Media System Platform.</td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                  <!-- Button -->\r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td align=\"left\" style=\"padding-bottom: 20px;\">\r\n" + //
                "                                      <table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n" + //
                "                                        <tr>\r\n" + //
                "                                          <td class=\"pb-20\" style=\"background:#ffcc09; color:#000000; font-family:Arial,sans-serif; font-size:14px; line-height:18px; padding:12px 30px; text-align:center; border-radius:0px 22px 22px 22px; font-weight:bold;\"><a href=\""
                + token
                + "\" target=\"_blank\" class=\"link-white\" style=\"color:#000000; text-decoration:none;\"><span class=\"link-white\" style=\"color:#000000; text-decoration:none;\">Verify Email</span></a></td>\r\n"
                + //
                "                                        </tr>\r\n" + //
                "                                      </table>\r\n" + //
                "                                    </td>\r\n" + //
                "                                  </tr>\r\n" + //
                "                                  \r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:20px; line-height:32px; text-align:left; padding-bottom:10px;\">Best Regards,</td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                  <tr>\r\n" + //
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:20px; line-height:32px; text-align:left; padding-bottom:10px;\">The example Team</td>\r\n"
                + //
                "                                  </tr>\r\n" + //
                "                                  <!-- END Button -->\r\n" + //
                "                                </table>\r\n" + //
                "                              </td>\r\n" + //
                "                            </tr>\r\n" + //
                "                          </table>\r\n" + //
                "                        </td>\r\n" + //
                "                      </tr>\r\n" + //
                "                    </table>\r\n" + //
                "                    <!-- END Article / Full Width Image + Title + Copy + Button -->\r\n" + //
                "      \r\n" + //
                "                    <!-- Footer -->\r\n" + //
                "                    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n" + //
                "                      <tr>\r\n" + //
                "                        <td class=\"p30-15 bbrr\" style=\"padding: 30px 10px; border-radius:0px 0px 26px 26px;\">\r\n"
                + //
                "                          <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\r\n"
                + //
                "                            <tr>\r\n" + //
                "                              <td class=\"pb10\" style=\"color:#ffffff; font-family:Arial,sans-serif; font-size:16px; line-height:20px; text-align:right; padding-bottom:10px;\">Copyright©"
                + currentYear + " example. All rights reserved</td>\r\n"
                + //
                "                              <td class=\"text-header\" style=\"color:#475c77; font-family:Arial,sans-serif; font-size:12px; line-height:16px; text-align:right;\"><img src=\"https://example.com/assets/images/emails/powered.png\" width=\"200\"/></td>\r\n"
                + //
                "                            </tr>\r\n" + //
                "                          </table>\r\n" + //
                "                        </td>\r\n" + //
                "                      </tr>\r\n" + //
                "                    </table>\r\n" + //
                "                    <!-- END Footer -->\r\n" + //
                "                  </td>\r\n" + //
                "                </tr>\r\n" + //
                "              </table>\r\n" + //
                "            </td>\r\n" + //
                "          </tr>\r\n" + //
                "        </table>\r\n" + //
                "      </body>\r\n" + //
                "      </html>";
        return body;
    }


}

// ctx.request().bodyHandler(buffer -> {
// JsonObject jsonObject = buffer.toJsonObject();
// String name = jsonObject.getString("name");
// String id = jsonObject.getString("id");

// if (id == null || id.isEmpty()) {
// JsonObject errorResponse = new JsonObject()
// .put("error", "ID parameter is required and cannot be empty.");
// ctx.response()
// .setStatusCode(401)
// .end(errorResponse.encode());

// }
// System.out.println("Received request body id: " + id);
// String token = this.jwtMiddleware.generateToken(ctx, id);
// // System.out.print(token);
// ctx.response()
// .putHeader("Authorization", "Bearer " + token)
// .end("{\"status\": \"success\"}");
// });