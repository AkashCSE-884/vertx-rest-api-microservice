package com.example.dbm.service;

import com.github.jknack.handlebars.internal.antlr.misc.MultiMap;
import com.ongres.scram.common.bouncycastle.pbkdf2.Arrays;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.mail.MailAttachment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import io.vertx.core.Vertx;

public class SendMailService {
    private final MailClient mailClient;
    private Vertx vertx;

    public SendMailService(Vertx vertx) {
        // Configure the Mail Client
        this.vertx = vertx;
        MailConfig mailConfig = new MailConfig()
                .setHostname("smtp.gmail.com")
                .setPort(587)
                .setStarttls(StartTLSOptions.REQUIRED)
                .setUsername("platform@example.com")
                .setPassword("yimzqmgjihabhxqx");

        // Create the Mail Client
        mailClient = MailClient.createShared(vertx, mailConfig);
    }

    public boolean sendMail(String to, String subject, String body) {
        Boolean isMailSent = false;
        try {
            MailMessage mailMessage = new MailMessage()
                    .setFrom("platform@example.com")
                    .setTo(to)
                    .setSubject(subject)
                    .setHtml(body);

            mailClient.sendMail(mailMessage, result -> {
                if (result.succeeded()) {
                    System.out.println("Email sent successfully!");
                } else {
                    System.err.println("Failed to send email: " + result.cause().getMessage());
                }
            });

            return true;
        } catch (Exception e) {
            System.err.println("Exception while sending email: " + e.getMessage());
            return false;
        }
    }

    public Future<Boolean> sendMailV2(String to, String subject, String body, List<String> filePaths) {
        Promise<Boolean> promise = Promise.promise();
        try {
            MailMessage mailMessage = new MailMessage()
                    .setFrom("platform@example.com")
                    .setTo(to)
                    .setSubject(subject)
                    .setHtml(body);

            List<MailAttachment> attachments = new ArrayList<>();

            if (!filePaths.isEmpty()) {
                for (int i = 0; i < filePaths.size(); i += 2) {
                    if (i + 1 < filePaths.size()) {
                        String fileContent = filePaths.get(i);
                        String filePath = filePaths.get(i + 1);

                        Buffer buffer = vertx.fileSystem().readFileBlocking(filePath);
                        // System.out.println("Read file: " + buffer.length() + " bytes length");

                        MailAttachment attachment = MailAttachment.create();
                        attachment.setContentType(fileContent);
                        attachment.setData(buffer);

                        String fileName = Paths.get(filePath).getFileName().toString();
                        attachment.setName(fileName);

                        attachments.add(attachment);

                        // System.out.println("Attachment added: " + fileName);
                    }
                }
            }
            // Set the list of attachments
            mailMessage.setAttachment(attachments);

            mailClient.sendMail(mailMessage, result -> {
                if (result.succeeded()) {
                    System.out.println("Email sent successfully!");
                    promise.complete(true);
                } else {
                    System.err.println("Failed to send email: " + result.cause().getMessage());
                    promise.complete(false);
                }
            });

        } catch (Exception e) {
            System.err.println("Exception while sending email: " + e.getMessage());
            promise.fail(e);
        }

        return promise.future();
    }

    // public boolean sendMail(String to, String subject, String body) {
    // try {
    // MailMessage mailMessage = new MailMessage()
    // .setFrom("washif.hossain@example.com")
    // .setTo(to)
    // .setSubject(subject)
    // .setHtml(body);

    // List<String> filePaths = new ArrayList<>();
    // filePaths.add("src/main/java/com/example/dbm/pubic/assets/img/verify_bg.png");
    // filePaths.add("src/main/java/com/example/dbm/pubic/assets/img/smb_tick.png");
    // filePaths.add("src/main/java/com/example/dbm/pubic/assets/img/plan-logo.png");
    // filePaths.add("src/main/java/com/example/dbm/pubic/assets/img/powered.png");

    // for (int i = 0; i < filePaths.size(); i++) {
    // String filePath = filePaths.get(i);
    // String cid = "image-" + i;

    // this.vertx.fileSystem().readFile(filePath, result -> {
    // if (result.succeeded()) {
    // Buffer buffer = result.result();
    // System.out.println("Read file: " + buffer.length() + " bytes length");

