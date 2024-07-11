package com.example.dbm.service;

import java.util.ArrayList;
import java.util.List;

import com.example.dbm.service_container.ServiceContainer;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class DataValidator {
    public DataValidator(ServiceContainer serviceContainer) {

    }

    public ValidationResult validateData(RoutingContext ctx, JsonObject jsonObject, String... fields) {
        List<String> errors = new ArrayList<>();

        for (String field : fields) {
            if (!jsonObject.containsKey(field)) {
                errors.add(field + " is required");
            }
        }

        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors);
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> errors;

        public ValidationResult(boolean isValid, List<String> errors) {
            this.isValid = isValid;
            this.errors = errors;
        }

        public boolean isValid() {
            return isValid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}