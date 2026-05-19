package com.company.phase2;

public class LimitsValidator implements PaymentValidator {
    @Override
    public boolean validate(PaymentRequest request) {
        System.out.println("Executing LimitsValidator via Strategy Pattern...");
        if (request.getAmount() <= 0) {
            System.err.println("Validation Failed: Amount must be greater than zero.");
            return false;
        }
        return true;
    }
}
