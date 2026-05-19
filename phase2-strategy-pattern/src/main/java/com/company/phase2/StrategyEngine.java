package com.company.phase2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StrategyEngine {

    private final List<PaymentConfig> configs;

    public StrategyEngine() {
        this.configs = loadConfig();
    }

    private List<PaymentConfig> loadConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/payment-config.json");
            return mapper.readValue(is, new TypeReference<List<PaymentConfig>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load payment config", e);
        }
    }

    public boolean processPayment(PaymentRequest request) {
        // 1. Find the configuration for this payment type
        Optional<PaymentConfig> configOpt = configs.stream()
                .filter(c -> c.getPaymentType().equals(request.getPaymentType()))
                .findFirst();

        if (configOpt.isEmpty()) {
            System.err.println("No configuration found for payment type: " + request.getPaymentType());
            return false;
        }

        PaymentConfig config = configOpt.get();
        System.out.println("Processing " + request.getPaymentType() + " payment. Resolving Strategies...");

        // 2. Resolve Strategy List based on Strings from Config
        List<PaymentValidator> activeValidators = new ArrayList<>();
        for (String validationName : config.getRequiredValidations()) {
            try {
                // Instantiates/Fetches the actual Strategy Object. Compile-time safe from this point forward.
                activeValidators.add(ValidatorFactory.getValidator(validationName));
            } catch (IllegalArgumentException e) {
                System.err.println("CRITICAL ERROR: " + e.getMessage());
                return false;
            }
        }

        // 3. Execution Phase (No Reflection)
        for (PaymentValidator validator : activeValidators) {
            if (!validator.validate(request)) {
                System.err.println("Validation execution failed. Aborting payment.");
                return false;
            }
        }

        System.out.println("All Strategy validations passed successfully!");
        return true;
    }
}
