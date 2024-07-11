package com.example.dbm.controller;

import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CountryController extends Controller {
    public CountryController(ServiceContainer serviceContainer) {
        super(serviceContainer);
    }

    public void getCountryList(RoutingContext ctx) {
        try {
            JsonArray jsonData = readJsonFromFile("src/main/java/com/example/dbm/pubic/assets/data/country.json");
            JsonObject result = new JsonObject();
            for (int i = 0; i < jsonData.size(); i++) {
                JsonObject country = jsonData.getJsonObject(i);
                String countryName = country.getString("countryName");
                new JsonObject().put("country", countryName);
        
                result.put("country" + (i + 1), countryName);
            }
            handleResponse(ctx, result);
        } catch (IOException e) {
            JsonObject error = new JsonObject();
            error.put("error", e.getMessage());
            handleResponse(ctx, error);
            // Handle exception here
        }
    }

    private JsonArray readJsonFromFile(String filePath) throws IOException {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(filePath));
            String jsonString = new String(jsonData, StandardCharsets.UTF_8);
            return new JsonArray(jsonString);
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
            throw e; // Re-throw the exception to be handled in getCountryList
        } catch (Exception ex) {
            System.err.println("Error creating JsonObject: " + ex.getMessage());
            throw new IOException("Error creating JsonObject", ex);
        }
    }

}
