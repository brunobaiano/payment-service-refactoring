package com.company.phase1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Legacy Payment Engine using Reflection-based processing.
 * 
 * This engine demonstrates how a legacy system would handle payment processing
 * with reflection-based method invocation and manual type dispatching.
 * 
 * Contrast with Phase 3 (ModernEngine):
 * - Phase 1: Method.invoke() for dynamic method calls, manual type checking
 * - Phase 3: Method references in Map for compile-time type safety
 * 
 * The engine now supports both:
 * 1. Legacy PaymentRequest (single generic type with paymentType discriminator)
 * 2. Separate CreditCard and Pix domain classes (improved design pattern)
 * 
 * This demonstrates how you'd refactor legacy code incrementally while maintaining
 * backwards compatibility, before adopting modern Java features.
 */
public class LegacyEngine {

    private final List<PaymentConfig> configs;
    private final ValidationMethods validationMethods;

    public LegacyEngine() {
        this.validationMethods = new ValidationMethods();
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

    /**
     * Original Legacy Method: Process PaymentRequest using reflection.
     * Kept for backwards compatibility.
     */
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
        System.out.println("Processing " + request.getPaymentType() + " payment. Running configured validations...");

        // 2. The Legacy Part: Iterate over strings and invoke methods via Reflection
        for (String methodName : config.getRequiredValidations()) {
            try {
                // We have to use Reflection to find a method by its exact String name
                Method method = ValidationMethods.class.getDeclaredMethod(methodName, PaymentRequest.class);
                
                // We invoke it and cast the result
                boolean isValid = (boolean) method.invoke(validationMethods, request);
                
                if (!isValid) {
                    System.err.println("Validation execution '" + methodName + "' resulted in failure. Aborting payment.");
                    return false;
                }

            } catch (NoSuchMethodException e) {
                // If someone mistyped the name in the JSON, the system crashes here!
                System.err.println("CRITICAL ERROR: Configuration references a non-existent method: " + methodName);
                return false;
            } catch (InvocationTargetException | IllegalAccessException e) {
                // Standard Reflection boilerplate exception handling. 
                System.err.println("CRITICAL ERROR: Failed to execute method reflectively: " + methodName);
                e.printStackTrace();
                return false;
            }
        }

        System.out.println("All reflective validations passed successfully!");
        return true;
    }

    /**
     * New Method: Process CreditCard using reflection-based dispatch and routing.
     * 
     * This demonstrates:
     * 1. Reflection-based type dispatching (new domain model support)
     * 2. Integration with PaymentRouter for tiered processing
     * 3. Fraud check logic via reflection
     * 4. Manual review detection
     */
    public boolean processPayment(CreditCard creditCard) {
        System.out.println("\n=== Processing CreditCard Payment via Reflection ===");
        System.out.println("Payment: " + creditCard);

        // 1. Validate using reflection-based dispatch
        if (!runValidations(creditCard)) {
            System.err.println("CreditCard validation failed.");
            return false;
        }

        // 2. Route payment using reflection-based routing
        String route = PaymentRouter.routePayment(creditCard);
        System.out.println("Routing: " + route);

        // 3. Check if fraud screening is required (using reflection introspection)
        if (PaymentRouter.requiresFraudCheck(creditCard)) {
            System.out.println("⚠️ Fraud check required for this transaction.");
        }

        // 4. Check if manual review is needed
        if (PaymentRouter.requiresManualReview(creditCard)) {
            System.out.println("⚠️ Manual review required for this transaction.");
            return false; // In a real system, this might queue for manual review
        }

        System.out.println("✓ CreditCard validation and routing passed!");
        return true;
    }

    /**
     * New Method: Process Pix using reflection-based dispatch and routing.
     * 
     * Similar to processPayment(CreditCard), but with PIX-specific logic.
     */
    public boolean processPayment(Pix pix) {
        System.out.println("\n=== Processing Pix Payment via Reflection ===");
        System.out.println("Payment: " + pix);

        // 1. Validate using reflection-based dispatch
        if (!runValidations(pix)) {
            System.err.println("Pix validation failed.");
            return false;
        }

        // 2. Route payment using reflection-based routing
        String route = PaymentRouter.routePayment(pix);
        System.out.println("Routing: " + route);

        // 3. Check if fraud screening is required
        if (PaymentRouter.requiresFraudCheck(pix)) {
            System.out.println("⚠️ Fraud check required for this transaction.");
        }

        // 4. Check if manual review is needed
        if (PaymentRouter.requiresManualReview(pix)) {
            System.out.println("⚠️ Manual review required for this transaction.");
            return false;
        }

        System.out.println("✓ Pix validation and routing passed!");
        return true;
    }

    /**
     * Reflection-based validation dispatcher for new domain classes.
     * 
     * This demonstrates the power of Java reflection combined with method overloading.
     * We use reflection to dynamically find the right validator method based on the
     * actual payment object's type.
     * 
     * Phase 3 eliminates this with pattern matching and method references.
     */
    private boolean runValidations(Object payment) {
        String paymentType = payment.getClass().getSimpleName();
        System.out.println("Running validations for: " + paymentType);

        // Get validators from config based on payment type
        Optional<PaymentConfig> configOpt = configs.stream()
                .filter(c -> c.getPaymentType().equals(getConfigPaymentType(payment)))
                .findFirst();

        if (configOpt.isEmpty()) {
            System.err.println("No validation config found for: " + paymentType);
            return false;
        }

        PaymentConfig config = configOpt.get();

        // Run each validator using reflection-based method dispatch
        for (String methodName : config.getRequiredValidations()) {
            try {
                // Reflection: Find the method matching the payment type
                Class<?> paramType = payment.getClass();
                Method method = ValidationMethods.class.getDeclaredMethod(methodName, paramType);

                // Invoke the method and check result
                boolean isValid = (boolean) method.invoke(validationMethods, payment);

                if (!isValid) {
                    System.err.println("Validation '" + methodName + "' failed for " + paymentType);
                    return false;
                }

            } catch (NoSuchMethodException e) {
                System.err.println("CRITICAL ERROR: No validator method '" + methodName + "' for type: " + paymentType);
                return false;
            } catch (InvocationTargetException | IllegalAccessException e) {
                System.err.println("CRITICAL ERROR: Failed to invoke '" + methodName + "': " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        System.out.println("✓ All validations passed for " + paymentType);
        return true;
    }

    /**
     * Map object types to configuration payment types.
     * Shows manual type checking that Pattern Matching eliminates in Phase 3.
     */
    private String getConfigPaymentType(Object payment) {
        if (payment instanceof CreditCard) {
            return "CREDIT_CARD";
        } else if (payment instanceof Pix) {
            return "PIX";
        }
        return "UNKNOWN";
    }
}
