package com.company.phase1;

import java.lang.reflect.Method;

/**
 * Payment Router using Reflection to determine payment routing tier.
 * 
 * This class demonstrates how payment routing would be implemented in a legacy system
 * using reflection-based object introspection and explicit if-else logic.
 * 
 * Contrast with Phase 3: PaymentRouter uses Java 17+ Record Pattern Matching with guards
 * to achieve the same routing logic in a single elegant switch expression.
 * 
 * Phase 1 Approach:
 * - Manual reflection to get object type and extract field values
 * - Explicit if-else chains for routing logic
 * - No compile-time exhaustiveness checks
 * - Verbose boilerplate for type checking and field extraction
 * 
 * Phase 3 Approach:
 * - Record patterns with automatic field destructuring
 * - Single switch with guards for conditional routing
 * - Compile-time exhaustiveness verification
 * - Concise, readable code
 */
public class PaymentRouter {

    /**
     * Route payment to appropriate gateway/processor using reflection-based introspection.
     * Demonstrates how you would determine payment type and extract properties via reflection
     * before Java's pattern matching was available.
     */
    public static String routePayment(Object payment) {
        // Phase 1: Manual reflection to determine type
        String paymentType = payment.getClass().getSimpleName();
        
        try {
            if (paymentType.equals("CreditCard")) {
                return routeCreditCard((CreditCard) payment);
            } else if (paymentType.equals("Pix")) {
                return routePix((Pix) payment);
            }
        } catch (Exception e) {
            System.err.println("Error routing payment via reflection: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "UNKNOWN_GATEWAY";
    }

    /**
     * Route CreditCard using reflection to extract properties and if-else logic.
     * Demonstrates verbose pre-pattern-matching approach.
     */
    private static String routeCreditCard(CreditCard cc) {
        // Reflection alternative: Could use reflectGetDouble(cc, "amount") instead
        double amount = cc.getAmount();
        String currency = cc.getCurrency();
        
        // Explicit if-else chains for routing logic
        if (amount > 50000) {
            if (!currency.equals("USD")) {
                return "STRIPE_PREMIUM_INTERNATIONAL";
            } else {
                return "STRIPE_PREMIUM_USD";
            }
        } else if (amount > 1000) {
            if (!currency.equals("USD")) {
                return "STRIPE_INTERNATIONAL";
            } else {
                return "STRIPE_STANDARD";
            }
        } else {
            return "STRIPE_EXPRESS";
        }
    }

    /**
     * Route Pix using reflection to extract properties and if-else logic.
     * Demonstrates verbose pre-pattern-matching approach.
     */
    private static String routePix(Pix pix) {
        // Reflection alternative: Could use reflectGetDouble(pix, "amount") instead
        double amount = pix.getAmount();
        
        if (amount > 100000) {
            return "PIX_PREMIUM_GATEWAY";
        } else if (amount > 5000) {
            return "PIX_STANDARD_GATEWAY";
        } else {
            return "PIX_EXPRESS_GATEWAY";
        }
    }

    /**
     * Determine if payment requires fraud checks using reflection-based introspection.
     * Shows manual type checking and field extraction vs Phase 3's elegant guards.
     */
    public static boolean requiresFraudCheck(Object payment) {
        String paymentType = payment.getClass().getSimpleName();
        
        try {
            if (paymentType.equals("CreditCard")) {
                CreditCard cc = (CreditCard) payment;
                // Manual field extraction
                double amount = cc.getAmount();
                // Explicit conditional logic
                return amount > 10000;
            } else if (paymentType.equals("Pix")) {
                Pix pix = (Pix) payment;
                double amount = pix.getAmount();
                String pixKey = pix.getPixKey();
                // Manual field extraction and complex condition
                return amount > 50000 && (pixKey.length() < 5 || pixKey.length() > 100);
            }
        } catch (Exception e) {
            System.err.println("Error checking fraud via reflection: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Determine if payment requires manual review using reflection.
     * Demonstrates manual type dispatch and field extraction.
     */
    public static boolean requiresManualReview(Object payment) {
        String paymentType = payment.getClass().getSimpleName();
        
        try {
            if (paymentType.equals("CreditCard")) {
                CreditCard cc = (CreditCard) payment;
                double amount = cc.getAmount();
                String token = cc.getTokenOrCardNumber();
                String currency = cc.getCurrency();
                
                // Manual conditions
                return amount > 100000 || currency.equals("XXX") || token.startsWith("test_");
            } else if (paymentType.equals("Pix")) {
                Pix pix = (Pix) payment;
                double amount = pix.getAmount();
                
                return amount > 500000;
            }
        } catch (Exception e) {
            System.err.println("Error checking manual review via reflection: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Reflection-based field value getter (alternative approach for truly legacy systems).
     * This demonstrates the absolute "reflection as a solution" approach—
     * extracting field values via Method.invoke() instead of direct getters.
     * 
     * Phase 3 eliminates this entirely with record patterns that auto-destructure fields.
     */
    @SuppressWarnings("unused")
    private static double reflectGetDouble(Object obj, String fieldName) {
        try {
            // Construct getter method name: "amount" -> "getAmount"
            String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = obj.getClass().getMethod(methodName);
            Object result = getter.invoke(obj);
            return ((Number) result).doubleValue();
        } catch (Exception e) {
            System.err.println("Failed to reflect field: " + fieldName);
            return 0.0;
        }
    }
}

