package com.company.phase2;

public class CardFormatValidator implements PaymentValidator {
    @Override
    public boolean validate(PaymentRequest request) {
        System.out.println("Executing CardFormatValidator via Strategy Pattern...");
        if (request.getTokenOrCardNumber() == null || request.getTokenOrCardNumber().isEmpty()) {
            System.err.println("Validation Failed: Card number is empty.");
            return false;
        }
        return true;
    }
}
