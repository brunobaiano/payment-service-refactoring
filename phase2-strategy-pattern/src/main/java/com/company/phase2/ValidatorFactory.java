package com.company.phase2;

import java.util.HashMap;
import java.util.Map;

public class ValidatorFactory {
    private static final Map<String, PaymentValidator> registry = new HashMap<>();

    static {
        // Here we map the string names from the JSON config to actual compiled classes.
        // This is safe at compile time, but we still maintain string-to-class mappings.
        registry.put("validateCardFormat", new CardFormatValidator());
        registry.put("checkFraudRisk", new FraudRiskValidator());
        registry.put("validateLimits", new LimitsValidator());
        registry.put("validatePixKey", new PixKeyValidator());
    }

    public static PaymentValidator getValidator(String name) {
        PaymentValidator validator = registry.get(name);
        if (validator == null) {
            throw new IllegalArgumentException("Unknown validation mechanism configured: " + name);
        }
        return validator;
    }
}
