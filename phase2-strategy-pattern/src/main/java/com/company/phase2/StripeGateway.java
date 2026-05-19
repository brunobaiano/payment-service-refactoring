package com.company.phase2;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;

public class StripeGateway {

    public StripeGateway() {
        String apiKey = System.getenv("STRIPE_SECRET_KEY");
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("sk_test_...")) {
            Stripe.apiKey = apiKey;
        } else {
            System.err.println("\n[WARNING] STRIPE_SECRET_KEY environment variable is missing or invalid. Using existing Stripe.apiKey context.");
        }
    }

    public boolean createCharge(PaymentRequest request) {
        System.out.println("Initiating Stripe Gateway call for Amount: " + request.getAmount() + " " + request.getCurrency());
        try {
            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount((long) (request.getAmount() * 100))
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
}
