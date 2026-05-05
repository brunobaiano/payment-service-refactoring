package com.company.phase1;

/**
 * Validation Methods using reflection-based dispatching.
 * 
 * This class demonstrates how validators would be called reflectively in a legacy system.
 * The LegacyEngine uses Method.invoke() to call these methods dynamically based on 
 * string configuration from payment-config.json.
 * 
 * Contrast with Phase 3:
 * - Phase 1: String-based reflection dispatch → Method.invoke() → runtime errors if method doesn't exist
 * - Phase 3: Method references in a typed Map → compile-time safety, no reflection overhead
 * 
 * New design supports both CreditCard and Pix types using reflection-based type checking.
 */
public class ValidationMethods {

    // Original legacy methods (kept for backwards compatibility with PaymentRequest)
    // These methods return boolean to indicate success (true) or failure (false)

    public boolean validateCardFormat(PaymentRequest request) {
        System.out.println("Executing validateCardFormat via Reflection...");
        if (request.getTokenOrCardNumber() == null || request.getTokenOrCardNumber().isEmpty()) {
            System.err.println("Validation Failed: Card number is empty.");
            return false;
        }
        return true;
    }

    public boolean checkFraudRisk(PaymentRequest request) {
        System.out.println("Executing checkFraudRisk via Reflection...");
        // Simulate a fraud check based on extreme amounts
        if (request.getAmount() > 10000) {
            System.err.println("Validation Failed: Amount flagged for high fraud risk.");
            return false;
        }
        return true;
    }

    public boolean validateLimits(PaymentRequest request) {
        System.out.println("Executing validateLimits via Reflection...");
        // Simulation: anything under negative is invalid. (Just an example)
        if (request.getAmount() <= 0) {
            System.err.println("Validation Failed: Amount must be greater than zero.");
            return false;
        }
        return true;
    }

    public boolean validatePixKey(PaymentRequest request) {
        System.out.println("Executing validatePixKey via Reflection...");
        // PIX keys must not be null
        if (request.getTokenOrCardNumber() == null) {
            System.err.println("Validation Failed: Pix key missing.");
            return false;
        }
        return true;
    }

    // ===== NEW METHODS FOR SEPARATE DOMAIN CLASSES (CreditCard, Pix) =====

    /**
     * Validate card format for CreditCard type.
     * Demonstrates reflection-based dispatch: called via Method.invoke() in LegacyEngine.
     */
    public boolean validateCardFormat(CreditCard creditCard) {
        System.out.println("Executing validateCardFormat via Reflection (CreditCard)...");
        if (creditCard.getTokenOrCardNumber() == null || creditCard.getTokenOrCardNumber().isEmpty()) {
            System.err.println("Validation Failed: Card number is empty.");
            return false;
        }
        System.out.println("✓ Card token valid: " + creditCard.getTokenOrCardNumber().substring(0, Math.min(6, creditCard.getTokenOrCardNumber().length())));
        return true;
    }

    /**
     * Check fraud risk for CreditCard type.
     * Demonstrates reflection-based conditional: amount > 10000 flag.
     */
    public boolean checkFraudRisk(CreditCard creditCard) {
        System.out.println("Executing checkFraudRisk via Reflection (CreditCard)...");
        if (creditCard.getAmount() > 10000) {
            System.err.println("Validation Failed: Amount flagged for high fraud risk (> 10000).");
            return false;
        }
        System.out.println("✓ Fraud risk check passed");
        return true;
    }

    /**
     * Validate limits for CreditCard type.
     */
    public boolean validateLimits(CreditCard creditCard) {
        System.out.println("Executing validateLimits via Reflection (CreditCard)...");
        if (creditCard.getAmount() > 0) {
            System.out.println("✓ Credit card amount valid: " + creditCard.getAmount());
            return true;
        }
        System.err.println("Validation Failed: Amount must be greater than zero.");
        return false;
    }

    /**
     * PIX key validation (returns false for CreditCard—used when validation runs against wrong type).
     */
    public boolean validatePixKey(CreditCard creditCard) {
        System.out.println("Executing validatePixKey via Reflection (CreditCard)...");
        return false; // CreditCard doesn't have PIX key
    }

    /**
     * Validate PIX key for Pix type.
     */
    public boolean validatePixKey(Pix pix) {
        System.out.println("Executing validatePixKey via Reflection (Pix)...");
        if (pix.getPixKey() == null || pix.getPixKey().isEmpty()) {
            System.err.println("Validation Failed: PIX key is empty.");
            return false;
        }
        System.out.println("✓ PIX key valid");
        return true;
    }

    /**
     * Check fraud risk for Pix type.
     * Demonstrates reflection-based conditional: amount > 50000 flag.
     */
    public boolean checkFraudRisk(Pix pix) {
        System.out.println("Executing checkFraudRisk via Reflection (Pix)...");
        if (pix.getAmount() > 50000) {
            System.err.println("Validation Failed: PIX amount flagged for high fraud risk (> 50000).");
            return false;
        }
        System.out.println("✓ Fraud risk check passed");
        return true;
    }

    /**
     * Validate limits for Pix type.
     */
    public boolean validateLimits(Pix pix) {
        System.out.println("Executing validateLimits via Reflection (Pix)...");
        if (pix.getAmount() > 0) {
            System.out.println("✓ PIX amount valid: " + pix.getAmount());
            return true;
        }
        System.err.println("Validation Failed: Amount must be greater than zero.");
        return false;
    }

    /**
     * Card format validation (returns false for Pix—used when validation runs against wrong type).
     */
    public boolean validateCardFormat(Pix pix) {
        System.out.println("Executing validateCardFormat via Reflection (Pix)...");
        return false; // PIX doesn't have card format
    }

    /**
     * Utility method: Dispatch validation based on object type using reflection.
     * Shows the manual type checking needed before Java had pattern matching.
     */
    public boolean dispatchValidation(String methodName, Object payment) {
        String paymentType = payment.getClass().getSimpleName();
        
        try {
            // Dynamically find and invoke the right method based on actual object type
            Class<?> paramType = payment.getClass();
            java.lang.reflect.Method method = ValidationMethods.class.getMethod(methodName, paramType);
            Object result = method.invoke(this, payment);
            return (boolean) result;
        } catch (NoSuchMethodException e) {
            System.err.println("CRITICAL ERROR: No validation method '" + methodName + "' for type: " + paymentType);
            return false;
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to execute validation '" + methodName + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
