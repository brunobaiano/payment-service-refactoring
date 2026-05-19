package com.company.phase2;

public class FraudRiskValidator implements PaymentValidator {
    @Override
    public boolean validate(PaymentRequest request) {
        System.out.println("Executing FraudRiskValidator via Strategy Pattern...");
        if (request.getAmount() > 10000) {
            System.err.println("Validation Failed: Amount flagged for high fraud risk.");
            return false;
        }
        return true;
    }
}
