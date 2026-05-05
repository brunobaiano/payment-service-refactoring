package com.company.phase1;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;

/**
 * Stripe Gateway supporting both legacy PaymentRequest and new domain classes.
 * 
 * Demonstrates backwards compatibility while gradually adopting improved domain models.
 * Uses method overloading (not type inheritance) to distinguish payment types—
 * a pattern that pre-dates sealed types and pattern matching.
 */
public class StripeGateway {

    public StripeGateway() {
        String apiKey = System.getenv("STRIPE_SECRET_KEY");
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("sk_test_...")) {
            Stripe.apiKey = apiKey;
        } else {
            System.err.println("\n[WARNING] STRIPE_SECRET_KEY environment variable is missing or invalid. Using existing Stripe.apiKey context.");
        }
    }

    /**
     * Legacy method: Create charge from PaymentRequest.
     * Kept for backwards compatibility.
     */
    public boolean createCharge(PaymentRequest request) {
        System.out.println("Initiating Stripe Gateway call for Amount: " + request.getAmount() + " " + request.getCurrency());
        try {
            // For testing stripe scenarios, Stripe allows passing special test tokens instead of card numbers.
            // Example: "tok_visa" (Success), "tok_chargeDeclined" (Decline), "tok_chargeDeclinedFraudulent" (Fraud)
            
            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount((long) (request.getAmount() * 100)) // Stripe uses cents
                    .setCurrency(request.getCurrency())
                    .setSource(request.getTokenOrCardNumber()) 
                    .setDescription("Charge for " + request.getPaymentType())
                    .build();

            Charge charge = Charge.create(params);
            
            System.out.println("Stripe Charge Successful! ID: " + charge.getId() + " Status: " + charge.getStatus());
            return true;

        } catch (StripeException e) {
            System.err.println("Stripe API failed: " + e.getUserMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error communicating with Stripe: " + e.getMessage());
            return false;
        }
    }

    /**
     * New method: Create charge from CreditCard domain class.
     * Demonstrates method overloading for different domain types (pre-pattern-matching style).
     */
    public boolean createCharge(CreditCard creditCard) {
        System.out.println("Initiating Stripe Gateway call for CreditCard Amount: " + creditCard.getAmount() + " " + creditCard.getCurrency());
        try {
            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount((long) (creditCard.getAmount() * 100)) // Stripe uses cents
                    .setCurrency(creditCard.getCurrency())
                    .setSource(creditCard.getTokenOrCardNumber())
                    .setDescription("Credit Card Charge")
                    .build();

            Charge charge = Charge.create(params);
            
            System.out.println("Stripe Charge Successful! ID: " + charge.getId() + " Status: " + charge.getStatus());
            return true;

        } catch (StripeException e) {
            System.err.println("Stripe API failed: " + e.getUserMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error communicating with Stripe: " + e.getMessage());
            return false;
        }
    }

    /**
     * New method: Create charge from Pix domain class.
     * Demonstrates method overloading for PIX payments (which use different parameters).
     */
    public boolean createCharge(Pix pix) {
        System.out.println("Initiating Stripe Gateway call for Pix Amount: " + pix.getAmount() + " " + pix.getCurrency());
        try {
            // PIX payments would typically use different API endpoints/parameters
            // This is simplified for demonstration
            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount((long) (pix.getAmount() * 100))
                    .setCurrency(pix.getCurrency())
                    .setSource(pix.getPixKey())
                    .setDescription("PIX Charge via " + pix.getPixKey())
                    .build();

            Charge charge = Charge.create(params);
            
            System.out.println("Stripe Charge Successful! ID: " + charge.getId() + " Status: " + charge.getStatus());
            return true;

        } catch (StripeException e) {
            System.err.println("Stripe API failed: " + e.getUserMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error communicating with Stripe: " + e.getMessage());
            return false;
        }
    }
}