    // MailAttachment attachment = MailAttachment.create();
    // attachment.setContentType("image/jpeg"); // set the correct MIME type here
    // attachment.setData(buffer);
    // attachment.setDisposition("inline");
    // attachment.setContentId("<" + cid + ">");
    // // Add the attachment to the mail message
    // mailMessage.setInlineAttachment(attachment);
    // } else {
    // System.out.println("Failed to read file: " + result.cause().getMessage());
    // }
    // });
    // }
    // vertx.setTimer(1000, id -> {
    // mailClient.sendMail(mailMessage, result -> {
    // if (result.succeeded()) {
    // System.out.println("Email sent successfully!");
    // } else {
    // System.err.println("Failed to send email: " + result.cause().getMessage());
    // }
    // });
    // });
    // return true;
    // } catch (Exception e) {
    // System.err.println("Exception while sending email: " + e.getMessage());
    // return false;
    // }
    // }

    // public void sendMail(RoutingContext ctx) {
    // ctx.request().bodyHandler(buffer -> {
    // JsonObject jsonObject = buffer.toJsonObject();
    // String to = jsonObject.getString("to");
    // String subject = jsonObject.getString("subject");
    // String body = jsonObject.getString("body");

    // boolean isMailSent = sendMail(to, subject, body);

    // if (isMailSent) {
    // ctx.response()
    // .putHeader("Content-Type", "application/json")
    // .end(new JsonObject().put("message", "Email sent successfully!").encode());
    // } else {
    // ctx.response()
    // .putHeader("Content-Type", "application/json")
    // .end(new JsonObject().put("message", "Failed to send email!").encode());
    // }
    // });
    //

    public String setHtml(String userName, String verifyToken) {
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
                "        <table width=\"90%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-image: url(cid:background-image-id);background-size: cover;background-repeat: no-repeat;margin: auto;\">\r\n"
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
                "                                    <td class=\"text-header\" style=\"color:#475c77; font-family:'Muli', Arial,sans-serif; font-size:12px; line-height:16px; text-align:right;\"><img src=\"https://example.com/assets/images/emails/plan-logo.png\" width=\"150\" style=\"visibility: hidden;\"/></td>\r\n"
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
                "                                    <td class=\"text pb20\" style=\"color:#000000; font-family:Arial,sans-serif; font-size:20px; line-height:32px; text-align:left; padding-bottom:20px;\">Hello '.$userName.',</td>\r\n"
                + //
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
                "                                          <td class=\"pb-20\" style=\"background:#ffcc09; color:#000000; font-family:Arial,sans-serif; font-size:14px; line-height:18px; padding:12px 30px; text-align:center; border-radius:0px 22px 22px 22px; font-weight:bold;\"><a href=\"https://example.com/backend/verify_email.php?token='.$verifyToken.'\" target=\"_blank\" class=\"link-white\" style=\"color:#000000; text-decoration:none;\"><span class=\"link-white\" style=\"color:#000000; text-decoration:none;\">Verify Email</span></a></td>\r\n"
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
                "                              <td class=\"pb10\" style=\"color:#ffffff; font-family:Arial,sans-serif; font-size:16px; line-height:20px; text-align:right; padding-bottom:10px;\">Copyright© '.date(\"Y\").' example. All rights reserved</td>\r\n"
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

    // public boolean sendMail(String to, String subject, String body) {
    // try {
    // MailMessage mailMessage = new MailMessage()
    // .setFrom("washif.hossain@example.com")
    // .setTo(to)
    // .setSubject(subject)
    // .setHtml(body);

    // List<String> filePaths = new ArrayList<>();
    // filePaths.add("src/main/java/com/example/dbm/pubic/assets/img/verify_bg.png");
    // filePaths.add("src/main/java/com/example/dbm/pubic/assets/img/smb_tick.png");
    // filePaths.add("src/main/java/com/example/dbm/pubic/assets/img/plan-logo.png");
    // filePaths.add("src/main/java/com/example/dbm/pubic/assets/img/powered.png");
    // // for (String filePath : filePaths) {
    // // try {
    // // byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

    // // MailAttachment attachment = MailAttachment.create()
    // // setContentType("image/jpeg"); // set the correct MIME type here
    // // setData(fileContent);
    // // setDisposition("inline");
    // // setContentId("<" + filePath + ">");

    // // // attachments.add(attachment);

    // // // Add attachment to the mail message
    // // mailMessage.addAttachment(attachment);
    // // } catch (Exception e) {
    // // e.printStackTrace();
    // // }
    // // }
    // // ;
    // // List<MailAttachment> attachments = new ArrayList<>();
    // // StringBuilder bodyBuilder = new StringBuilder(body);
    // StringBuilder bodyBuilder = new StringBuilder(body);

    // for (int i = 0; i < filePaths.size(); i++) {
    // String filePath = filePaths.get(i);
    // String cid = "image-" + i;

    // this.vertx.fileSystem().readFile(filePath, result -> {
    // if (result.succeeded()) {
    // Buffer buffer = result.result();
    // System.out.println("Read file: " + buffer.length() + " bytes length");

    // MailAttachment attachment = MailAttachment.create();
    // attachment.setContentType("image/jpeg"); // set the correct MIME type here
    // attachment.setData(buffer);
    // attachment.setDisposition("inline");
    // attachment.setContentId("<" + cid + ">");
    // // Add the attachment to the mail message
    // mailMessage.setInlineAttachment(attachment);
    // } else {
    // System.out.println("Failed to read file: " + result.cause().getMessage());
    // }
    // });
    // }

    // // Wait for all files to be read and then send the email
    // vertx.setTimer(1000, id -> {
    // mailClient.sendMail(mailMessage, result -> {
    // if (result.succeeded()) {
    // System.out.println("Email sent successfully!");
    // } else {
    // System.err.println("Failed to send email: " + result.cause().getMessage());
    // }
    // });
    // });

    // //
    // this.vertx.fileSystem().readFile("src/main/java/com/example/dbm/pubic/assets/img/verify_bg.png",
    // // result -> {
    // // if (result.succeeded()) {
    // // Buffer buffer = result.result();
    // // System.out.println("Read file: " + buffer.length() + " bytes length");
    // // MailAttachment attachment = MailAttachment.create();
    // // attachment.setContentType("image/jpeg");
    // // attachment.setData(buffer);
    // // attachment.setDisposition("inline");
    // // attachment.setContentId("<background-image-id>");

    // // mailMessage.setInlineAttachment(attachment);

    // // } else {
    // // System.out.println("Failed to read file: " + result.cause().getMessage());
    // // }
    // // });

    // // mailClient.sendMail(mailMessage, result -> {
    // // if (result.succeeded()) {
    // // System.out.println("Email sent successfully!");
    // // } else {
    // // System.err.println("Failed to send email: " +
    // result.cause().getMessage());
    // // }
    // // });
    // // vertx.setTimer(1000, id -> {
    // // mailMessage.setInlineAttachments(attachments);
    // // mailMessage.setHtml(bodyBuilder.toString());

    // // mailClient.sendMail(mailMessage, result -> {
    // // if (result.succeeded()) {
    // // System.out.println("Email sent successfully!");
    // // } else {
    // // System.err.println("Failed to send email: " +
    // result.cause().getMessage());
    // // }
    // // });
    // // });
    // return true;
    // } catch (Exception e) {
    // System.err.println("Exception while sending email: " + e.getMessage());
    // return false;
    // }
    // }
    class FileAttachment {
        private final Buffer content;
        private final String contentType;
        private final String fileName;
        private final String disposition;
        private final String contentId;

        public FileAttachment(String content, String contentType, String fileName, String disposition,
                String contentId) {
            this.content = Buffer.buffer(content);
            this.contentType = contentType;
            this.fileName = fileName;
            this.disposition = disposition;
            this.contentId = contentId;
        }

        public FileAttachment(String content, String contentType, String fileName, String disposition) {
            this(content, contentType, fileName, disposition, null);
        }

        public Buffer getContent() {
            return content;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFileName() {
            return fileName;
        }

        public String getDisposition() {
            return disposition;
        }

        public String getContentId() {
            return contentId;
        }
    }
}
